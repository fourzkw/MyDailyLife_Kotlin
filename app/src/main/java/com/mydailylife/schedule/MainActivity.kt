package com.mydailylife.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.mydailylife.schedule.reminder.ReminderIntents

class MainActivity : ComponentActivity() {
    private var openScheduleId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openScheduleId = intent?.getStringExtra(ReminderIntents.EXTRA_SCHEDULE_ID)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            MdlApp(
                openScheduleId = openScheduleId,
                onOpenScheduleConsumed = { openScheduleId = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openScheduleId = intent.getStringExtra(ReminderIntents.EXTRA_SCHEDULE_ID)
    }
}
