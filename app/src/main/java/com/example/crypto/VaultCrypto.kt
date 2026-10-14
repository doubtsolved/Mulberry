package com.example.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

    // 32-byte AES-256 Master Vault Key
    const val VAULT_KEY_HEX = "f0e67a11bcd402d9851a86dcd157e0257155b7755567dcabb3603216ff04095a"

    private val secretKeySpec: SecretKeySpec by lazy {
        val keyBytes = VAULT_KEY_HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Decrypts an encrypted .bin payload into an ephemeral temp .pdf file.
     * Extracts the prepended 12-byte GCM nonce and decrypts using AES/GCM/NoPadding.
     * Uses buffered streams to avoid loading hundreds of megabytes into JVM heap memory.
     */
    fun decryptPayloadToFile(encryptedBinFile: File, outputTempPdfFile: File) {
        if (!encryptedBinFile.exists() || encryptedBinFile.length() < 12) {
            throw IllegalArgumentException("Source encrypted file does not exist or has invalid size: ${encryptedBinFile.absolutePath}")
        }

        outputTempPdfFile.parentFile?.mkdirs()

        // 1. Fast-path: check if file is already a raw unencrypted PDF
        try {
            FileInputStream(encryptedBinFile).use { fis ->
                val header = ByteArray(5)
                val read = fis.read(header)
                if (read >= 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                    header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
                ) {
                    FileInputStream(encryptedBinFile).use { inStream ->
                        FileOutputStream(outputTempPdfFile).use { outStream ->
                            inStream.copyTo(outStream, bufferSize = 8192)
                        }
                    }
                    return
                }
            }
        } catch (_: Exception) {}

        // 2. Stream decrypt AES-256-GCM using fixed 8KB chunks to prevent OutOfMemoryError
        FileInputStream(encryptedBinFile).use { fis ->
            // Extract the prepended 12-byte nonce/IV
            val nonce = ByteArray(12)
            val bytesRead = fis.read(nonce)
            if (bytesRead != 12) {
                throw IllegalStateException("Invalid encrypted header or corrupted payload.")
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec)

            FileOutputStream(outputTempPdfFile).use { fos ->
                val inBuffer = ByteArray(8192)
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
}
