@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import com.example.ui.navigation.MulberryBottomNavBar
import com.example.ui.navigation.MulberryNavigationRail
import com.example.ui.navigation.MulberryTab
import com.example.ui.screens.AgendaScreen
import com.example.ui.reader.ReaderActivity
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SnipsScreen
import com.example.ui.screens.PermissionGatewayScreen
import com.example.ui.screens.desk.DeskRootScreen
import com.example.ui.theme.MulberryTheme
import com.example.util.PermissionUtils
import com.example.viewmodel.MulberryViewModel
import com.example.BuildConfig
import com.example.ui.components.CustomizeTabsSheet
import com.example.ui.components.UpdateBottomSheet
import com.example.util.DownloadState
import com.example.util.ReleaseInfo
import com.example.util.UpdateCheckResult
import com.example.util.UpdateManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MulberryViewModel by viewModels()

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val context = LocalContext.current

            var permissionsGranted by remember {
                mutableStateOf(PermissionUtils.areAllRequiredPermissionsGranted(context))
            }

            // Continuously listen to resume events so if user grants permissions outside the app, UI updates immediately
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                permissionsGranted = PermissionUtils.areAllRequiredPermissionsGranted(context)
            }

            MulberryTheme(themeMode = themeMode) {
                AnimatedContent(
                    targetState = permissionsGranted,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "PermissionGateTransition"
                ) { granted ->
                    if (granted) {
                        MulberryApp(viewModel = viewModel)
                    } else {
                        PermissionGatewayScreen(
                            onAllPermissionsGranted = {
                                permissionsGranted = true
                            }
                        )
                    }
                }
            }
        }
    }

    fun checkAndRequestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val uri = Uri.parse("package:$packageName")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(fallback)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MulberryApp(viewModel: MulberryViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val visibleTabs by viewModel.visibleTabs.collectAsState()
    val showCustomizeTabsSheet by viewModel.showCustomizeTabsSheet.collectAsState()
    val isReaderOpen by viewModel.isReaderOpen.collectAsState()
    val currentBook by viewModel.currentReadingBook.collectAsState()
    val activeBreadcrumbs by viewModel.activeBreadcrumbs.collectAsState()
    val activeFolderFilter by viewModel.activeFolderFilter.collectAsState()

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var launchUpdateRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var launchDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var showLaunchUpdateSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Silent background check on launch
    LaunchedEffect(Unit) {
        delay(1200)
        try {
            val result = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
            if (result is UpdateCheckResult.UpdateAvailable) {
                launchUpdateRelease = result.release
                showLaunchUpdateSheet = true
            }
        } catch (_: Exception) {}
    }

    // System Back Navigation: Switch non-library tabs back to Library
    BackHandler(enabled = !isReaderOpen && currentTab != MulberryTab.LIBRARY) {
        viewModel.setTab(MulberryTab.LIBRARY)
    }

    // System Back Navigation: On root Library tab, require double press to exit
    val isAtRootLibrary = !isReaderOpen && currentTab == MulberryTab.LIBRARY && activeBreadcrumbs.size <= 1 && activeFolderFilter == null
    BackHandler(enabled = isAtRootLibrary) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val showBottomBar = !isReaderOpen

    val initialPageIndex = remember { visibleTabs.indexOf(currentTab).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { visibleTabs.size }
    )

    // Synchronize programmatic tab changes to the pager immediately
    LaunchedEffect(currentTab, visibleTabs) {
        val targetIndex = visibleTabs.indexOf(currentTab)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Synchronize sideways user sliding gestures back to viewModel's currentTab.
    // By observing settledPage, we only update the selected tab AFTER a swipe finishes,
    // avoiding race conditions during multi-page programmatic navigation.
    LaunchedEffect(pagerState, visibleTabs) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledIndex ->
                val newTab = visibleTabs.getOrNull(settledIndex)
                if (newTab != null && newTab != currentTab) {
                    viewModel.setTab(newTab)
                }
            }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

    Box(modifier = Modifier.fillMaxSize()) {
        if (isWideScreen && showBottomBar) {
            Row(modifier = Modifier.fillMaxSize()) {
                MulberryNavigationRail(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        viewModel.setTab(tab)
                        val targetIndex = visibleTabs.indexOf(tab)
                        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(targetIndex)
                            }
                        }
                    },
                    tabs = visibleTabs,
                    onTabLongClick = { viewModel.openCustomizeTabsSheet() }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        val tab = visibleTabs.getOrNull(pageIndex) ?: MulberryTab.LIBRARY
                        when (tab) {
                            MulberryTab.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.SNIPS -> SnipsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.DESK -> DeskRootScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        MulberryBottomNavBar(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                viewModel.setTab(tab)
                                val targetIndex = visibleTabs.indexOf(tab)
                                if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetIndex)
                                    }
                                }
                            },
                            tabs = visibleTabs,
                            onTabLongClick = { viewModel.openCustomizeTabsSheet() }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        val tab = visibleTabs.getOrNull(pageIndex) ?: MulberryTab.LIBRARY
                        when (tab) {
                            MulberryTab.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.SNIPS -> SnipsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.DESK -> DeskRootScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                            MulberryTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // PDF Reader Activity Launch
        LaunchedEffect(isReaderOpen, currentBook) {
            if (isReaderOpen && currentBook != null) {
                val book = currentBook!!
                ReaderActivity.launch(
                    context = context,
                    bookPath = book.binFilePath.ifEmpty { book.uriString },
                    bookTitle = book.title,
                    bookId = book.id
                )
                viewModel.closeReader()
            }
        }

        // Silent Launch Update Sheet
        if (showLaunchUpdateSheet && launchUpdateRelease != null) {
            UpdateBottomSheet(
                release = launchUpdateRelease!!,
                currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.0.0" },
                downloadState = launchDownloadState,
                onStartDownload = {
                    coroutineScope.launch {
                        UpdateManager.downloadApk(
                            context = context,
                            release = launchUpdateRelease!!,
                            onProgress = { state -> launchDownloadState = state }
                        )
                    }
                },
                onCancelDownload = {
                    launchDownloadState = DownloadState.Idle
                },
                onInstall = {
                    val completed = launchDownloadState as? DownloadState.Completed
                    if (completed != null) {
                        UpdateManager.installApk(context, completed.apkFile)
                    }
                },
                onDismiss = {
                    showLaunchUpdateSheet = false
                    launchDownloadState = DownloadState.Idle
                }
            )
        }

        // Tab Customization Modal
        if (showCustomizeTabsSheet) {
            CustomizeTabsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.closeCustomizeTabsSheet() }
            )
        }
    }
}
