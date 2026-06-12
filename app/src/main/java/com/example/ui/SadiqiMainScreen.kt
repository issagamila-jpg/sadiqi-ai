package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun SadiqiMainScreen(viewModel: SadiqiViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    val chatLogs by viewModel.chatLogs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val focusModeActive by viewModel.focusModeActive.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SlateCardSurface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple(0, "صديقي", Icons.Default.Person),
                    Triple(1, "العادات", Icons.Default.Star),
                    Triple(2, "المهام", Icons.Default.Check),
                    Triple(3, "الإشعارات", Icons.Default.Notifications),
                    Triple(4, "الضبط", Icons.Default.Settings)
                )

                tabs.forEach { (index, title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan,
                            unselectedIconColor = CharcoalGray,
                            unselectedTextColor = CharcoalGray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateDarkBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = SlateCardSurface,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, CyberCyan.copy(alpha = 0.2f)),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (focusModeActive) WarnAmber else CyberCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SADIQI AI | صديقي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (focusModeActive) WarnAmber else CyberCyan,
                                letterSpacing = 1.sp
                            )
                        }

                        Row {
                            if (focusModeActive) {
                                Badge(
                                    containerColor = WarnAmber,
                                    contentColor = Color.Black,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("وضع الدراسة مفعّل", fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                }
                            }
                            IconButton(onClick = { viewModel.toggleFocusMode() }) {
                                Icon(
                                    imageVector = if (focusModeActive) Icons.Default.Star else Icons.Default.PlayArrow,
                                    contentDescription = "تفعيل وضع التركيز",
                                    tint = if (focusModeActive) WarnAmber else CharcoalGray
                                )
                            }
                        }
                    }
                }

                // Main Content View based on Tab State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> ChatTabScreen(
                            viewModel = viewModel,
                            chatLogs = chatLogs,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            isGenerating = isGenerating
                        )
                        1 -> HabitsTabScreen(viewModel = viewModel, habits = habits)
                        2 -> TasksTabScreen(viewModel = viewModel, tasks = tasks)
                        3 -> NotificationsTabScreen(viewModel = viewModel, notifications = notifications)
                        4 -> SettingsTabScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: CHAT & VOICE ENGINE INTERACTIVE SPACE
// -------------------------------------------------------------
@Composable
fun ChatTabScreen(
    viewModel: SadiqiViewModel,
    chatLogs: List<ChatLog>,
    isListening: Boolean,
    isSpeaking: Boolean,
    isGenerating: Boolean
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll to bottom when new logs appear
    LaunchedEffect(chatLogs.size) {
        if (chatLogs.isNotEmpty()) {
            listState.animateScrollToItem(chatLogs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatLogs) { log ->
                    ChatBubble(log = log, onSpeakClick = { viewModel.speak(log.message) })
                }
                if (isGenerating) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                                modifier = Modifier.border(0.5.dp, CyberCyan, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "صديقي يكتب الآن...",
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Floating Voice Orb Animation
        Box(
            modifier = Modifier
                .fillOuterVoiceHeight()
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, SlateDarkBackground.copy(alpha = 0.9f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            VoiceOrbVisualizer(
                isSpeaking = isSpeaking,
                isListening = isListening,
                onClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
            )
        }

        // Suggestion Quick Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val suggestChips = listOf(
                "زيد مهمة تذكيرية" to "زيد مهمة مراجعة الدارجيجة ليوما مع 20:00",
                "تعلم عاداتي" to "learn_mock",
                "لخص الكليبرود" to "summarize_clip"
            )

            suggestChips.forEach { (label, actionText) ->
                SuggestionChip(
                    onClick = {
                        if (actionText == "learn_mock") {
                            viewModel.learnMockHabits()
                        } else if (actionText == "summarize_clip") {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                viewModel.sendMessage("لخص ليا هاد النص الطويل أخويا: \n$clip")
                            } else {
                                viewModel.sendMessage("لخص ليا الميساج ولكن الكليبورد خاوي! يرجى نسخ نص أولا.")
                            }
                        } else {
                            viewModel.sendMessage(actionText)
                        }
                    },
                    label = { Text(label, fontSize = 11.sp, color = CyberCyan) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = SlateCardSurface,
                        labelColor = CyberCyan
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = CyberCyan.copy(alpha = 0.5f),
                        enabled = true
                    )
                )
            }
        }

        // Input text block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("اكتب رسالة بالدارجة...", color = CharcoalGray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCardSurface,
                    unfocusedContainerColor = SlateCardSurface,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CharcoalGray
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    }
                )
            )

            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    } else {
                        viewModel.triggerSimulatedVoiceText("شنو هي العادات ديالي لي تعلمتي ليوما؟")
                    }
                },
                containerColor = CyberCyan,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.PlayArrow,
                    contentDescription = "إرسال",
                    tint = Color.Black
                )
            }
        }
    }
}

