package com.palmastro.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onWipeComplete: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showWipeDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.isWipeComplete) { if (state.isWipeComplete) onWipeComplete() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // Analysis Style Section
            SettingsSection(icon = Icons.Outlined.Palette, title = "Analysis Style") {
                val tones = listOf("scientific" to "🔬 Scientific", "healing" to "🌿 Healing", "roast_safe" to "🔥 Straight Talk")
                tones.forEach { (key, label) ->
                    SettingsRadioItem(label = label, selected = state.tone == key, onClick = { viewModel.setTone(key) })
                }
            }

            // Reminders Section
            SettingsSection(icon = Icons.Outlined.Notifications, title = "Reminders") {
                val reminders = listOf("30d" to "Every 30 days", "monthly" to "1st of month", "off" to "Off")
                reminders.forEach { (key, label) ->
                    SettingsRadioItem(label = label, selected = state.reminders == key, onClick = { viewModel.setReminders(key) })
                }
            }

            // Privacy Section
            SettingsSection(icon = Icons.Outlined.Shield, title = "Privacy") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Raw Images", fontSize = 16.sp)
                        Text("Auto-deleted after 24 hours", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.rawMediaRetention, onCheckedChange = { viewModel.setRetention(it) })
                }
            }

            // Danger Zone
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
                        Text("Danger Zone", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("This cannot be undone. All results, settings, and scan data will be permanently erased.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Delete All Data") }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("PalmAstro v0.1.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Everything?", fontWeight = FontWeight.SemiBold) },
            text = { Text("All scan results, journal entries, and settings will be permanently deleted. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { showWipeDialog = false; viewModel.deleteAllData() }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsSection(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsRadioItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(52.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 20.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp)
    }
}
