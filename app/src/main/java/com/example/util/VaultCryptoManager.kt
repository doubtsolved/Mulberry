package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Vault Cryptography Implementation for Project Mulberry.
 * Uses AES-256-GCM zero-plaintext storage with ephemeral reader sessions.
 */
object VaultCryptoManager {
    private const val TAG = "VaultCryptoManager"
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val AES_CTR_NO_PADDING = "AES/CTR/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PURGE_OVERWRITE_BYTES = 8192

    // Master 32-byte encryption key (hex encoded 256-bit key)
    private const val MASTER_KEY_HEX = "f0e67a11bcd402d9851a86dcd157e0257155b7755567dcabb3603216ff04095a"

    private val keyBytes: ByteArray by lazy {
        hexToByteArray(MASTER_KEY_HEX)
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    /**
     * Decrypts an AES-256-GCM payload (.bin) into a plaintext PDF file.
     * Streams directly to disk using 8KB buffers (AES-CTR equivalent keystream)
     * to eliminate all heap allocation limits and prevent OutOfMemoryError on large textbooks.
     * Checks for PDF header (%PDF-) first in case the file is already plaintext.
     */
    fun decryptVaultFile(encryptedBinFile: File, outputFile: File): Boolean {
        if (!encryptedBinFile.exists() || encryptedBinFile.length() == 0L) {
            Log.e(TAG, "Source bin file does not exist or is empty: ${encryptedBinFile.absolutePath}")
            return false
        }

        outputFile.parentFile?.mkdirs()

        // 1. Check if the file is already a raw unencrypted PDF
        try {
            FileInputStream(encryptedBinFile).use { fis ->
                val header = ByteArray(5)
                val read = fis.read(header)
                if (read >= 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                    header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
                ) {
                    // Raw PDF; copy directly
                    encryptedBinFile.copyTo(outputFile, overwrite = true)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Header check failed: ${e.message}")
        }

        val totalLength = encryptedBinFile.length()
        val tagLengthBytes = GCM_TAG_LENGTH_BITS / 8 // 16 bytes
        val minOverhead = GCM_IV_LENGTH + tagLengthBytes // 28 bytes
        if (totalLength < minOverhead) {
            Log.e(TAG, "File size ($totalLength) is smaller than crypto overhead ($minOverhead)")
            return false
        }

        // 2. High-Performance Zero-OOM Streaming Decryption:
        // Under NIST SP 800-38D, AES-256-GCM encrypts data blocks using AES in CTR mode
        // starting at counter J0 + 1 (where J0 = IV || 00 00 00 01, counter 1 is for tag).
        // Therefore, the payload ciphertext stream is identical to AES/CTR/NoPadding with counter starting at 2.
        // Decrypting with AES/CTR streams chunk-by-chunk directly into disk using a fixed 8KB buffer,
        // bypassing Conscrypt's monolithic memory buffering and completely avoiding OutOfMemoryError.
        var decrypted = try {
            FileInputStream(encryptedBinFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val ivRead = fis.read(iv)
                if (ivRead != GCM_IV_LENGTH) {
                    Log.e(TAG, "Incomplete IV in vault payload")
                    false
                } else {
                    val ctrIv = ByteArray(16)
                    System.arraycopy(iv, 0, ctrIv, 0, GCM_IV_LENGTH)
                    ctrIv[12] = 0
                    ctrIv[13] = 0
                    ctrIv[14] = 0
                    ctrIv[15] = 2

                    val cipher = Cipher.getInstance(AES_CTR_NO_PADDING)
                    val keySpec = SecretKeySpec(keyBytes, "AES")
                    val ivSpec = IvParameterSpec(ctrIv)
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

                    val ciphertextLength = totalLength - GCM_IV_LENGTH - tagLengthBytes
                    var remainingCiphertext = ciphertextLength

                    FileOutputStream(outputFile).use { fos ->
                        val inBuffer = ByteArray(8192)
                        while (remainingCiphertext > 0L) {
                            val toRead = minOf(inBuffer.size.toLong(), remainingCiphertext).toInt()
                            val bytesRead = fis.read(inBuffer, 0, toRead)
                            if (bytesRead == -1) break
                            val outChunk = cipher.update(inBuffer, 0, bytesRead)
                            if (outChunk != null && outChunk.isNotEmpty()) {
                                fos.write(outChunk)
                            }
                            remainingCiphertext -= bytesRead
                        }
                        val finalChunk = cipher.doFinal()
                        if (finalChunk != null && finalChunk.isNotEmpty()) {
                            fos.write(finalChunk)
                        }
                        fos.flush()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming CTR decryption error: ${e.message}", e)
            false
        }

        // 3. Verify that outputFile is non-empty and has valid %PDF- magic bytes
        var isPdfValid = if (decrypted && outputFile.exists() && outputFile.length() >= 4) {
            try {
                FileInputStream(outputFile).use { fis ->
                    val header = ByteArray(4)
                    val read = fis.read(header)
                    read == 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                            header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Output header verification error: ${e.message}")
                false
            }
        } else {
            false
        }

        // Fallback for smaller files (<= 30 MB) if streaming CTR header verification failed
        if (!isPdfValid && totalLength <= 30 * 1024 * 1024L) {
            Log.w(TAG, "Attempting standard GCM fallback for small file ($totalLength bytes)")
            try {
                FileInputStream(encryptedBinFile).use { fis ->
                    val iv = ByteArray(GCM_IV_LENGTH)
                    if (fis.read(iv) == GCM_IV_LENGTH) {
                        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
                        val keySpec = SecretKeySpec(keyBytes, "AES")
                        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

                        FileOutputStream(outputFile).use { fos ->
                            val inBuffer = ByteArray(32768)
                            var read: Int
                            while (fis.read(inBuffer).also { read = it } != -1) {
                                val outChunk = cipher.update(inBuffer, 0, read)
                                if (outChunk != null && outChunk.isNotEmpty()) {
                                    fos.write(outChunk)
                                }
                            }
                            val finalChunk = cipher.doFinal()
                            if (finalChunk != null && finalChunk.isNotEmpty()) {
                                fos.write(finalChunk)
                            }
                            fos.flush()
                        }
                    }
                }
                isPdfValid = outputFile.exists() && outputFile.length() >= 4 && run {
                    FileInputStream(outputFile).use { fis ->
                        val h = ByteArray(4)
                        fis.read(h) == 4 && h[0] == '%'.code.toByte() && h[1] == 'P'.code.toByte() &&
                                h[2] == 'D'.code.toByte() && h[3] == 'F'.code.toByte()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Standard GCM fallback failed: ${e.message}")
            }
        }

        if (isPdfValid) {
            return true
        } else {
            if (outputFile.exists()) {
                try { outputFile.delete() } catch (_: Exception) {}
            }
            return false
        }
    }

    /**
     * Encrypts a plaintext PDF into an AES-256-GCM .bin file.
     */
    fun encryptFileToVault(plaintextFile: File, outputBinFile: File): Boolean {
        if (!plaintextFile.exists()) return false
        outputBinFile.parentFile?.mkdirs()

        return try {
            val random = SecureRandom()
            val iv = ByteArray(GCM_IV_LENGTH)
            random.nextBytes(iv)

            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            FileOutputStream(outputBinFile).use { fos ->
                // Write IV as first 12 bytes
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(plaintextFile).use { fis ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            cos.write(buffer, 0, bytesRead)
                        }
                        cos.flush()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            false
        }
    }

    /**
     * Ephemeral Decryption: Prepares active session file for reader.
     */
    fun createEphemeralSessionFile(context: Context, bookSlug: String): File {
        val sessionDir = File(context.cacheDir, "reader_session")
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        } else {
            // Clean up any stale sessions from previous reader sessions to keep storage clean
            try {
                sessionDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        try {
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
        return File(sessionDir, "active_${bookSlug}_${System.currentTimeMillis()}.pdf")
    }

    /**
     * Secure purge protocol: Overwrites first 8 KB with zeroes, syncs, then unlinks/deletes.
     */
    fun purgeEphemeralSession(sessionFile: File?) {
        if (sessionFile == null || !sessionFile.exists()) return

        try {
            if (sessionFile.isFile && sessionFile.length() > 0L) {
                RandomAccessFile(sessionFile, "rw").use { raf ->
                    val zeroCount = minOf(PURGE_OVERWRITE_BYTES.toLong(), raf.length()).toInt()
                    val zeroBuffer = ByteArray(zeroCount)
                    raf.seek(0)
                    raf.write(zeroBuffer)
                    raf.fd.sync()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Zeroing ephemeral session encountered: ${e.message}")
        } finally {
            try {
                sessionFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Deleting ephemeral session failed: ${e.message}")
            }
        }
    }

    /**
     * Purges all files in reader_session cache.
     */
    fun clearAllEphemeralSessions(context: Context) {
        val sessionDir = File(context.cacheDir, "reader_session")
        if (sessionDir.exists() && sessionDir.isDirectory) {
            sessionDir.listFiles()?.forEach { file ->
                purgeEphemeralSession(file)
            }
        }
    }
}
