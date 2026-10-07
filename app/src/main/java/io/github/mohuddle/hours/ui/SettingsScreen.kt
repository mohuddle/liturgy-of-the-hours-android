package io.github.mohuddle.hours.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeModel
import io.github.mohuddle.hours.notify.PermissionCtas
import io.github.mohuddle.hours.ui.theme.HoursAccent
import io.github.mohuddle.hours.ui.theme.HoursForeground
import io.github.mohuddle.hours.ui.theme.HoursMuted

const val SETTINGS_CAPTION =
    "Hour reminders stay on screen until you tap the notification or open the app. A church bell rings when they appear. Scripture is the Berean Standard Bible. Prime defaults to 07:00 so it does not collide with Morning Prayer at 06:00."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    hours: List<Hour>,
    onBack: () -> Unit,
    onChange: (notificationsEnabled: Boolean, hours: List<Hour>) -> Unit,
    permissionCtas: PermissionCtas = PermissionCtas(false, false),
    onAllowNotifications: () -> Unit = {},
    onAllowExactAlarms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pickingId by remember { mutableStateOf<String?>(null) }
    val picking = hours.firstOrNull { it.id == pickingId }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HoursBackButton(onClick = onBack)
                Text(
                    text = "Settings",
                    color = HoursForeground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (notificationsEnabled) "Hour reminders on" else "Hour reminders off",
                    color = HoursForeground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { onChange(it, hours) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HoursAccent,
                        checkedTrackColor = HoursAccent.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.semantics { contentDescription = "Hour reminders" },
                )
            }

            if (permissionCtas.allowNotifications) {
                TextButton(
                    onClick = onAllowNotifications,
                    modifier = Modifier.semantics { contentDescription = "Allow notifications" },
                ) {
                    Text("Allow notifications", color = HoursAccent)
                }
            }
            if (permissionCtas.allowExactAlarms) {
                TextButton(
                    onClick = onAllowExactAlarms,
                    modifier = Modifier.semantics { contentDescription = "Allow exact alarms" },
                ) {
                    Text("Allow exact alarms", color = HoursAccent)
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (hour in hours) {
                    val color = if (hour.enabled) HoursForeground else HoursMuted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hour.name,
                            color = color,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = hour.enabled,
                            onCheckedChange = { enabled ->
                                onChange(
                                    notificationsEnabled,
                                    hours.map { if (it.id == hour.id) it.copy(enabled = enabled) else it },
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HoursAccent,
                                checkedTrackColor = HoursAccent.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "${hour.shortName} enabled"
                            },
                        )
                        Text(
                            text = hour.time,
                            color = color,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable { pickingId = hour.id }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .semantics { contentDescription = "${hour.shortName} time" },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = SETTINGS_CAPTION,
                color = HoursMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (picking != null) {
        key(picking.id) {
            val parts = picking.time.split(":")
            val state = rememberTimePickerState(
                initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = { pickingId = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val time = OfficeModel.parseHm(
                                "${state.hour}:${OfficeModel.pad2(state.minute)}",
                                picking.defaultTime,
                            )
                            onChange(
                                notificationsEnabled,
                                hours.map { if (it.id == picking.id) it.copy(time = time) else it },
                            )
                            pickingId = null
                        },
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { pickingId = null }) { Text("Cancel") }
                },
                text = { TimeInput(state = state) },
            )
        }
    }
}
