package com.tlib.inappupdate.controller

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo

interface AppUpdateController {

    var onUpdateAvailable: ((AppUpdateInfo) -> Unit)?
    var onDownloadProgress: ((Int) -> Unit)?
    var onDownloadComplete: (() -> Unit)?
    var onUpdateNotAvailable: (() -> Unit)?
    var onError: ((Exception) -> Unit)?

    fun initialize(activity: Activity)
    fun checkForUpdate()
    fun startFlexibleUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo)
    fun checkForWaitingUpdate(onReady: () -> Unit)
    fun completeUpdate()
    fun unregisterListener()
}