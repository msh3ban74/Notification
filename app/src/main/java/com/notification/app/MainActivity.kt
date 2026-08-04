package com.notification.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notification.app.ui.components.PremiumTopAppBar
import com.notification.app.ui.screens.*
import com.notification.app.ui.theme.NotificationTheme
import com.notification.app.ui.viewmodel.MainViewModel
import com.notification.app.ui.designsystem.RafeeqSpacing

sealed class Screen(val route: String, val titleEn: String, val titleAr: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", "البداية", Icons.Default.Notifications)
    object Dashboard : Screen("dashboard", "Dashboard", "لوحة التحكم", Icons.Default.Home)
    object AiChat : Screen("ai_chat", "AI Assistant", "المساعد الذكي", Icons.Default.AutoAwesome)
    object Tasks : Screen("tasks", "Tasks", "المهام", Icons.Default.CheckCircle)
    object Notifications : Screen("notifications", "Notifications", "الإشعارات", Icons.Default.NotificationsActive)
    object Ledger : Screen("ledger", "Ledger", "دفتر الديون", Icons.Default.AccountBalanceWallet)
    object Gam3iya : Screen("gam3iya", "Gam3iya", "الجمعيات", Icons.Default.Group)
    object Islamic : Screen("islamic", "Islamic", "إسلاميات", Icons.Default.Mosque)
    object Settings : Screen("settings", "Settings", "الإعدادات", Icons.Default.Settings)
    object Auth : Screen("auth", "Auth", "دخول", Icons.Default.Lock)
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

                    // Bottom navigation contains ONLY 4 items
                    val bottomBarScreens = listOf(
                        Screen.Dashboard,
                        Screen.AiChat,
                        Screen.Tasks,
                        Screen.Notifications
                    )

                    Scaffold(
                        topBar = {
                            if (currentRoute != Screen.Auth.route && currentRoute != Screen.Splash.route) {
                                PremiumTopAppBar(
                                    onProfileClick = {
                                        navController.navigate(Screen.Settings.route)
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute != Screen.Auth.route && currentRoute != Screen.Splash.route) {
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
                            addSplashScreen(navController, isLoggedIn)
                            addDashboardScreen(navController)
                            addAiChatScreen()
                            addTasksScreen()
                            addNotificationsScreen()
                            addLegacyScreens()
                            addSettingsScreen()
                            addAuthScreen(navController)
                        }
                    }
                }
            }
        }
    }

    private fun NavGraphBuilder.addSplashScreen(navController: NavHostController) {
        composable(Screen.Splash.route) {
            SplashScreen(
                isArabic = false,
                onSplashFinished = {
                    val destination = if (isLoggedIn) Screen.Dashboard.route else Screen.Auth.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
    }

    private fun NavGraphBuilder.addDashboardScreen(navController: NavHostController) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                onNavigateToGam3iya = { navController.navigate(Screen.Gam3iya.route) },
                onNavigateToReminders = { navController.navigate(Screen.Tasks.route) },
                onNavigateToIslamic = { navController.navigate(Screen.Islamic.route) }
            )
        }
    }

    private fun NavGraphBuilder.addAiChatScreen() {
        composable(Screen.AiChat.route) {
            AiChatScreen(
                messages = chatMessages,
                isLoading = isAiLoading,
                isArabic = isArabic,
                onSendMessage = { viewModel.sendAiMessage(it) }
            )
        }
    }

    private fun NavGraphBuilder.addTasksScreen() {
        composable(Screen.Tasks.route) {
            RemindersScreen(
                reminders = reminders,
                isArabic = isArabic,
                onAddReminder = { viewModel.addReminder(it) },
                onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                onDeleteReminder = { viewModel.deleteReminder(it) }
            )
        }
    }

    private fun NavGraphBuilder.addNotificationsScreen() {
        composable(Screen.Notifications.route) {
            // Placeholder for future notifications/alarm history screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RafeeqSpacing.medium)
            ) {
                Text(
                    text = "Notifications History",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.padding(RafeeqSpacing.medium))
                Text(
                    text = "Coming soon...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private fun NavGraphBuilder.addLegacyScreens() {
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

        composable(Screen.Islamic.route) {
            IslamicRemindersScreen(
                prayerTimes = prayerTimes,
                isArabic = isArabic
            )
        }
    }

    private fun NavGraphBuilder.addSettingsScreen() {
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
    }

    private fun NavGraphBuilder.addAuthScreen(navController: NavHostController) {
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
    }
}
