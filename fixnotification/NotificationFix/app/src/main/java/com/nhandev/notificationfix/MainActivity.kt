package com.nhandev.notificationfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nhandev.notificationfix.ui.theme.NotificationFixTheme
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import javax.inject.Inject
import com.nhandev.notificationfix.domain.ShizukuHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var shizukuHelper: ShizukuHelper

    // Shizuku permission result listener
    private val shizukuPermListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 1001) {
            // ViewModel will re-check on next refresh
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register Shizuku permission listener
        Shizuku.addRequestPermissionResultListener(shizukuPermListener)

        setContent {
            NotificationFixTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermListener)
    }
}
