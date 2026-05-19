package com.tlib.inappupdate.controller

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability

class FakeUpdateController(
    private val context: Context
) : AppUpdateController {

    private val fakeManager = FakeAppUpdateManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isDownloadCompleted = false
    private var pendingInfo: AppUpdateInfo? = null

    override var onUpdateAvailable: ((AppUpdateInfo) -> Unit)? = null
    override var onDownloadProgress: ((Int) -> Unit)? = null
    override var onDownloadComplete: (() -> Unit)? = null
    override var onUpdateNotAvailable: (() -> Unit)? = null
    override var onError: ((Exception) -> Unit)? = null

    override fun initialize(activity: Activity) {
        checkForUpdate()
    }

    override fun checkForUpdate() {

        fakeManager.setUpdateAvailable(2)

        fakeManager.appUpdateInfo
            .addOnSuccessListener { info ->

                pendingInfo = info

                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    postMain { onUpdateAvailable?.invoke(info) }
                } else {
                    postMain { onUpdateNotAvailable?.invoke() }
                }
            }
            .addOnFailureListener {
                postMain { onError?.invoke(it) }
            }
    }

    override fun startFlexibleUpdate(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo
    ) {
        simulateDownload()
    }

    private fun simulateDownload() {

        isDownloadCompleted = false

        val steps = listOf(0, 10, 35, 60, 85, 100)

        Thread {

            for (progress in steps) {
                Thread.sleep(400)

                postMain {
                    onDownloadProgress?.invoke(progress)
                }
            }

            isDownloadCompleted = true

            postMain {
                onDownloadComplete?.invoke()
            }

        }.start()
    }

    override fun checkForWaitingUpdate(onReady: () -> Unit) {
        if (isDownloadCompleted) {
            postMain { onReady() }
        }
    }

    override fun completeUpdate() {
        isDownloadCompleted = false
    }

    override fun unregisterListener() {
        // no-op
    }

    private fun postMain(block: () -> Unit) {
        mainHandler.post { block() }
    }
}