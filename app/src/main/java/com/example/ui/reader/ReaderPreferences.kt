package com.example.ui.reader

import android.content.Context
import androidx.compose.ui.graphics.ColorMatrix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DualPageMode(val label: String) {
    AUTO("Auto Spread"),
    FORCE_SINGLE("Single Page"),
    FORCE_DUAL("Dual Spread");

    fun next(): DualPageMode = when (this) {
        AUTO -> FORCE_SINGLE
        FORCE_SINGLE -> FORCE_DUAL
        FORCE_DUAL -> AUTO
    }

    companion object {
        fun from(value: String?): DualPageMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO
        }
    }
}

data class ReaderPreferences(
    val eyeRelaxEnabled: Boolean = false,
    val eyeRelaxWarmth: Float = 0.35f,
    val eyeRelaxDim: Float = 0.0f,
    val eyeRelaxGrayscale: Boolean = false,
    val dualPageMode: DualPageMode = DualPageMode.AUTO,
    val firstPageIsCover: Boolean = true
) {
    /**
     * Computes a Compose ColorMatrix attenuating the blue channel by (1f - warmth * 0.45f)
     * and scaling RGB values by (1f - dim * 0.5f), optionally combined with grayscale E-Ink mode.
     */
    fun computeColorMatrix(): ColorMatrix {
        val clampedWarmth = eyeRelaxWarmth.coerceIn(0f, 1f)
        val clampedDim = eyeRelaxDim.coerceIn(0f, 0.60f)

        val dimScale = 1f - clampedDim * 0.5f
        val redScale = dimScale
        val greenScale = dimScale * (1f - clampedWarmth * 0.08f)
        val blueScale = dimScale * (1f - clampedWarmth * 0.45f)

        val calibrationMatrix = ColorMatrix(
            floatArrayOf(
                redScale, 0f, 0f, 0f, 0f,
                0f, greenScale, 0f, 0f, 0f,
                0f, 0f, blueScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        if (eyeRelaxGrayscale) {
            val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
            calibrationMatrix.timesAssign(grayscaleMatrix)
        }

        return calibrationMatrix
    }
}

/**
 * Represents the 0-based page indices rendered in a single pager slot (spread).
 */
data class PageSpread(
    val spreadIndex: Int,
    val leftPageIndex: Int,
    val rightPageIndex: Int?
) {
    val isSingleCenteredCover: Boolean
        get() = spreadIndex == 0 && rightPageIndex == null && leftPageIndex == 0
}

object DualSpreadCalculator {
    /**
     * Total number of spreads for [totalPages] when dual-page mode is active.
     * - If firstPageIsCover == true:
     *   Spread 0 -> Page 1 (idx 0)
     *   Spread 1 -> Pages 2 & 3 (idx 1 & 2)
     *   Spread i -> Pages 2i & 2i+1 (idx 2i-1 & 2i)
     * - If firstPageIsCover == false:
     *   Spread i -> Pages 2i+1 & 2i+2 (idx 2i & 2i+1)
     */
    fun getSpreadCount(totalPages: Int, isDualActive: Boolean, firstPageIsCover: Boolean): Int {
        val safeTotal = totalPages.coerceAtLeast(1)
        if (!isDualActive || safeTotal <= 1) return safeTotal
        return if (firstPageIsCover) {
            1 + (safeTotal / 2)
        } else {
            (safeTotal + 1) / 2
        }
    }

    fun getSpreadForIndex(
        spreadIndex: Int,
        totalPages: Int,
        isDualActive: Boolean,
        firstPageIsCover: Boolean
    ): PageSpread {
        val safeTotal = totalPages.coerceAtLeast(1)
        if (!isDualActive || safeTotal <= 1) {
            val clamped = spreadIndex.coerceIn(0, safeTotal - 1)
            return PageSpread(spreadIndex = clamped, leftPageIndex = clamped, rightPageIndex = null)
        }

        return if (firstPageIsCover) {
            if (spreadIndex <= 0) {
                PageSpread(spreadIndex = 0, leftPageIndex = 0, rightPageIndex = null)
            } else {
                val leftIdx = (2 * spreadIndex - 1).coerceIn(0, safeTotal - 1)
                val rightCandidate = 2 * spreadIndex
                val rightIdx = if (rightCandidate < safeTotal) rightCandidate else null
                PageSpread(spreadIndex = spreadIndex, leftPageIndex = leftIdx, rightPageIndex = rightIdx)
            }
        } else {
            val leftIdx = (2 * spreadIndex).coerceIn(0, safeTotal - 1)
            val rightCandidate = 2 * spreadIndex + 1
            val rightIdx = if (rightCandidate < safeTotal) rightCandidate else null
            PageSpread(spreadIndex = spreadIndex, leftPageIndex = leftIdx, rightPageIndex = rightIdx)
        }
    }

    fun pageIndexToSpreadIndex(
        pageIndex: Int,
        totalPages: Int,
        isDualActive: Boolean,
        firstPageIsCover: Boolean
    ): Int {
        val safeTotal = totalPages.coerceAtLeast(1)
        val clampedPage = pageIndex.coerceIn(0, safeTotal - 1)
        if (!isDualActive || safeTotal <= 1) return clampedPage

        return if (firstPageIsCover) {
            if (clampedPage == 0) 0 else (clampedPage + 1) / 2
        } else {
            clampedPage / 2
        }
    }
}

object ReaderPreferencesManager {
    private const val PREFS_NAME = "mulberry_prefs"
    private const val KEY_EYE_RELAX_ENABLED = "eye_relax_enabled"
    private const val KEY_EYE_RELAX_WARMTH = "eye_relax_warmth"
    private const val KEY_EYE_RELAX_DIM = "eye_relax_dim"
    private const val KEY_EYE_RELAX_GRAYSCALE = "eye_relax_grayscale"
    private const val KEY_DUAL_PAGE_MODE = "dual_page_mode"
    private const val KEY_FIRST_PAGE_IS_COVER = "first_page_is_cover"

    private val _preferences = MutableStateFlow(ReaderPreferences())
    val preferences: StateFlow<ReaderPreferences> = _preferences.asStateFlow()

    private var isInitialized = false

    fun init(context: Context): ReaderPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = ReaderPreferences(
            eyeRelaxEnabled = prefs.getBoolean(KEY_EYE_RELAX_ENABLED, false),
            eyeRelaxWarmth = prefs.getFloat(KEY_EYE_RELAX_WARMTH, 0.35f).coerceIn(0f, 1f),
            eyeRelaxDim = prefs.getFloat(KEY_EYE_RELAX_DIM, 0.0f).coerceIn(0f, 0.60f),
            eyeRelaxGrayscale = prefs.getBoolean(KEY_EYE_RELAX_GRAYSCALE, false),
            dualPageMode = DualPageMode.from(prefs.getString(KEY_DUAL_PAGE_MODE, DualPageMode.AUTO.name)),
            firstPageIsCover = prefs.getBoolean(KEY_FIRST_PAGE_IS_COVER, true)
        )
        _preferences.value = loaded
        isInitialized = true
        return loaded
    }

    fun update(context: Context, transform: (ReaderPreferences) -> ReaderPreferences) {
        if (!isInitialized) {
            init(context)
        }
        val updated = transform(_preferences.value).let {
            it.copy(
                eyeRelaxWarmth = it.eyeRelaxWarmth.coerceIn(0f, 1f),
                eyeRelaxDim = it.eyeRelaxDim.coerceIn(0f, 0.60f)
            )
        }
        _preferences.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EYE_RELAX_ENABLED, updated.eyeRelaxEnabled)
            .putFloat(KEY_EYE_RELAX_WARMTH, updated.eyeRelaxWarmth)
            .putFloat(KEY_EYE_RELAX_DIM, updated.eyeRelaxDim)
            .putBoolean(KEY_EYE_RELAX_GRAYSCALE, updated.eyeRelaxGrayscale)
            .putString(KEY_DUAL_PAGE_MODE, updated.dualPageMode.name)
            .putBoolean(KEY_FIRST_PAGE_IS_COVER, updated.firstPageIsCover)
            .apply()
    }
}
