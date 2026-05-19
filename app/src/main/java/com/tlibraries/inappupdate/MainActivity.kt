package com.tlibraries.inappupdate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tlib.inappupdate.core.UpdateInitializer
import com.tlib.inappupdate.core.UpdateManager
import com.tlibraries.inappupdate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startUpdateFlow()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initializeUpdateSystem()
        handlePostNotificationPermission()
    }

    // Initialization and for the Activity which is the Entry point of the Application where update needs to be installed
    private fun initializeUpdateSystem() {
        UpdateInitializer.init(this, BuildConfig.DEBUG)
        UpdateManager.init { this }

        UpdateManager.handleInstallIfReady()
    }

    // Update Flow Entry Point or where dialog needs to be shown
    private fun startUpdateFlow() {
        UpdateManager.startUpdateFlow(this)
    }

    // Notification Permission Handling
    private fun handlePostNotificationPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) return

        if (hasNotificationPermission()) {
            startUpdateFlow()
        } else {
            requestNotificationPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        notificationPermissionLauncher.launch(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }
}