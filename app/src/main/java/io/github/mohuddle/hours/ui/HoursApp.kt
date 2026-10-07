package io.github.mohuddle.hours.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.domain.Hour
import java.time.LocalDateTime
import kotlinx.coroutines.launch

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
    onSaveSettings: suspend (notificationsEnabled: Boolean, hours: List<Hour>) -> HoursSnapshot,
    onScheduleChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf(snapshot) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HoursRoutes.HOURS,
        modifier = modifier,
    ) {
        composable(HoursRoutes.HOURS) {
            HoursScreen(
                hours = current.hours,
                verse = current.verse,
                verseError = current.verseError,
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
                state = officeViewState(now, hourId, current.hours, current.officeBook),
                onBack = { navController.popBackStack() },
            )
        }
        composable(HoursRoutes.SETTINGS) {
            SettingsScreen(
                notificationsEnabled = current.notificationsEnabled,
                hours = current.hours,
                onBack = { navController.popBackStack() },
                onChange = { notificationsEnabled, hours ->
                    current = current.copy(
                        notificationsEnabled = notificationsEnabled,
                        hours = hours,
                    )
                    scope.launch {
                        current = onSaveSettings(notificationsEnabled, hours)
                        onScheduleChanged()
                    }
                },
            )
        }
    }
}