private fun Modifier.fillOuterVoiceHeight() = this.height(115.dp)

// -------------------------------------------------------------
// CHAT BUBBLE LAYOUT
// -------------------------------------------------------------
@Composable
fun ChatBubble(log: ChatLog, onSpeakClick: () -> Unit) {
    val isUser = log.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) CyberCyan else SlateCardSurface
    val textColor = if (isUser) Color.Black else IceWhite
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CyberCyan, CircleShape)
                        .border(1.dp, CyberCyanDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Sadiqi",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .border(
                        width = 0.5.dp,
                        color = if (isUser) Color.Transparent else CyberCyan.copy(alpha = 0.3f),
                        shape = shape
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = log.message,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onSpeakClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "نطق الرسالة بالصوت",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SlateCardSurface, CircleShape)
                        .border(1.dp, CharcoalGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "أنت",
                        tint = IceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PULSING HOLOGRAPHIC ORB COMPOSABLE
// -------------------------------------------------------------
@Composable
fun VoiceOrbVisualizer(isSpeaking: Boolean, isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()

    // Breathing pulse
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else if (isSpeaking) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else if (isSpeaking) 1000 else 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Glowing blur simulation
    val glowThickness by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = if (isListening) 16f else if (isSpeaking) 12f else 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 400 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(Color.Transparent)
        ) {
            // Glow backdrop
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val baseRadius = (size.width / 3.5f) * scalePulse
                
                // Secondary wave
                drawCircle(
                    color = if (isListening) DeepCoralRed.copy(alpha = 0.2f) else if (isSpeaking) WarnAmber.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.1f),
                    radius = baseRadius + glowThickness * 1.5f,
                    center = centerOffset
                )

                // Primary glowing neon ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.8f), Color.Transparent),
                        center = centerOffset,
                        radius = baseRadius + glowThickness
                    ),
                    radius = baseRadius + glowThickness,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Core Interactive physical Sphere
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(DeepCoralRed, Color(0xFF990000))
                            } else if (isSpeaking) {
                                listOf(WarnAmber, Color(0xFF996600))
                            } else {
                                listOf(CyberCyan, SlateCardSurface)
                            }
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, IceWhite.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "أيقونة الصوت",
                    tint = if (isListening || isSpeaking) Color.Black else CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isListening) "كنسمعك دابا... هضر" else if (isSpeaking) "يتكلم صديقي..." else "إضغط على صديقي للتحدث",
            fontSize = 11.sp,
            color = if (isListening) DeepCoralRed else if (isSpeaking) WarnAmber else CyberCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// -------------------------------------------------------------
