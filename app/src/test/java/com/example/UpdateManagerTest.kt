package com.example

import com.example.util.ReleaseInfo
import com.example.util.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testGitHubEndpointCoordinates() {
        assertEquals("doubtsolved", UpdateManager.GITHUB_OWNER)
        assertEquals("Mulberry", UpdateManager.GITHUB_REPO)
        assertEquals(
            "https://api.github.com/repos/doubtsolved/Mulberry/releases/latest",
            UpdateManager.RELEASES_API_URL
        )
    }

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(UpdateManager.isNewerVersion("1.1.0", "1.0.0"))
        assertTrue(UpdateManager.isNewerVersion("v1.2.0", "1.0.0"))
        assertTrue(UpdateManager.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(UpdateManager.isNewerVersion("1.10.0", "1.9.0"))
        assertTrue(UpdateManager.isNewerVersion("1.1", "1.0"))

        // Equal versions
        assertFalse(UpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0", "1.0.0"))

        // Older versions
        assertFalse(UpdateManager.isNewerVersion("0.9.0", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("v0.8.5", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun testReleaseInfoFormattedSize() {
        val release = ReleaseInfo(
            tagName = "v1.2.0",
            version = "1.2.0",
            title = "Mulberry v1.2.0",
            changelog = "• Added search highlights",
            publishedAt = "2026-10-02T12:00:00Z",
            apkDownloadUrl = "https://github.com/doubtsolved/Mulberry/releases/download/v1.2.0/Mulberry-v1.2.0.apk",
            apkFileName = "Mulberry-v1.2.0.apk",
            apkSizeBytes = 19_293_798L // ~18.4 MB
        )

        assertEquals("18.4 MB", release.formattedSize)
    }
}
