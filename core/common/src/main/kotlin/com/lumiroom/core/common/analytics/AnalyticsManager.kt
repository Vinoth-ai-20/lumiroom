package com.lumiroom.core.common.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {

    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics

    fun logEvent(eventName: String, params: Map<String, String>? = null) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            bundle.putString(key, value)
        }
        analytics.logEvent(eventName, bundle)
    }

    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    fun logRoomCreated(roomId: String) {
        logEvent("room_created", mapOf("room_id" to roomId))
    }

    fun logFurniturePlaced(furnitureId: String, category: String) {
        logEvent("furniture_placed", mapOf("furniture_id" to furnitureId, "category" to category))
    }

    fun logAiAssistantQueried() {
        logEvent("ai_assistant_queried")
    }

    fun logReportExported(roomId: String) {
        logEvent("report_exported", mapOf("room_id" to roomId))
    }
}
