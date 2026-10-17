package com.example.data.catalog

import android.content.Context
import android.util.Log
import com.example.data.dao.BookDao
import com.example.data.model.BookEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class CatalogSyncEngine(
    private val bookDao: BookDao
) {
    companion object {
        private const val TAG = "CatalogSyncEngine"
        private const val PRIMARY_JSDELIVR_URL = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/catalog.json"
        private const val FALLBACK_GITHUB_URL = "https://raw.githubusercontent.com/doubtsolved/mulberry-catalog/main/catalog.json"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun syncCatalog(): Result<CatalogResponse> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val jsdelivrUrl = "$PRIMARY_JSDELIVR_URL?t=$timestamp"
        val fallbackUrl = "$FALLBACK_GITHUB_URL?t=$timestamp"

        // Fetch catalog.json first from jsDelivr CDN, falling back to raw GitHub origin if needed
        var json = fetchUrl(jsdelivrUrl)
        if (json.isNullOrBlank()) {
            Log.w(TAG, "jsDelivr fetch failed or empty, attempting fallback to GitHub origin...")
            json = fetchUrl(fallbackUrl)
        }

        if (json.isNullOrBlank()) {
            // Seed sample books if database is completely empty so user has immediate high-yield catalog
            val existingBooks = bookDao.getAllBooksSync()
            if (existingBooks.isEmpty()) {
                seedInitialHighYieldBooks()
            }
            return@withContext Result.failure(IOException("Failed to fetch catalog from jsDelivr CDN and GitHub fallback"))
        }

        try {
            val catalog = gson.fromJson(json, CatalogResponse::class.java)
            if (catalog == null || catalog.books.isEmpty()) {
                return@withContext Result.failure(IOException("Empty catalog received"))
            }

            reconcileCatalogWithDatabase(catalog)
            Result.success(catalog)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse catalog JSON: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchUrl(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.Builder().noCache().noStore().build())
                .header("Cache-Control", "no-cache, no-store")
                .header("User-Agent", "Mulberry-Android-Applet")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "HTTP ${response.code} fetching $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception fetching $url: ${e.message}")
            null
        }
    }

    private suspend fun reconcileCatalogWithDatabase(catalog: CatalogResponse) {
        val existingBooks = bookDao.getAllBooksSync()
        val existingMap = existingBooks.associateBy { it.slug.ifEmpty { it.title } }
        val remoteSlugs = mutableListOf<String>()

        val toInsert = mutableListOf<BookEntity>()
        val toUpdate = mutableListOf<BookEntity>()

        for (remote in catalog.books) {
            if (remote.id.isBlank()) continue
            remoteSlugs.add(remote.id)

            val existing = existingMap[remote.id] ?: existingMap[remote.title]

            val githubMirror = remote.download.mirrors.firstOrNull {
                it.provider.equals("github", ignoreCase = true)
            }?.url ?: remote.download.mirrors.firstOrNull()?.url ?: ""

            val driveMirror = remote.download.mirrors.firstOrNull {
                it.provider.equals("drive", ignoreCase = true)
            }?.url ?: ""

            val telegramDeepLink = remote.download.telegram?.deepLink ?: ""

            if (existing != null) {
                // Preserve local progress, user bookmarks, and download status
                val updated = existing.copy(
                    slug = remote.id,
                    title = remote.title,
                    author = remote.author,
                    edition = remote.edition,
                    prof = remote.prof,
                    year = remote.year,
                    subject = remote.subject,
                    type = remote.type,
                    tagsCsv = remote.tags.joinToString(","),
                    pageCount = if (existing.pageCount > 1) existing.pageCount else remote.totalPages,
                    sizeMb = remote.sizeMb,
                    coverUrl = remote.coverUrl,
                    downloadUrl = githubMirror,
                    driveFallbackUrl = driveMirror,
                    telegramDeepLink = telegramDeepLink,
                    uploader = remote.uploader
                )
                toUpdate.add(updated)
            } else {
                // Insert brand new catalog item
                val newBook = BookEntity(
                    vaultId = 1,
                    slug = remote.id,
                    title = remote.title,
                    author = remote.author,
                    fileName = "${remote.id}.bin",
                    uriString = "",
                    pageCount = remote.totalPages,
                    fileSizeBytes = (remote.sizeMb * 1024 * 1024).toLong(),
                    edition = remote.edition,
                    prof = remote.prof,
                    year = remote.year,
                    subject = remote.subject,
                    type = remote.type,
                    tagsCsv = remote.tags.joinToString(","),
                    sizeMb = remote.sizeMb,
                    coverUrl = remote.coverUrl,
                    downloadUrl = githubMirror,
                    driveFallbackUrl = driveMirror,
                    telegramDeepLink = telegramDeepLink,
                    uploader = remote.uploader,
                    isDownloaded = false,
                    binFilePath = ""
                )
                toInsert.add(newBook)
            }
        }

        if (toInsert.isNotEmpty()) {
            bookDao.insertBooks(toInsert)
        }
        if (toUpdate.isNotEmpty()) {
            bookDao.updateBooks(toUpdate)
        }

        // Ghost Book Pruning: delete books not in remoteSlugs UNLESS isDownloaded == 1
        if (remoteSlugs.isNotEmpty()) {
            bookDao.pruneGhostBooks(remoteSlugs)
        }
    }

    private suspend fun seedInitialHighYieldBooks() {
        val initialBooks = listOf(
            // --- 1st Prof: Anatomy ---
            BookEntity(
                vaultId = 1,
                slug = "inderbir_singh_textbook_of_anatomy",
                title = "Inderbir Singh Textbook of Anatomy",
                author = "Inderbir Singh",
                edition = "Fifth Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,inderbir,singh,standard textbook,textbook",
                pageCount = 1207,
                sizeMb = 63.4,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/inderbir_singh_textbook_of_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/inderbir_singh_textbook_of_anatomy.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "ak_datta_volume_1_thorax_abdomen_and_pelvis",
                title = "AK Datta Vol 1: Thorax, Abdomen & Pelvis",
                author = "AK Dutta",
                edition = "9th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,abdomen,anatomy,and,datta,dutta,pelvis,standard textbook,thorax,volume",
                pageCount = 416,
                sizeMb = 325.1,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/ak_datta_volume_1_thorax_abdomen_and_pelvis.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/ak_datta_volume_1_thorax_abdomen_and_pelvis.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "ak_dutta_anatomy_volume_2_head_and_neck",
                title = "AK Dutta Anatomy Volume 2: Head and Neck",
                author = "AK Dutta",
                edition = "5th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,and,dutta,head,neck,standard textbook,volume",
                pageCount = 390,
                sizeMb = 280.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/ak_dutta_anatomy_volume_2_head_and_neck.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/ak_dutta_anatomy_volume_2_head_and_neck.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "volume_3_superior_and_inferior_extremities",
                title = "Volume 3: Superior and Inferior Extremities",
                author = "AK Dutta",
                edition = "4th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,and,dutta,extremities,gross anatomy,inferior,standard textbook,superior,volume",
                pageCount = 410,
                sizeMb = 260.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/volume_3_superior_and_inferior_extremities.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/volume_3_superior_and_inferior_extremities.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "vishram_singh_clinical_and_surgical_anatomy",
                title = "Vishram Singh Clinical and Surgical Anatomy",
                author = "Vishram Singh",
                edition = "3rd Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,and,clinical,singh,standard textbook,surgical,vishram",
                pageCount = 520,
                sizeMb = 75.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/vishram_singh_clinical_and_surgical_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/vishram_singh_clinical_and_surgical_anatomy.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "bd_chaurasias_human_anatomy_volume_1",
                title = "BD Chaurasia's Human Anatomy, Volume 1",
                author = "B.D. Chaurasia",
                edition = "8th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,chaurasia,human,standard textbook,upper limb and thorax,volume",
                pageCount = 450,
                sizeMb = 48.2,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/bd_chaurasia_human_anatomy_vol1.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "bd_chaurasias_human_anatomy_volume_2",
                title = "BD Chaurasia's Human Anatomy, Volume 2",
                author = "B.D. Chaurasia",
                edition = "8th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,abdomen and pelvis,anatomy,chaurasia,human,lower limb,standard textbook,volume",
                pageCount = 480,
                sizeMb = 51.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/bd_chaurasia_human_anatomy_vol1.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "bd_chaurasias_human_anatomy_volume_3_4",
                title = "BD Chaurasia's Human Anatomy, Volume 3 & 4",
                author = "B.D. Chaurasia",
                edition = "8th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,brain-neuroanatomy (volume 4),chaurasia,head & neck (volume 3),human,standard textbook,volume",
                pageCount = 620,
                sizeMb = 64.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/bd_chaurasia_human_anatomy_vol1.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "bdc_handbook_of_general_anatomy",
                title = "BDC Handbook of General Anatomy",
                author = "B.D. Chaurasia",
                edition = "6th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anatomy,bdc,chaurasia,general,general anatomy,handbook,standard textbook",
                pageCount = 310,
                sizeMb = 32.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/bd_chaurasia_human_anatomy_vol1.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "marrow_anatomy_notes",
                title = "Marrow Anatomy Notes",
                author = "Marrow",
                edition = "8th Ed",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Handwritten Notes",
                tagsCsv = "1st prof,1st year,anatomy,handwritten,handwritten notes,marrow,notes",
                pageCount = 591,
                sizeMb = 129.1,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/marrow_anatomy_notes.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/marrow_anatomy_notes.bin",
                isDownloaded = true
            ),
            BookEntity(
                vaultId = 1,
                slug = "grays_atlas_of_anatomy",
                title = "Gray’s Atlas of Anatomy",
                author = "Richard L. Drake, A. Wayne Vogl",
                edition = "3rd Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Atlas / Practical",
                tagsCsv = "1st prof,1st year,adam,anatomy,and,atlas,atlas / practical,drake,gray,mitchell,paul,richard,richardson,tibbitts,vogl,wayne",
                pageCount = 577,
                sizeMb = 61.4,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/grays_atlas_of_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/grays_atlas_of_anatomy.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "mcminns_clinical_atlas_of_human_anatomy",
                title = "McMinn's Clinical Atlas of Human Anatomy",
                author = "Peter H. Abrahams, Jonathan D. Spratt",
                edition = "7th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Atlas / Practical",
                tagsCsv = "1st prof,1st year,abrahams,albert,anatomy,and,atlas,atlas / practical,clinical,human,jonathan,loukas,marios,mcminn,peter,prof,schoo,spratt,van",
                pageCount = 418,
                sizeMb = 261.4,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/mcminns_clinical_atlas_of_human_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/mcminns_clinical_atlas_of_human_anatomy.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "netters_atlas_of_human_anatomy",
                title = "Netter's Atlas of Human Anatomy",
                author = "Frank H. Netter",
                edition = "7th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Anatomy",
                type = "Atlas / Practical",
                tagsCsv = "1st prof,1st year,anatomy,atlas,atlas / practical,frank,human,netter",
                pageCount = 640,
                sizeMb = 110.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/grays_atlas_of_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/netters_atlas_of_human_anatomy.bin",
                isDownloaded = false
            ),

            // --- 1st Prof: Physiology ---
            BookEntity(
                vaultId = 1,
                slug = "indu_khurana_medical_physiology",
                title = "Indu Khurana Medical Physiology",
                author = "Dr. Indu Khurana",
                edition = "General",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Physiology",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,indian author,indu,khurana,medical,physiology,standard textbook",
                pageCount = 1014,
                sizeMb = 66.9,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/indu_khurana_medical_physiology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/indu_khurana_medical_physiology.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "guyton_and_hall_physiology",
                title = "Guyton and Hall Textbook of Medical Physiology",
                author = "Arthur C. Guyton, John E. Hall",
                edition = "14th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Physiology",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,and,guyton,hall,john,medical,michael,physiology,standard textbook,textbook",
                pageCount = 1152,
                sizeMb = 59.6,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/guyton_and_hall_physiology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/guyton_and_hall_physiology.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "marrow_physiology_notes",
                title = "Marrow Physiology Notes",
                author = "Marrow",
                edition = "8th Ed",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Physiology",
                type = "Handwritten Notes",
                tagsCsv = "1st prof,1st year,marrow,notes,physiology,handwritten notes",
                pageCount = 420,
                sizeMb = 52.4,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/marrow_physiology_notes.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/marrow_physiology_notes.bin",
                isDownloaded = false
            ),

            // --- 1st Prof: Biochemistry ---
            BookEntity(
                vaultId = 1,
                slug = "lehninger_principles_of_biochemistry",
                title = "Lehninger Principles of Biochemistry",
                author = "David L. Nelson, Michael M. Cox",
                edition = "8th Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Biochemistry",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,biochemistry,cox,david,lehninger,michael,nelson,principles,standard textbook",
                pageCount = 1180,
                sizeMb = 92.5,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/lehninger_principles_of_biochemistry.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/lehninger_principles_of_biochemistry.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "harpers_illustrated_biochemistry",
                title = "Harper's Illustrated Biochemistry",
                author = "Victor W. Rodwell, David A. Bender",
                edition = "31st Edition",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Biochemistry",
                type = "Standard Textbook",
                tagsCsv = "1st prof,1st year,anthony,biochemistry,botham,gold standard,harper,illustrated,kathleen,kennelly,mcguinness,owen,peter,rodwell,standard textbook,victor,weil",
                pageCount = 832,
                sizeMb = 68.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/harpers_illustrated_biochemistry.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/harpers_illustrated_biochemistry.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "marrow_biochemistry_notes",
                title = "Marrow Biochemistry Notes",
                author = "Marrow",
                edition = "8th Ed",
                prof = "1st Prof",
                year = "1st Year",
                subject = "Biochemistry",
                type = "Handwritten Notes",
                tagsCsv = "1st prof,1st year,biochemistry,marrow,notes,handwritten notes",
                pageCount = 380,
                sizeMb = 46.2,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/marrow_biochemistry_notes.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/marrow_biochemistry_notes.bin",
                isDownloaded = false
            ),

            // --- 2nd Prof ---
            BookEntity(
                vaultId = 1,
                slug = "robbins_cotran_pathologic_basis_disease",
                title = "Robbins & Cotran Pathologic Basis of Disease",
                author = "Vinay Kumar, Abul K. Abbas",
                edition = "10th Edition",
                prof = "2nd Prof",
                year = "2nd Year",
                subject = "Pathology",
                type = "Standard Textbook",
                tagsCsv = "2nd prof,pathology,robbins,neoplasia,standard textbook",
                pageCount = 1392,
                sizeMb = 84.5,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/robbins_cotran_pathologic_basis_disease.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof2/releases/download/v1-books/robbins_cotran_pathologic_basis_disease.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "marrow_pathology_notes",
                title = "Marrow Pathology Notes",
                author = "Marrow",
                edition = "8th Ed",
                prof = "2nd Prof",
                year = "2nd Year",
                subject = "Pathology",
                type = "Handwritten Notes",
                tagsCsv = "2nd prof,pathology,marrow,notes,handwritten notes",
                pageCount = 512,
                sizeMb = 64.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/marrow_anatomy_notes.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof2/releases/download/v1-books/marrow_pathology_notes.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "kd_tripathi_essentials_medical_pharmacology",
                title = "Essentials of Medical Pharmacology",
                author = "K.D. Tripathi",
                edition = "8th Edition",
                prof = "2nd Prof",
                year = "2nd Year",
                subject = "Pharmacology",
                type = "Standard Textbook",
                tagsCsv = "2nd prof,pharmacology,kdt,drugs,standard textbook",
                pageCount = 1060,
                sizeMb = 72.1,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/kd_tripathi_essentials_medical_pharmacology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof2/releases/download/v1-books/kd_tripathi_essentials_medical_pharmacology.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "ananthanarayan_paniker_microbiology",
                title = "Ananthanarayan and Paniker's Textbook of Microbiology",
                author = "Reba Kanungo",
                edition = "11th Edition",
                prof = "2nd Prof",
                year = "2nd Year",
                subject = "Microbiology",
                type = "Standard Textbook",
                tagsCsv = "2nd prof,microbiology,bacteria,viruses,standard textbook",
                pageCount = 740,
                sizeMb = 62.4,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/guyton_and_hall_physiology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof2/releases/download/v1-books/ananthanarayan_paniker_microbiology.bin",
                isDownloaded = false
            ),

            // --- 3rd Prof Part 1 ---
            BookEntity(
                vaultId = 1,
                slug = "ks_narayan_reddy_forensic_medicine",
                title = "The Essentials of Forensic Medicine and Toxicology",
                author = "Dr. K.S. Narayan Reddy",
                edition = "34th Edition",
                prof = "3rd Prof Part 1",
                year = "3rd Year",
                subject = "Forensic Medicine",
                type = "Standard Textbook",
                tagsCsv = "3rd prof,forensic medicine,toxicology,autopsy,standard textbook",
                pageCount = 612,
                sizeMb = 54.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof3/releases/download/v1-books/ks_narayan_reddy_forensic_medicine.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "park_preventive_social_medicine",
                title = "Park's Textbook of Preventive and Social Medicine",
                author = "K. Park",
                edition = "26th Edition",
                prof = "3rd Prof Part 1",
                year = "3rd Year",
                subject = "Community Medicine",
                type = "Standard Textbook",
                tagsCsv = "3rd prof,community medicine,psm,epidemiology,standard textbook",
                pageCount = 952,
                sizeMb = 78.5,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/indu_khurana_medical_physiology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof3/releases/download/v1-books/park_preventive_social_medicine.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "parsons_diseases_of_the_eye",
                title = "Parson's Diseases of the Eye",
                author = "Ramanjit Sihota, Radhika Tandon",
                edition = "23rd Edition",
                prof = "3rd Prof Part 1",
                year = "3rd Year",
                subject = "Ophthalmology",
                type = "Standard Textbook",
                tagsCsv = "3rd prof,ophthalmology,eye,cornea,retina,standard textbook",
                pageCount = 624,
                sizeMb = 56.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/grays_atlas_of_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof3/releases/download/v1-books/parsons_diseases_of_the_eye.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "dhingra_diseases_of_ear_nose_throat",
                title = "Diseases of Ear, Nose and Throat",
                author = "P.L. Dhingra, Shruti Dhingra",
                edition = "8th Edition",
                prof = "3rd Prof Part 1",
                year = "3rd Year",
                subject = "ENT",
                type = "Standard Textbook",
                tagsCsv = "3rd prof,ent,otolaryngology,ear,nose,throat,standard textbook",
                pageCount = 538,
                sizeMb = 48.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/mcminns_clinical_atlas_of_human_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof3/releases/download/v1-books/dhingra_diseases_of_ear_nose_throat.bin",
                isDownloaded = false
            ),

            // --- Final Prof ---
            BookEntity(
                vaultId = 1,
                slug = "harrisons_principles_of_internal_medicine",
                title = "Harrison's Principles of Internal Medicine",
                author = "J. Larry Jameson, Anthony S. Fauci",
                edition = "21st Edition",
                prof = "Final Prof",
                year = "4th Year",
                subject = "General Medicine",
                type = "Standard Textbook",
                tagsCsv = "final prof,medicine,harrisons,internal medicine,standard textbook",
                pageCount = 1840,
                sizeMb = 124.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/guyton_and_hall_physiology.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof4/releases/download/v1-books/harrisons_principles_of_internal_medicine.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "bailey_and_love_surgery",
                title = "Bailey & Love's Short Practice of Surgery",
                author = "P. Ronan O'Connell, Andrew W. McCaskie",
                edition = "28th Edition",
                prof = "Final Prof",
                year = "4th Year",
                subject = "General Surgery",
                type = "Standard Textbook",
                tagsCsv = "final prof,surgery,bailey,love,clinical,standard textbook",
                pageCount = 1620,
                sizeMb = 118.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/inderbir_singh_textbook_of_anatomy.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof4/releases/download/v1-books/bailey_and_love_surgery.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "dc_dutta_textbook_of_obstetrics",
                title = "DC Dutta's Textbook of Obstetrics",
                author = "Hiralal Konar",
                edition = "9th Edition",
                prof = "Final Prof",
                year = "4th Year",
                subject = "Obstetrics & Gynaecology",
                type = "Standard Textbook",
                tagsCsv = "final prof,obg,obstetrics,gynaecology,pregnancy,standard textbook",
                pageCount = 712,
                sizeMb = 64.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/bd_chaurasia_human_anatomy_vol1.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof4/releases/download/v1-books/dc_dutta_textbook_of_obstetrics.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "ghai_essential_pediatrics",
                title = "Ghai Essential Pediatrics",
                author = "Vinod K. Paul, Arvind Bagga",
                edition = "9th Edition",
                prof = "Final Prof",
                year = "4th Year",
                subject = "Paediatrics",
                type = "Standard Textbook",
                tagsCsv = "final prof,pediatrics,paediatrics,child health,standard textbook",
                pageCount = 824,
                sizeMb = 68.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/lehninger_principles_of_biochemistry.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof4/releases/download/v1-books/ghai_essential_pediatrics.bin",
                isDownloaded = false
            ),
            BookEntity(
                vaultId = 1,
                slug = "maheshwari_essential_orthopaedics",
                title = "Essential Orthopaedics",
                author = "J. Maheshwari, Vikram A. Mhaskar",
                edition = "6th Edition",
                prof = "Final Prof",
                year = "4th Year",
                subject = "Orthopaedics",
                type = "Standard Textbook",
                tagsCsv = "final prof,orthopaedics,fractures,joints,standard textbook",
                pageCount = 412,
                sizeMb = 44.0,
                coverUrl = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/ak_datta_volume_1_thorax_abdomen_and_pelvis.webp",
                downloadUrl = "https://github.com/doubtsolved/mulberry-vault-prof4/releases/download/v1-books/maheshwari_essential_orthopaedics.bin",
                isDownloaded = false
            )
        )
        bookDao.insertBooks(initialBooks)
    }
}
