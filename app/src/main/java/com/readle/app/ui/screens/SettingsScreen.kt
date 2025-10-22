package com.readle.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.readle.app.R
import com.readle.app.data.preferences.ExportFormat
import com.readle.app.data.preferences.ScannerLibrary
import com.readle.app.data.preferences.ThemeMode
import com.readle.app.ui.viewmodel.ImportExportState
import com.readle.app.ui.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmailSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val scannerLibrary by viewModel.scannerLibrary.collectAsState()
    val importExportState by viewModel.importExportState.collectAsState()
    val dnbApiEnabled by viewModel.dnbApiEnabled.collectAsState()
    val googleBooksApiEnabled by viewModel.googleBooksApiEnabled.collectAsState()
    val isbnDbApiEnabled by viewModel.isbnDbApiEnabled.collectAsState()
    val absServerUrl by viewModel.audiobookshelfServerUrl.collectAsState()
    val absApiToken by viewModel.audiobookshelfApiToken.collectAsState()
    val absLoginState by viewModel.audiobookshelfLoginState.collectAsState()
    val absImportState by viewModel.audiobookshelfImportState.collectAsState()
    val pocketbookEmail by viewModel.pocketbookEmail.collectAsState()
    val pocketbookPassword by viewModel.pocketbookPassword.collectAsState()
    val pocketbookSyncState by viewModel.pocketbookSyncState.collectAsState()
    val pocketbookCleanTitles by viewModel.pocketbookCleanTitles.collectAsState()
    
    // Email settings
    val smtpServer by viewModel.smtpServer.collectAsState()
    val smtpPort by viewModel.smtpPort.collectAsState()
    val smtpUsername by viewModel.smtpUsername.collectAsState()
    val smtpPassword by viewModel.smtpPassword.collectAsState()
    val smtpFromEmail by viewModel.smtpFromEmail.collectAsState()
    val pocketbookSendToEmail by viewModel.pocketbookSendToEmail.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }
    var exportTargetUri by remember { mutableStateOf<Uri?>(null) }
    var showAbsLoginDialog by remember { mutableStateOf(false) }
    var absServerUrlInput by remember { mutableStateOf(absServerUrl) }
    var pocketbookEmailInput by remember { mutableStateOf(pocketbookEmail) }
    var pocketbookPasswordInput by remember { mutableStateOf(pocketbookPassword) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    // Email input states - initialize with empty strings, LaunchedEffect will load saved values
    var smtpServerInput by remember { mutableStateOf("") }
    var smtpPortInput by remember { mutableStateOf("587") }
    var smtpUsernameInput by remember { mutableStateOf("") }
    var smtpPasswordInput by remember { mutableStateOf("") }
    var smtpFromEmailInput by remember { mutableStateOf("") }
    var pocketbookSendToEmailInput by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportTargetUri = uri
            viewModel.exportBooks()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFile = uri
            showImportDialog = true
        }
    }

    LaunchedEffect(importExportState) {
        when (val state = importExportState) {
            is ImportExportState.ExportSuccess -> {
                exportTargetUri?.let { targetUri ->
                    try {
                        // Copy the internal file to the user-selected location
                        context.contentResolver.openOutputStream(targetUri)?.use { output ->
                            state.file.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Optionally share the file
                        val shareUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            state.file
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, 
                            context.getString(R.string.action_share_export)))
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsScreen", "Failed to copy export: ${e.message}", e)
                    }
                }
                
                exportTargetUri = null
                viewModel.resetImportExportState()
            }
            is ImportExportState.ImportSuccess -> {
                viewModel.resetImportExportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(absLoginState) {
        when (val state = absLoginState) {
            is com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Success -> {
                showAbsLoginDialog = false
                viewModel.resetAudiobookshelfLoginState()
            }
            else -> {}
        }
    }

    LaunchedEffect(absServerUrl) {
        absServerUrlInput = absServerUrl
    }

    LaunchedEffect(pocketbookEmail) {
        pocketbookEmailInput = pocketbookEmail
    }

    LaunchedEffect(pocketbookPassword) {
        pocketbookPasswordInput = pocketbookPassword
    }

    // Sync SMTP fields - EXACTLY like the other fields above
    LaunchedEffect(smtpServer) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(smtpServer): Loading '$smtpServer' into input (was: '$smtpServerInput')")
        smtpServerInput = smtpServer
    }
    
    LaunchedEffect(smtpPort) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(smtpPort): Loading '$smtpPort' into input (was: '$smtpPortInput')")
        smtpPortInput = smtpPort.toString()
    }
    
    LaunchedEffect(smtpUsername) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(smtpUsername): Loading '$smtpUsername' into input (was: '$smtpUsernameInput')")
        smtpUsernameInput = smtpUsername
    }
    
    LaunchedEffect(smtpPassword) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(smtpPassword): Loading password into input")
        smtpPasswordInput = smtpPassword
    }
    
    LaunchedEffect(smtpFromEmail) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(smtpFromEmail): Loading '$smtpFromEmail' into input (was: '$smtpFromEmailInput')")
        smtpFromEmailInput = smtpFromEmail
    }
    
    LaunchedEffect(pocketbookSendToEmail) {
        android.util.Log.d("SettingsScreen", "LaunchedEffect(pocketbookSendToEmail): Loading '$pocketbookSendToEmail' into input (was: '$pocketbookSendToEmailInput')")
        pocketbookSendToEmailInput = pocketbookSendToEmail
    }

    LaunchedEffect(absImportState) {
        when (val state = absImportState) {
            is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Success -> {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.abs_import_success,
                        state.imported,
                        state.updated,
                        state.total
                    ),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAudiobookshelfImportState()
            }
            is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Error -> {
                // Error is shown in UI directly
            }
            else -> {}
        }
    }

    LaunchedEffect(pocketbookEmail) {
        pocketbookEmailInput = pocketbookEmail
    }

    LaunchedEffect(pocketbookPassword) {
        pocketbookPasswordInput = pocketbookPassword
    }

    var showUnmatchedBooksDialog by remember { mutableStateOf(false) }
    var unmatchedBooks by remember { mutableStateOf<List<com.readle.app.data.api.pocketbook.PocketbookBook>>(emptyList()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pocketbookSyncState) {
        when (val state = pocketbookSyncState) {
            is com.readle.app.ui.viewmodel.PocketbookSyncState.Success -> {
                if (state.unmatchedBooks.isNotEmpty()) {
                    unmatchedBooks = state.unmatchedBooks
                    showUnmatchedBooksDialog = true
                } else {
                    val message = buildString {
                        append("PocketBook Sync: ${state.updated} books updated (${state.scanned} scanned)")
                        if (state.audiobookshelfSynced > 0) {
                            append(", ${state.audiobookshelfSynced} synced to Audiobookshelf")
                        }
                    }
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetPocketbookSyncState()
                }
            }
            is com.readle.app.ui.viewmodel.PocketbookSyncState.Error -> {
                // Error is shown in UI directly
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (themeMode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScannerDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_scanner),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (scannerLibrary) {
                                ScannerLibrary.ML_KIT -> stringResource(R.string.scanner_ml_kit)
                                ScannerLibrary.ZXING -> stringResource(R.string.scanner_zxing)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // API Settings Section
            Text(
                text = stringResource(R.string.settings_section_apis),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deutsche Nationalbibliothek (DNB)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = dnbApiEnabled,
                            onCheckedChange = { viewModel.setDnbApiEnabled(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Books",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = googleBooksApiEnabled,
                            onCheckedChange = { viewModel.setGoogleBooksApiEnabled(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ISBNdb",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Benötigt API-Token (derzeit nicht verfügbar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isbnDbApiEnabled,
                            onCheckedChange = { viewModel.setIsbnDbApiEnabled(it) },
                            enabled = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audiobookshelf Settings Section
            Text(
                text = stringResource(R.string.settings_section_audiobookshelf),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = absServerUrlInput,
                        onValueChange = { absServerUrlInput = it },
                        label = { Text(stringResource(R.string.abs_server_url)) },
                        placeholder = { Text(stringResource(R.string.abs_server_url_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = absServerUrlInput.startsWith("http://", ignoreCase = true)
                    )
                    
                    // Warning for HTTP (cleartext)
                    if (absServerUrlInput.startsWith("http://", ignoreCase = true)) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ HTTP (nicht HTTPS)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Android blockiert unverschlüsselte HTTP-Verbindungen. Nutze HTTPS oder aktiviere Cleartext-Traffic in den App-Einstellungen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (absServerUrlInput != absServerUrl && absServerUrlInput.isNotBlank()) {
                        Button(
                            onClick = {
                                viewModel.setAudiobookshelfServerUrl(absServerUrlInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save_server_url))
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.abs_status,
                            if (absApiToken.isNotBlank()) stringResource(R.string.abs_connected)
                            else stringResource(R.string.abs_not_configured)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (absApiToken.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (absApiToken.isBlank()) {
                        Button(
                            onClick = { showAbsLoginDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = absServerUrl.isNotBlank()
                        ) {
                            Text(stringResource(R.string.abs_login))
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.logoutFromAudiobookshelf() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.abs_logout))
                            }

                            Button(
                                onClick = { viewModel.importFromAudiobookshelf() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = absImportState !is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Loading
                            ) {
                                if (absImportState is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else {
                                    Text(stringResource(R.string.abs_import_books))
                                }
                            }
                        }
                    }

                    if (absLoginState is com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Error) {
                        Text(
                            text = (absLoginState as com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (absImportState is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Error) {
                        Text(
                            text = (absImportState as com.readle.app.ui.viewmodel.AudiobookshelfImportState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (absImportState is com.readle.app.ui.viewmodel.AudiobookshelfImportState.Loading) {
                        Text(
                            text = stringResource(R.string.abs_import_in_progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PocketBook Settings Section
            Text(
                text = "PocketBook",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Cloud API Settings Card (for Reading Progress Sync)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lesestatus synchronisieren",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Verbinde dich mit deinem PocketBook Cloud Account, um deinen Lesestatus zu synchronisieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = pocketbookEmailInput,
                        onValueChange = { pocketbookEmailInput = it },
                        label = { Text(stringResource(R.string.pocketbook_email)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_email_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pocketbookPasswordInput,
                        onValueChange = { pocketbookPasswordInput = it },
                        label = { Text(stringResource(R.string.pocketbook_password)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    if ((pocketbookEmailInput != pocketbookEmail || pocketbookPasswordInput != pocketbookPassword) &&
                        pocketbookEmailInput.isNotBlank() && pocketbookPasswordInput.isNotBlank()) {
                        Button(
                            onClick = {
                                viewModel.setPocketbookEmail(pocketbookEmailInput)
                                viewModel.setPocketbookPassword(pocketbookPasswordInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save_credentials))
                        }
                    }

                    if (pocketbookEmail.isNotBlank() && pocketbookPassword.isNotBlank()) {
                        Button(
                            onClick = { viewModel.syncWithPocketbook() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = pocketbookSyncState !is com.readle.app.ui.viewmodel.PocketbookSyncState.Loading
                        ) {
                            if (pocketbookSyncState is com.readle.app.ui.viewmodel.PocketbookSyncState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text(stringResource(R.string.action_sync_reading_progress))
                            }
                        }
                    }

                    if (pocketbookSyncState is com.readle.app.ui.viewmodel.PocketbookSyncState.Error) {
                        Text(
                            text = (pocketbookSyncState as com.readle.app.ui.viewmodel.PocketbookSyncState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (pocketbookSyncState is com.readle.app.ui.viewmodel.PocketbookSyncState.Loading) {
                        Text(
                            text = stringResource(R.string.msg_syncing_pocketbook),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email Upload Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bücher hochladen via E-Mail",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Konfiguriere SMTP, um Bücher per E-Mail an dein PocketBook zu senden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Email Settings Button
                    Button(
                        onClick = { onNavigateToEmailSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("E-Mail-Einstellungen konfigurieren →")
                    }
                    
                    // Title Cleaning Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Titel bereinigen",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Entfernt Seriennummern aus Buchtiteln beim Upload",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pocketbookCleanTitles,
                            onCheckedChange = { viewModel.setPocketbookCleanTitles(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.label_import_export),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Export Format Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExportFormatDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_export_format),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (exportFormat) {
                                ExportFormat.JSON -> stringResource(R.string.format_json)
                                ExportFormat.XML -> stringResource(R.string.format_xml)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { exportLauncher.launch("readle_export.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = importExportState !is ImportExportState.Loading
            ) {
                if (importExportState is ImportExportState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.action_export))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = importExportState !is ImportExportState.Loading
            ) {
                if (importExportState is ImportExportState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.action_import))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Danger Zone - Delete All Books
            Text(
                text = "Gefahrenzone",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Alle Bücher löschen",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Löscht alle Bücher aus deiner Bibliothek. Diese Aktion kann nicht rückgängig gemacht werden!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.action_delete_all_books))
                    }
                }
            }

            when (importExportState) {
                is ImportExportState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (importExportState as ImportExportState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is ImportExportState.ImportSuccess -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.msg_import_success
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }
        }
    }

    if (showImportDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.dialog_import_mode_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_import_mode_message))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = File(context.cacheDir, "import.json")
                    context.contentResolver.openInputStream(selectedFile!!)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.importBooks(file, replaceExisting = false)
                    showImportDialog = false
                }) {
                    Text(stringResource(R.string.dialog_import_merge))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val file = File(context.cacheDir, "import.json")
                    context.contentResolver.openInputStream(selectedFile!!)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.importBooks(file, replaceExisting = true)
                    showImportDialog = false
                }) {
                    Text(stringResource(R.string.dialog_import_replace))
                }
            }
        )
    }

    if (showAbsLoginDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = {
                showAbsLoginDialog = false
                viewModel.resetAudiobookshelfLoginState()
            },
            title = { Text(stringResource(R.string.abs_login_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.abs_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.abs_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    // Error message
                    if (absLoginState is com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Error) {
                        val errorMessage = (absLoginState as com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Error).message
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    if (absLoginState is com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.loginToAudiobookshelf(username, password)
                    },
                    enabled = username.isNotBlank() && password.isNotBlank() &&
                            absLoginState !is com.readle.app.ui.viewmodel.AudiobookshelfLoginState.Loading
                ) {
                    Text(stringResource(R.string.abs_login))
                }
            },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAbsLoginDialog = false
                            viewModel.resetAudiobookshelfLoginState()
                        }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

    // Delete All Books Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_all_title)) },
            text = { Text(stringResource(R.string.dialog_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllBooks()
                        showDeleteAllDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.action_delete_all_books),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Unmatched PocketBook Books Dialog
    if (showUnmatchedBooksDialog) {
        AlertDialog(
            onDismissRequest = {
                showUnmatchedBooksDialog = false
                viewModel.resetPocketbookSyncState()
            },
            title = { Text("Unmatched Books Found") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "The following books were read in PocketBook but not found in your library:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    unmatchedBooks.forEach { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "by ${book.metadata.authors ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!book.metadata.isbn.isNullOrBlank()) {
                                    Text(
                                        text = "ISBN: ${book.metadata.isbn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Would you like to add these books to your library as 'READ'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createBooksFromPocketbook(unmatchedBooks)
                        showUnmatchedBooksDialog = false
                        viewModel.resetPocketbookSyncState()
                        Toast.makeText(
                            context,
                            "${unmatchedBooks.size} books added to library",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Text("Add Books")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnmatchedBooksDialog = false
                        viewModel.resetPocketbookSyncState()
                    }
                ) {
                    Text("Skip")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    // Scanner Library Selection Dialog
    if (showScannerDialog) {
        AlertDialog(
            onDismissRequest = { showScannerDialog = false },
            title = { Text(stringResource(R.string.settings_scanner)) },
            text = {
                Column {
                    Text(
                        text = "Wähle die Barcode-Scanner-Bibliothek für ISBN-Scans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    ScannerLibrary.values().forEach { library ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setScannerLibrary(library)
                                    showScannerDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scannerLibrary == library,
                                onClick = {
                                    viewModel.setScannerLibrary(library)
                                    showScannerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (library) {
                                        ScannerLibrary.ML_KIT -> stringResource(R.string.scanner_ml_kit)
                                        ScannerLibrary.ZXING -> stringResource(R.string.scanner_zxing)
                                    }
                                )
                                Text(
                                    text = when (library) {
                                        ScannerLibrary.ML_KIT -> "Google ML Kit - Modern, schnell, benötigt Google Services"
                                        ScannerLibrary.ZXING -> "ZXing - Open Source, funktioniert ohne Google Services"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScannerDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    // Export Format Selection Dialog
    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text(stringResource(R.string.settings_export_format)) },
            text = {
                Column {
                    ExportFormat.values().forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setExportFormat(format)
                                    showExportFormatDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportFormat == format,
                                onClick = {
                                    viewModel.setExportFormat(format)
                                    showExportFormatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (format) {
                                    ExportFormat.JSON -> stringResource(R.string.format_json)
                                    ExportFormat.XML -> stringResource(R.string.format_xml)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(title, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

