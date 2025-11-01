package com.readle.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.readle.app.R
import com.readle.app.data.api.model.BookSearchResult
import com.readle.app.data.model.ReadingCategory
import com.readle.app.ui.viewmodel.AddBookUiState
import com.readle.app.ui.viewmodel.AddBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    scannedIsbn: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onIsbnProcessed: () -> Unit = {},
    viewModel: AddBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isbn by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var series by remember { mutableStateOf("") }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<BookSearchResult?>(null) }
    var selectedBooks by remember { mutableStateOf<Set<BookSearchResult>>(emptySet()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // Process scanned ISBN
    LaunchedEffect(scannedIsbn) {
        if (!scannedIsbn.isNullOrBlank()) {
            isbn = scannedIsbn
            viewModel.searchByIsbn(scannedIsbn)
            onIsbnProcessed()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddBookUiState.Success) {
            // Reset state and stay on this screen
            viewModel.resetState()
        }
    }

    // Auto-add single result from ISBN search (no dialog)
    LaunchedEffect(uiState, isbn) {
        if (uiState is AddBookUiState.SearchResults && isbn.isNotBlank()) {
            val searchResults = uiState as AddBookUiState.SearchResults
            val results = searchResults.results
            
            // Auto-add if ISBN search returns exactly 1 result
            if (results.size == 1) {
                val book = results.first()
                val isAlreadyInLibrary = searchResults.alreadyInLibrary[book] ?: false
                
                if (!isAlreadyInLibrary) {
                    // Auto-add with default values (not owned, not read, no rating)
                    viewModel.addBookToLibrary(book, isOwned = false, isRead = false, rating = 0)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.ime.union(WindowInsets.navigationBars),
        floatingActionButton = {
            if (isMultiSelectMode && selectedBooks.isNotEmpty()) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        showCategoryDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${selectedBooks.size}")
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { 
                    if (isMultiSelectMode) {
                        Text("${selectedBooks.size} ${stringResource(R.string.label_selected)}")
                    } else {
                        Text(stringResource(R.string.action_add_book))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isMultiSelectMode) {
                            // Exit multi-select mode
                            isMultiSelectMode = false
                            selectedBooks = emptySet()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isMultiSelectMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        TextButton(onClick = {
                            // Select all visible results
                            val searchResults = (uiState as? AddBookUiState.SearchResults)
                            if (searchResults != null) {
                                selectedBooks = searchResults.results.toSet()
                            }
                        }) {
                            Text(stringResource(R.string.action_select_all))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // ISBN input with scan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    label = { Text("ISBN") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.action_scan_isbn),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Search by ISBN button (always visible)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.searchByIsbn(isbn) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isbn.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_search) + " ISBN")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Author input
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.field_author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Series input
            OutlinedTextField(
                value = series,
                onValueChange = { series = it },
                label = { Text(stringResource(R.string.field_series)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Search by title/author/series button (always visible)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.searchByTitleAuthor(title, author, series) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() || author.isNotBlank() || series.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_search) + " " + stringResource(R.string.field_title) + "/" + stringResource(R.string.field_author) + "/" + stringResource(R.string.field_series))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is AddBookUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AddBookUiState.SearchResults -> {
                    val searchResults = (uiState as AddBookUiState.SearchResults)
                    val results = searchResults.results
                    val alreadyInLibrary = searchResults.alreadyInLibrary
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { book ->
                            SearchResultItem(
                                book = book,
                                isSelected = selectedBooks.contains(book),
                                isMultiSelectMode = isMultiSelectMode,
                                isAlreadyInLibrary = alreadyInLibrary[book] ?: false,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        // Toggle selection
                                        selectedBooks = if (selectedBooks.contains(book)) {
                                            selectedBooks - book
                                        } else {
                                            selectedBooks + book
                                        }
                                    } else {
                                        // Single select - open dialog immediately
                                        selectedBook = book
                                        showCategoryDialog = true
                                    }
                                },
                                onLongClick = {
                                    // Enter multi-select mode
                                    isMultiSelectMode = true
                                    selectedBooks = setOf(book)
                                }
                            )
                        }
                    }
                }
                is AddBookUiState.Error -> {
                    Text(
                        text = (uiState as AddBookUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {}
            }
        }
    }

    if (showCategoryDialog) {
        val searchResults = uiState as? AddBookUiState.SearchResults
        val existingStatus = selectedBook?.let { searchResults?.existingBookStatus?.get(it) }
        val isAlreadyInLibrary = selectedBook?.let { searchResults?.alreadyInLibrary?.get(it) } ?: false
        
        CategorySelectionDialog(
            book = selectedBook,
            books = selectedBooks.toList(),
            existingStatus = existingStatus,
            isAlreadyInLibrary = isAlreadyInLibrary,
            onDismiss = { showCategoryDialog = false },
            onStatusSelected = { isOwned, isRead, rating ->
                if (isMultiSelectMode && selectedBooks.isNotEmpty()) {
                    // Add all selected books
                    selectedBooks.forEach { book ->
                        viewModel.addBookToLibrary(book, isOwned, isRead, rating)
                    }
                    // Exit multi-select mode
                    isMultiSelectMode = false
                    selectedBooks = emptySet()
                } else if (selectedBook != null) {
                    // Single book
                    viewModel.addBookToLibrary(selectedBook!!, isOwned, isRead, rating)
                }
                showCategoryDialog = false
            }
        )
    }
}

@Composable
fun ScanIsbnTab(
    onNavigateToScanner: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onNavigateToScanner,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_scan_isbn))
        }
    }
}

@Composable
fun EnterIsbnTab(
    isbn: String,
    onIsbnChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = isbn,
            onValueChange = onIsbnChange,
            label = { Text(stringResource(R.string.search_isbn_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = isbn.isNotBlank()
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_search))
        }
    }
}

@Composable
fun SearchTitleAuthorTab(
    title: String,
    author: String,
    series: String,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onSeriesChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = author,
            onValueChange = onAuthorChange,
            label = { Text(stringResource(R.string.field_author)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = series,
            onValueChange = onSeriesChange,
            label = { Text(stringResource(R.string.field_series)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() || author.isNotBlank() || series.isNotBlank()
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_search))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    book: BookSearchResult,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    isAlreadyInLibrary: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !isAlreadyInLibrary,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAlreadyInLibrary -> MaterialTheme.colorScheme.surfaceVariant
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAlreadyInLibrary) 0.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox in multi-select mode
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null // Handled by card click
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (book.coverUrl != null) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = stringResource(R.string.cd_book_cover),
                        modifier = Modifier.size(60.dp, 90.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAlreadyInLibrary)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAlreadyInLibrary)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            color = if (isAlreadyInLibrary)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAlreadyInLibrary) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.msg_already_in_library),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // API Source Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = when (book.source) {
                        com.readle.app.data.api.model.BookDataSource.DNB -> "DNB"
                        com.readle.app.data.api.model.BookDataSource.GOOGLE_BOOKS -> "Google"
                        com.readle.app.data.api.model.BookDataSource.ISBN_DB -> "ISBN"
                        com.readle.app.data.api.model.BookDataSource.OPEN_LIBRARY -> "OpenLib"
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun CategorySelectionDialog(
    book: BookSearchResult? = null,
    books: List<BookSearchResult> = emptyList(),
    existingStatus: Pair<Boolean, Boolean>? = null,  // (isOwned, isRead)
    isAlreadyInLibrary: Boolean = false,
    onDismiss: () -> Unit,
    onStatusSelected: (Boolean, Boolean, Int) -> Unit  // (isOwned, isRead, rating)
) {
    var isOwned by remember { mutableStateOf(true) }
    var isRead by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(0) }

    // Reset rating when isRead changes to false
    LaunchedEffect(isRead) {
        if (!isRead) {
            rating = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (isAlreadyInLibrary) stringResource(R.string.msg_already_in_library)
                else stringResource(R.string.label_select_category)
            )
        },
        text = {
            Column {
                // Show book information at the top for single book selection
                if (book != null && books.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (book.coverUrl != null) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = stringResource(R.string.cd_book_cover),
                                modifier = Modifier.size(60.dp, 90.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Show series information if available
                            if (!book.series.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!book.seriesNumber.isNullOrBlank()) {
                                        "${book.series} #${book.seriesNumber}"
                                    } else {
                                        book.series
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                } else if (books.isNotEmpty()) {
                    // Show count for multiple books
                    Text(
                        text = "${books.size} ${stringResource(R.string.label_selected)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Show existing status if book is already in library
                if (isAlreadyInLibrary && existingStatus != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.label_current_status),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    if (existingStatus.first) append("Besessen")
                                    if (existingStatus.first && existingStatus.second) append(" & ")
                                    if (existingStatus.second) append("Gelesen")
                                    if (!existingStatus.first && !existingStatus.second) append("Wunschliste")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Switches for owned and read status
                Text(stringResource(R.string.label_book_status))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Besessen")
                    Switch(
                        checked = isOwned,
                        onCheckedChange = { isOwned = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gelesen")
                    Switch(
                        checked = isRead,
                        onCheckedChange = { isRead = it }
                    )
                }

                // Rating (only if read)
                if (isRead) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.label_rating_optional))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = stringResource(R.string.label_stars, star),
                                    tint = if (star <= rating) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStatusSelected(isOwned, isRead, rating) }) {
                Text(stringResource(R.string.action_add_book))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}


