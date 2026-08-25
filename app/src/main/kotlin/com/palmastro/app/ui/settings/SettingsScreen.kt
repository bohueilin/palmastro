package com.palmastro.app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.BuildConfig
import com.palmastro.app.R
import com.palmastro.app.ui.components.SectionHeader
import com.palmastro.app.ui.legal.LEGAL_DOC_PRIVACY
import com.palmastro.app.ui.legal.LEGAL_DOC_TERMS
import com.palmastro.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/** Android 13, the first release where POST_NOTIFICATIONS is a runtime grant. */
private const val POST_NOTIFICATIONS_SDK = 33

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onWipeComplete: () -> Unit,
    onOpenLegal: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showWipeDialog by remember { mutableStateOf(false) }
    // Non-null while the diagnostic report is being previewed before sending.
    var diagnosticReport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.isWipeComplete) { if (state.isWipeComplete) onWipeComplete() }

    // POST_NOTIFICATIONS is requested only when the user opts into reminders
    // (spec: reminders are opt-in; permission asked lazily).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Reminder stays enabled either way; worker no-ops without permission. */ }

    val supportEmail = stringResource(R.string.settings_support_email)
    val supportErrorMessage = stringResource(R.string.settings_support_error, supportEmail)
    val diagnosticSubject = stringResource(R.string.settings_diagnostic_export)
    val diagnosticErrorMessage = stringResource(R.string.settings_diagnostic_error)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ToneSection(selected = state.tone, onSelect = { viewModel.setTone(it) })

            LanguageSection(selected = state.language, onSelect = { viewModel.setLanguage(it) })

            RemindersSection(
                selected = state.reminders,
                onSelect = { viewModel.setReminders(it) },
                onRequestPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            )

            // Privacy
            SettingsSection(icon = Icons.Outlined.Shield, title = stringResource(R.string.settings_section_privacy)) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_retention_title), fontSize = 16.sp, lineHeight = 23.sp)
                        Text(
                            stringResource(R.string.settings_retention_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.rawMediaRetention, onCheckedChange = { viewModel.setRetention(it) })
                }
            }

            // Legal & Support (P0 store requirements)
            SettingsSection(icon = Icons.Outlined.Policy, title = stringResource(R.string.settings_section_legal)) {
                SettingsLinkRow(
                    icon = Icons.Outlined.PrivacyTip,
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = { onOpenLegal(LEGAL_DOC_PRIVACY) },
                )
                SettingsLinkRow(
                    icon = Icons.Outlined.Gavel,
                    label = stringResource(R.string.settings_terms),
                    onClick = { onOpenLegal(LEGAL_DOC_TERMS) },
                )
                SettingsLinkRow(
                    icon = Icons.Outlined.Email,
                    label = stringResource(R.string.settings_support),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            scope.launch { snackbarHostState.showSnackbar(supportErrorMessage) }
                        }
                    },
                )
                SettingsLinkRow(
                    icon = Icons.Outlined.BugReport,
                    label = stringResource(R.string.settings_diagnostic_export),
                    onClick = { diagnosticReport = viewModel.buildDiagnosticReport() },
                )
            }

            // About
            SettingsSection(icon = Icons.Outlined.Info, title = stringResource(R.string.settings_section_about)) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Danger zone
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_wipe),
                            fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error, letterSpacing = 0.5.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.settings_wipe_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        enabled = !state.isWiping,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (state.isWiping) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                        } else {
                            Text(stringResource(R.string.settings_wipe))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_wipe_confirm_title), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.settings_wipe_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { showWipeDialog = false; viewModel.deleteAllData() },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.settings_wipe_confirm_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (state.wipeError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWipeError() },
            icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_wipe_error_title), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.settings_wipe_error_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWipeError() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }

    // Preview before send: the user reads the exact text that would leave the device.
    diagnosticReport?.let { report ->
        AlertDialog(
            onDismissRequest = { diagnosticReport = null },
            icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_diagnostic_export), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(R.string.settings_diagnostic_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(report, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        diagnosticReport = null
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                            putExtra(Intent.EXTRA_SUBJECT, diagnosticSubject)
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, null))
                        } catch (_: ActivityNotFoundException) {
                            scope.launch { snackbarHostState.showSnackbar(diagnosticErrorMessage) }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_diagnostic_send)) }
            },
            dismissButton = {
                TextButton(onClick = { diagnosticReport = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Analysis style (PRD §45 display names: Analytical / Gentle / Direct). */
@Composable
private fun ToneSection(selected: String, onSelect: (String) -> Unit) {
    SettingsSection(icon = Icons.Outlined.Palette, title = stringResource(R.string.settings_tone_label)) {
        val tones = listOf(
            "scientific" to stringResource(R.string.settings_tone_analytical),
            "healing" to stringResource(R.string.settings_tone_gentle),
            "roast_safe" to stringResource(R.string.settings_tone_direct),
        )
        tones.forEach { (key, label) ->
            SettingsRadioItem(label = label, selected = selected == key, onClick = { onSelect(key) })
        }
    }
}

/** Per-app locale + profile.language. */
@Composable
private fun LanguageSection(selected: String, onSelect: (String) -> Unit) {
    SettingsSection(icon = Icons.Outlined.Language, title = stringResource(R.string.settings_language_label)) {
        val languages = listOf(
            "system" to stringResource(R.string.settings_language_system),
            "en" to stringResource(R.string.settings_language_english),
            "zh-TW" to stringResource(R.string.settings_language_zh_tw),
        )
        languages.forEach { (key, label) ->
            SettingsRadioItem(label = label, selected = selected == key, onClick = { onSelect(key) })
        }
    }
}

/** Reminders are opt-in; [onRequestPermission] fires only when a cadence is switched on. */
@Composable
private fun RemindersSection(
    selected: String,
    onSelect: (String) -> Unit,
    onRequestPermission: () -> Unit,
) {
    SettingsSection(icon = Icons.Outlined.Notifications, title = stringResource(R.string.settings_reminder_label)) {
        val reminders = listOf(
            "30d" to stringResource(R.string.settings_reminder_30d),
            "monthly" to stringResource(R.string.settings_reminder_monthly),
            "off" to stringResource(R.string.settings_reminder_off),
        )
        reminders.forEach { (key, label) ->
            SettingsRadioItem(
                label = label,
                selected = selected == key,
                onClick = {
                    // Re-tapping the current cadence must not reschedule: that would
                    // restart the countdown and re-prompt for the permission.
                    if (selected != key) {
                        onSelect(key)
                        if (key != "off" && Build.VERSION.SDK_INT >= POST_NOTIFICATIONS_SDK) {
                            onRequestPermission()
                        }
                    }
                },
            )
        }
        Text(
            stringResource(R.string.settings_reminder_permission_note),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SettingsSection(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        SectionHeader(icon = icon, title = title, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsRadioItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 20.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SettingsLinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 20.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
