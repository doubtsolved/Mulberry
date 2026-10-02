package com.example.data.repository

import com.example.data.dao.BookDao
import com.example.data.dao.VaultDao
import com.example.data.model.BookEntity
import com.example.data.model.fileSize
import com.example.data.model.path
import com.example.util.cleanBookTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BookRepository(
    private val bookDao: BookDao,
    private val vaultDao: VaultDao? = null
) {
    suspend fun syncVaultWithDisk(vaultPath: String, vaultId: String) = withContext(Dispatchers.IO) {
        val rootDir = File(vaultPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext

        // Step 1: Scan disk for current PDFs
        val diskFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
            .toList()
        val diskPathMap = diskFiles.associateBy { it.absolutePath }

        // Step 2: Fetch current DB books for this vault
        val vaultIdInt = vaultId.toIntOrNull() ?: -1
        val dbBooks = if (vaultIdInt != -1) {
            bookDao.getBooksByVaultSync(vaultIdInt)
        } else {
            bookDao.getAllBooksSync().filter { it.uriString.startsWith(vaultPath) }
        }
        val dbPathMap = dbBooks.associateBy { it.path }

        // Step 3: Compute changes
        // 3a. New files to insert
        val newBooks = diskFiles.filterNot { dbPathMap.containsKey(it.absolutePath) }.map { file ->
            BookEntity(
                vaultId = if (vaultIdInt != -1) vaultIdInt else 1,
                title = cleanBookTitle(file.nameWithoutExtension),
                fileName = file.name,
                uriString = file.absolutePath,
                fileSizeBytes = file.length(),
                pageCount = 1,
                lastReadPage = 1,
                progressPercent = 0,
                lastReadTimestamp = 0L,
                parentFolder = file.parentFile?.name ?: "Vault",
                isFavorite = false
            )
        }

        // 3b. Missing files to remove from DB
        val deletedBookPaths = dbBooks.filterNot { diskPathMap.containsKey(it.path) }.map { it.path }

        // 3c. Modified files (size or timestamp changed)
        val modifiedBooks = dbBooks.filter { book ->
            val diskFile = diskPathMap[book.path]
            diskFile != null && diskFile.length() != book.fileSize
        }.map { book ->
            val diskFile = diskPathMap[book.path]!!
            book.copy(fileSizeBytes = diskFile.length())
        }

        // Step 4: Batch execute in Room
        if (newBooks.isNotEmpty()) {
            bookDao.insertBooks(newBooks)
        }
        if (deletedBookPaths.isNotEmpty()) {
            bookDao.deleteBooksByPaths(deletedBookPaths)
        }
        if (modifiedBooks.isNotEmpty()) {
            bookDao.updateBooks(modifiedBooks)
        }

        // Hydrate saved state from .mulberry metadata
        try {
            VaultMetadataManager.hydrateDatabaseFromMetadata(vaultPath, vaultId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update vault count if vaultDao is available
        if (vaultDao != null && vaultIdInt != -1) {
            try {
                val vault = vaultDao.getAllVaultsSync().find { it.id == vaultIdInt }
                if (vault != null) {
                    vaultDao.updateVault(vault.copy(bookCount = diskFiles.size))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
