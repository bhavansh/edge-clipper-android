package dev.bmg.edgepanel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bmg.edgepanel.service.EdgePanelService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EdgePanelScreen(
                onStartClick = {
                    if (!Settings.canDrawOverlays(this)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    } else {
                        startService(Intent(this, EdgePanelService::class.java))
                    }
                }
            )
        }
    }
}

@Composable
fun EdgePanelScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text("Edge Panel", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Version: 1.0")
        Text("Developer: Bhavansh")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Permissions required:", style = MaterialTheme.typography.titleMedium)
        Text("• Display over other apps")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartClick) {
            Text("Start Edge Panel")
        }
    }
}