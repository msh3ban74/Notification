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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.lazy.itemsIndexed
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
    onOpenNotifications: () -> Unit = {},
    // AI sprint — chat controls.
    onStopGeneration: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onClearChat: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

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
                    val lastModelIndex = messages.indexOfLast { it.role == "model" }
                    itemsIndexed(messages) { index, msg ->
                        val isUser = msg.role == "user"
                        val text = msg.parts.firstOrNull { !it.text.isNullOrBlank() }?.text ?: ""
                        if (text.isNotBlank()) {
                            // The newest model reply reveals progressively
                            // (typing effect); older ones render instantly.
                            ChatBubble(
                                text = text,
                                isUser = isUser,
                                animateReveal = !isUser && index == lastModelIndex
                            )
                            // Copy / share row under every model reply.
                            if (!isUser) {
                                MessageActionsRow(
                                    isArabic = isArabic,
                                    onCopy = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(text)) },
                                    onShare = { shareText(context, text) },
                                    // Regenerate only under the newest reply.
                                    onRegenerate = if (index == lastModelIndex && !isLoading) onRegenerate else null
                                )
                            }
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

                // While generating, this becomes a STOP button so the user
                // can always cancel — the conversation never traps them.
                FilledIconButton(
                    onClick = {
                        if (isLoading) {
                            onStopGeneration()
                        } else if (inputText.isNotBlank()) {
                            val messageToSend = inputText
                            inputText = ""
                            onSendMessage(messageToSend)
                        }
                    },
                    enabled = isLoading || inputText.isNotBlank(),
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
                        imageVector = if (isLoading) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isLoading) {
                            if (isArabic) "إيقاف" else "Stop"
                        } else {
                            if (isArabic) "إرسال" else "Send"
                        }
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

/** Copy / share / regenerate actions under a model reply. */
@Composable
private fun MessageActionsRow(
    isArabic: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: (() -> Unit)?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Spacing.xs, top = 2.dp)
    ) {
        TextButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isArabic) "نسخ" else "Copy", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isArabic) "مشاركة" else "Share", style = MaterialTheme.typography.labelMedium)
        }
        if (onRegenerate != null) {
            TextButton(onClick = onRegenerate, contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isArabic) "إعادة" else "Regenerate", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
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
 * Markdown-lite for Rafeeq's replies. Handles headers (#/##/###), bullets
 * (- / *), **bold**, inline `code`, fenced ```code blocks``` (monospace),
 * and simple pipe tables (rendered as spaced monospace rows, separator
 * rows dropped). Anything else prints as plain text — raw markdown syntax
 * is never shown.
 */
private fun renderMarkdownLite(text: String): androidx.compose.ui.text.AnnotatedString {
    val mono = androidx.compose.ui.text.SpanStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )
    val bold = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)

    fun androidx.compose.ui.text.AnnotatedString.Builder.appendInline(src: String) {
        var rest = src
        while (rest.isNotEmpty()) {
            val boldStart = rest.indexOf("**")
            val codeStart = rest.indexOf("`")
            // Nothing special left.
            if (boldStart < 0 && codeStart < 0) { append(rest); break }
            // Whichever marker comes first.
            val useBold = boldStart >= 0 && (codeStart < 0 || boldStart < codeStart)
            if (useBold) {
                val end = rest.indexOf("**", boldStart + 2)
                if (end < 0) { append(rest); break }
                append(rest.substring(0, boldStart))
                pushStyle(bold); append(rest.substring(boldStart + 2, end)); pop()
                rest = rest.substring(end + 2)
            } else {
                val end = rest.indexOf("`", codeStart + 1)
                if (end < 0) { append(rest); break }
                append(rest.substring(0, codeStart))
                pushStyle(mono); append(rest.substring(codeStart + 1, end)); pop()
                rest = rest.substring(end + 1)
            }
        }
    }

    return androidx.compose.ui.text.buildAnnotatedString {
        var inFence = false
        text.split("\n").forEachIndexed { lineIndex, rawLine ->
            val trimmed = rawLine.trimStart()

            // Fenced code block toggling.
            if (trimmed.startsWith("```")) {
                inFence = !inFence
                return@forEachIndexed
            }
            if (lineIndex > 0) append("\n")
            if (inFence) {
                pushStyle(mono); append(rawLine); pop()
                return@forEachIndexed
            }
            // Headers.
            if (trimmed.startsWith("#")) {
                pushStyle(bold); append(trimmed.trimStart('#', ' ')); pop()
                return@forEachIndexed
            }
            // Pipe table: drop separator rows, render cells spaced.
            if (trimmed.startsWith("|") && trimmed.indexOf('|', 1) >= 0) {
                val cells = trimmed.trim('|').split("|").map { it.trim() }
                if (cells.all { it.isEmpty() || it.all { ch -> ch == '-' || ch == ':' } }) {
                    return@forEachIndexed // separator row → skip
                }
                pushStyle(mono); append(cells.joinToString("   ")); pop()
                return@forEachIndexed
            }
            // Bullets.
            val line = when {
                trimmed.startsWith("- ") -> rawLine.replaceFirst("- ", "• ")
                trimmed.startsWith("* ") -> rawLine.replaceFirst("* ", "• ")
                else -> rawLine
            }
            appendInline(line)
        }
    }
}

/**
 * Premium chat bubble: large 22dp roundness with a subtle tail,
 * gold for the user, calm charcoal for Rafeeq. Max width keeps
 * lines comfortable to read; Rafeeq's replies render markdown-lite.
 */
@Composable
fun ChatBubble(text: String, isUser: Boolean, animateReveal: Boolean = false) {
    // Progressive "typing" reveal for the newest reply — the streaming
    // experience without the fragility of real SSE + tool interplay.
    var shown by remember(text) { mutableStateOf(if (animateReveal) 0 else text.length) }
    LaunchedEffect(text, animateReveal) {
        if (!animateReveal) { shown = text.length; return@LaunchedEffect }
        // ~45 chars/frame step keeps long replies quick but visibly typed.
        while (shown < text.length) {
            shown = (shown + 3).coerceAtMost(text.length)
            kotlinx.coroutines.delay(12)
        }
    }
    val display = text.take(shown)

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
                    renderMarkdownLite(display)
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