// TAB 1: DYNAMIC LEARNED HABITS TELEMETRY & STUDY GUARDIAN
// -------------------------------------------------------------
@Composable
fun HabitsTabScreen(viewModel: SadiqiViewModel, habits: List<Habit>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "استخبار وتحليل العادات العقلية 🧠",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "صديقي يراقب تصفحك وإشعارات تطبيقات ومستويات تشتتك طوال اليوم في الخلفية للتعرف التلقائي على نشاطك اليومي وتصنيفه.",
                        fontSize = 12.sp,
                        color = CharcoalGray,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = { viewModel.learnMockHabits() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("محاكاة تعلم عادات جديدة", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        if (habits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "فارغ",
                            tint = CharcoalGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "لم يتم تحليل أي عادة بعد.",
                            color = CharcoalGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "اضغط على الزر أعلاه لمحاكاة تعلم العادات من الإشعارات السابقة.",
                            color = CharcoalGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "العادات المستخلصة (${habits.size}):",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = IceWhite
                )
            }

            items(habits) { habit ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, IceWhite.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                habit.name,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (habit.name.contains("سهر")) WarnAmber else CyberCyan,
                                fontSize = 15.sp
                            )
                            
                            Badge(
                                containerColor = if (habit.name.contains("سهر")) WarnAmber.copy(0.2f) else CyberCyan.copy(0.2f),
                                contentColor = if (habit.name.contains("سهر")) WarnAmber else CyberCyan
                            ) {
                                Text("ثقة: ${(habit.confidence * 100).toInt()}%", modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "التوقيت التقريبي: ${habit.timeOfDay} | المدة: ${habit.durationMinutes} دقيقة",
                            color = IceWhite,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "الوصف: ${habit.description}",
                            color = CharcoalGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { habit.confidence },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (habit.name.contains("سهر")) WarnAmber else CyberCyan,
                            trackColor = SlateDarkBackground
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: SUGGESTED DAILY TASKS & ALMANAC
// -------------------------------------------------------------
@Composable
fun TasksTabScreen(viewModel: SadiqiViewModel, tasks: List<DailyTask>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "تعديل المهام اليومية بالذكاء الاصطناعي 📝",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "يتنبأ صديقي بالمهام المناسبة لتحسين نومك وتنظيم مراجعتك تلقائياً بالتوافق مع العادات التي تعلمها مسبقاً عن هاتفنا.",
                        fontSize = 12.sp,
                        color = CharcoalGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.autoSuggestTasks() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اقتراح جدول المهام بالذكاء الاصطناعي", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "فارغ",
                            tint = CharcoalGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "اللائحة خالية الآن.",
                            color = CharcoalGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "اضغط على زر الاقتراح أعلاه ليقوم صديقي بتحليل عاداتك وتوليد 3 مهام روتينية مخصصة بالدارجة المغربية.",
                            color = CharcoalGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "المهام اليومية الجارية:",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = IceWhite
                    )

                    TextButton(onClick = { viewModel.clearNotifications() }) {
                        Text("ترتيب السجل", color = DeepCoralRed, fontSize = 11.sp)
                    }
                }
            }

            items(tasks) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = if (task.isCompleted) CharcoalGray.copy(0.2f) else CyberCyan.copy(0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.completeTask(task.id, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CyberCyan,
                                uncheckedColor = CharcoalGray
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (task.isCompleted) CharcoalGray else IceWhite,
                                style = LocalTextStyle.current.copy(
                                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                fontSize = 11.sp,
                                color = CharcoalGray
                            )

                            if (task.suggestedByAi) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Badge(containerColor = CyberCyan.copy(alpha = 0.1f), contentColor = CyberCyan) {
                                    Text("مقترح بالذكاء الاصطناعي ✨", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "مسح",
                                tint = DeepCoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: NOTIFICATIONS INTERCEPTOR & CLIPBOARD ANALYZER
// -------------------------------------------------------------
@Composable
fun NotificationsTabScreen(viewModel: SadiqiViewModel, notifications: List<NotificationLog>) {
    var mockTitle by remember { mutableStateOf("") }
    var mockText by remember { mutableStateOf("") }
    var mockApp by remember { mutableStateOf("whatsapp") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mock Generator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "محاكي ومحلل الرسائل والإشعارات 📲",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "اختبر محاكاة اعتراض إشعارات الهاتف للتأكد من قدرة صديقي على تلخيص الميساجات الطويلة وتحديد مسببات التشتيت خلال وقت الدراسة:",
                        fontSize = 11.sp,
                        color = CharcoalGray,
                        lineHeight = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = mockTitle,
                        onValueChange = { mockTitle = it },
                        label = { Text("المرسل / العنوان التجريبي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CharcoalGray
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = mockText,
                        onValueChange = { mockText = it },
                        label = { Text("نص الرسالة الطويلة جداً للمحاكاة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CharcoalGray
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("اختر التطبيق التجريبي المشتت:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("whatsapp", "tiktok", "facebook", "gmail").forEach { app ->
                            val isSelected = mockApp == app
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) CyberCyan else SlateDarkBackground, RoundedCornerShape(8.dp))
                                    .clickable { mockApp = app }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    app.uppercase(),
                                    color = if (isSelected) Color.Black else CharcoalGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (mockTitle.isNotBlank() && mockText.isNotBlank()) {
                                viewModel.addSimulatedNotification(mockTitle, mockText, mockApp)
                                mockTitle = ""
                                mockText = ""
                            } else {
                                viewModel.addSimulatedNotification(
                                    "صاحبك حكيم",
                                    "فينك أخويا واش نتا فدار؟ راني حصلت فواحد التمرين د لماط ومعقلتش على القاعدة د الاشتقاق، تبارك الله عليك عتقني ولا كملتي الصبر غي صيفط ليا دبا دبا، راني سهران كنسنى الرد ديالك ضروري!",
                                    "whatsapp"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (mockTitle.isBlank()) "محاكاة إشعار واتساب للتلخيص" else "إرسال الإشعار المحاكى للمحلل",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // List
        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "السجل خالي، لم ترِد إشعارات بعد.",
                        color = CharcoalGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "سجل الإشعارات المعترضة (${notifications.size}):",
                        fontWeight = FontWeight.ExtraBold,
                        color = IceWhite,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { viewModel.clearNotifications() }) {
                        Text("مسح السجل", color = DeepCoralRed, fontSize = 11.sp)
                    }
                }
            }

            items(notifications) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, if (log.wastingTimeDetected) WarnAmber.copy(0.4f) else CharcoalGray.copy(0.2f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (log.wastingTimeDetected) WarnAmber else CyberCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.packageName.replace("com.", "").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (log.wastingTimeDetected) WarnAmber else CyberCyan
                                )
                            }
                            
                            Text(
                                text = "منذ قليل",
                                color = CharcoalGray,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "العنوان: " + log.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = IceWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "المحتوى: " + log.text,
                            fontSize = 12.sp,
                            color = IceWhite.copy(0.8f)
                        )

                        if (log.summary.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = CharcoalGray.copy(0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Badge(containerColor = CyberCyan.copy(0.1f), contentColor = CyberCyan) {
                                Text("تلخيص صديقي الذكي ✨", modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.summary,
                                fontSize = 12.sp,
                                color = CyberCyan,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.analyzeNotification(log) },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDarkBackground),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("طلب تلخيص فوري للمحتوى", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: CONFIG & ACCESSIBILITY SERVICE SETUP DIRECTIVES
// -------------------------------------------------------------
@Composable
fun SettingsTabScreen(viewModel: SadiqiViewModel) {
    var studyStartInput by remember { mutableStateOf("08:00") }
    var studyEndInput by remember { mutableStateOf("18:00") }
    val focusModeActive by viewModel.focusModeActive.collectAsState()
    val accessibilityConnected by viewModel.accessibilityConnected.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Study config hours
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "تعديل أوقات الدراسة والتركيز 🎯",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "حدد الوقت الذي تلتزم فيه بالدراسة اليومية ليقوم صديقي بصد التنبيهات المشتتة للتطبيقات وغسل عقلك نصياً وصوتياً:",
                        fontSize = 11.sp,
                        color = CharcoalGray,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = studyStartInput,
                            onValueChange = { studyStartInput = it },
                            label = { Text("البداية (مثلاً 08:00)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CharcoalGray
                            )
                        )
                        OutlinedTextField(
                            value = studyEndInput,
                            onValueChange = { studyEndInput = it },
                            label = { Text("النهاية (مثلاً 18:00)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CharcoalGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.setStudyHours(studyStartInput, studyEndInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ وتحديث الأوقات", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // Accessibility instructions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, CharcoalGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "كيفاش تخدم خدمة تسهيل الاستخدام والاعتراض؟ 🛠️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لتفعيل القراءة الحقيقية للإشعارات والتحكم الصوتي في التطبيقات على هاتفك، اتبع الخطوات التالية:",
                        fontSize = 12.sp,
                        color = CharcoalGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val instructions = listOf(
                        "1. اذهب لإعدادات نظام الهاتف الحقيقي (Settings).",
                        "2. ابحث عن 'تسهيل الاستخدام' أو 'إمكانية الوصول' (Accessibility).",
                        "3. اختر 'الخدمات المثبتة' (Installed Services).",
                        "4. فعل الخيار الخاص بـ 'Sadiqi AI Voice & Automation Assistant'.",
                        "5. لتفعيل اعتراض الإشعارات: ابحث عن 'الوصول للإشعارات' (Notification Access) في إعدادات الهاتف وامنح الصلاحية لـ Sadiqi AI."
                    )

                    instructions.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 11.sp,
                            color = IceWhite,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CharcoalGray.copy(0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع محاكاة الخدمات النشط", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IceWhite)
                            Text("تجاوز الصلاحيات الحقيقية للتجريب داخل التطبيق", fontSize = 10.sp, color = CharcoalGray)
                        }

                        Switch(
                            checked = accessibilityConnected,
                            onCheckedChange = { viewModel.toggleAccessibilitySimulated() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = SlateDarkBackground
                            )
                        )
                    }
                }
            }
        }
    }
}
