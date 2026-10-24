package com.example

import com.example.ui.reader.DualPageMode
import com.example.ui.reader.DualSpreadCalculator
import com.example.ui.reader.ReaderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPreferencesTest {

    @Test
    fun computeColorMatrix_attenuatesBlueChannelAndScalesRgbForDimming() {
        val prefs = ReaderPreferences(
            eyeRelaxEnabled = true,
            eyeRelaxWarmth = 0.50f,
            eyeRelaxDim = 0.40f,
            eyeRelaxGrayscale = false
        )
        val matrix = prefs.computeColorMatrix()
        val values = matrix.values

        // dimScale = 1f - 0.40f * 0.5f = 0.80f
        // redScale = 0.80f
        // blueScale = 0.80f * (1f - 0.50f * 0.45f) = 0.80f * 0.775f = 0.62f
        assertEquals(0.80f, values[0], 0.001f)
        assertEquals(0.62f, values[12], 0.001f)
        assertEquals(1.0f, values[18], 0.001f)
    }

    @Test
    fun dualPageMode_cyclesInExpectedOrder() {
        assertEquals(DualPageMode.FORCE_SINGLE, DualPageMode.AUTO.next())
        assertEquals(DualPageMode.FORCE_DUAL, DualPageMode.FORCE_SINGLE.next())
        assertEquals(DualPageMode.AUTO, DualPageMode.FORCE_DUAL.next())
    }

    @Test
    fun dualSpreadCalculator_withFirstPageCoverTrue_mapsCoverAloneAndPairsSubsequentPages() {
        // Total 5 pages (1-based: 1..5, 0-based: 0..4)
        // Spread 0 -> Page 1 (idx 0, null)
        // Spread 1 -> Pages 2 & 3 (idx 1, 2)
        // Spread 2 -> Pages 4 & 5 (idx 3, 4)
        val totalPages = 5
        val spreadCount = DualSpreadCalculator.getSpreadCount(
            totalPages = totalPages,
            isDualActive = true,
            firstPageIsCover = true
        )
        assertEquals(3, spreadCount)

        val spread0 = DualSpreadCalculator.getSpreadForIndex(0, totalPages, isDualActive = true, firstPageIsCover = true)
        assertEquals(0, spread0.leftPageIndex)
        assertNull(spread0.rightPageIndex)
        assertTrue(spread0.isSingleCenteredCover)

        val spread1 = DualSpreadCalculator.getSpreadForIndex(1, totalPages, isDualActive = true, firstPageIsCover = true)
        assertEquals(1, spread1.leftPageIndex) // Page 2
        assertEquals(2, spread1.rightPageIndex) // Page 3

        val spread2 = DualSpreadCalculator.getSpreadForIndex(2, totalPages, isDualActive = true, firstPageIsCover = true)
        assertEquals(3, spread2.leftPageIndex) // Page 4
        assertEquals(4, spread2.rightPageIndex) // Page 5

        // Reverse lookup: pageIndex -> spreadIndex
        assertEquals(0, DualSpreadCalculator.pageIndexToSpreadIndex(0, totalPages, true, true))
        assertEquals(1, DualSpreadCalculator.pageIndexToSpreadIndex(1, totalPages, true, true))
        assertEquals(1, DualSpreadCalculator.pageIndexToSpreadIndex(2, totalPages, true, true))
        assertEquals(2, DualSpreadCalculator.pageIndexToSpreadIndex(3, totalPages, true, true))
        assertEquals(2, DualSpreadCalculator.pageIndexToSpreadIndex(4, totalPages, true, true))
    }

    @Test
    fun dualSpreadCalculator_withFirstPageCoverFalse_pairsFromFirstPageAndHandlesTrailingOddPage() {
        // Total 5 pages (0-based: 0..4)
        // Spread 0 -> Pages 1 & 2 (idx 0, 1)
        // Spread 1 -> Pages 3 & 4 (idx 2, 3)
        // Spread 2 -> Page 5 & blank (idx 4, null)
        val totalPages = 5
        val spreadCount = DualSpreadCalculator.getSpreadCount(
            totalPages = totalPages,
            isDualActive = true,
            firstPageIsCover = false
        )
        assertEquals(3, spreadCount)

        val spread0 = DualSpreadCalculator.getSpreadForIndex(0, totalPages, isDualActive = true, firstPageIsCover = false)
        assertEquals(0, spread0.leftPageIndex)
        assertEquals(1, spread0.rightPageIndex)

        val spread2 = DualSpreadCalculator.getSpreadForIndex(2, totalPages, isDualActive = true, firstPageIsCover = false)
        assertEquals(4, spread2.leftPageIndex)
        assertNull(spread2.rightPageIndex)
    }
}
