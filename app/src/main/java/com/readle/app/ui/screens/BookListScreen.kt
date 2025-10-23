package com.readle.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import com.readle.app.ui.icons.Newsstand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.readle.app.R
import com.readle.app.data.model.BookEntity
import com.readle.app.ui.viewmodel.BookListViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    onNavigateToAddBook: () -> Unit,
    onNavigateToEditBook: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    initialFilterText: String? = null,
    viewModel: BookListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ownedFilter by viewModel.ownedFilter.collectAsState()
    val readFilter by viewModel.readFilter.collectAsState()
    val books by viewModel.books.collectAsState()
    val selectedBooks by viewModel.selectedBooks.collectAsState()
    val viewFilter by viewModel.viewFilter.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val lastSwipeAction by viewModel.lastSwipeAction.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showReuploadConfirmDialog by remember { mutableStateOf(false) }
    var hasPocketbookCreds by remember { mutableStateOf(false) }
    var showUndoButton by remember { mutableStateOf(false) }
    
    // Auto-hide undo button after 10 seconds
    LaunchedEffect(lastSwipeAction) {
        if (lastSwipeAction != null) {
            showUndoButton = true
            kotlinx.coroutines.delay(10000) // 10 seconds
            showUndoButton = false
            viewModel.clearLastSwipeAction()
        } else {
            showUndoButton = false
        }
    }
    
    // Apply initial filter text if provided
    LaunchedEffect(initialFilterText) {
        if (initialFilterText != null) {
            viewModel.setViewFilter(initialFilterText)
        }
    }

    // Check if Pocketbook credentials are set
    LaunchedEffect(Unit) {
        hasPocketbookCreds = viewModel.hasPocketbookCredentials()
    }

    // Handle upload state changes
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is com.readle.app.ui.viewmodel.UploadState.Success -> {
                val state = uploadState as com.readle.app.ui.viewmodel.UploadState.Success
                Toast.makeText(
                    context,
                    "Uploaded ${state.uploaded} book(s) to Pocketbook Cloud${if (state.failed > 0) " (${state.failed} failed)" else ""}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetUploadState()
            }
            is com.readle.app.ui.viewmodel.UploadState.Error -> {
                val state = uploadState as com.readle.app.ui.viewmodel.UploadState.Error
                Toast.makeText(
                    context,
                    "Upload failed: ${state.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetUploadState()
            }
            else -> {}
        }
    }

    // Calculate canUpload reactively based on selected books
    val canUpload = remember(selectedBooks, books) {
        if (selectedBooks.isEmpty()) {
            false
        } else {
            val selectedBookEntities = books.filter { selectedBooks.contains(it.id) }
            selectedBookEntities.all { !it.audiobookshelfId.isNullOrBlank() }
        }
    }

    // Handle back button to deselect books
    BackHandler(enabled = selectedBooks.isNotEmpty()) {
        viewModel.deselectAllBooks()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "(${books.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        TextField(
                            value = viewFilter,
                            onValueChange = { viewModel.setViewFilter(it) },
                            placeholder = { 
                                Text(
                                    text = stringResource(R.string.hint_filter_category),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_filter),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (viewFilter.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setViewFilter("") },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_clear_filter),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                    if (selectedBooks.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription
                                    = stringResource(R.string.cd_more_options))
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_select_all)) },
                                    onClick = {
                                        viewModel.selectAllBooks()
                                        showOptionsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_deselect_all)) },
                                    onClick = {
                                        viewModel.deselectAllBooks()
                                        showOptionsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete_selected)) },
                                    onClick = {
                                        showDeleteDialog = true
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedBooks.isNotEmpty()) {
                // Determine action based on current state of selected books
                val selectedBookEntities = books.filter { selectedBooks.contains(it.id) }
                val allAreOwned = selectedBookEntities.all { it.isOwned }
                val allAreRead = selectedBookEntities.all { it.isRead }
                
                // If all are owned/read, action will be to set to false. Otherwise, set to true.
                val willSetOwnedTo = !allAreOwned
                val willSetReadTo = !allAreRead
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel selection button
                    SmallFloatingActionButton(
                        onClick = { viewModel.deselectAllBooks() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Toggle Owned button - shows what will be set
                    SmallFloatingActionButton(
                        onClick = { viewModel.toggleSelectedBooksOwned() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = if (willSetOwnedTo) Icons.Filled.Newsstand else Icons.Default.Remove,
                            contentDescription = if (willSetOwnedTo) "Mark as owned" else "Mark as not owned",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Toggle Read button - shows what will be set
                    SmallFloatingActionButton(
                        onClick = { viewModel.toggleSelectedBooksRead() },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(
                            imageVector = if (willSetReadTo) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (willSetReadTo) "Mark as read" else "Mark as not read",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Delete button
                    SmallFloatingActionButton(
                        onClick = { showDeleteDialog = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    
                    // Upload button
                    val uploadEnabled = hasPocketbookCreds && canUpload
                    val isUploading = uploadState is com.readle.app.ui.viewmodel.UploadState.Progress
                    SmallFloatingActionButton(
                        onClick = {
                            if (uploadEnabled && !isUploading) {
                                // Check if any selected books are already sent to Pocketbook
                                if (viewModel.hasAlreadySentBooks()) {
                                    showReuploadConfirmDialog = true
                                } else {
                                    viewModel.uploadSelectedBooksToPocketbook()
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                            alpha = if (uploadEnabled && !isUploading) 1f else 0.38f
                        ),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                            alpha = if (uploadEnabled && !isUploading) 1f else 0.38f
                        )
                    ) {
                        if (isUploading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                        }
                    }
                }
            } else {
                FloatingActionButton(onClick = onNavigateToAddBook) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_book))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Filter Chips (instead of Tabs)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Owned Filter Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_owned),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(60.dp)
                    )
                    androidx.compose.material3.FilterChip(
                        selected = ownedFilter == com.readle.app.ui.viewmodel.FilterState.ALL,
                        onClick = { viewModel.setOwnedFilter(com.readle.app.ui.viewmodel.FilterState.ALL) },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = ownedFilter == com.readle.app.ui.viewmodel.FilterState.YES,
                        onClick = { viewModel.setOwnedFilter(com.readle.app.ui.viewmodel.FilterState.YES) },
                        label = { Text(stringResource(R.string.filter_yes)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = ownedFilter == com.readle.app.ui.viewmodel.FilterState.NO,
                        onClick = { viewModel.setOwnedFilter(com.readle.app.ui.viewmodel.FilterState.NO) },
                        label = { Text(stringResource(R.string.filter_no)) }
                    )
                }
                
                // Read Filter Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_read),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(60.dp)
                    )
                    androidx.compose.material3.FilterChip(
                        selected = readFilter == com.readle.app.ui.viewmodel.FilterState.ALL,
                        onClick = { viewModel.setReadFilter(com.readle.app.ui.viewmodel.FilterState.ALL) },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = readFilter == com.readle.app.ui.viewmodel.FilterState.YES,
                        onClick = { viewModel.setReadFilter(com.readle.app.ui.viewmodel.FilterState.YES) },
                        label = { Text(stringResource(R.string.filter_yes)) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = readFilter == com.readle.app.ui.viewmodel.FilterState.NO,
                        onClick = { viewModel.setReadFilter(com.readle.app.ui.viewmodel.FilterState.NO) },
                        label = { Text(stringResource(R.string.filter_no)) }
                    )
                }
            }

            if (books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.msg_no_books),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(books, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            isSelected = selectedBooks.contains(book.id),
                            onBookClick = { onNavigateToEditBook(book.id) },
                            onBookLongClick = { viewModel.toggleBookSelection(book.id) },
                            onSelectionToggle = { viewModel.toggleBookSelection(book.id) },
                            onToggleOwned = { viewModel.toggleBookOwned(book.id) },
                            onToggleRead = { viewModel.toggleBookRead(book.id) },
                            showCheckbox = selectedBooks.isNotEmpty()
                        )
                    }

                    // Extra space at the bottom so last items are visible above keyboard
                    item { Spacer(modifier = Modifier.height(300.dp)) }
                }
            }
        }
            
            // Undo button - positioned absolutely at bottom-left
            AnimatedVisibility(
                visible = showUndoButton && selectedBooks.isEmpty(),
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { 
                        viewModel.undoLastSwipe()
                        showUndoButton = false
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_multiple_title)) },
            text = { Text(stringResource(R.string.dialog_delete_multiple_message, selectedBooks.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelectedBooks()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.action_delete_book))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Move dialog removed - no more categories


    // Reupload confirmation dialog
    if (showReuploadConfirmDialog) {
        val alreadySentCount = viewModel.getAlreadySentBooksCount()
        AlertDialog(
            onDismissRequest = { showReuploadConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_reupload_title)) },
            text = {
                Text(
                    if (alreadySentCount == 1) {
                        stringResource(R.string.dialog_reupload_single)
                    } else {
                        stringResource(R.string.dialog_reupload_multiple, alreadySentCount)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.uploadSelectedBooksToPocketbook(forceReupload = true)
                    showReuploadConfirmDialog = false
                }) {
                    Text(stringResource(R.string.action_resend))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReuploadConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Upload progress dialog
    if (uploadState is com.readle.app.ui.viewmodel.UploadState.Progress) {
        val progressState = uploadState as com.readle.app.ui.viewmodel.UploadState.Progress
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss during upload */ },
            title = { Text(stringResource(R.string.dialog_upload_title)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.msg_uploading_book, progressState.currentIndex, progressState.totalBooks),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = progressState.currentBook,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = progressState.currentIndex.toFloat() / progressState.totalBooks.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun BookListItem(
    book: BookEntity,
    isSelected: Boolean,
    onBookClick: () -> Unit,
    onBookLongClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onToggleOwned: () -> Unit,
    onToggleRead: () -> Unit,
    showCheckbox: Boolean
) {
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 100.dp.toPx() }
    
    val anchors = mapOf(
        -swipeThresholdPx to -1,  // Swipe left → toggle owned
        0f to 0,                  // Center (neutral)
        swipeThresholdPx to 1     // Swipe right → toggle read
    )
    
    // Calculate alpha based on swipe progress
    val leftSwipeAlpha = (swipeableState.offset.value / -swipeThresholdPx).coerceIn(0f, 1f)
    val rightSwipeAlpha = (swipeableState.offset.value / swipeThresholdPx).coerceIn(0f, 1f)
    
    // Handle toggle on swipe
    LaunchedEffect(swipeableState.currentValue) {
        when (swipeableState.currentValue) {
            -1 -> {  // Swiped left - toggle owned
                onToggleOwned()
                swipeableState.snapTo(0)
            }
            1 -> {  // Swiped right - toggle read
                onToggleRead()
                swipeableState.snapTo(0)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Swipe background layer - behind the card
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Check/Close icon (visually on left, but swipe RIGHT toggles READ)
            // Green background if marking as read, red background if marking as unread
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        if (book.isRead) {
                            Color(0xFFEF5350).copy(alpha = 0.7f) // Red for "mark as unread"
                        } else {
                            Color(0xFF66BB6A).copy(alpha = 0.7f) // Green for "mark as read"
                        },
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (book.isRead) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = "Toggle Read (swipe right)",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Right side: Newsstand icon (visually on right, but swipe LEFT toggles OWNED)
            // Green background if marking as owned, red background if marking as not owned
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        if (book.isOwned) {
                            Color(0xFFEF5350).copy(alpha = 0.7f) // Red for "mark as not owned"
                        } else {
                            Color(0xFF66BB6A).copy(alpha = 0.7f) // Green for "mark as owned"
                        },
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Newsstand,
                    contentDescription = "Toggle Owned (swipe left)",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Card on top - will slide over the background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    orientation = Orientation.Horizontal,
                    enabled = !showCheckbox
                )
                .combinedClickable(
                    onClick = if (showCheckbox) onSelectionToggle else onBookClick,
                    onLongClick = onBookLongClick
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box {
                // Status icons in top-right corner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Owned icon
                    if (book.isOwned) {
                        Icon(
                            imageVector = Icons.Filled.Newsstand,
                            contentDescription = "Owned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    // Read icon
                    if (book.isRead) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Read",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    // Cloud icon (if uploaded via email to Pocketbook)
                    if (book.uploadedViaEmail) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "In Pocketbook Cloud",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCheckbox) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 44.dp) // Space for max 2 icons (16dp each + 4dp gap + 8dp margin)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            if (book.rating > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                repeat(book.rating) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (book.series != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val seriesText = if (book.seriesNumber != null) {
                                "${book.series} #${book.seriesNumber}"
                            } else {
                                book.series
                            }
                            Text(
                                text = seriesText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

