package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "notifications")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val text: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnalyzed: Boolean = false,
    val summary: String = "",
    val wastingTimeDetected: Boolean = false
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g. "Study", "Sleeping", "Gaming", "Social MediaScroll"
    val timeOfDay: String, // e.g. "14:30" or "Night"
    val durationMinutes: Int,
    val confidence: Float, // e.g. 0.85
    val description: String, // e.g. "سهران مع طيكطوك" / "متبع القراية فالعشية"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val suggestedByAi: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_logs")
data class ChatLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val message: String,
    val isVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAOs ---

@Dao
interface SadiqiDao {
    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationLog): Long

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Query("UPDATE notifications SET isAnalyzed = 1, summary = :summary, wastingTimeDetected = :wastingTime WHERE id = :id")
    suspend fun updateNotificationAnalysis(id: Int, summary: String, wastingTime: Boolean)

    // Habits
    @Query("SELECT * FROM habits ORDER BY timestamp DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Int)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<DailyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask): Long

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    // Chat Logs
    @Query("SELECT * FROM chat_logs ORDER BY timestamp ASC")
    fun getAllChatLogs(): Flow<List<ChatLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatLog(chat: ChatLog): Long

    @Query("DELETE FROM chat_logs")
    suspend fun clearChatLogs()
}

// --- Room Database ---

@Database(
    entities = [NotificationLog::class, Habit::class, DailyTask::class, ChatLog::class],
    version = 1,
    exportSchema = false
)
abstract class SadiqiDatabase : RoomDatabase() {
    abstract fun sadiqiDao(): SadiqiDao

    companion object {
        @Volatile
        private var INSTANCE: SadiqiDatabase? = null

        fun getDatabase(context: Context): SadiqiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SadiqiDatabase::class.java,
                    "sadiqi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
