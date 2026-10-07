package io.github.mohuddle.hours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.data.hoursStore
import io.github.mohuddle.hours.ui.HoursApp
import io.github.mohuddle.hours.ui.theme.HoursTheme
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = hoursStore(this)
        setContent {
            HoursTheme {
                var snapshot by remember { mutableStateOf<HoursSnapshot?>(null) }
                var now by remember { mutableStateOf(LocalDateTime.now()) }
                LaunchedEffect(Unit) {
                    now = LocalDateTime.now()
                    snapshot = store.load(now)
                }
                val loaded = snapshot
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (loaded == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        HoursApp(
                            snapshot = loaded,
                            now = now,
                            onSaveSettings = { notificationsEnabled, hours ->
                                store.saveSettings(notificationsEnabled, hours)
                                store.load(now)
                            },
                        )
                    }
                }
            }
        }
    }
}
