package com.example.ui.screens.desk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.desk.DeskRepository
import com.example.viewmodel.MulberryViewModel
import java.io.File

sealed interface DeskScreenDestination {
    data object Root : DeskScreenDestination
    data class FolderDetail(val folderName: String) : DeskScreenDestination
    data class NoteEditor(val folderName: String, val file: File) : DeskScreenDestination
}

@Composable
fun DeskNavigationHost(
    viewModel: MulberryViewModel,
    onOpenBookAtPage: (bookName: String, page: Int) -> Unit,
    onEditorActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { DeskRepository(context) }
    var currentDestination by remember { mutableStateOf<DeskScreenDestination>(DeskScreenDestination.Root) }
    val activeVault by viewModel.activeVault.collectAsState()

    // Dynamically bind DeskRepository to active vault directory
    LaunchedEffect(activeVault) {
        if (activeVault != null) {
            val rootPath = activeVault!!.pathDisplay.ifEmpty { activeVault!!.uriString }
            val dir = File(rootPath)
            repository.setVaultDirectory(dir)
        }
    }

    // Notify parent activity whether NoteEditor is active to hide/show bottom tabs
    LaunchedEffect(currentDestination) {
        onEditorActiveChanged(currentDestination is DeskScreenDestination.NoteEditor)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEditorActiveChanged(false)
        }
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
        },
        label = "DeskNavigation",
        modifier = modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            is DeskScreenDestination.Root -> {
                DeskRootScreen(
                    viewModel = viewModel,
                    repository = repository,
                    onOpenFolder = { folderName ->
                        currentDestination = DeskScreenDestination.FolderDetail(folderName)
                    },
                    onOpenNote = { folderName, file ->
                        currentDestination = DeskScreenDestination.NoteEditor(folderName, file)
                    },
                    onOpenBookAtPage = onOpenBookAtPage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is DeskScreenDestination.FolderDetail -> {
                FolderDetailScreen(
                    folderName = destination.folderName,
                    repository = repository,
                    viewModel = viewModel,
                    onBack = {
                        currentDestination = DeskScreenDestination.Root
                    },
                    onOpenNote = { file ->
                        currentDestination = DeskScreenDestination.NoteEditor(destination.folderName, file)
                    },
                    onOpenBook = { bookName ->
                        onOpenBookAtPage(bookName, 1)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is DeskScreenDestination.NoteEditor -> {
                NoteEditorScreen(
                    file = destination.file,
                    folderName = destination.folderName,
                    repository = repository,
                    viewModel = viewModel,
                    onBack = {
                        currentDestination = DeskScreenDestination.FolderDetail(destination.folderName)
                    },
                    onOpenBookAtPage = onOpenBookAtPage,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
