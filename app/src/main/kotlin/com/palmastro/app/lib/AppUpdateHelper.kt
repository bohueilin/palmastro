package com.palmastro.app.lib

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdateHelper(private val activity: Activity) {
    private val updateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    fun checkForUpdate(
        onUpdateAvailable: (staleDays: Int) -> Unit = {},
        onNoUpdate: () -> Unit = {},
        onError: (Exception) -> Unit = {},
    ) {
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val staleDays = info.clientVersionStalenessDays() ?: 0
                onUpdateAvailable(staleDays)
            } else {
                onNoUpdate()
            }
        }.addOnFailureListener { e ->
            onError(e)
        }
    }

    fun startFlexibleUpdate() {
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                updateManager.startUpdateFlow(
                    info, activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
            }
        }
    }

    fun startImmediateUpdate() {
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                updateManager.startUpdateFlow(
                    info, activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                )
            }
        }
    }
}
