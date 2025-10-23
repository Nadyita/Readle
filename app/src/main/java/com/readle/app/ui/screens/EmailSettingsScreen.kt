package com.readle.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readle.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val smtpServer by viewModel.smtpServer.collectAsState()
    val smtpPort by viewModel.smtpPort.collectAsState()
    val smtpUsername by viewModel.smtpUsername.collectAsState()
    val smtpPassword by viewModel.smtpPassword.collectAsState()
    val smtpFromEmail by viewModel.smtpFromEmail.collectAsState()
    val pocketbookSendToEmail by viewModel.pocketbookSendToEmail.collectAsState()
    val emailTestState by viewModel.emailTestState.collectAsState()

    var smtpServerInput by remember { mutableStateOf("") }
    var smtpPortInput by remember { mutableStateOf("465") }
    var smtpUsernameInput by remember { mutableStateOf("") }
    var smtpPasswordInput by remember { mutableStateOf("") }
    var smtpFromEmailInput by remember { mutableStateOf("") }
    var pocketbookSendToEmailInput by remember { mutableStateOf("") }

    // Load saved values
    LaunchedEffect(smtpServer) {
        smtpServerInput = smtpServer
    }

    LaunchedEffect(smtpPort) {
        if (smtpPort > 0) {
            smtpPortInput = smtpPort.toString()
        }
    }

    LaunchedEffect(smtpUsername) {
        smtpUsernameInput = smtpUsername
    }

    LaunchedEffect(smtpPassword) {
        smtpPasswordInput = smtpPassword
    }

    LaunchedEffect(smtpFromEmail) {
        smtpFromEmailInput = smtpFromEmail
    }

    LaunchedEffect(pocketbookSendToEmail) {
        pocketbookSendToEmailInput = pocketbookSendToEmail
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Mail Einstellungen (PBSync)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SMTP Server Einstellungen",
                style = MaterialTheme.typography.titleMedium
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
                        value = smtpServerInput,
                        onValueChange = { smtpServerInput = it },
                        label = { Text("SMTP Server") },
                        placeholder = { Text("smtp.gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = smtpPortInput,
                        onValueChange = { smtpPortInput = it },
                        label = { Text("SMTP Port") },
                        placeholder = { Text("465") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = smtpUsernameInput,
                        onValueChange = { smtpUsernameInput = it },
                        label = { Text("Benutzername") },
                        placeholder = { Text("your.email@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = smtpPasswordInput,
                        onValueChange = { smtpPasswordInput = it },
                        label = { Text("Passwort") },
                        placeholder = { Text("App-Passwort") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = smtpFromEmailInput,
                        onValueChange = { smtpFromEmailInput = it },
                        label = { Text("Absender E-Mail") },
                        placeholder = { Text("your.email@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = pocketbookSendToEmailInput,
                        onValueChange = { pocketbookSendToEmailInput = it },
                        label = { Text("Pocketbook E-Mail") },
                        placeholder = { Text("username@pbsync.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            if (smtpServerInput.isNotBlank() && smtpUsernameInput.isNotBlank()) {
                Button(
                    onClick = {
                        viewModel.setSmtpServer(smtpServerInput)
                        viewModel.setSmtpPort(smtpPortInput.toIntOrNull() ?: 465)
                        viewModel.setSmtpUsername(smtpUsernameInput)
                        viewModel.setSmtpPassword(smtpPasswordInput)
                        viewModel.setSmtpFromEmail(smtpFromEmailInput)
                        viewModel.setPocketbookSendToEmail(pocketbookSendToEmailInput)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Einstellungen speichern")
                }
            }

            // Test connection button
            if (smtpServer.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()) {
                Button(
                    onClick = { viewModel.testEmailConnection() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = emailTestState !is com.readle.app.ui.viewmodel.EmailTestState.Loading
                ) {
                    if (emailTestState is com.readle.app.ui.viewmodel.EmailTestState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Verbindung testen")
                    }
                }
            }

            // Test result messages
            when (val state = emailTestState) {
                is com.readle.app.ui.viewmodel.EmailTestState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✓ Verbindung erfolgreich!",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is com.readle.app.ui.viewmodel.EmailTestState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "✗ Verbindung fehlgeschlagen",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Help text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hinweise:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Standard-Port: 465 (SSL/TLS) oder 587 (STARTTLS)\n" +
                                "• Gmail: Verwenden Sie ein App-Passwort\n" +
                                "• Pocketbook E-Mail: Finden Sie diese in Ihrem Pocketbook-Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}







