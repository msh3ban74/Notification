package com.notification.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notification.app.domain.model.SmartItemType
import com.notification.app.ui.components.PremiumTopAppBar
import com.notification.app.ui.components.SmartItemBottomSheet
import com.notification.app.ui.screens.*
import com.notification.app.ui.theme.NotificationTheme
import com.notification.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val titleEn: String, val titleAr: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", "البداية", Icons.Default.Notifications)

    // Sprint 1 — the four primary Bottom Navigation destinations.
    object Dashboard : Screen("dashboard", "Dashboard", "الرئيسية", Icons.Default.Dashboard)
    object AiChat : Screen("ai_chat", "AI Assistant", "المساعد الذكي", Icons.Default.AutoAwesome)
    object Tasks : Screen("tasks", "Tasks", "المهام", Icons.Default.Assignment)
    object Notifications : Screen("notifications", "Notifications", "الإشعارات", Icons.Default.NotificationsActive)

    // Existing feature screens — kept fully intact and reachable from
    // Dashboard's Quick Actions, but no longer shown in Bottom Navigation.
    object Home : Screen("home", "Home", "الرئيسية", Icons.Default.Home)
    object Reminders : Screen("reminders", "Reminders", "التذكيرات", Icons.Default.NotificationsActive)
    object Ledger : Screen("ledger", "Ledger", "دفتر الديون", Icons.Default.AccountBalanceWallet)
    object Gam3iya : Screen("gam3iya", "Gam3iya", "الجمعيات", Icons.Default.Group)
    object Islamic : Screen("islamic", "Islamic", "إسلاميات", Icons.Default.Mosque)
    object HealthNotes : Screen("health_notes", "Health/Notes", "الصحة", Icons.Default.WaterDrop)

    // Settings — reachable ONLY from the profile button in the Top App Bar.
    object Settings : Screen("settings", "Settings", "الإعدادات", Icons.Default.Settings)
    object Auth : Screen("auth", "Auth", "دخول", Icons.Default.Lock)

    // Sprint 2 — Smart Item Engine Foundation. Placeholder destination
    // reached from the Dashboard "+" bottom sheet. Parametrized by the
    // SmartItemType id (e.g. "gam3iya", "bill"). No chrome (top/bottom
    // bar) is shown here, same as Splash/Auth. Types that already have a
    // real form (Task — Sprint 3, Debt — Sprint 4) route to their own
    // screens below instead.
    object SmartItemPlaceholder : Screen(
        "smart_item/{itemId}", "Coming Soon", "قريبًا", Icons.Default.Add
    ) {
        fun createRoute(itemId: String) = "smart_item/$itemId"
    }

    // Sprint 3 — Smart Task: the first real Smart Item form. Reuses the
    // existing Reminder pipeline (entity, ViewModel, scheduler).
    // Sprint 5 — also the EDIT destination: an optional reminderId argument
    // pre-fills the same form for an existing reminder.
    object CreateTask : Screen(
        "create_task?reminderId={reminderId}", "New Task", "مهمة جديدة", Icons.Default.Add
    ) {
        fun createRoute(reminderId: Long? = null) =
            if (reminderId != null) "create_task?reminderId=$reminderId" else "create_task"
    }

    // Sprint 4 — Smart Debt: reuses the existing Ledger implementation.
    object CreateDebt : Screen("create_debt", "New Debt", "دين جديد", Icons.Default.Add)

    // Sprint 5 — Bill / Appointment / Medicine: the SAME shared reminder
    // form as Task, parametrized per type (SmartReminderFormConfig). One
    // pipeline, zero duplicated forms.
    object CreateSmartReminder : Screen(
        "create_smart/{itemId}", "New Item", "عنصر جديد", Icons.Default.Add
    ) {
        fun createRoute(itemId: String) = "create_smart/$itemId"
    }

    // Sprint 5 — Smart Gam3iya: reuses the existing Gam3iya implementation
    // (viewModel.createGam3iya → createGam3iyaWithMembers).
    object CreateGam3iya : Screen("create_gam3iya", "New Gam3iya", "جمعية جديدة", Icons.Default.Add)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val language by viewModel.language.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val userName by viewModel.userName.collectAsState()
            val userEmail by viewModel.userEmail.collectAsState()
            val geminiApiKey by viewModel.geminiApiKey.collectAsState()
            val lastSyncTime by viewModel.lastSyncTime.collectAsState()

            val reminders by viewModel.allReminders.collectAsState()
            val persons by viewModel.allPersons.collectAsState()
            val transactions by viewModel.allTransactions.collectAsState()
            val gam3iyas by viewModel.allGam3iyas.collectAsState()
            val gam3iyaMembers by viewModel.allGam3iyaMembers.collectAsState()
            val alarms by viewModel.allAlarms.collectAsState()
            val prayerTimes by viewModel.prayerTimes.collectAsState()
            val workNotes by viewModel.allWorkNotes.collectAsState()
            val chatMessages by viewModel.chatMessages.collectAsState()
            val isAiLoading by viewModel.isAiLoading.collectAsState()
            val aiSuggestions by viewModel.aiSuggestions.collectAsState()
            val aiSuggestionsLoading by viewModel.aiSuggestionsLoading.collectAsState()
            val waterCount by viewModel.waterCount.collectAsState()

            val isArabic = language == "ar"
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            NotificationTheme(darkTheme = isDarkMode, isArabic = isArabic) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Sprint 2 — Smart Item Engine Foundation.
                    var showSmartItemSheet by remember { mutableStateOf(false) }

                    // Sprint 3/4 — app-level snackbar (e.g. "Task created
                    // successfully" after returning from a Smart Item form).
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    // Sprint 1: Bottom Navigation contains ONLY these four
                    // primary destinations. Settings is intentionally
                    // excluded — it's reachable only from the Top App Bar's
                    // profile button.
                    val bottomBarScreens = listOf(
                        Screen.Dashboard,
                        Screen.AiChat,
                        Screen.Tasks,
                        Screen.Notifications
                    )

                    // Screens that show the Bottom Navigation + Top App Bar
                    // "chrome". Splash and Auth are full-bleed and excluded.
                    val chromeRoutes = bottomBarScreens.map { it.route }
                    val showChrome = currentRoute in chromeRoutes

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            if (showChrome) {
                                val currentScreen = bottomBarScreens.firstOrNull { it.route == currentRoute }
                                PremiumTopAppBar(
                                    title = if (currentScreen != null) {
                                        if (isArabic) currentScreen.titleAr else currentScreen.titleEn
                                    } else {
                                        if (isArabic) "رفيق" else "Rafeeq"
                                    },
                                    onProfileClick = { navController.navigate(Screen.Settings.route) }
                                )
                            }
                        },
                        bottomBar = {
                            if (showChrome) {
                                NavigationBar {
                                    bottomBarScreens.forEach { screen ->
                                        NavigationBarItem(
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Dashboard.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.titleEn
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = if (isArabic) screen.titleAr else screen.titleEn,
                                                    maxLines = 1
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            // Sprint 2 — Smart Item Engine Foundation.
                            // Sprint 2 (UI/UX Polish) — same behavior (opens
                            // the Smart Item sheet, Dashboard-only), only the
                            // visual weight was refined: a slightly larger
                            // touch target, a stronger resting elevation
                            // that lifts further on press, and a scale-in
                            // entrance so it doesn't just pop into place.
                            androidx.compose.animation.AnimatedVisibility(
                                visible = currentRoute == Screen.Dashboard.route,
                                enter = androidx.compose.animation.scaleIn(
                                    animationSpec = tween(com.notification.app.ui.designsystem.AppAnimationDuration.normal)
                                ) + androidx.compose.animation.fadeIn(
                                    animationSpec = tween(com.notification.app.ui.designsystem.AppAnimationDuration.normal)
                                ),
                                exit = androidx.compose.animation.scaleOut(
                                    animationSpec = tween(com.notification.app.ui.designsystem.AppAnimationDuration.fast)
                                ) + androidx.compose.animation.fadeOut(
                                    animationSpec = tween(com.notification.app.ui.designsystem.AppAnimationDuration.fast)
                                )
                            ) {
                                FloatingActionButton(
                                    onClick = { showSmartItemSheet = true },
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = com.notification.app.ui.designsystem.AppElevation.high,
                                        pressedElevation = com.notification.app.ui.designsystem.AppElevation.high + com.notification.app.ui.designsystem.AppElevation.medium
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = if (isArabic) "إضافة" else "Add",
                                        modifier = Modifier.size(com.notification.app.ui.designsystem.AppDimens.fabIconSize)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                fadeIn(animationSpec = tween(280)) +
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(280)
                                    )
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(200)) +
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(200)
                                    )
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(280)) +
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(280)
                                    )
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(200)) +
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(200)
                                    )
                            }
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(
                                    isArabic = isArabic,
                                    onSplashFinished = {
                                        // Sprint 1: Dashboard is now the default landing screen.
                                        val destination = if (isLoggedIn) Screen.Dashboard.route else Screen.Auth.route
                                        navController.navigate(destination) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Dashboard.route) {
                                // Sprint 3/4: the Dashboard now shows REAL
                                // summaries read from the existing Room flows
                                // already collected above — no fake data.
                                DashboardScreen(
                                    isArabic = isArabic,
                                    reminders = reminders,
                                    persons = persons,
                                    transactions = transactions,
                                    alarms = alarms,
                                    gam3iyaMembers = gam3iyaMembers,
                                    prayerTimes = prayerTimes,
                                    workNotes = workNotes,
                                    waterCount = waterCount,
                                    aiSuggestions = aiSuggestions,
                                    aiSuggestionsLoading = aiSuggestionsLoading,
                                    onRefreshSuggestions = {
                                        viewModel.refreshAiSuggestions(isArabic = isArabic)
                                    },
                                    onPullRefresh = {
                                        // Pull-to-refresh: bypass the TTL cache.
                                        viewModel.refreshAiSuggestions(isArabic = isArabic, force = true)
                                    },
                                    onWaterClick = { viewModel.incrementWater() },
                                    onAskRafeeq = { question ->
                                        // Existing assistant flow: open the AI
                                        // tab and send through the same pipeline.
                                        navController.navigate(Screen.AiChat.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        viewModel.sendAiMessage(question)
                                    },
                                    onNavigateToTasks = {
                                        navController.navigate(Screen.Tasks.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToNotifications = {
                                        navController.navigate(Screen.Notifications.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                                    onNavigateToGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                                    onNavigateToIslamic = { navController.navigate(Screen.Islamic.route) },
                                    onNavigateToHealthNotes = { navController.navigate(Screen.HealthNotes.route) }
                                )
                            }

                            composable(Screen.Tasks.route) {
                                // Sprint 1: "Tasks" reuses the existing, fully
                                // functional RemindersScreen and its real
                                // ViewModel-backed data.
                                // Sprint 5: editing opens the same Smart Task
                                // form pre-filled with the reminder.
                                RemindersScreen(
                                    reminders = reminders,
                                    isArabic = isArabic,
                                    onAddReminder = { viewModel.addReminder(it) },
                                    onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                    onEditReminder = { reminder ->
                                        navController.navigate(Screen.CreateTask.createRoute(reminder.id))
                                    }
                                )
                            }

                            composable(Screen.Notifications.route) {
                                // Sprint 5: real scheduled notifications from
                                // the existing reminder + alarm flows.
                                NotificationsScreen(
                                    reminders = reminders,
                                    alarms = alarms,
                                    isArabic = isArabic
                                )
                            }

                            composable(Screen.Home.route) {
                                HomeScreen(
                                    userName = userName,
                                    reminders = reminders,
                                    prayerTimes = prayerTimes,
                                    isArabic = isArabic,
                                    onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                                    onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                                    onNavigateToGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                                    onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) },
                                    onToggleReminder = { viewModel.toggleReminderCompleted(it) }
                                )
                            }

                            composable(Screen.Reminders.route) {
                                RemindersScreen(
                                    reminders = reminders,
                                    isArabic = isArabic,
                                    onAddReminder = { viewModel.addReminder(it) },
                                    onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                    onEditReminder = { reminder ->
                                        navController.navigate(Screen.CreateTask.createRoute(reminder.id))
                                    }
                                )
                            }

                            composable(Screen.Ledger.route) {
                                LedgerScreen(
                                    persons = persons,
                                    transactions = transactions,
                                    isArabic = isArabic,
                                    onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                    onAddTransaction = { viewModel.addLedgerTransaction(it) },
                                    onDeleteTransaction = { viewModel.deleteLedgerTransaction(it) },
                                    onUpdateTransaction = { viewModel.updateLedgerTransaction(it) }
                                )
                            }

                            composable(Screen.Gam3iya.route) {
                                Gam3iyaScreen(
                                    gam3iyas = gam3iyas,
                                    isArabic = isArabic,
                                    getMembersForGam3iya = { id -> viewModel.getMembersForGam3iya(id) },
                                    onCreateGam3iya = { title, total, installment, members, startDate ->
                                        viewModel.createGam3iya(title, total, installment, members, startDate)
                                    }
                                )
                            }

                            composable(Screen.AiChat.route) {
                                AiChatScreen(
                                    messages = chatMessages,
                                    isLoading = isAiLoading,
                                    isArabic = isArabic,
                                    onSendMessage = { viewModel.sendAiMessage(it) }
                                )
                            }

                            composable(Screen.Islamic.route) {
                                IslamicRemindersScreen(
                                    prayerTimes = prayerTimes,
                                    isArabic = isArabic
                                )
                            }

                            composable(Screen.HealthNotes.route) {
                                HealthWorkNotesScreen(
                                    workNotes = workNotes,
                                    waterCount = waterCount,
                                    isArabic = isArabic,
                                    onIncrementWater = { viewModel.incrementWater() },
                                    onAddNote = { title, content -> viewModel.addWorkNote(title, content) },
                                    onToggleNoteDone = { viewModel.toggleWorkNoteDone(it) },
                                    onDeleteNote = { viewModel.deleteWorkNote(it) }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    currentLanguage = language,
                                    isDarkMode = isDarkMode,
                                    isLoggedIn = isLoggedIn,
                                    userName = userName,
                                    userEmail = userEmail,
                                    geminiApiKey = geminiApiKey,
                                    lastSyncTime = lastSyncTime,
                                    isArabic = isArabic,
                                    onSetLanguage = { viewModel.setLanguage(it) },
                                    onSetDarkMode = { viewModel.setDarkMode(it) },
                                    onSetGeminiKey = { viewModel.setGeminiApiKey(it) },
                                    onTriggerBackup = { viewModel.triggerBackupSync() },
                                    onSignOut = {
                                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                        viewModel.setUserAuth("Guest User", "", false)
                                    }
                                )
                            }

                            composable(Screen.Auth.route) {
                                AuthScreen(
                                    onSignInSuccess = { name, email ->
                                        viewModel.setUserAuth(name, email, true)
                                        viewModel.restoreFromBackup()
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Auth.route) { inclusive = true }
                                        }
                                    },
                                    onSkip = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Auth.route) { inclusive = true }
                                        }
                                    },
                                    isArabic = isArabic
                                )
                            }

                            // Sprint 2 — Smart Item Engine Foundation.
                            // Single placeholder destination reused for the
                            // types that don't have a real form yet.
                            composable(
                                route = Screen.SmartItemPlaceholder.route,
                                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val itemId = backStackEntry.arguments?.getString("itemId")
                                val item = SmartItemType.all.firstOrNull { it.id == itemId }
                                ComingSoonScreen(
                                    titleEn = item?.titleEn ?: "",
                                    titleAr = item?.titleAr ?: "",
                                    isArabic = isArabic,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // Sprint 3 — Smart Task. Saving goes through the
                            // EXISTING reminder pipeline (viewModel.addReminder
                            // → repository → AlarmManagerScheduler), then
                            // returns to Tasks with a confirmation snackbar.
                            // Sprint 5 — the optional reminderId argument turns
                            // the same form into the EDIT flow (update +
                            // reschedule via viewModel.updateTaskReminder).
                            composable(
                                route = Screen.CreateTask.route,
                                arguments = listOf(
                                    navArgument("reminderId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                )
                            ) { backStackEntry ->
                                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
                                val editing = reminders.firstOrNull { it.id == reminderId }
                                CreateTaskScreen(
                                    isArabic = isArabic,
                                    initial = editing,
                                    onSave = { reminder ->
                                        if (editing != null) {
                                            viewModel.updateTaskReminder(reminder)
                                        } else {
                                            viewModel.addReminder(reminder)
                                        }
                                        navController.navigate(Screen.Tasks.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                when {
                                                    editing != null && isArabic -> "تم تحديث المهمة بنجاح"
                                                    editing != null -> "Task updated successfully."
                                                    isArabic -> "تم إنشاء المهمة بنجاح"
                                                    else -> "Task created successfully."
                                                }
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // Sprint 5 — Bill / Appointment / Medicine: the
                            // SAME shared form as Task, configured per type.
                            // Everything saves through the one existing
                            // reminder pipeline and appears on the Tasks tab
                            // and in the Notifications feed.
                            composable(
                                route = Screen.CreateSmartReminder.route,
                                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val itemId = backStackEntry.arguments?.getString("itemId")
                                val config = SmartReminderFormConfig.forItemId(itemId)
                                CreateTaskScreen(
                                    isArabic = isArabic,
                                    config = config,
                                    onSave = { reminder ->
                                        viewModel.addReminder(reminder)
                                        navController.navigate(Screen.Tasks.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) config.savedMessageAr else config.savedMessageEn
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // Sprint 5 — Smart Gam3iya. Saving goes through the
                            // EXISTING creation pipeline (viewModel.createGam3iya
                            // → createGam3iyaWithMembers + Gam3iyaCalculator),
                            // then returns to the Dashboard.
                            composable(Screen.CreateGam3iya.route) {
                                CreateGam3iyaScreen(
                                    isArabic = isArabic,
                                    onSave = { title, total, installment, members, startDate ->
                                        viewModel.createGam3iya(title, total, installment, members, startDate)
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم إنشاء الجمعية بنجاح" else "Gam3iya created successfully."
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // Sprint 4 — Smart Debt. Saving goes through the
                            // EXISTING ledger implementation (viewModel.addDebt
                            // composes insertPerson/insertLedgerTransaction and
                            // the reminder pipeline for the due date), then
                            // returns to the Dashboard.
                            composable(Screen.CreateDebt.route) {
                                CreateDebtScreen(
                                    persons = persons,
                                    isArabic = isArabic,
                                    onSave = { existingPersonId, personName, amount, isLent, dueDate, note ->
                                        viewModel.addDebt(
                                            existingPersonId = existingPersonId,
                                            personName = personName,
                                            amount = amount,
                                            isLent = isLent,
                                            dueDate = dueDate,
                                            note = note
                                        )
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم حفظ الدين بنجاح" else "Debt saved successfully."
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // Sprint 2 — Smart Item Engine Foundation.
                    // Opened by the Dashboard "+" FAB above.
                    // Sprint 3/4: "task" and "debt" open their real forms.
                    // Sprint 5: "bill", "appointment", "medicine" (shared
                    // reminder form) and "gam3iya" too; every remaining type
                    // still lands on the placeholder.
                    if (showSmartItemSheet) {
                        SmartItemBottomSheet(
                            isArabic = isArabic,
                            onDismiss = { showSmartItemSheet = false },
                            onItemSelected = { item ->
                                showSmartItemSheet = false
                                when (item.id) {
                                    "task" -> navController.navigate(Screen.CreateTask.createRoute())
                                    "debt" -> navController.navigate(Screen.CreateDebt.route)
                                    "bill", "appointment", "medicine" -> navController.navigate(
                                        Screen.CreateSmartReminder.createRoute(item.id)
                                    )
                                    "gam3iya" -> navController.navigate(Screen.CreateGam3iya.route)
                                    else -> navController.navigate(
                                        Screen.SmartItemPlaceholder.createRoute(item.id)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
