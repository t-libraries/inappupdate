package com.tlib.inappupdate.core

import android.content.Context
import com.tlib.inappupdate.controller.AppUpdateController
import com.tlib.inappupdate.controller.FakeUpdateController
import com.tlib.inappupdate.controller.RealUpdateController

object UpdateInitializer {

    private var controller: AppUpdateController? = null

    fun init(context: Context, isDebug: Boolean) {
        if (controller != null) return

        controller = if (isDebug) {
            FakeUpdateController(context.applicationContext)
        } else {
            RealUpdateController(context.applicationContext)
        }

    }

    fun getController(): AppUpdateController {
        return controller
            ?: throw IllegalStateException("SDK not initialized")
    }
}