package com.example.data

import kotlinx.coroutines.flow.Flow

class SadiqiRepository(private val dao: SadiqiDao) {

    val allNotifications: Flow<List<NotificationLog>> = dao.getAllNotifications()
    val allHabits: Flow<List<Habit>> = dao.getAllHabits()
    val allTasks: Flow<List<DailyTask>> = dao.getAllTasks()
    val allChatLogs: Flow<List<ChatLog>> = dao.getAllChatLogs()

    // Notifications
    suspend fun insertNotification(notification: NotificationLog): Long {
        return dao.insertNotification(notification)
    }

    suspend fun updateNotificationAnalysis(id: Int, summary: String, wastingTimeDetected: Boolean) {
        dao.updateNotificationAnalysis(id, summary, wastingTimeDetected)
    }

    suspend fun clearNotifications() {
        dao.clearNotifications()
    }

    // Habits
    suspend fun insertHabit(habit: Habit): Long {
        return dao.insertHabit(habit)
    }

    suspend fun deleteHabit(id: Int) {
        dao.deleteHabit(id)
    }

    suspend fun clearHabits() {
        dao.clearHabits()
    }

    // Tasks
    suspend fun insertTask(task: DailyTask): Long {
        return dao.insertTask(task)
    }

    suspend fun updateTaskStatus(id: Int, completed: Boolean) {
        dao.updateTaskStatus(id, completed)
    }

    suspend fun deleteTask(id: Int) {
        dao.deleteTask(id)
    }

    suspend fun clearTasks() {
        dao.clearTasks()
    }

    // Chat Logs
    suspend fun insertChatLog(chatLog: ChatLog): Long {
        return dao.insertChatLog(chatLog)
    }

    suspend fun clearChatLogs() {
        dao.clearChatLogs()
    }
}
