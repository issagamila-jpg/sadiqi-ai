package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import com.example.voice.SadiqiVoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SadiqiViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SadiqiViewModel"
    private val database = SadiqiDatabase.getDatabase(application)
    private val repository = SadiqiRepository(database.sadiqiDao())

    // UI States
    val notifications: StateFlow<List<NotificationLog>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<DailyTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatLogs: StateFlow<List<ChatLog>> = repository.allChatLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Listening & Speaking states
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _focusModeActive = MutableStateFlow(false)
    val focusModeActive: StateFlow<Boolean> = _focusModeActive.asStateFlow()

    private val _studyHourStart = MutableStateFlow("08:00")
    val studyHourStart: StateFlow<String> = _studyHourStart.asStateFlow()

    private val _studyHourEnd = MutableStateFlow("18:00")
    val studyHourEnd: StateFlow<String> = _studyHourEnd.asStateFlow()

    private val _accessibilityConnected = MutableStateFlow(false)
    val accessibilityConnected: StateFlow<Boolean> = _accessibilityConnected.asStateFlow()

    // Voice Engine
    private var voiceEngine: SadiqiVoiceEngine? = null

    init {
        // Setup default voice assistant engine
        voiceEngine = SadiqiVoiceEngine(
            context = application,
            onSpeechResult = { text ->
                handleVoiceResult(text)
            },
            onListeningStateChanged = { listening ->
                _isListening.value = listening
            },
            onTtsSpeakingStateChanged = { speaking ->
                _isSpeaking.value = speaking
            }
        )

        // Add an initial greeting message if the chat is empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.allChatLogs.collect { logs ->
                if (logs.isEmpty()) {
                    repository.insertChatLog(
                        ChatLog(
                            sender = "ai",
                            message = "أهلاً بك! أنا صديقي (Sadiqi AI)، المساعد الشخصي الذكي ديالك بالدارجة. تقدّرك تهضر معايا بالصوت ولا تكتب ليا، وأنا هنا باش نلخص ليك الميساجات، ننظم ليك وقتك، ونبهك إلا كنتي كتضيع الوقت. باش نقدر نبدأ؟"
                        )
                    )
                }
            }
        }
    }

    private val SYSTEM_INST_COACH = """
        You are Sadiqi AI (صديقي الذكي), an ultra-smart, protective, and energetic Moroccan personal voice assistant.
        You speak exclusively in warm, natural Moroccan Darija (الداريجة المغربية) with Arabic script (e.g. "أهلاً", "شنو كاين اليوم؟", "نوض تقرا بركة من اللعب فالتلفون").
        Your goal is to help the user learn good habits, summarize notifications, suggest daily tasks, and guard them from wasting time.
        When replying, be extremely supportive, funny (using popular local proverbs when appropriate), and direct.
        If the user asks you to execute a command, answer as if you executed it and explain what you did.
    """.trimIndent()

    fun sendMessage(text: String, isVoice: Boolean = false) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // Log user message
            repository.insertChatLog(ChatLog(sender = "user", message = text, isVoice = isVoice))
            
            _isGenerating.value = true

            // Formulate prompt with habits context if available
            val currentHabits = habits.value
            val habitContext = if (currentHabits.isNotEmpty()) {
                "User current learned habits:\n" + currentHabits.joinToString("\n") { 
                    "- ${it.name} at ${it.timeOfDay} during ${it.durationMinutes} mins (Confidence: ${it.confidence})"
                }
            } else {
                "No habits learned yet."
            }

            val fullPrompt = """
                $habitContext
                
                User Message: "$text"
                
                Respond in friendly, protective, and concise Moroccan Darija. Keep it under 3 sentences.
            """.trimIndent()

            try {
                // Call Gemini
                val aiReply = GeminiClient.generateResponse(fullPrompt, SYSTEM_INST_COACH)
                repository.insertChatLog(ChatLog(sender = "ai", message = aiReply))
                
                // Read text out loud if user interacted with voice or if active speaking mode is desired
                if (isVoice) {
                    speak(aiReply)
                }
            } catch (e: Exception) {
                repository.insertChatLog(ChatLog(sender = "ai", message = "عذراً أخويا/أختي، وقع مشكل فالاتصال بالشبكة. حاول مرة أخرى."))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun handleVoiceResult(text: String) {
        // Quick Voice offline command parsing for direct automation before calling Gemini
        val cleanText = text.lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            when {
                // Task Command: "زيد مهمة كذا" or "add task"
                cleanText.contains("زيد مهمة") || cleanText.contains("مهمة جديدة") || cleanText.contains("add task") -> {
                    val taskTitle = text.substringAfter("زيد مهمة").substringAfter("مهمة جديدة").substringAfter("add task").trim()
                    if (taskTitle.isNotEmpty()) {
                        repository.insertTask(DailyTask(title = taskTitle, description = "تمت إضافتها عبر الأوامر الصوتية", suggestedByAi = false))
                        val reply = "واخا! زدت ليك مهمة '$taskTitle' للائحة المهام اليومية."
                        repository.insertChatLog(ChatLog(sender = "user", message = text, isVoice = true))
                        repository.insertChatLog(ChatLog(sender = "ai", message = reply))
                        speak(reply)
                    } else {
                        sendMessage(text, isVoice = true)
                    }
                }
                // Toggle study/focus mode commands
                cleanText.contains("خدم وضع الدراسة") || cleanText.contains("focus mode") || cleanText.contains("خدم التركيز") -> {
                    _focusModeActive.value = true
                    val reply = "تم تفعيل وضع الدراسة بنجاح! دابا غانراقب الإشعارات ديالك غي تقرب لشي تطبيق بحال تيكطوك غانبهك فالبلاصة."
                    repository.insertChatLog(ChatLog(sender = "user", message = text, isVoice = true))
                    repository.insertChatLog(ChatLog(sender = "ai", message = reply))
                    speak(reply)
                }
                // Clear habits
                cleanText.contains("مسح العادات") || cleanText.contains("فرمت العادات") -> {
                    repository.clearHabits()
                    val reply = "مزيان، مسحت كاع العادات المخزنة مؤقتاً."
                    repository.insertChatLog(ChatLog(sender = "user", message = text, isVoice = true))
                    repository.insertChatLog(ChatLog(sender = "ai", message = reply))
                    speak(reply)
                }
                // Check daily schedule
                cleanText.contains("شنو عندي اليوم") || cleanText.contains("برنامج اليوم") -> {
                    val replyHeader = "هاهو البرنامج ديالك ليوما: "
                    repository.allTasks.collect { tasks ->
                        val taskList = if (tasks.isEmpty()) "ماعندك حتى شي مهمة دابا، مرتاح مع راسك!" else tasks.joinToString(", ") { it.title }
                        val fullReply = "$replyHeader $taskList."
                        repository.insertChatLog(ChatLog(sender = "user", message = text, isVoice = true))
                        repository.insertChatLog(ChatLog(sender = "ai", message = fullReply))
                        speak(fullReply)
                    }
                }
                else -> {
                    // Send to general Gemini brain with Speech
                    sendMessage(text, isVoice = true)
                }
            }
        }
    }

    // Voice Engine Actions
    fun speak(text: String) {
        voiceEngine?.speak(text)
    }

    fun stopSpeaking() {
        voiceEngine?.stopSpeaking()
    }

    fun startListening() {
        voiceEngine?.startListening()
    }

    fun stopListening() {
        voiceEngine?.stopListening()
    }

    fun triggerSimulatedVoiceText(text: String) {
        _isListening.value = false
        handleVoiceResult(text)
    }

    // Notifications Analytics: "كيلخص الرسائل الطويلة وكيعرف واش كتضيع الوقت"
    fun analyzeNotification(log: NotificationLog) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            val prompt = """
                Analyze this notification received on user's phone:
                App: ${log.packageName}
                Sender/Title: ${log.title}
                Content/Text: ${log.text}
                
                Is the user wasting time if they open this app during study times? Yes or No.
                Provide a short, 1-sentence bullet point summary in clean Moroccan Darija.
                Also, add a funny protective Moroccan Darija warning if the app is a time-wasting app (like TikTok, Instagram, Snapchat, PUBG) and they should go study list.
                
                Respond in structured JSON format like this:
                {
                  "isWaste": true/false,
                  "summary": "ملخص الميساج بالدارجة",
                  "alert": "جملة تنبيهية مضحكة بالدارجة بحال نوض تقرا بركة من الضياع د الوقت"
                }
            """.trimIndent()

            try {
                val apiResult = GeminiClient.generateResponse(prompt)
                Log.d(TAG, "Notification Analysis Raw: $apiResult")
                
                val cleanJson = extractJson(apiResult)
                val jsonObj = JSONObject(cleanJson)
                val isWaste = jsonObj.getBoolean("isWaste")
                val summary = jsonObj.getString("summary")
                val alert = if (isWaste) jsonObj.optString("alert", "نوض تقرا!") else ""

                repository.updateNotificationAnalysis(log.id, summary, isWaste)

                if (isWaste && _focusModeActive.value) {
                    // Alert user with Speech aloud!
                    speak(alert)
                    repository.insertChatLog(ChatLog(sender = "ai", message = "⚠️ تنبيه تضييع الوقت: $alert"))
                } else if (summary.isNotEmpty()) {
                    repository.insertChatLog(ChatLog(sender = "ai", message = "📊 ملخص الإشعار: $summary"))
                }
            } catch (e: Exception) {
                // Fallback analysis
                val isWaste = isWasteTimePackage(log.packageName)
                val summary = "رسالة قصيرة من طرف ${log.title}: ${log.text}"
                repository.updateNotificationAnalysis(log.id, summary, isWaste)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun isWasteTimePackage(pkg: String): Boolean {
        return pkg.contains("tiktok") || pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("youtube") || pkg.contains("game")
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf("{")
        val end = raw.lastIndexOf("}") + 1
        return if (start in 0 until end) raw.substring(start, end) else raw
    }

    // Auto Daily Tasks Generator: "كيقترح عليك مهام يومية تلقائياً بناءً على العادات"
    fun autoSuggestTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            val currentHabits = habits.value
            
            val prompt = """
                The user has the following learned phone habits:
                ${currentHabits.joinToString("\n") { "- Name: ${it.name}, Time: ${it.timeOfDay}, Duration: ${it.durationMinutes} mins." }}
                
                Suggest exactly 3 healthy daily tasks in Moroccan Darija to improve their discipline, study schedules, or sleep cycles.
                Response must be in strict JSON Array format:
                [
                  {
                    "title": "عنوان المهمة بالدارجة المكتوبة بحروف عربية وتكون مركزة",
                    "description": "شرح بسيط مبهج ومحفز للمهمة بالدارجة"
                  },
                  ...
                ]
            """.trimIndent()

            try {
                val response = GeminiClient.generateResponse(prompt)
                val jsonStr = extractJsonArray(response)
                val jsonArray = JSONArray(jsonStr)
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.getString("title")
                    val desc = obj.getString("description")
                    repository.insertTask(DailyTask(title = title, description = desc, suggestedByAi = true))
                }
                
                speak("أهلاً! قتارحت عليك 3 ديال المهام جداد ليوما فجدول الأعمال ديالك باش ننظم وقتنا حسن!")
            } catch (e: Exception) {
                // Mock fallback tasks
                repository.insertTask(DailyTask(title = "مراجعة الدروس و تلخيص المحاضرات", description = "جلس 45 دقيقة مركز بلا تيكطوك بلا إنستغرام.", suggestedByAi = true))
                repository.insertTask(DailyTask(title = "شرب الماء والراحة", description = "شرب كاس د لماء و رتاح شوية من مورا كل حصة.", suggestedByAi = true))
                repository.insertTask(DailyTask(title = "النوم المبكر في 10 ليلاً", description = "بعد على الشاشات ساعة قبل النوم باش تفيق نشيط.", suggestedByAi = true))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun extractJsonArray(raw: String): String {
        val start = raw.indexOf("[")
        val end = raw.lastIndexOf("]") + 1
        return if (start in 0 until end) raw.substring(start, end) else raw
    }

    // Interactive custom state manipulation for learning demo
    fun addSimulatedNotification(title: String, text: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = NotificationLog(title = title, text = text, packageName = "com.$appName")
            val id = repository.insertNotification(log)
            analyzeNotification(log.copy(id = id.toInt()))
        }
    }

    fun learnMockHabits() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHabits()
            repository.insertHabit(Habit(name = "سهر ف التلفون (تيكطوك)", timeOfDay = "23:45", durationMinutes = 120, confidence = 0.95f, description = "ساهي ف التيكطوك وقت النعاس"))
            repository.insertHabit(Habit(name = "الدراسة الصباحية", timeOfDay = "09:00", durationMinutes = 180, confidence = 0.88f, description = "متبع القراية د لونجلي"))
            repository.insertHabit(Habit(name = "ألعاب الهاتف (ببجي)", timeOfDay = "16:20", durationMinutes = 90, confidence = 0.72f, description = "اللعب فالعشية من مورا الحصص"))
            speak("تعلمت العادات ديالك بنجاح! دابا نقدر نقتارح عليك جداول مهام يومية محفزة فالبلاصة.")
        }
    }

    fun toggleFocusMode() {
        _focusModeActive.value = !_focusModeActive.value
        val stateMsg = if (_focusModeActive.value) "مفعّل" else "غير مفعّل"
        speak("وضع التركيز والدراسة الآن $stateMsg.")
    }

    fun toggleAccessibilitySimulated() {
        _accessibilityConnected.value = !_accessibilityConnected.value
        val stateMsg = if (_accessibilityConnected.value) "متصل بنجاح" else "غير متصل"
        speak("محاكي خدمة تسهيل الاستخدام الآن $stateMsg.")
    }

    fun setStudyHours(start: String, end: String) {
        _studyHourStart.value = start
        _studyHourEnd.value = end
        speak("تم تحديث أوقات الدراسة بنجاح من $start إلى $end. غانشدو النيشان دابا!")
    }

    fun completeTask(id: Int, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaskStatus(id, isChecked)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(id)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearNotifications()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine?.destroy()
    }
}
