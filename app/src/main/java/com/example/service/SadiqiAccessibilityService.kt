package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class SadiqiAccessibilityService : AccessibilityService() {

    private val TAG = "SadiqiAccessibility"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Intercept activities to detect time-wasting apps or on-screen content
        val packageName = event.packageName?.toString() ?: ""
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Active Window changed. Top package: $packageName")
            // Send broadcast to update active application and evaluate distraction status
            val intent = Intent("com.example.sadiqi.WINDOW_CHANGED").apply {
                putExtra("package_name", packageName)
                setPackage(this@SadiqiAccessibilityService.packageName)
            }
            sendBroadcast(intent)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Sadiqi Accessibility Service connected successfully.")
    }
}
