package com.palmastro.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onWipeComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showWipeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isWipeComplete) {
        if (state.isWipeComplete) onWipeComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("分析風格", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val tones = listOf("scientific" to "科學分析", "healing" to "療癒關懷", "roast_safe" to "犀利直說")
            tones.forEach { (key, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = state.tone == key,
                            onClick = { viewModel.setTone(key) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = state.tone == key, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("提醒設定", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val reminders = listOf("30d" to "每30天", "monthly" to "每月1號", "off" to "關閉")
            reminders.forEach { (key, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = state.reminders == key,
                            onClick = { viewModel.setReminders(key) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = state.reminders == key, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("保留原始影像", fontSize = 16.sp)
                    Text("影像將在24小時後自動刪除", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.rawMediaRetention, onCheckedChange = { viewModel.setRetention(it) })
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Button(
                onClick = { showWipeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("刪除所有資料") }
            Text("此操作無法復原。所有掃描結果、設定將被清除。", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("確認刪除") },
            text = { Text("確定要刪除所有資料嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = { showWipeDialog = false; viewModel.deleteAllData() }) { Text("確認刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("取消") }
            },
        )
    }
}
