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
    // SmartItemType id (e.g. "debt", "gam3iya"). No chrome (top/bottom
    // bar) is shown here, same as Splash/Auth.
    object SmartItemPlaceholder : Screen(
        "smart_item/{itemId}", "Coming Soon", "قريبًا", Icons.Default.Add
    ) {
        fun createRoute(itemId: String) = "smart_item/$itemId"
    }
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
            val prayerTimes by viewModel.prayerTimes.collectAsState()
            val workNotes by viewModel.allWorkNotes.collectAsState()
            val chatMessages by viewModel.chatMessages.collectAsState()
            val isAiLoading by viewModel.isAiLoading.collectAsState()
            val waterCount by viewModel.waterCount.collectAsState()

            val isArabic = language == "ar"
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            NotificationTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Sprint 2 — Smart Item Engine Foundation.
                    var showSmartItemSheet by remember { mutableStateOf(false) }

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
                        topBar = {
                            if (showChrome) {
                                val currentScreen = bottomBarScreens.firstOrNull { it.route == currentRoute }
                                PremiumTopAppBar(
                                    title = if (currentScreen != null) {
                                        if (isArabic) currentScreen.titleAr else currentScreen.titleEn
                                    } else {
                                        "Notification"
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
                                DashboardScreen(
                                    isArabic = isArabic,
                                    onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                                    onNavigateToGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                                    onNavigateToIslamic = { navController.navigate(Screen.Islamic.route) },
                                    onNavigateToHealthNotes = { navController.navigate(Screen.HealthNotes.route) }
                                )
                            }

                            composable(Screen.Tasks.route) {
                                // Sprint 1: "Tasks" reuses the existing, fully
                                // functional RemindersScreen and its real
                                // ViewModel-backed data. RemindersScreen.kt
                                // itself is untouched.
                                RemindersScreen(
                                    reminders = reminders,
                                    isArabic = isArabic,
                                    onAddReminder = { viewModel.addReminder(it) },
                                    onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) }
                                )
                            }

                            composable(Screen.Notifications.route) {
                                NotificationsScreen(isArabic = isArabic)
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
                                    onDeleteReminder = { viewModel.deleteReminder(it) }
                                )
                            }

                            composable(Screen.Ledger.route) {
                                LedgerScreen(
                                    persons = persons,
                                    transactions = transactions,
                                    isArabic = isArabic,
                                    onAddPerson = { name, phone -> viewModel.addPerson(name, phone) },
                                    onAddTransaction = { viewModel.addLedgerTransaction(it) },
                                    onDeleteTransaction = { viewModel.deleteLedgerTransaction(it) }
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
                            // Single placeholder destination reused for
                            // every type selected from the bottom sheet.
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
                        }
                    }

                    // Sprint 2 — Smart Item Engine Foundation.
                    // Opened by the Dashboard "+" FAB above.
                    if (showSmartItemSheet) {
                        SmartItemBottomSheet(
                            isArabic = isArabic,
                            onDismiss = { showSmartItemSheet = false },
                            onItemSelected = { item ->
                                showSmartItemSheet = false
                                navController.navigate(Screen.SmartItemPlaceholder.createRoute(item.id))
                            }
                        )
                    }
                }
            }
        }
    }
}
