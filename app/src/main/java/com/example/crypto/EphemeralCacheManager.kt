package com.example.crypto

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class EphemeralCacheManager(private val context: Context) {

    val sessionDir: File
        get() = File(context.cacheDir, "reader_session").apply { mkdirs() }

    /**
     * Decrypts a vault .bin file into a transient cache PDF file.
     * Prior sessions are wiped first.
     */
    fun openEphemeralPdf(binFile: File): File {
        purgeEphemeralSession()
        val tempPdf = File(sessionDir, "active_${System.currentTimeMillis()}.pdf")
        VaultCrypto.decryptPayloadToFile(binFile, tempPdf)
        return tempPdf
    }

    /**
     * Completely wipes and zeroes out the temporary plaintext PDF files.
     * Overwrites bytes with 0x00 buffers before deletion for anti-forensic safety.
     */
    fun purgeEphemeralSession() {
        if (sessionDir.exists()) {
            sessionDir.listFiles()?.forEach { file ->
                try {
                    val length = file.length().coerceAtLeast(0L)
                    if (length > 0) {
                        FileOutputStream(file).use { fos ->
                            val zeroBuffer = ByteArray(minOf(length.toInt(), 8192))
                            var remaining = length
                            while (remaining > 0) {
                                val toWrite = minOf(remaining, zeroBuffer.size.toLong()).toInt()
                                fos.write(zeroBuffer, 0, toWrite)
                                remaining -= toWrite
                            }
                            fos.flush()
                        }
                    }
                    file.delete()
                } catch (_: Exception) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Total bytes occupied by ephemeral session cache.
     */
    fun getSessionCacheBytes(): Long {
        if (!sessionDir.exists()) return 0L
        return sessionDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
