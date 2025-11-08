package com.readle.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readle.app.R
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
                title = { Text(stringResource(R.string.pocketbook_email_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.email_settings_smtp_section),
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
                        label = { Text(stringResource(R.string.pocketbook_smtp_server)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_smtp_server_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = smtpPortInput,
                        onValueChange = { smtpPortInput = it },
                        label = { Text(stringResource(R.string.pocketbook_smtp_port)) },
                        placeholder = { Text("465") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = smtpUsernameInput,
                        onValueChange = { smtpUsernameInput = it },
                        label = { Text(stringResource(R.string.pocketbook_username)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_username_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None
                        )
                    )

                    OutlinedTextField(
                        value = smtpPasswordInput,
                        onValueChange = { smtpPasswordInput = it },
                        label = { Text(stringResource(R.string.pocketbook_password)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = smtpFromEmailInput,
                        onValueChange = { smtpFromEmailInput = it },
                        label = { Text(stringResource(R.string.pocketbook_sender_email)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_from_email_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = pocketbookSendToEmailInput,
                        onValueChange = { pocketbookSendToEmailInput = it },
                        label = { Text(stringResource(R.string.pocketbook_pocketbook_email)) },
                        placeholder = { Text(stringResource(R.string.pocketbook_pocketbook_email_hint)) },
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
                    Text(stringResource(R.string.action_save_settings))
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
                        Text(stringResource(R.string.action_test_connection))
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
                            text = stringResource(R.string.email_test_success),
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
                                text = stringResource(R.string.email_test_failed),
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
                        text = stringResource(R.string.email_settings_hints_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.email_settings_hints_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}










