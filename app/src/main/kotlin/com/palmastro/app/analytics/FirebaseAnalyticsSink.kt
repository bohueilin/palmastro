package com.palmastro.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseAnalyticsSink {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                analytics = FirebaseAnalytics.getInstance(context)
            }
        } catch (_: Exception) {
        }
    }

    fun send(eventName: String, props: Map<String, Any>) {
        val fa = analytics ?: return
        val bundle = Bundle()
        props.entries.take(25).forEach { (key, value) ->
            val safeKey = key.take(40).replace(Regex("[^a-zA-Z0-9_]"), "_")
            when (value) {
                is String -> bundle.putString(safeKey, value.take(100))
                is Int -> bundle.putInt(safeKey, value)
                is Long -> bundle.putLong(safeKey, value)
                is Double -> bundle.putDouble(safeKey, value)
                is Float -> bundle.putFloat(safeKey, value)
                is Boolean -> bundle.putBoolean(safeKey, value)
            }
        }
        try {
            fa.logEvent(eventName.take(40), bundle)
        } catch (_: Exception) {
        }
    }
}
