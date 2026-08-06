package com.notification.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.data.remote.GeminiContent
import com.notification.app.ui.designsystem.AppRadius
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.MaroonPrimaryDark
import com.notification.app.ui.theme.OnPrimary
import com.notification.app.ui.theme.Spacing

/**
 * Rafeeq AI Assistant — the heart of the application.
 *
 * Rafeeq Design Language: a calm, premium conversation surface —
 * generous spacing, large rounded bubbles, an elegant empty state with
 * smart suggestion pills, a breathing "thinking" indicator, and a large
 * pill-shaped input with a gold send button.
 *
 * UI ONLY — the message list, loading flag, and onSendMessage callback
 * (backed by the existing Gemini pipeline) are unchanged.
 */
@Composable
fun AiChatScreen(
    messages: List<GeminiContent>,
    isLoading: Boolean,
    isArabic: Boolean,
    onSendMessage: (String) -> Unit,
    // v1.0 Copilot — execute buttons under Rafeeq's replies, wired to
    // EXISTING navigation flows only.
    onOpenTasks: () -> Unit = {},
    onOpenLedger: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keep the newest message in view.
    LaunchedEffect(messages.size, isLoading) {
        val count = messages.size + if (isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Conversation area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                AssistantEmptyState(
                    isArabic = isArabic,
                    onSuggestionClick = onSendMessage
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(
                        start = Spacing.md, end = Spacing.md,
                        top = Spacing.md, bottom = Spacing.md
                    )
                ) {
                    items(messages) { msg ->
                        val isUser = msg.role == "user"
                        val text = msg.parts.firstOrNull()?.text ?: ""
                        if (text.isNotBlank()) {
                            ChatBubble(text = text, isUser = isUser)
                        }
                    }

                    if (isLoading) {
                        item { ThinkingIndicator(isArabic = isArabic) }
                    }

                    // v1.0 Copilot — contextual execute buttons derived from
                    // Rafeeq's LAST reply: each action opens an EXISTING flow.
                    if (!isLoading) {
                        val lastReply = messages.lastOrNull { it.role == "model" }
                            ?.parts?.firstOrNull()?.text
                        if (lastReply != null) {
                            item {
                                ActionChipsRow(
                                    replyText = lastReply,
                                    isArabic = isArabic,
                                    onOpenTasks = onOpenTasks,
                                    onOpenLedger = onOpenLedger,
                                    onOpenNotifications = onOpenNotifications
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input bar — large premium pill + gold send circle.
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "اسأل رفيق عن أي شيء..." else "Ask Rafeeq anything...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaroonPrimary
                        ),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val messageToSend = inputText
                            inputText = ""
                            onSendMessage(messageToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaroonPrimary,
                        contentColor = OnPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isArabic) "إرسال" else "Send"
                    )
                }
            }
        }
    }
}

/**
 * Elegant welcome: the gold Rafeeq mark with a soft halo, a warm
 * greeting, and smart suggestion pills that start a conversation.
 */
@Composable
private fun AssistantEmptyState(
    isArabic: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = if (isArabic) listOf(
        "ما هي مهامي اليوم؟",
        "هل لدي فواتير مستحقة؟",
        "أخبرني عن ديوني",
        "اضبط منبهًا بكرة ٦ الصبح"
    ) else listOf(
        "What are my tasks today?",
        "Do I have bills due?",
        "Tell me about my debts",
        "Set an alarm for 6 AM tomorrow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Gold mark with halo
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(MaroonPrimary.copy(alpha = 0.10f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        Brush.linearGradient(listOf(MaroonPrimary, MaroonPrimaryDark)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = if (isArabic) "مرحبًا، أنا رفيق" else "Hello, I'm Rafeeq",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = if (isArabic) "كيف يمكنني مساعدتك اليوم؟" else "How can I help you today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Suggestion pills — compact: wrap to content, smaller text/padding.
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    onClick = { onSuggestionClick(suggestion) },
                    shape = AppRadius.button,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Spacing.md,
                            vertical = Spacing.sm
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaroonPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Soft breathing dots — "Rafeeq is thinking". */
@Composable
private fun ThinkingIndicator(isArabic: Boolean) {
    val infinite = rememberInfiniteTransition(label = "Thinking")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "ThinkingPhase"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(vertical = Spacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                val distance = kotlin.math.abs(phase - index)
                val emphasis = 1f - minOf(distance, 1f)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(0.35f + 0.65f * emphasis)
                        .background(MaroonPrimary, CircleShape)
                )
            }
        }
        Text(
            text = if (isArabic) "رفيق يفكر..." else "Rafeeq is thinking...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * v1.0 Copilot — contextual execute buttons under Rafeeq's last reply.
 * Simple keyword routing; every chip opens an EXISTING flow, so the
 * assistant's advice is always one tap from action.
 */
@Composable
private fun ActionChipsRow(
    replyText: String,
    isArabic: Boolean,
    onOpenTasks: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    data class ChipAction(val label: String, val onClick: () -> Unit)

    val lower = replyText.lowercase()
    val chips = buildList {
        if ("فاتورة" in replyText || "فواتير" in replyText || "bill" in lower) {
            add(ChipAction(if (isArabic) "افتح الفواتير" else "Open Bills", onOpenTasks))
        }
        if ("دين" in replyText || "ديون" in replyText || "فلوس" in replyText ||
            "debt" in lower || "owe" in lower
        ) {
            add(ChipAction(if (isArabic) "افتح الدفتر" else "Open Ledger", onOpenLedger))
        }
        if ("مهمة" in replyText || "مهام" in replyText || "تذكير" in replyText ||
            "task" in lower || "reminder" in lower
        ) {
            add(ChipAction(if (isArabic) "افتح المهام" else "Open Tasks", onOpenTasks))
        }
        if ("منبه" in replyText || "alarm" in lower) {
            add(ChipAction(if (isArabic) "افتح التنبيهات" else "Open Alerts", onOpenNotifications))
        }
    }.distinctBy { it.label }.take(3)

    if (chips.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        chips.forEach { chip ->
            AssistChip(
                onClick = chip.onClick,
                label = { Text(chip.label, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaroonPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Markdown-lite for Rafeeq's replies: **bold** spans and "- " bullets
 * render properly instead of showing raw markdown characters.
 */
private fun renderMarkdownLite(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        text.split("\n").forEachIndexed { lineIndex, rawLine ->
            if (lineIndex > 0) append("\n")
            val line = when {
                rawLine.trimStart().startsWith("- ") ->
                    rawLine.replaceFirst("- ", "• ")
                rawLine.trimStart().startsWith("* ") ->
                    rawLine.replaceFirst("* ", "• ")
                else -> rawLine
            }
            var rest = line
            while (true) {
                val start = rest.indexOf("**")
                val end = if (start >= 0) rest.indexOf("**", start + 2) else -1
                if (start < 0 || end < 0) {
                    append(rest)
                    break
                }
                append(rest.substring(0, start))
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(rest.substring(start + 2, end))
                pop()
                rest = rest.substring(end + 2)
            }
        }
    }
}

/**
 * Premium chat bubble: large 22dp roundness with a subtle tail,
 * gold for the user, calm charcoal for Rafeeq. Max width keeps
 * lines comfortable to read; Rafeeq's replies render markdown-lite.
 */
@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaroonPrimary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) OnPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 8.dp,
                bottomEnd = if (isUser) 8.dp else 22.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = if (isUser) {
                    androidx.compose.ui.text.AnnotatedString(text)
                } else {
                    renderMarkdownLite(text)
                },
                // Follow the content's own direction so Arabic replies read
                // right-to-left (from the right edge) and English left-to-right,
                // instead of always starting from the left.
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = androidx.compose.ui.text.style.TextDirection.Content
                ),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}
