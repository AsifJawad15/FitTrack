package com.asifjawad.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.asifjawad.fittrack.ui.FitTrackApp
import com.asifjawad.fittrack.ui.theme.FittrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FittrackTheme {
                FitTrackApp()
            }
        }
    }
}
