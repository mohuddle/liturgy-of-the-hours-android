package io.github.mohuddle.hours.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeBook
import io.github.mohuddle.hours.domain.OfficeModel
import io.github.mohuddle.hours.domain.OfficeSection
import io.github.mohuddle.hours.ui.theme.HoursAccent
import io.github.mohuddle.hours.ui.theme.HoursForeground
import io.github.mohuddle.hours.ui.theme.HoursMuted
import java.time.LocalDateTime
import java.util.Locale

data class OfficeViewState(
    val hourId: String,
    val heading: String,
    val hourName: String,
    val sections: List<OfficeSection>,
    val error: String?,
)

fun officeViewState(
    now: LocalDateTime,
    hourId: String,
    hours: List<Hour>,
    book: OfficeBook?,
): OfficeViewState {
    val hour = hours.firstOrNull { it.id == hourId } ?: OfficeModel.hourById(hourId)
    val built = OfficeModel.buildOffice(now, hour, book, OfficeModel.isoDate(now))
    if (book == null) {
        return OfficeViewState(
            hourId = built.hourId,
            heading = built.heading,
            hourName = built.hourName,
            sections = emptyList(),
            error = "The Office is not ready yet.",
        )
    }
    return OfficeViewState(
        hourId = built.hourId,
        heading = built.heading,
        hourName = built.hourName,
        sections = built.sections,
        error = null,
    )
}

fun featuredOfficeHourId(now: LocalDateTime, hours: List<Hour>): String? {
    val settings = linkedMapOf<String, Any?>()
    for (hour in hours) {
        settings["${hour.id}Enabled"] = hour.enabled
        settings["${hour.id}Time"] = hour.time
    }
    return OfficeModel.featuredHour(OfficeModel.scheduleState(now, settings))?.id
}

@Composable
fun OfficeScreen(
    state: OfficeViewState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = state.heading,
                    color = HoursForeground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = state.hourName,
                color = HoursAccent,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(20.dp))

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = HoursForeground,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    for (section in state.sections) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = section.label.uppercase(Locale.ROOT),
                                color = HoursMuted,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            for (line in section.body.split("\n")) {
                                if (line.isEmpty()) continue
                                Text(
                                    text = line,
                                    color = HoursForeground,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HoursBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A241C))
            .semantics { contentDescription = "Back" },
    ) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.12f
            drawLine(
                color = HoursForeground,
                start = Offset(w * 0.62f, h * 0.22f),
                end = Offset(w * 0.28f, h * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = HoursForeground,
                start = Offset(w * 0.28f, h * 0.5f),
                end = Offset(w * 0.62f, h * 0.78f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
