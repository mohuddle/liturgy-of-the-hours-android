package io.github.mohuddle.hours.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeModel
import io.github.mohuddle.hours.domain.VerseRecord
import io.github.mohuddle.hours.ui.theme.HoursAccent
import io.github.mohuddle.hours.ui.theme.HoursForeground
import io.github.mohuddle.hours.ui.theme.HoursMuted
import java.time.LocalDateTime

@Composable
fun HoursScreen(
    hours: List<Hour>,
    verse: VerseRecord?,
    verseError: String?,
    now: LocalDateTime,
    onOpenOffice: (hourId: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val schedule = OfficeModel.scheduleState(now, settingsFrom(hours))
    val featuredId = featuredOfficeHourId(now, hours)
    val featured = hours.firstOrNull { it.id == featuredId } ?: OfficeModel.featuredHour(schedule)
    val inWindow = OfficeModel.isCurrentWindow(
        schedule.current,
        schedule.nowMinutes,
        schedule.next,
        20,
    )
    val crossColor = if (inWindow) HoursAccent else HoursForeground
    val subtitle = OfficeModel.heroMeta(featured)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JerusalemCross(color = crossColor)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "Liturgy of the Hours",
                        color = HoursForeground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            color = HoursAccent,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                IconButton(
                    onClick = { featured?.let { onOpenOffice(it.id) } },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A241C))
                        .semantics { contentDescription = "The Office" },
                ) {
                    BellIcon(color = HoursForeground)
                }
                Spacer(Modifier.size(8.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A241C))
                        .semantics { contentDescription = "Settings" },
                ) {
                    GearIcon(color = HoursForeground)
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (hour in hours) {
                    val current = schedule.current?.id == hour.id
                    val color = when {
                        !hour.enabled -> HoursMuted
                        current -> HoursAccent
                        else -> HoursForeground
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenOffice(hour.id) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hour.shortName,
                            color = color,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = hour.time,
                            color = color,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "TODAY\u2019S SCRIPTURE",
                color = HoursMuted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (verse != null) {
                Text(
                    text = verse.reference,
                    color = HoursForeground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = verse.text,
                    color = HoursForeground,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "BSB",
                    color = HoursMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            } else {
                Text(
                    text = verseError ?: "Today\u2019s Scripture will appear here.",
                    color = HoursForeground,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private fun settingsFrom(hours: List<Hour>): Map<String, Any?> {
    val settings = linkedMapOf<String, Any?>()
    for (hour in hours) {
        settings["${hour.id}Enabled"] = hour.enabled
        settings["${hour.id}Time"] = hour.time
    }
    return settings
}

@Composable
private fun BellIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.62f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.22f)
            close()
        }
        drawPath(path, color)
        drawLine(
            color,
            Offset(w * 0.18f, h * 0.68f),
            Offset(w * 0.82f, h * 0.68f),
            strokeWidth = w * 0.08f,
        )
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.84f))
    }
}

@Composable
private fun GearIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = w * 0.12f),
        )
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = Offset(w / 2f, h / 2f),
        )
        val teeth = 8
        for (i in 0 until teeth) {
            val angle = Math.toRadians((i * 360.0 / teeth) - 90.0)
            val cx = w / 2f + (w * 0.38f * kotlin.math.cos(angle)).toFloat()
            val cy = h / 2f + (h * 0.38f * kotlin.math.sin(angle)).toFloat()
            drawCircle(color, radius = w * 0.07f, center = Offset(cx, cy))
        }
    }
}
