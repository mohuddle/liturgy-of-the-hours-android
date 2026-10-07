package io.github.mohuddle.hours.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.notify.PermissionCtas
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
    openOfficeHourId: String? = null,
    onHoursOpened: () -> Unit = {},
    permissionCtas: PermissionCtas = PermissionCtas(false, false),
    onAllowNotifications: () -> Unit = {},
    onAllowExactAlarms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf(snapshot) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val suppressNextHoursDismiss = remember { booleanArrayOf(openOfficeHourId != null) }
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route != HoursRoutes.HOURS) return@OnDestinationChangedListener
            if (suppressNextHoursDismiss[0]) {
                suppressNextHoursDismiss[0] = false
            } else {
                onHoursOpened()
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    LaunchedEffect(openOfficeHourId) {
        val hourId = openOfficeHourId ?: return@LaunchedEffect
        navController.navigate(HoursRoutes.office(hourId))
    }
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
                permissionCtas = permissionCtas,
                onAllowNotifications = onAllowNotifications,
                onAllowExactAlarms = onAllowExactAlarms,
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
