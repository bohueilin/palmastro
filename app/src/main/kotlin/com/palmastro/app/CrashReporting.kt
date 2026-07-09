package com.palmastro.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporting {
    private var initialized = false

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                initialized = true
            }
        } catch (_: Exception) {
        }
    }

    fun log(message: String) {
        if (initialized) {
            try { FirebaseCrashlytics.getInstance().log(message) } catch (_: Exception) {}
        }
    }

    fun recordException(throwable: Throwable) {
        if (initialized) {
            try { FirebaseCrashlytics.getInstance().recordException(throwable) } catch (_: Exception) {}
        }
    }

    fun setUserId(id: String) {
        if (initialized) {
            try { FirebaseCrashlytics.getInstance().setUserId(id) } catch (_: Exception) {}
        }
    }
}
