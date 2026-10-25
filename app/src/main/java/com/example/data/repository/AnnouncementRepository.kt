package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.util.BookContributionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AnnouncementsResponse(
    val version: Int = 1,
    val lastUpdated: Long = 0L,
    val banners: List<AnnouncementBanner> = emptyList()
)

data class AnnouncementBanner(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badge: String = "",
    val imageUrl: String = "",
    val bgColor: String = "#4a2511",
    val textColor: String = "#ffffff",
    val targetProf: String = "All",
    val priority: Int = 1,
    val active: Boolean = true,
    val action: BannerAction = BannerAction(),
    val updatedAt: Long = 0L
)

data class BannerAction(
    val type: String = "NONE", // OPEN_BOOK, FILTER_CATEGORY, FILTER_SUBJECT, OPEN_URL, OPEN_CONTRIBUTE, NONE
    val target: String = ""
)

class AnnouncementRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val cacheFile: File
        get() = File(appContext.cacheDir, CACHE_FILE_NAME)

    // OkHttpClient configured with Dns.SYSTEM (with fallback resilience via appDns if system DNS fails)
    private val systemHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(Dns.SYSTEM)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fallbackDnsHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(BookContributionManager.appDns)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _allBanners = MutableStateFlow<List<AnnouncementBanner>>(emptyList())
    val allBanners: StateFlow<List<AnnouncementBanner>> = _allBanners.asStateFlow()

    init {
        // Hydrate immediately from local cache if available so offline launch is instant
        val cachedJson = readCachedJson()
        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseAnnouncementsJson(cachedJson)
            if (parsed.banners.isNotEmpty()) {
                _allBanners.value = parsed.banners
            }
        }
    }

    /**
     * Fetches `announcements.json` from jsDelivr CDN (or GitHub Raw fallback) on IO dispatcher,
     * saves the raw JSON to `File(context.cacheDir, "announcements_cache.json")`,
     * and falls back to the cached file if offline or if the network call fails.
     */
    suspend fun fetchAnnouncements(): List<AnnouncementBanner> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val primaryUrl = "$PRIMARY_JSDELIVR_URL?t=$timestamp"
        val fallbackUrl = "$FALLBACK_GITHUB_URL?t=$timestamp"

        var rawJson = executeGet(primaryUrl)
        if (rawJson.isNullOrBlank()) {
            Log.w(TAG, "Primary jsDelivr announcements fetch failed, trying GitHub Raw fallback...")
            rawJson = executeGet(fallbackUrl)
        }

        if (!rawJson.isNullOrBlank()) {
            val parsed = parseAnnouncementsJson(rawJson)
            // Persist valid JSON to announcements_cache.json
            saveCachedJson(rawJson)
            _allBanners.value = parsed.banners
            return@withContext parsed.banners
        }

        // Offline or network failure: load from announcements_cache.json
        val cachedJson = readCachedJson()
        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseAnnouncementsJson(cachedJson)
            _allBanners.value = parsed.banners
            return@withContext parsed.banners
        }

        _allBanners.value
    }

    /**
     * Filters banners according to active state and student's selected Prof year:
     * - Only keeps banners where `active == true`
     * - Keeps banners where `targetProf` equals "All" (case-insensitive) OR matches `currentSelectedProf` (case-insensitive)
     * - Sorts remaining banners ascending by `priority`
     */
    fun filterBannersForProf(
        banners: List<AnnouncementBanner>,
        currentSelectedProf: String
    ): List<AnnouncementBanner> {
        val normalizedProf = currentSelectedProf.trim()
        return banners
            .asSequence()
            .filter { it.active }
            .filter { banner ->
                val target = banner.targetProf.trim()
                target.isEmpty() ||
                    target.equals("All", ignoreCase = true) ||
                    target.equals("All Prof", ignoreCase = true) ||
                    target.equals(normalizedProf, ignoreCase = true)
            }
            .sortedBy { it.priority }
            .toList()
    }

    private fun executeGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.Builder().noCache().noStore().build())
            .header("Cache-Control", "no-cache, no-store")
            .header("User-Agent", "Mulberry-Android-Applet")
            .build()

        // Try with Dns.SYSTEM first as specified
        try {
            systemHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) return body
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dns.SYSTEM fetch exception for $url: ${e.message}")
        }

        // Fallback to DoH-enabled client if system DNS failed in container/network
        return try {
            fallbackDnsHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "HTTP ${response.code} fetching $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback fetch exception for $url: ${e.message}")
            null
        }
    }

    private fun saveCachedJson(rawJson: String) {
        try {
            cacheFile.writeText(rawJson, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing announcements_cache.json: ${e.message}")
        }
    }

    private fun readCachedJson(): String? {
        return try {
            if (cacheFile.exists() && cacheFile.canRead()) {
                cacheFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading announcements_cache.json: ${e.message}")
            null
        }
    }

    /**
     * Resilient JSON parser using org.json so missing fields, null values, or markdown-wrapped URLs
     * never throw exceptions and always fall back to safe defaults.
     */
    internal fun parseAnnouncementsJson(rawJson: String): AnnouncementsResponse {
        return try {
            val root = JSONObject(rawJson.trim())
            val version = root.optInt("version", 1)
            val lastUpdated = root.optLong("lastUpdated", 0L)
            val bannersArray = root.optJSONArray("banners") ?: JSONArray()

            val parsedBanners = mutableListOf<AnnouncementBanner>()
            for (i in 0 until bannersArray.length()) {
                val obj = bannersArray.optJSONObject(i) ?: continue
                val actionObj = obj.optJSONObject("action")

                val rawImageUrl = obj.optString("imageUrl", "").trim()
                val cleanImageUrl = sanitizeUrl(rawImageUrl)

                val action = if (actionObj != null) {
                    BannerAction(
                        type = actionObj.optString("type", "NONE").trim().ifEmpty { "NONE" },
                        target = sanitizeUrl(actionObj.optString("target", "").trim())
                    )
                } else {
                    BannerAction()
                }

                val banner = AnnouncementBanner(
                    id = obj.optString("id", "banner_$i").trim(),
                    title = obj.optString("title", "").trim(),
                    subtitle = obj.optString("subtitle", "").trim(),
                    badge = obj.optString("badge", "").trim(),
                    imageUrl = cleanImageUrl,
                    bgColor = obj.optString("bgColor", "#4a2511").trim().ifEmpty { "#4a2511" },
                    textColor = obj.optString("textColor", "#ffffff").trim().ifEmpty { "#ffffff" },
                    targetProf = obj.optString("targetProf", "All").trim().ifEmpty { "All" },
                    priority = obj.optInt("priority", 1),
                    active = obj.optBoolean("active", true),
                    action = action,
                    updatedAt = obj.optLong("updatedAt", lastUpdated)
                )
                parsedBanners.add(banner)
            }

            AnnouncementsResponse(
                version = version,
                lastUpdated = lastUpdated,
                banners = parsedBanners
            )
        } catch (e: Exception) {
            Log.e(TAG, "Malformed announcements JSON: ${e.message}")
            AnnouncementsResponse()
        }
    }

    /**
     * Strips accidental markdown link syntax `[text](url)` if present.
     */
    private fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        val markdownRegex = Regex("""^\[.*?]\((.*?)\)$""")
        val match = markdownRegex.find(trimmed)
        return match?.groupValues?.getOrNull(1)?.trim() ?: trimmed
    }

    companion object {
        private const val TAG = "AnnouncementRepository"
        private const val CACHE_FILE_NAME = "announcements_cache.json"
        const val PRIMARY_JSDELIVR_URL =
            "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/announcements.json"
        const val FALLBACK_GITHUB_URL =
            "https://raw.githubusercontent.com/doubtsolved/mulberry-catalog/main/announcements.json"

        @Volatile
        private var INSTANCE: AnnouncementRepository? = null

        fun getInstance(context: Context): AnnouncementRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnnouncementRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
