package com.tlib.inappupdate.controller

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.lang.ref.WeakReference

class RealUpdateController(context: Context) : AppUpdateController {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(context.applicationContext)
    private var installStateListener: InstallStateUpdatedListener? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var pendingUpdateInfo: AppUpdateInfo? = null
    private var hasTriggeredComplete = false

    // Callbacks
    override var onUpdateAvailable: ((AppUpdateInfo) -> Unit)? = null
    override var onDownloadProgress: ((Int) -> Unit)? = null
    override var onDownloadComplete: (() -> Unit)? = null
    override var onUpdateNotAvailable: (() -> Unit)? = null
    override var onError: ((Exception) -> Unit)? = null

    companion object {
        private const val TAG = "RealUpdateController"
        private const val AUTO_START_UPDATE = true
    }

    override fun initialize(activity: Activity) {
        Log.d(TAG, "Initializing update controller")
        currentActivityRef = WeakReference(activity)
        registerPersistentListener()
        checkForUpdate()
    }

    override fun checkForUpdate() {
        Log.d(TAG, "Checking for updates...")
        registerPersistentListener()

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                Log.d(TAG, "Update availability: ${info.updateAvailability()}")
                Log.d(TAG, "Install status: ${info.installStatus()}")
                Log.d(TAG, "Available version: ${info.availableVersionCode()}")

                when (info.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            Log.d(TAG, "Update available and flexible update is allowed")
                            pendingUpdateInfo = info

                            onUpdateAvailable?.invoke(info)

                            if (AUTO_START_UPDATE) {
                                currentActivityRef?.get()?.let { activity ->
                                    startFlexibleUpdate(activity, info)
                                } ?: run {
                                    Log.w(TAG, "Cannot auto-start: Activity reference is null")
                                }
                            }
                        } else {
                            Log.w(TAG, "Update available but flexible update not allowed")
                        }
                    }

                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        Log.d(TAG, "Update already in progress, resuming...")
                        pendingUpdateInfo = info
                        onUpdateAvailable?.invoke(info)

                        currentActivityRef?.get()?.let { activity ->
                            startFlexibleUpdate(activity, info)
                        }
                    }

                    else -> {
                        Log.d(TAG, "No update available")
                        onUpdateNotAvailable?.invoke()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check for updates", e)
                onError?.invoke(e)
            }
    }

    override fun startFlexibleUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        Log.d(TAG, "Starting flexible update flow...")
        currentActivityRef = WeakReference(activity)
        pendingUpdateInfo = appUpdateInfo
        hasTriggeredComplete = false

        registerPersistentListener()

        try {
            appUpdateManager.startUpdateFlow(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            ).addOnSuccessListener {
                Log.d(TAG, "Update flow started successfully")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start update flow", e)
                onError?.invoke(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting update flow", e)
            onError?.invoke(e)
        }
    }

    override fun checkForWaitingUpdate(onReady: () -> Unit) {
        Log.d(TAG, "Checking for waiting updates...")

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when (info.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        Log.d(TAG, "Update downloaded and waiting for installation")
                        onReady()
                    }
                    InstallStatus.PENDING -> {
                        Log.d(TAG, "Update pending")
                    }
                    InstallStatus.DOWNLOADING -> {
                        Log.d(TAG, "Update currently downloading")
                    }
                    else -> {
                        Log.d(TAG, "No waiting update found, status: ${info.installStatus()}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check for waiting update", e)
                onError?.invoke(e)
            }
    }

    override fun completeUpdate() {
        Log.d(TAG, "Completing update...")

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(TAG, "Calling completeUpdate() to restart app")
                appUpdateManager.completeUpdate()
            } else {
                Log.w(TAG, "Cannot complete update: Status is ${info.installStatus()}")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to complete update", e)
            onError?.invoke(e)
        }
    }

    override fun unregisterListener() {
        installStateListener?.let {
            Log.d(TAG, "Unregistering install state listener")
            appUpdateManager.unregisterListener(it)
            installStateListener = null
        }
    }

    private fun registerPersistentListener() {
        if (installStateListener != null) {
            Log.d(TAG, "Listener already registered, skipping")
            return
        }

        Log.d(TAG, "Registering install state listener")

        val listener = InstallStateUpdatedListener { state ->
            fun dispatch(action: () -> Unit) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    action()
                } else {
                    mainHandler.post { action() }
                }
            }

            Log.d(TAG, "=================================")
            Log.d(TAG, "Install Status Update:")
            Log.d(TAG, "Status: ${state.installStatus()}")
            Log.d(TAG, "Bytes Downloaded: ${state.bytesDownloaded()}")
            Log.d(TAG, "Total Bytes: ${state.totalBytesToDownload()}")

            when (state.installStatus()) {
                InstallStatus.PENDING -> {
                    Log.d(TAG, "Update pending - waiting to start")
                }

                InstallStatus.DOWNLOADING -> {
                    val progress = if (state.totalBytesToDownload() > 0) {
                        ((state.bytesDownloaded() * 100) / state.totalBytesToDownload()).toInt()
                    } else {
                        0
                    }

                    Log.d(TAG, "Downloading update: $progress%")
                    Log.d(TAG, "Downloaded: ${state.bytesDownloaded()} / ${state.totalBytesToDownload()} bytes")

                    dispatch { onDownloadProgress?.invoke(progress) }

                    if (progress >= 100 && !hasTriggeredComplete) {
                        Log.d(TAG, "Download reached 100%")
                        hasTriggeredComplete = true
                        dispatch { onDownloadComplete?.invoke() }
                    }
                }

                InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Update downloaded successfully!")
                    Log.d(TAG, "Ready to install - call completeUpdate()")

                    if (!hasTriggeredComplete) {
                        hasTriggeredComplete = true
                        dispatch { onDownloadComplete?.invoke() }
                    }
                }

                InstallStatus.INSTALLING -> {
                    Log.d(TAG, "Installing update...")
                }

                InstallStatus.INSTALLED -> {
                    Log.d(TAG, "Update installed successfully!")
                }

                InstallStatus.CANCELED -> {
                    Log.d(TAG, "Update canceled by user")
                    hasTriggeredComplete = false
                }

                InstallStatus.FAILED -> {
                    Log.e(TAG, "Update download failed!")
                    Log.e(TAG, "Error code: ${state.installErrorCode()}")
                    dispatch {
                        onError?.invoke(Exception("Download failed with error code: ${state.installErrorCode()}"))
                    }
                    hasTriggeredComplete = false
                }

                InstallStatus.UNKNOWN -> {
                    Log.d(TAG, "Unknown install status")
                }

                else -> {
                    Log.d(TAG, "Unhandled state: ${state.installStatus()}")
                }
            }
            Log.d(TAG, "=================================")
        }

        installStateListener = listener
        appUpdateManager.registerListener(listener)
    }
}