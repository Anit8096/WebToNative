package com.kmp.webtonative

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kmp.webtonative.navigation.NavGraph
import com.kmp.webtonative.notification.NotificationHelper
import com.kmp.webtonative.ui.theme.WebToNativeTheme

class MainActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val showNotificationRunnable = Runnable {
        @SuppressLint("MissingPermission")
        NotificationHelper.show(this)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) scheduleNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        setContent {
            WebToNativeTheme {
                NavGraph()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(showNotificationRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted && !shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (NotificationHelper.shouldShowToday(this)) {
            scheduleNotification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(showNotificationRunnable)
    }

    private fun scheduleNotification() {
        handler.removeCallbacks(showNotificationRunnable)
        handler.postDelayed(showNotificationRunnable, 8_000)
    }
}