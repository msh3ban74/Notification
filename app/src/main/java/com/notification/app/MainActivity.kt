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
    object Dashboard : Screen("dashboard", "My Day", "يومك", Icons.Default.Dashboard)
    object AiChat : Screen("ai_chat", "AI Assistant", "المساعد الذكي", Icons.Default.AutoAwesome)
    object Tasks : Screen("tasks", "Tasks", "المهام", Icons.Default.Assignment)
    object Notifications : Screen("notifications", "Notifications", "الإشعارات", Icons.Default.NotificationsActive)

    // Existing feature screens — reachable from Dashboard's Quick Actions,
    // not shown in Bottom Navigation. (The old Home surface + its duplicate
    // Reminders route were removed — Dashboard is the home surface and the
    // Tasks route hosts the live reminders screen.)
    object Ledger : Screen("ledger", "Debts", "الديون", Icons.Default.AccountBalanceWallet)

    // Opens the Debts screen straight into one person's detail (from the
    // dashboard money card). Separate route so the Debts bottom-nav tab
    // selection logic is untouched.
    object LedgerPerson : Screen(
        "ledger_person/{personId}", "Debts", "الديون", Icons.Default.AccountBalanceWallet
    ) {
        fun createRoute(personId: Long) = "ledger_person/$personId"
    }
    object Gam3iya : Screen("gam3iya", "Gam3iya", "الجمعيات", Icons.Default.Group)
    object Islamic : Screen("islamic", "Islamic", "إسلاميات", Icons.Default.Mosque)
    object HealthNotes : Screen("health_notes", "Health/Notes", "الصحة", Icons.Default.WaterDrop)

    // Settings — reachable ONLY from the profile button in the Top App Bar.
    object Settings : Screen("settings", "Settings", "الإعدادات", Icons.Default.Settings)
    object Auth : Screen("auth", "Auth", "دخول", Icons.Default.Lock)

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

    // «مناسباتك» — weddings, birthdays, gatherings: a dedicated form that
    // rides the reminder pipeline (pre-alerts included).
    object CreateEvent : Screen("create_event", "New Occasion", "مناسبة جديدة", Icons.Default.Add)

    // Sprint 5 — Bill / Appointment / Medicine: the SAME shared reminder
    // form as Task, parametrized per type (SmartReminderFormConfig). One
    // pipeline, zero duplicated forms.
    object CreateSmartReminder : Screen(
        "create_smart/{itemId}", "New Item", "عنصر جديد", Icons.Default.Add
    ) {
        fun createRoute(itemId: String) = "create_smart/$itemId"
    }

    // جمعية أنا فيها — participant-only tracking form.
    object CreateGam3iya : Screen("create_gam3iya", "My Gam3iya", "جمعيتي", Icons.Default.Add)

    // Stability sprint — Smart Alarm: reuses the existing AlarmEntity +
    // addAlarm + AlarmManagerScheduler pipeline.
    object CreateAlarm : Screen("create_alarm", "New Alarm", "منبه جديد", Icons.Default.Add)

    // Final Product sprint (Phase B) — money items: bill / installment /
    // subscription, all backed by FinancialItemEntity. The financeType arg
    // is a FinancialType name.
    object CreateFinancial : Screen(
        "create_financial/{financeType}", "New Item", "عنصر مالي", Icons.Default.Add
    ) {
        fun createRoute(financeType: String) = "create_financial/$financeType"
    }

    // Edit an existing financial item by id.
    object EditFinancial : Screen(
        "edit_financial/{itemId}", "Edit", "تعديل", Icons.Default.Add
    ) {
        fun createRoute(itemId: Long) = "edit_financial/$itemId"
    }

    // Installment payment-plan detail (payments, history, attachments).
    object InstallmentDetail : Screen(
        "installment_detail/{itemId}", "Installment", "قسط", Icons.Default.CreditCard
    ) {
        fun createRoute(itemId: Long) = "installment_detail/$itemId"
    }

    // The money list (bills / installments / subscriptions).
    object Financial : Screen("financial", "Money", "المالية", Icons.Default.AccountBalanceWallet)

    // Final Product sprint (Phase C) — the habit engine screen.
    object Habits : Screen("habits", "Habits", "العادات", Icons.Default.Add)

    // Global search across every module.
    object Search : Screen("search", "Search", "بحث", Icons.Default.Search)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Rafeeq Vivid is light-only: dark status/navigation bar icons so
        // they stay readable over the white canvas.
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            val language by viewModel.language.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val userName by viewModel.userName.collectAsState()
            val userEmail by viewModel.userEmail.collectAsState()
            val lastSyncTime by viewModel.lastSyncTime.collectAsState()

            val reminders by viewModel.allReminders.collectAsState()
            // Phase D — archived tasks stay out of Dashboard/Notifications;
            // only the Tasks list (archive view) shows them.
            val visibleReminders = remember(reminders) { reminders.filter { !it.isArchived } }
            val persons by viewModel.allPersons.collectAsState()
            val transactions by viewModel.allTransactions.collectAsState()
            val gam3iyas by viewModel.allGam3iyas.collectAsState()
            val alarms by viewModel.allAlarms.collectAsState()
            val financialItems by viewModel.allFinancialItems.collectAsState()
            val habits by viewModel.allHabits.collectAsState()
            val habitLogs by viewModel.allHabitLogs.collectAsState()
            val prayerTimes by viewModel.prayerTimes.collectAsState()
            val workNotes by viewModel.allWorkNotes.collectAsState()
            val chatMessages by viewModel.chatMessages.collectAsState()
            val isAiLoading by viewModel.isAiLoading.collectAsState()
            val waterCount by viewModel.waterCount.collectAsState()

            val isArabic = language == "ar"
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            // Rafeeq Vivid — light-only by design (white canvas, indigo
            // identity, green for done). No dark theme, on purpose.
            NotificationTheme(isArabic = isArabic) {
                // Ask for the notification permission on first launch so
                // alerts and alarms can actually appear (Android 13+).
                com.notification.app.ui.permissions.RequestCorePermissions()
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
                                    onProfileClick = { navController.navigate(Screen.Settings.route) },
                                    onSearchClick = { navController.navigate(Screen.Search.route) }
                                )
                            }
                        },
                        bottomBar = {
                            if (showChrome) {
                                RafeeqFloatingNav(
                                    screens = bottomBarScreens,
                                    currentRoute = currentRoute,
                                    isArabic = isArabic,
                                    onSelect = { screen ->
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
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
                                    // Visual polish — the FAB is the app's
                                    // primary gold accent: full champagne
                                    // gold with a deep floating shadow.
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
                                // «يومك» — one chronological answer to
                                // "إيه اللي عليّا؟", read from the existing
                                // Room flows already collected above.
                                DashboardScreen(
                                    isArabic = isArabic,
                                    userName = userName,
                                    gam3iyas = gam3iyas,
                                    reminders = visibleReminders,
                                    persons = persons,
                                    transactions = transactions,
                                    alarms = alarms,
                                    prayerTimes = prayerTimes,
                                    workNotes = workNotes,
                                    waterCount = waterCount,
                                    financialItems = financialItems,
                                    onToggleReminderDone = { viewModel.toggleReminderCompleted(it) },
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
                                    onNavigateToLedgerPerson = { pid -> navController.navigate(Screen.LedgerPerson.createRoute(pid)) },
                                    onNavigateToGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                                    onNavigateToIslamic = { navController.navigate(Screen.Islamic.route) },
                                    onNavigateToHealthNotes = { navController.navigate(Screen.HealthNotes.route) },
                                    onNavigateToFinancial = { navController.navigate(Screen.Financial.route) },
                                    habits = habits,
                                    habitLogs = habitLogs,
                                    onNavigateToHabits = { navController.navigate(Screen.Habits.route) }
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
                                    onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                    onEditReminder = { reminder ->
                                        navController.navigate(Screen.CreateTask.createRoute(reminder.id))
                                    },
                                    onTogglePin = { viewModel.toggleReminderPinned(it) },
                                    onSetArchived = { reminder, archived ->
                                        viewModel.setReminderArchived(reminder, archived)
                                    },
                                    onDuplicate = { viewModel.duplicateReminder(it) },
                                    // The Tasks "+" opens the SAME organized
                                    // sheet as the Dashboard — every type gets
                                    // its own dedicated form.
                                    onCreateTask = { showSmartItemSheet = true }
                                )
                            }

                            composable(Screen.Notifications.route) {
                                // Sprint 5: real scheduled notifications from
                                // the existing reminder + alarm flows.
                                NotificationsScreen(
                                    reminders = visibleReminders,
                                    alarms = alarms,
                                    isArabic = isArabic,
                                    onCreateFirst = {
                                        navController.navigate(Screen.Tasks.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) }
                                )
                            }

                            composable(Screen.Ledger.route) {
                                LedgerScreen(
                                    persons = persons,
                                    transactions = transactions,
                                    isArabic = isArabic,
                                    onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                    onAddPersonFull = { viewModel.addPersonFull(it) },
                                    onAddTransaction = { viewModel.addLedgerTransaction(it) },
                                    onDeleteTransaction = { viewModel.deleteLedgerTransaction(it) },
                                    onUpdateTransaction = { viewModel.updateLedgerTransaction(it) },
                                    onUpdatePerson = { viewModel.updatePerson(it) },
                                    onDeletePerson = { viewModel.deletePerson(it) },
                                    getLedgerAttachments = { id -> viewModel.getLedgerAttachmentsForPerson(id) },
                                    onAddLedgerAttachment = { personId, uri -> viewModel.addLedgerAttachment(personId, 0, uri, "RECEIPT", "") },
                                    onDeleteLedgerAttachment = { viewModel.deleteLedgerAttachment(it) }
                                )
                            }

                            composable(
                                Screen.LedgerPerson.route,
                                arguments = listOf(navArgument("personId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val personId = backStackEntry.arguments?.getLong("personId")
                                LedgerScreen(
                                    persons = persons,
                                    transactions = transactions,
                                    isArabic = isArabic,
                                    onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                    onAddPersonFull = { viewModel.addPersonFull(it) },
                                    onAddTransaction = { viewModel.addLedgerTransaction(it) },
                                    onDeleteTransaction = { viewModel.deleteLedgerTransaction(it) },
                                    onUpdateTransaction = { viewModel.updateLedgerTransaction(it) },
                                    onUpdatePerson = { viewModel.updatePerson(it) },
                                    onDeletePerson = { viewModel.deletePerson(it) },
                                    getLedgerAttachments = { id -> viewModel.getLedgerAttachmentsForPerson(id) },
                                    onAddLedgerAttachment = { pid, uri -> viewModel.addLedgerAttachment(pid, 0, uri, "RECEIPT", "") },
                                    onDeleteLedgerAttachment = { viewModel.deleteLedgerAttachment(it) },
                                    initialPersonId = personId
                                )
                            }

                            composable(Screen.Gam3iya.route) {
                                Gam3iyaScreen(
                                    viewModel = viewModel,
                                    isArabic = isArabic
                                )
                            }

                            composable(Screen.AiChat.route) {
                                AiChatScreen(
                                    messages = chatMessages,
                                    isLoading = isAiLoading,
                                    isArabic = isArabic,
                                    onSendMessage = { viewModel.sendAiMessage(it) },
                                    onOpenTasks = {
                                        navController.navigate(Screen.Tasks.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenLedger = { navController.navigate(Screen.Ledger.route) },
                                    onOpenFinancial = { navController.navigate(Screen.Financial.route) },
                                    onOpenNotifications = {
                                        navController.navigate(Screen.Notifications.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onStopGeneration = { viewModel.stopAiGeneration() },
                                    onRegenerate = { viewModel.regenerateLastResponse() },
                                    onClearChat = { viewModel.clearChat() }
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
                                val backupState by viewModel.backupState.collectAsState()
                                val restoreState by viewModel.restoreState.collectAsState()
                                val fileBackupState by viewModel.fileBackupState.collectAsState()
                                val fileRestoreState by viewModel.fileRestoreState.collectAsState()
                                val restorePreview by viewModel.restorePreview.collectAsState()
                                val autoCloudBackup by viewModel.autoCloudBackup.collectAsState()
                                SettingsScreen(
                                    currentLanguage = language,
                                    isLoggedIn = isLoggedIn,
                                    userName = userName,
                                    userEmail = userEmail,
                                    lastSyncTime = lastSyncTime,
                                    isArabic = isArabic,
                                    onSetLanguage = { viewModel.setLanguage(it) },
                                    onTriggerBackup = { viewModel.triggerBackupSync() },
                                    backupState = backupState,
                                    onTriggerRestore = { viewModel.triggerRestore() },
                                    restoreState = restoreState,
                                    suggestedBackupFileName = viewModel.suggestedBackupFileName(),
                                    onExportBackupFile = { viewModel.exportBackupToFile(it) },
                                    onPickRestoreFile = { viewModel.prepareRestoreFromFile(it) },
                                    fileBackupState = fileBackupState,
                                    fileRestoreState = fileRestoreState,
                                    restorePreview = restorePreview,
                                    onConfirmRestore = { viewModel.confirmRestoreFromFile() },
                                    onCancelRestore = { viewModel.cancelRestorePreview() },
                                    autoCloudBackup = autoCloudBackup,
                                    onSetAutoCloudBackup = { viewModel.setAutoCloudBackup(it) },
                                    onSignOut = {
                                        // Stability sprint — REAL logout:
                                        // Firebase sign-out + persisted session
                                        // cleared + cached AI/user state reset +
                                        // back stack fully cleared so Back can
                                        // never return into authed screens.
                                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                        viewModel.onLogout()
                                        navController.navigate(Screen.Auth.route) {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
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

                            // Smart Alarm. Saving goes
                            // through the EXISTING alarm pipeline
                            // (viewModel.addAlarm → AlarmManagerScheduler),
                            // then returns to the Dashboard with a confirmation.
                            composable(Screen.CreateAlarm.route) {
                                CreateAlarmScreen(
                                    isArabic = isArabic,
                                    onSave = { alarm ->
                                        viewModel.addAlarm(alarm)
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم ضبط المنبه" else "Alarm set."
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // Phase B — Bill / Installment / Subscription.
                            composable(
                                route = Screen.CreateFinancial.route,
                                arguments = listOf(navArgument("financeType") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val ft = com.notification.app.domain.model.FinancialType
                                    .fromString(backStackEntry.arguments?.getString("financeType"))
                                CreateFinancialScreen(
                                    isArabic = isArabic,
                                    type = ft,
                                    onSave = { item ->
                                        viewModel.addFinancialItem(item)
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم الحفظ" else "Saved."
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // Phase B — edit an existing financial item.
                            composable(
                                route = Screen.EditFinancial.route,
                                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
                                val editing = financialItems.firstOrNull { it.id == itemId }
                                if (editing != null) {
                                    CreateFinancialScreen(
                                        isArabic = isArabic,
                                        type = com.notification.app.domain.model.FinancialType.fromString(editing.type),
                                        initial = editing,
                                        onSave = { item ->
                                            viewModel.updateFinancialItem(item)
                                            navController.popBackStack()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (isArabic) "تم التحديث" else "Updated."
                                                )
                                            }
                                        },
                                        onCancel = { navController.popBackStack() }
                                    )
                                } else {
                                    androidx.compose.runtime.LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }

                            // Sprint 6 — installment payment-plan detail.
                            composable(
                                route = Screen.InstallmentDetail.route,
                                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
                                val target = financialItems.firstOrNull { it.id == itemId }
                                if (target != null) {
                                    InstallmentDetailScreen(
                                        viewModel = viewModel,
                                        item = target,
                                        isArabic = isArabic,
                                        onBack = { navController.popBackStack() },
                                        onEdit = { navController.navigate(Screen.EditFinancial.createRoute(target.id)) },
                                        onDelete = {
                                            viewModel.deleteFinancialItem(target)
                                            navController.popBackStack()
                                        }
                                    )
                                } else {
                                    androidx.compose.runtime.LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }

                            // Phase B — the money list.
                            composable(Screen.Financial.route) {
                                FinancialListScreen(
                                    isArabic = isArabic,
                                    items = financialItems,
                                    onBack = { navController.popBackStack() },
                                    onEdit = { navController.navigate(Screen.EditFinancial.createRoute(it.id)) },
                                    onOpenDetail = { navController.navigate(Screen.InstallmentDetail.createRoute(it.id)) },
                                    onDelete = { viewModel.deleteFinancialItem(it) },
                                    onTogglePaid = { viewModel.updateFinancialItem(it.copy(isPaid = !it.isPaid)) },
                                    onAdd = { navController.navigate(Screen.CreateFinancial.createRoute("BILL")) },
                                    // Phase D — duplicate as a fresh unpaid copy.
                                    onDuplicate = {
                                        viewModel.addFinancialItem(
                                            it.copy(
                                                id = 0,
                                                isPaid = false,
                                                createdAt = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                )
                            }

                            // Phase C — the habit engine.
                            composable(Screen.Habits.route) {
                                HabitsScreen(
                                    isArabic = isArabic,
                                    habits = habits,
                                    logs = habitLogs,
                                    onBack = { navController.popBackStack() },
                                    onAddHabit = { viewModel.addHabit(it) },
                                    onDeleteHabit = { viewModel.deleteHabit(it) },
                                    onArchiveHabit = { viewModel.updateHabit(it) },
                                    onToggleToday = { habitId, dayStart, done ->
                                        viewModel.setHabitDone(habitId, dayStart, done)
                                    }
                                )
                            }

                            // Global search across every module.
                            composable(Screen.Search.route) {
                                SearchScreen(
                                    isArabic = isArabic,
                                    reminders = reminders,
                                    persons = persons,
                                    gam3iyas = gam3iyas,
                                    financialItems = financialItems,
                                    habits = habits,
                                    onBack = { navController.popBackStack() },
                                    onOpenTask = { navController.navigate(Screen.CreateTask.createRoute(it)) },
                                    onOpenLedger = { navController.navigate(Screen.Ledger.route) },
                                    onOpenGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                                    onOpenInstallment = { navController.navigate(Screen.InstallmentDetail.createRoute(it)) },
                                    onOpenFinancial = { navController.navigate(Screen.EditFinancial.createRoute(it)) },
                                    onOpenHabits = { navController.navigate(Screen.Habits.route) }
                                )
                            }

                            // رفيق شخصي: تسجيل الجمعية اللي المستخدم مشترك
                            // فيها (وضع "مدير الجمعية" اتشال نهائيًا).
                            composable(Screen.CreateGam3iya.route) {
                                ParticipantGam3iyaForm(
                                    viewModel = viewModel,
                                    isArabic = isArabic,
                                    existing = null,
                                    onDone = {
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم حفظ الجمعية وتفعيل التذكير الشهري بالقسط" else "Gam3iya saved with a monthly installment reminder"
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
                                    onSave = { existingPersonId, personName, amount, isLent, receivedDate, dueDate, note ->
                                        viewModel.addDebt(
                                            existingPersonId = existingPersonId,
                                            personName = personName,
                                            amount = amount,
                                            isLent = isLent,
                                            receivedDate = receivedDate,
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

                            // «مناسباتك» — the dedicated occasions form.
                            composable(Screen.CreateEvent.route) {
                                CreateEventScreen(
                                    isArabic = isArabic,
                                    onSave = { reminder ->
                                        viewModel.addReminder(reminder)
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم حفظ المناسبة وسيصلك تذكير قبلها" else "Occasion saved — you'll be reminded."
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // Smart Item Engine — opened by the Dashboard "+" FAB.
                    // Every type routes to a REAL screen: task/debt/alarm/
                    // gam3iya/habit to their dedicated forms, bill/installment/
                    // subscription to the financial form, and every other type
                    // to the shared Smart Reminder form with its category
                    // preset (see the `when` below). No placeholder screens.
                    if (showSmartItemSheet) {
                        SmartItemBottomSheet(
                            isArabic = isArabic,
                            onDismiss = { showSmartItemSheet = false },
                            onItemSelected = { item ->
                                showSmartItemSheet = false
                                when (item.id) {
                                    "task" -> navController.navigate(Screen.CreateTask.createRoute())
                                    "debt" -> navController.navigate(Screen.CreateDebt.route)
                                    "alarm" -> navController.navigate(Screen.CreateAlarm.route)
                                    "bill" -> navController.navigate(
                                        Screen.CreateFinancial.createRoute("BILL")
                                    )
                                    "installment" -> navController.navigate(
                                        Screen.CreateFinancial.createRoute("INSTALLMENT")
                                    )
                                    "subscription" -> navController.navigate(
                                        Screen.CreateFinancial.createRoute("SUBSCRIPTION")
                                    )
                                    "appointment", "medicine" -> navController.navigate(
                                        Screen.CreateSmartReminder.createRoute(item.id)
                                    )
                                    "gam3iya" -> navController.navigate(Screen.CreateGam3iya.route)
                                    "habit" -> navController.navigate(Screen.Habits.route)
                                    "event" -> navController.navigate(Screen.CreateEvent.route)
                                    else -> navController.navigate(
                                        Screen.CreateSmartReminder.createRoute(item.id)
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

/**
 * Rafeeq Design DNA — the FLOATING NAV.
 *
 * The second signature element: navigation floats above the content on a
 * soft glass pill, detached from the screen edges. The selected tab sits
 * inside an animated indigo capsule; unselected tabs stay quiet gray.
 */
@Composable
private fun RafeeqFloatingNav(
    screens: List<Screen>,
    currentRoute: String?,
    isArabic: Boolean,
    onSelect: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.97f),
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, androidx.compose.ui.graphics.Color(0xFFEDEDF8)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    val capsule by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected)
                            com.notification.app.ui.theme.Primary.copy(alpha = 0.10f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                        animationSpec = tween(250),
                        label = "navCapsule"
                    )
                    val tint by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected) com.notification.app.ui.theme.Primary
                        else androidx.compose.ui.graphics.Color(0xFF9CA3AF),
                        animationSpec = tween(250),
                        label = "navTint"
                    )
                    Surface(
                        onClick = { if (!selected) onSelect(screen) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                        color = capsule,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.titleEn,
                                tint = tint,
                                modifier = Modifier.size(23.dp)
                            )
                            Text(
                                text = if (isArabic) screen.titleAr else screen.titleEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
