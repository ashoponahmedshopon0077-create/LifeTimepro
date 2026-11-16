package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.data.model.UserProfile
import com.example.data.repository.LifeTimeRepository
import com.example.notifications.LifeTimeNotificationHelper
import com.example.ui.components.AnniversaryCelebrationDialog
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.AgeAndCalculatorScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FriendshipScreen
import com.example.ui.screens.GiftsScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoveAndRelationshipScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PrivacyLockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LifeTimeTheme
import com.example.ui.viewmodel.LifeTimeViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LifeTimeNotificationHelper.ensureChannels(this)
        setContent {
            AppRoot()
        }
    }
}

private data class NavSection(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var preAuthLanguage by remember { mutableStateOf("en") }

    val user = currentUser
    if (user == null) {
        val strings = stringsForLanguage(preAuthLanguage)
        CompositionLocalProvider(LocalAppStrings provides strings) {
            LifeTimeTheme(themeMode = "romantic", languageCode = preAuthLanguage) {
                AuthScreen(
                    selectedLanguage = preAuthLanguage,
                    onLanguageChange = { preAuthLanguage = it },
                    onAuthSuccess = {}
                )
            }
        }
    } else {
        key(user.uid) {
            AuthenticatedShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "LifeTime User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedShell(
    currentUserId: String,
    displayName: String,
    email: String,
    photoUrl: String
) {
    val viewModel: LifeTimeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LifeTimeViewModel(
                    repository = LifeTimeRepository(db, Firebase.auth),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email,
                    initialPhotoUrl = photoUrl
                )
            }
        }
    )

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val activeCelebration by viewModel.activeCelebration.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success<UserProfile?>)?.data
    val languageCode = profile?.languageCode ?: "en"
    val themeMode = profile?.themeMode ?: "romantic"
    val strings = stringsForLanguage(languageCode)

    CompositionLocalProvider(LocalAppStrings provides strings) {
        LifeTimeTheme(themeMode = themeMode, languageCode = languageCode) {
            val pinHash = profile?.privacyPinHash.orEmpty()
            if (pinHash.isNotBlank() && !isUnlocked) {
                PrivacyLockScreen(
                    storedPinHash = pinHash,
                    biometricEnabled = profile?.biometricEnabled ?: false,
                    onUnlocked = { viewModel.markUnlocked() }
                )
            } else {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(statusMessage) {
                    statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                val allSections = listOf(
                    NavSection("home", strings.navHome, Icons.Default.Home),
                    NavSection("age", strings.navAge, Icons.Default.Cake),
                    NavSection("love", strings.navLove, Icons.Default.Favorite),
                    NavSection("friendship", strings.navFriends, Icons.Default.People),
                    NavSection("groups", strings.navGroups, Icons.Default.Groups),
                    NavSection("memories", strings.navMemories, Icons.Default.PhotoLibrary),
                    NavSection("gifts", strings.navGifts, Icons.Default.CardGiftcard),
                    NavSection("calendar", strings.navCalendar, Icons.Default.CalendarMonth),
                    NavSection("notifications", strings.navAlerts, Icons.Default.Notifications),
                    NavSection("settings", strings.navSettings, Icons.Default.Settings)
                )

                val bottomSections = listOf(
                    allSections[0], // Home
                    allSections[1], // My Age
                    allSections[2], // Love
                    allSections[5], // Memories
                    allSections[7]  // Calendar
                )

                fun navigateTo(route: String) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                                Text(
                                    text = "${strings.appName} ❤️",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp)
                                )
                                allSections.forEach { sec ->
                                    NavigationDrawerItem(
                                        selected = currentRoute == sec.route,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            navigateTo(sec.route)
                                        },
                                        icon = { Icon(sec.icon, contentDescription = null) },
                                        label = { Text(sec.label) },
                                        modifier = Modifier.testTag("drawer_nav_${sec.route}")
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp

                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(allSections.find { it.route == currentRoute }?.label ?: strings.appName)
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier.testTag("open_drawer_button")
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { navigateTo("notifications") }) {
                                            Icon(Icons.Default.Notifications, contentDescription = strings.navAlerts)
                                        }
                                        IconButton(onClick = { navigateTo("settings") }) {
                                            Icon(Icons.Default.Settings, contentDescription = strings.navSettings)
                                        }
                                    }
                                )
                            },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar {
                                        bottomSections.forEach { sec ->
                                            NavigationBarItem(
                                                selected = currentRoute == sec.route,
                                                onClick = { navigateTo(sec.route) },
                                                icon = { Icon(sec.icon, contentDescription = sec.label) },
                                                label = { Text(sec.label, maxLines = 1) },
                                                modifier = Modifier.testTag("bottom_nav_${sec.route}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                if (useRail) {
                                    NavigationRail(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        allSections.forEach { sec ->
                                            NavigationRailItem(
                                                selected = currentRoute == sec.route,
                                                onClick = { navigateTo(sec.route) },
                                                icon = { Icon(sec.icon, contentDescription = sec.label) },
                                                label = { Text(sec.label, maxLines = 1) }
                                            )
                                        }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = "home",
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable("home") { HomeScreen(viewModel = viewModel, onNavigate = ::navigateTo) }
                                    composable("age") { AgeAndCalculatorScreen(viewModel = viewModel) }
                                    composable("love") { LoveAndRelationshipScreen(viewModel = viewModel) }
                                    composable("friendship") { FriendshipScreen(viewModel = viewModel) }
                                    composable("groups") { GroupsScreen(viewModel = viewModel) }
                                    composable("memories") { MemoriesScreen(viewModel = viewModel) }
                                    composable("gifts") { GiftsScreen(viewModel = viewModel) }
                                    composable("calendar") { CalendarScreen(viewModel = viewModel) }
                                    composable("notifications") { NotificationsScreen(viewModel = viewModel) }
                                    composable("settings") { SettingsScreen(viewModel = viewModel, onSignedOut = {}) }
                                }
                            }
                        }
                    }
                }

                activeCelebration?.let { celeb ->
                    AnniversaryCelebrationDialog(
                        data = celeb,
                        onDismiss = { viewModel.closeCelebration() }
                    )
                }
            }
        }
    }
}
