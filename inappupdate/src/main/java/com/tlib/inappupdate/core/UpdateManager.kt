package com.tlib.inappupdate.core

import android.app.PendingIntent
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.tlib.inappupdate.notification.UpdateNotificationHelper
import com.tlib.inappupdate.notification.UpdateNotificationHelper.ACTION_INSTALL_UPDATE
import com.tlib.inappupdate.ui.InstalledDialogFragment
import com.tlib.inappupdate.ui.UpdateDialogFragment

object UpdateManager {

    private lateinit var activityProvider: () -> FragmentActivity

    private var activityA: FragmentActivity? = null

    var availableVersion = ""

    fun init(provider: () -> FragmentActivity) {
        activityProvider = provider
        setupCallbacks()
    }

    private fun setupCallbacks() {
        val controller = UpdateInitializer.getController()

        controller.onUpdateAvailable = { info ->

        }

        controller.onDownloadComplete = {
            showInstallDialog()
            showNotification()
        }
    }


    fun startUpdateFlow(activity: FragmentActivity) {

        val controller = UpdateInitializer.getController()

        controller.checkForUpdate()

        activityA = activity

        val fragmentManager = activityA?.supportFragmentManager ?: return

        if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) {
            return
        }



        controller.onUpdateAvailable = onUpdateAvailable@{ info ->

            val fm = activityA?.supportFragmentManager ?: return@onUpdateAvailable

            if (fm.isStateSaved ||
                fm.isDestroyed ||
                fm.findFragmentByTag("update") != null
            ) {
                return@onUpdateAvailable
            }

            activityA?.let { activity ->

                availableVersion = info.availableVersionCode().toString()

                UpdateDialogFragment.newInstance(availableVersion).apply {

                    onUpdateClicked = {
                        controller.startFlexibleUpdate(activity, info)
                    }

                }.show(fm, "update")
            }
        }
    }

    fun handleInstallIfReady() {

        UpdateInitializer.getController()
            .checkForWaitingUpdate {
                UpdateInitializer.getController().completeUpdate()
            }
    }

    private fun showInstallDialog() {

        val activity = activityA ?: return

        if (activity.isFinishing || activity.isDestroyed) return

        val fragmentManager = activity.supportFragmentManager

        if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) {
            return
        }

        if (
            fragmentManager.findFragmentByTag("install_dialog") != null
        ) {
            return
        }

        InstalledDialogFragment.newInstance(availableVersion).apply {

            onUpdateClicked = {

                UpdateNotificationHelper.cancelNotification(activity)

                val launchIntent = activity.packageManager
                    .getLaunchIntentForPackage(activity.packageName)
                    ?.apply {
                        action = ACTION_INSTALL_UPDATE
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }

                activity.startActivity(launchIntent)
            }

        }.show(fragmentManager, "install_dialog")
    }

    fun handleAppStart() {

        UpdateInitializer.getController().checkForWaitingUpdate {
            UpdateInitializer.getController().completeUpdate()
        }
    }

    private fun showNotification() {

        val activity = activityProvider()

        val launchIntent = activity.packageManager
            .getLaunchIntentForPackage(activity.packageName)
            ?.apply {
                action = ACTION_INSTALL_UPDATE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent = PendingIntent.getActivity(
            activity,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        UpdateNotificationHelper.showReadyToInstallNotification(
            activity,
            pendingIntent
        )
    }

    fun handleInstallIntent(intent: Intent?) {

        if (intent?.action == ACTION_INSTALL_UPDATE) {
            UpdateNotificationHelper.cancelNotification(activityProvider())
            UpdateInitializer.getController().completeUpdate()
        }
    }
}