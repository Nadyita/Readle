package com.readle.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readle.app.R
import com.readle.app.data.model.BookEntity
import com.readle.app.data.model.ReadingCategory
import com.readle.app.ui.util.htmlToAnnotatedString
import com.readle.app.ui.viewmodel.EditBookUiState
import com.readle.app.ui.viewmodel.EditBookViewModel
import com.readle.app.ui.viewmodel.EditBookUploadState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    onFilterByText: (String) -> Unit = {},
    viewModel: EditBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val isEmailConfigured by viewModel.isEmailConfigured.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isEditMode by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var series by remember { mutableStateOf("") }
    var seriesNumber by remember { mutableStateOf("") }
    var isEBook by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(0) }
    var isOwned by remember { mutableStateOf(true) }
    var isRead by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is EditBookUiState.BookLoaded -> {
                val book = state.book
                title = book.title
                author = book.author
                description = book.description ?: ""
                series = book.series ?: ""
                seriesNumber = book.seriesNumber ?: ""
                isEBook = book.isEBook
                comments = book.comments ?: ""
                rating = book.rating
                isOwned = book.isOwned
                isRead = book.isRead
            }
            is EditBookUiState.Success -> {
                isEditMode = false
                onNavigateBack()
            }
            else -> {}
        }
    }

    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is EditBookUploadState.Success -> {
                snackbarHostState.showSnackbar("Buch erfolgreich hochgeladen")
                viewModel.resetUploadState()
            }
            is EditBookUploadState.Error -> {
                snackbarHostState.showSnackbar("Upload fehlgeschlagen: ${state.message}")
                viewModel.resetUploadState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_edit_book)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_book))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            is EditBookUiState.Loading, is EditBookUiState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is EditBookUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (uiState as EditBookUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title field
                    if (isEditMode) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.field_title)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.field_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Author field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = author,
                                onValueChange = { author = it },
                                label = { Text(stringResource(R.string.field_author)) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.field_author),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        
                        if (author.isNotBlank()) {
                            IconButton(onClick = { 
                                onFilterByText("author=$author")
                                onNavigateBack()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Filter books by this author"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Series field - only show in edit mode or if series is not blank
                    if (isEditMode || series.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode) {
                                OutlinedTextField(
                                    value = series,
                                    onValueChange = { series = it },
                                    label = { Text(stringResource(R.string.field_series)) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.field_series),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = series,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                            
                            if (series.isNotBlank()) {
                                IconButton(onClick = { 
                                    onFilterByText("series=$series")
                                    onNavigateBack()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Filter books in this series"
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Series Number and eBook
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Series Number - only show in edit mode or if series number is not blank
                        if (isEditMode || seriesNumber.isNotBlank()) {
                            if (isEditMode) {
                                OutlinedTextField(
                                    value = seriesNumber,
                                    onValueChange = { seriesNumber = it },
                                    label = { Text(stringResource(R.string.field_series_number)) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.field_series_number),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = seriesNumber,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (isEditMode || seriesNumber.isNotBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isEBook,
                                onCheckedChange = if (isEditMode) { { isEBook = it } } else null,
                                enabled = isEditMode
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.field_is_ebook))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display description as HTML (always read-only)
                    if (description.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.field_description),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = description.htmlToAnnotatedString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = if (isEditMode) {
                                        Modifier
                                            .padding(12.dp)
                                            .heightIn(max = 120.dp)
                                            .verticalScroll(scrollState)
                                    } else {
                                        Modifier.padding(12.dp)
                                    },
                                    maxLines = Int.MAX_VALUE
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comments field
                    if (isEditMode) {
                        OutlinedTextField(
                            value = comments,
                            onValueChange = { comments = it },
                            label = { Text(stringResource(R.string.field_comments)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    } else if (comments.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.field_comments),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = comments,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Only show rating for books that have been read
                    if (isRead) {
                        Text(stringResource(R.string.label_rating_label), style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { 
                                        rating = star
                                        // Auto-save rating in view mode
                                        if (!isEditMode) {
                                            viewModel.updateRating(bookId, star)
                                            // Show snackbar notification
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.msg_rating_updated, star)
                                                )
                                            }
                                        }
                                    }
                                ) {
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    if (isEditMode) {
                        // Edit mode: Show Save and Cancel buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    // Reset to loaded book values
                                    when (val state = uiState) {
                                        is EditBookUiState.BookLoaded -> {
                                            val book = state.book
                                            title = book.title
                                            author = book.author
                                            description = book.description ?: ""
                                            series = book.series ?: ""
                                            seriesNumber = book.seriesNumber ?: ""
                                            isEBook = book.isEBook
                                            comments = book.comments ?: ""
                                            rating = book.rating
                                            isOwned = book.isOwned
                                            isRead = book.isRead
                                        }
                                        else -> {}
                                    }
                                    isEditMode = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }

                            Button(
                                onClick = {
                                    viewModel.updateBook(
                                        bookId = bookId,
                                        title = title,
                                        author = author,
                                        description = description.ifBlank { null },
                                        publishDate = null,
                                        language = null,
                                        originalLanguage = null,
                                        series = series.ifBlank { null },
                                        seriesNumber = seriesNumber.ifBlank { null },
                                        isEBook = isEBook,
                                        comments = comments.ifBlank { null },
                                        rating = rating,
                                        isOwned = isOwned,
                                        isRead = isRead
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = title.isNotBlank() && author.isNotBlank()
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    } else {
                        // View mode: Show Upload button (only for eBooks and if email is configured)
                        val currentBook = (uiState as? EditBookUiState.BookLoaded)?.book
                        if (currentBook?.isEBook == true && isEmailConfigured) {
                            Button(
                                onClick = { viewModel.uploadBookToPocketbook(bookId) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uploadState !is EditBookUploadState.Uploading
                            ) {
                                if (uploadState is EditBookUploadState.Uploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.action_upload_to_pocketbook))
                            }
                        }
                    }
                }
            }
        }
    }
}

