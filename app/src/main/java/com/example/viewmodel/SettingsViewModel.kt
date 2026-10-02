package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MulberryDatabase
import com.example.data.model.BookEntity
import com.example.data.model.UserProfile
import com.example.data.model.VaultEntity
import com.example.data.repository.BookRepository
import com.example.data.repository.MulberryRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VaultMetadataManager
import com.example.ui.theme.IconStyle
import com.example.ui.theme.ThemeMode
import com.example.util.BackupManager
import com.example.util.StorageMetricsHelper
import com.example.util.DownloadState
import com.example.util.ReleaseInfo
import com.example.util.UpdateCheckResult
import com.example.util.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MulberryDatabase.getInstance(application)
    private val repository = MulberryRepository(
        vaultDao = database.vaultDao(),
        bookDao = database.bookDao(),
        agendaTaskDao = database.agendaTaskDao(),
        examDao = database.examDao(),
        annotationDao = database.annotationDao()
    )
    private val bookRepository = BookRepository(database.bookDao(), database.vaultDao())
    private val prefs = application.getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
    private val userProfileRepo = UserProfileRepository.getInstance(application)

    // User Profile
    val userProfile: StateFlow<UserProfile> = userProfileRepo.userProfile

    fun saveProfile(updated: UserProfile, activeVaultPath: String? = null) {
        viewModelScope.launch {
            userProfileRepo.updateProfile(updated, activeVaultPath)
        }
    }

    // 1. Vaults & Books
    val vaults: StateFlow<List<VaultEntity>> = repository.allVaults.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val books: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 2. Active Theme
    private val _activeTheme = MutableStateFlow(
        ThemeMode.from(prefs.getString("theme_mode", ThemeMode.PAPER.name))
    )
    val activeTheme: StateFlow<ThemeMode> = _activeTheme.asStateFlow()

    // Icon Style (VIBRANT vs ADAPTIVE)
    private val _iconStyle = MutableStateFlow(
        try {
            IconStyle.valueOf(prefs.getString("ICON_STYLE_KEY", IconStyle.VIBRANT.name) ?: IconStyle.VIBRANT.name)
        } catch (_: Exception) {
            IconStyle.VIBRANT
        }
    )
    val iconStyle: StateFlow<IconStyle> = _iconStyle.asStateFlow()

    // 3. Default Reading Mode (Single-Page Focus)
    private val _isSinglePageDefault = MutableStateFlow(prefs.getBoolean("single_page_default", true))
    val isSinglePageDefault: StateFlow<Boolean> = _isSinglePageDefault.asStateFlow()

    // 4. Page Flip Animation (Subtle overlapping sheet with soft shadow)
    private val _isPageFlipAnimationEnabled = MutableStateFlow(prefs.getBoolean("pref_page_flip_animation", true))
    val isPageFlipAnimationEnabled: StateFlow<Boolean> = _isPageFlipAnimationEnabled.asStateFlow()

    // 5. Keep Screen Awake
    private val _keepScreenAwake = MutableStateFlow(prefs.getBoolean("keep_screen_awake", false))
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    // 5. Live Storage & Cache Size
    private val _cacheSizeBytes = MutableStateFlow(StorageMetricsHelper.getCacheSizeBytes(application))
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _databaseSizeBytes = MutableStateFlow(StorageMetricsHelper.getDatabaseSizeBytes(application))
    val databaseSizeBytes: StateFlow<Long> = _databaseSizeBytes.asStateFlow()

    init {
        refreshStorageMetrics()
    }

    fun setTheme(theme: ThemeMode) {
        _activeTheme.value = theme
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun setIconStyle(style: IconStyle) {
        _iconStyle.value = style
        prefs.edit().putString("ICON_STYLE_KEY", style.name).apply()
    }

    fun setReadingMode(isSinglePage: Boolean) {
        _isSinglePageDefault.value = isSinglePage
        val modeName = if (isSinglePage) "SINGLE_PAGE" else "CONTINUOUS"
        prefs.edit()
            .putBoolean("single_page_default", isSinglePage)
            .putString("DEFAULT_READING_MODE", modeName)
            .apply()
        try {
            android.preference.PreferenceManager.getDefaultSharedPreferences(getApplication<Application>())
                .edit()
                .putBoolean("single_page_default", isSinglePage)
                .putString("DEFAULT_READING_MODE", modeName)
                .apply()
        } catch (_: Exception) {}
    }

    fun toggleKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        prefs.edit().putBoolean("keep_screen_awake", enabled).apply()
    }

    fun togglePageFlipAnimation(enabled: Boolean) {
        _isPageFlipAnimationEnabled.value = enabled
        prefs.edit().putBoolean("pref_page_flip_animation", enabled).apply()
        try {
            android.preference.PreferenceManager.getDefaultSharedPreferences(getApplication<Application>())
                .edit()
                .putBoolean("pref_page_flip_animation", enabled)
                .apply()
        } catch (_: Exception) {}
    }

    fun refreshStorageMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            _cacheSizeBytes.value = StorageMetricsHelper.getCacheSizeBytes(context)
            _databaseSizeBytes.value = StorageMetricsHelper.getDatabaseSizeBytes(context)
        }
    }

    fun refreshCacheSize() {
        refreshStorageMetrics()
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            StorageMetricsHelper.clearCache(context)
            _cacheSizeBytes.value = StorageMetricsHelper.getCacheSizeBytes(context)
            _databaseSizeBytes.value = StorageMetricsHelper.getDatabaseSizeBytes(context)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun renameVault(vault: VaultEntity, newName: String) {
        viewModelScope.launch {
            repository.updateVault(vault.copy(name = newName.trim()))
        }
    }

    fun unbindVault(vault: VaultEntity) {
        viewModelScope.launch {
            repository.deleteVault(vault)
        }
    }

    fun bindVaultFolder(uri: Uri, folderName: String) {
        viewModelScope.launch {
            val segment = uri.lastPathSegment ?: folderName
            val display = segment.substringAfterLast(":").ifEmpty { folderName }
            repository.addVault(
                name = folderName,
                uriString = uri.toString(),
                pathDisplay = display
            )
        }
    }

    fun forceFlushMetadata(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val currentVaults = database.vaultDao().getAllVaultsSync()
            currentVaults.forEach { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    try {
                        VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), app)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun forceReindexVaults(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentVaults = database.vaultDao().getAllVaultsSync()
            currentVaults.forEach { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    try {
                        bookRepository.syncVaultWithDisk(root, vault.id.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun exportZipBackup(vaultRootPath: String, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = BackupManager.exportVaultBackup(getApplication(), vaultRootPath)
            withContext(Dispatchers.Main) {
                onResult(uri)
            }
        }
    }

    fun restoreZipBackup(zipUri: Uri, vaultRootPath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupManager.restoreVaultBackup(getApplication(), zipUri, vaultRootPath)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun exportJsonBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportJsonBackup()
            withContext(Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun importJsonBackup(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importJsonBackup(json)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    // -------------------------------------------------------------------------
    // IN-APP UPDATE SYSTEM
    // -------------------------------------------------------------------------
    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadJob: kotlinx.coroutines.Job? = null

    fun checkForUpdate(currentVersion: String, onResult: ((UpdateCheckResult) -> Unit)? = null) {
        if (_isCheckingForUpdate.value) return
        viewModelScope.launch {
            _isCheckingForUpdate.value = true
            val result = UpdateManager.checkForUpdate(currentVersion)
            _isCheckingForUpdate.value = false
            _updateCheckResult.value = result
            withContext(Dispatchers.Main) {
                onResult?.invoke(result)
            }
        }
    }

    fun startDownload(release: ReleaseInfo) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            UpdateManager.downloadApk(
                context = getApplication(),
                release = release,
                onProgress = { state ->
                    _downloadState.value = state
                }
            )
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    fun installApk(context: Context, apkFile: java.io.File) {
        UpdateManager.installApk(context, apkFile)
    }

    fun dismissUpdate() {
        _updateCheckResult.value = null
        _downloadState.value = DownloadState.Idle
    }
}
