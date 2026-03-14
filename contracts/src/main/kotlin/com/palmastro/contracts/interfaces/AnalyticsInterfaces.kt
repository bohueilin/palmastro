package com.palmastro.contracts.interfaces

interface AnalyticsEmitter {
    fun emit(eventName: String, props: Map<String, Any>)
}
