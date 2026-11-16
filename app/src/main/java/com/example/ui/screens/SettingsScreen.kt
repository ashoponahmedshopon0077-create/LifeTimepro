package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.auth.signOutUser
import com.example.data.model.AgeProfile
import com.example.data.model.Friendship
import com.example.data.model.GiftItem
import com.example.data.model.LifeMoment
import com.example.data.model.Relationship
import com.example.data.model.UserProfile
import com.example.export.ShareAndExportHelper
import com.example.security.SecurityLockHelper
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState
import com.example.widget.WidgetSyncPrefs

@Composable
fun SettingsScreen(
    viewModel: LifeTimeViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val strings = LocalAppStrings.current

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val agesState by viewModel.ageProfiles.collectAsStateWithLifecycle()
    val relsState by viewModel.relationships.collectAsStateWithLifecycle()
    val frState by viewModel.friendships.collectAsStateWithLifecycle()
    val momState by viewModel.moments.collectAsStateWithLifecycle()
    val giftState by viewModel.gifts.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJson.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success<UserProfile?>)?.data
    val ages = (agesState as? UiState.Success<List<AgeProfile>>)?.data.orEmpty()
    val rels = (relsState as? UiState.Success<List<Relationship>>)?.data.orEmpty()
    val friendships = (frState as? UiState.Success<List<Friendship>>)?.data.orEmpty()
    val moments = (momState as? UiState.Success<List<LifeMoment>>)?.data.orEmpty()
    val gifts = (giftState as? UiState.Success<List<GiftItem>>)?.data.orEmpty()

    var showPinDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account Profile Card
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (!profile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.photoUrl,
                                contentDescription = profile?.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                            )
                        } else {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(60.dp)) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.padding(16.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile?.displayName ?: "LifeTime User", style = MaterialTheme.typography.titleLarge)
                            Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(strings.cloudSyncBadge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedButton(onClick = { showEditNameDialog = true }) {
                        Text("Edit Profile Name")
                    }
                }
            }
        }

        // Language & Theme
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = (profile?.languageCode ?: "en") == "en",
                            onClick = { profile?.let { viewModel.updateProfile(it.copy(languageCode = "en")) } },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = profile?.languageCode == "bn",
                            onClick = { profile?.let { viewModel.updateProfile(it.copy(languageCode = "bn")) } },
                            label = { Text("বাংলা (Bengali)") }
                        )
                    }

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("romantic" to "Romantic", "light" to "Light", "dark" to "Dark", "system" to "System").forEach { (key, label) ->
                            FilterChip(
                                selected = (profile?.themeMode ?: "romantic") == key,
                                onClick = { profile?.let { viewModel.updateProfile(it.copy(themeMode = key)) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Privacy & Security
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.privacySecurity, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(if (profile?.privacyPinHash.isNullOrBlank()) strings.setPrivacyPin else "PIN Lock Active")
                        }
                        OutlinedButton(onClick = { showPinDialog = true }) {
                            Text(if (profile?.privacyPinHash.isNullOrBlank()) "Set PIN" else "Change / Remove")
                        }
                    }

                    if (SecurityLockHelper.canUseBiometrics()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Text(strings.biometricLock)
                            }
                            Switch(
                                checked = profile?.biometricEnabled ?: false,
                                onCheckedChange = { enabled ->
                                    profile?.let { viewModel.updateProfile(it.copy(biometricEnabled = enabled)) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Backup, Restore & PDF Export
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.backupRestore, style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { viewModel.exportCloudBackup() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportJson)
                    }
                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.restoreJson)
                    }
                    OutlinedButton(
                        onClick = {
                            ShareAndExportHelper.exportPdfReportAndShare(
                                context = context,
                                userName = profile?.displayName ?: "LifeTime User",
                                ages = ages,
                                relationships = rels,
                                friendships = friendships,
                                moments = moments,
                                gifts = gifts
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportPdf)
                    }
                    OutlinedButton(
                        onClick = { WidgetSyncPrefs.refreshAllWidgets(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync All 6 Home Screen Widgets")
                    }
                }
            }
        }

        // Sign Out & Account Deletion
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignedOut
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.signOut)
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.deleteAccountData)
                    }
                }
            }
        }
    }

    if (showEditNameDialog && profile != null) {
        var newName by remember { mutableStateOf(profile.displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile Name") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateProfile(profile.copy(displayName = newName.trim()))
                        showEditNameDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showPinDialog && profile != null) {
        var pinText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.setPrivacyPin) },
            text = {
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinText = it },
                    label = { Text("Enter 4-6 digit PIN (leave empty to disable)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val hash = if (pinText.length >= 4) SecurityLockHelper.hashPin(pinText) else ""
                    viewModel.updateProfile(profile.copy(privacyPinHash = hash))
                    showPinDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (exportedJson != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportedJson() },
            title = { Text("Cloud Backup Ready") },
            text = {
                OutlinedTextField(
                    value = exportedJson!!,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    ShareAndExportHelper.shareText(context, "LifeTime Backup JSON", exportedJson!!)
                    viewModel.clearExportedJson()
                }) { Text("Share Backup") }
            },
            dismissButton = { TextButton(onClick = { viewModel.clearExportedJson() }) { Text("Close") } }
        )
    }

    if (showRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(strings.restoreJson) },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste exported JSON backup") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (jsonInput.isNotBlank()) {
                        viewModel.restoreCloudBackup(jsonInput)
                        showRestoreDialog = false
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Data?") },
            text = { Text("This permanently deletes all your ages, relationships, friendships, memories, gifts, letters, and cloud backup.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAllCloudData {
                            signOutUser(credentialManager, scope, onSignedOut)
                        }
                    }
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) } }
        )
    }
}
