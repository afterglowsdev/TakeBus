package io.github.afterglowsdev.takebus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.afterglowsdev.takebus.ui.TakeBusApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = remember { AppContainer(applicationContext) }
            TakeBusApp(container = container)
        }
    }
}

