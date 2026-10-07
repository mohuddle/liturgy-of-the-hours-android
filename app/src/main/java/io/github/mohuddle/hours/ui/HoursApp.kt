package io.github.mohuddle.hours.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.ui.theme.HoursForeground
import java.time.LocalDateTime

object HoursRoutes {
    const val HOURS = "hours"
    const val SETTINGS = "settings"
    const val HOUR_ID = "hourId"
    const val OFFICE = "office/{$HOUR_ID}"

    fun office(hourId: String) = "office/$hourId"
}

@Composable
fun HoursApp(
    snapshot: HoursSnapshot,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HoursRoutes.HOURS,
        modifier = modifier,
    ) {
        composable(HoursRoutes.HOURS) {
            HoursScreen(
                hours = snapshot.hours,
                verse = snapshot.verse,
                verseError = snapshot.verseError,
                now = now,
                onOpenOffice = { hourId -> navController.navigate(HoursRoutes.office(hourId)) },
                onOpenSettings = { navController.navigate(HoursRoutes.SETTINGS) },
            )
        }
        composable(
            route = HoursRoutes.OFFICE,
            arguments = listOf(
                navArgument(HoursRoutes.HOUR_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val hourId = entry.arguments?.getString(HoursRoutes.HOUR_ID) ?: "morning"
            OfficeScreen(
                state = officeViewState(now, hourId, snapshot.hours, snapshot.officeBook),
                onBack = { navController.popBackStack() },
            )
        }
        composable(HoursRoutes.SETTINGS) {
            SettingsStubScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SettingsStubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            Spacer(Modifier.height(24.dp))
        }
    }
}
