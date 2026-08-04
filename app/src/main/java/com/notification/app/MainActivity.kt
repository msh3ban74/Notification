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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notification.app.ui.screens.*
import com.notification.app.ui.theme.NotificationTheme
import com.notification.app.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val titleEn: String, val titleAr: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", "البداية", Icons.Default.Notifications)
    object Home : Screen("home", "Home", "الرئيسية", Icons.Default.Home)
    object Reminders : Screen("reminders", "Reminders", "التذكيرات", Icons.Default.NotificationsActive)
    object Ledger : Screen("ledger", "Ledger", "دفتر الديون", Icons.Default.AccountBalanceWallet)
    object Gam3iya : Screen("gam3iya", "Gam3iya", "الجمعيات", Icons.Default.Group)
    object AiChat : Screen("ai_chat", "AI Assistant", "المساعد الذكي", Icons.Default.AutoAwesome)
    object Islamic : Screen("islamic", "Islamic", "إسلاميات", Icons.Default.Mosque)
    object HealthNotes : Screen("health_notes", "Health/Notes", "الصحة", Icons.Default.WaterDrop)
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

                    val bottomBarScreens = listOf(
                        Screen.Home,
                        Screen.Reminders,
                        Screen.Ledger,
                        Screen.Gam3iya,
                        Screen.Islamic,
                        Screen.AiChat,
                        Screen.HealthNotes,
                        Screen.Settings
                    )

                    Scaffold(
                        bottomBar = {
                            if (currentRoute != Screen.Auth.route && currentRoute != Screen.Splash.route) {
                                NavigationBar {
                                    bottomBarScreens.forEach { screen ->
                                        NavigationBarItem(
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Home.route) {
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
                            composable(Screen.Splash.route) {
                                SplashScreen(
                                    isArabic = isArabic,
                                    onSplashFinished = {
                                        val destination = if (isLoggedIn) Screen.Home.route else Screen.Auth.route
                                        navController.navigate(destination) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
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
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Auth.route) { inclusive = true }
                                        }
                                    },
                                    onSkip = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Auth.route) { inclusive = true }
                                        }
                                    },
                                    isArabic = isArabic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
