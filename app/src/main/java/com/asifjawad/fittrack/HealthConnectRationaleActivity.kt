package com.asifjawad.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asifjawad.fittrack.ui.theme.FittrackTheme

class HealthConnectRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FittrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Health Connect access",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FitTrack reads your Health Connect steps, sleep, exercise, calories, weight, and height so it can build a local daily progress summary."
                        )
                        Text(
                            text = "Data stays on this device in the app database. FitTrack does not upload health data or use cloud login."
                        )
                        Text(
                            text = "You can remove these permissions any time from Android Settings > Health Connect > App permissions."
                        )
                    }
                }
            }
        }
    }
}
