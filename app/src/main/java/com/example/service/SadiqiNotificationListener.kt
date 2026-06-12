package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.NotificationLog
import com.example.data.SadiqiDatabase
import com.example.data.SadiqiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SadiqiNotificationListener : NotificationListenerService() {

    private val TAG = "SadiqiNotificationListener"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: SadiqiRepository

    override fun onCreate() {
        super.onCreate()
         try {
             val db = SadiqiDatabase.getDatabase(applicationContext)
             repository = SadiqiRepository(db.sadiqiDao())
         } catch (e: Exception) {
             Log.e(TAG, "Error initializing Sadiqi database in Notification listener", e)
         }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: "unknown"
        
        // Skip our own app notifications to avoid loop triggers
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        Log.d(TAG, "Intercepted notification: App=$packageName Title=$title Text=$text")

        // Save to Room Database asynchronously
        serviceScope.launch {
            try {
                // Determine initial waste status based on core package names
                val isDistractingApp = isWasteTimeApp(packageName)
                val notificationLog = NotificationLog(
                     title = title,
                     text = text,
                     packageName = packageName,
                     isAnalyzed = false,
                     wastingTimeDetected = isDistractingApp
                )
                repository.insertNotification(notificationLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert notification log", e)
            }
        }
    }

    private fun isWasteTimeApp(packageName: String): Boolean {
        val wastefulKeywords = listOf("tiktok", "instagram", "facebook", "youtube", "twitter", "snapchat", "netflix", "game", "clash", "pubg", "freefire")
        return wastefulKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
