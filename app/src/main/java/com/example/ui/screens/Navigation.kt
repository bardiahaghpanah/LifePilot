package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel
import kotlinx.coroutines.launch

enum class AppRoute(val titleFa: String, val icon: ImageVector) {
    DASHBOARD("داشبورد", Icons.Default.Dashboard),
    CALENDAR("تقویم جلالی", Icons.Default.CalendarMonth),
    FINANCE("مدیریت مالی", Icons.Default.AccountBalanceWallet),
    INSTALLMENTS("اقساط و بدهی", Icons.Default.Payments),
    TASKS("وظایف و کارها", Icons.Default.TaskAlt),
    PROJECTS("کار و پروژه‌ها", Icons.Default.Workspaces),
    UNIVERSITY("دانشگاه", Icons.Default.School),
    MEETINGS("جلسات و قرارها", Icons.Default.Groups),
    NOTES("یادداشت‌ها", Icons.Default.NoteAlt),
    HABITS("عادت‌ها", Icons.Default.LocalFireDepartment),
    PLANNER("برنامه‌ریز هوشمند", Icons.Default.AutoAwesome),
    SEARCH("جستجوی سراسری", Icons.Default.Search),
    ACTIVITY("تاریخچه فعالیت‌ها", Icons.Default.History),
    NOTIFICATIONS("مرکز اعلان‌ها", Icons.Default.Notifications),
    REPORTS("گزارش‌ها و تحلیل", Icons.Default.BarChart),
    SETTINGS("تنظیمات", Icons.Default.Settings)
}

private enum class QuickAddType {
    TASK, INSTALLMENT, EXPENSE, INCOME, MEETING, EVENT, NOTE, HABIT, PROJECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifePilotApp(viewModel: LifePilotViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifePilotTheme(settings = state.settings) {
        // Optional Local PIN Lock Screen
        if (state.settings.appLockPin.isNotBlank() && !state.isUnlocked) {
            AppPinLockGate(
                onUnlock = { pin -> viewModel.unlockWithPin(pin) }
            )
            return@LifePilotTheme
        }

        val tokens = LocalGlassTokens.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        var currentRoute by remember { mutableStateOf(AppRoute.DASHBOARD) }
        var isSidebarCollapsed by remember { mutableStateOf(false) }
        var showQuickActionSheet by remember { mutableStateOf(false) }
        var activeQuickDialog by remember { mutableStateOf<QuickAddType?>(null) }

        LaunchedEffect(Unit) {
            viewModel.snackbarMessage.collect { msg ->
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }

        // BackHandler returns to Dashboard from any secondary screen
        BackHandler(enabled = currentRoute != AppRoute.DASHBOARD) {
            currentRoute = AppRoute.DASHBOARD
        }

        GlassCanvasBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 840.dp

                if (isExpandedScreen) {
                    // Tablet / Foldable / Wide Screen: Collapsible Right-Side RTL Navigation Sidebar
                    Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        ExpandedRtlSidebar(
                            currentRoute = currentRoute,
                            collapsed = isSidebarCollapsed,
                            unreadCount = state.unreadNotificationsCount,
                            useFa = state.settings.usePersianDigits,
                            onToggleCollapse = { isSidebarCollapsed = !isSidebarCollapsed },
                            onSelectRoute = { currentRoute = it }
                        )
                        Scaffold(
                            containerColor = Color.Transparent,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                LifePilotTopBar(
                                    title = currentRoute.titleFa,
                                    unreadCount = state.unreadNotificationsCount,
                                    useFa = state.settings.usePersianDigits,
                                    showMenuButton = false,
                                    onMenuClick = {},
                                    onSearchClick = { currentRoute = AppRoute.SEARCH },
                                    onNotificationsClick = { currentRoute = AppRoute.NOTIFICATIONS }
                                )
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { showQuickActionSheet = true },
                                    containerColor = tokens.accentPrimary,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("global_quick_add_fab")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "افزودن سریع")
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                LifePilotRouteContent(
                                    route = currentRoute,
                                    state = state,
                                    viewModel = viewModel,
                                    onNavigate = { currentRoute = it },
                                    onOpenQuickAdd = { showQuickActionSheet = true }
                                )
                            }
                        }
                    }
                } else {
                    // Mobile: Right-Side RTL ModalNavigationDrawer + Bottom NavigationBar
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(300.dp),
                                drawerContainerColor = tokens.cardSurfaceElevated,
                                drawerContentColor = tokens.inkPrimary
                            ) {
                                DrawerNavigationContent(
                                    currentRoute = currentRoute,
                                    unreadCount = state.unreadNotificationsCount,
                                    useFa = state.settings.usePersianDigits,
                                    onSelectRoute = { route ->
                                        currentRoute = route
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                LifePilotTopBar(
                                    title = currentRoute.titleFa,
                                    unreadCount = state.unreadNotificationsCount,
                                    useFa = state.settings.usePersianDigits,
                                    showMenuButton = true,
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onSearchClick = { currentRoute = AppRoute.SEARCH },
                                    onNotificationsClick = { currentRoute = AppRoute.NOTIFICATIONS }
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = tokens.cardSurfaceElevated,
                                    tonalElevation = 6.dp
                                ) {
                                    val primaryTabs = listOf(
                                        AppRoute.DASHBOARD,
                                        AppRoute.CALENDAR,
                                        AppRoute.FINANCE,
                                        AppRoute.TASKS
                                    )
                                    primaryTabs.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentRoute == tab,
                                            onClick = { currentRoute = tab },
                                            icon = { Icon(tab.icon, contentDescription = tab.titleFa) },
                                            label = { Text(tab.titleFa, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                                        )
                                    }
                                    NavigationBarItem(
                                        selected = currentRoute !in primaryTabs,
                                        onClick = { scope.launch { drawerState.open() } },
                                        icon = { Icon(Icons.Default.Apps, contentDescription = "همه ماژول‌ها") },
                                        label = { Text("ماژول‌ها", maxLines = 1) },
                                        modifier = Modifier.testTag("bottom_nav_more")
                                    )
                                }
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { showQuickActionSheet = true },
                                    containerColor = tokens.accentPrimary,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("global_quick_add_fab")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "افزودن سریع")
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                LifePilotRouteContent(
                                    route = currentRoute,
                                    state = state,
                                    viewModel = viewModel,
                                    onNavigate = { currentRoute = it },
                                    onOpenQuickAdd = { showQuickActionSheet = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Quick Actions Sheet
        if (showQuickActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickActionSheet = false },
                containerColor = tokens.cardSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اقدامات سریع LifePilot", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                    val actions = listOf(
                        Triple(QuickAddType.TASK, "وظیفه جدید", Icons.Default.TaskAlt),
                        Triple(QuickAddType.EXPENSE, "ثبت هزینه", Icons.Default.ArrowUpward),
                        Triple(QuickAddType.INCOME, "ثبت درآمد", Icons.Default.ArrowDownward),
                        Triple(QuickAddType.INSTALLMENT, "طرح قسط جدید", Icons.Default.Payments),
                        Triple(QuickAddType.MEETING, "زمان‌بندی جلسه", Icons.Default.Groups),
                        Triple(QuickAddType.EVENT, "رویداد تقویم", Icons.Default.Event),
                        Triple(QuickAddType.NOTE, "یادداشت جدید", Icons.Default.NoteAlt),
                        Triple(QuickAddType.HABIT, "عادت جدید", Icons.Default.LocalFireDepartment),
                        Triple(QuickAddType.PROJECT, "پروژه جدید", Icons.Default.Workspaces)
                    )
                    actions.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (type, lbl, icon) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            showQuickActionSheet = false
                                            activeQuickDialog = type
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = tokens.accentPrimary.copy(alpha = 0.09f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(icon, contentDescription = lbl, tint = tokens.accentPrimary)
                                        Text(lbl, style = MaterialTheme.typography.labelMedium, color = tokens.inkPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Quick Add Dialogs
        when (activeQuickDialog) {
            QuickAddType.TASK -> TaskEditorDialog(
                existing = null,
                projects = state.projects,
                onSave = { viewModel.saveTask(it); activeQuickDialog = null },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.EXPENSE -> TransactionEditorDialog(
                defaultCurrency = state.settings.defaultCurrency,
                defaultType = "EXPENSE",
                currentRate = state.currentUsdRate?.rateIrtPerUsd,
                onSave = { t, d, a, c, ty, cat, ep, st, r, n ->
                    viewModel.addTransaction(t, d, a, c, ty, cat, ep, st, r, n)
                    activeQuickDialog = null
                },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.INCOME -> TransactionEditorDialog(
                defaultCurrency = state.settings.defaultCurrency,
                defaultType = "INCOME",
                currentRate = state.currentUsdRate?.rateIrtPerUsd,
                onSave = { t, d, a, c, ty, cat, ep, st, r, n ->
                    viewModel.addTransaction(t, d, a, c, ty, cat, ep, st, r, n)
                    activeQuickDialog = null
                },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.INSTALLMENT -> InstallmentCreatorDialog(
                defaultCurrency = state.settings.defaultCurrency,
                onSave = { t, cr, o, cur, cnt, pAmt, f, fd, n ->
                    viewModel.createInstallmentPlan(t, cr, o, cur, cnt, pAmt, f, fd, n)
                    activeQuickDialog = null
                },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.MEETING -> MeetingEditorDialog(
                existing = null,
                onSave = { viewModel.saveMeeting(it); activeQuickDialog = null },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.EVENT -> CalendarEventEditorDialog(
                initialDateEpochDay = JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId),
                onSave = { viewModel.saveCalendarEvent(it); activeQuickDialog = null },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.NOTE -> NoteEditorDialog(
                existing = null,
                onSave = { viewModel.saveNote(it) },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.HABIT -> HabitCreatorDialog(
                onSave = { viewModel.saveHabit(it); activeQuickDialog = null },
                onDismiss = { activeQuickDialog = null }
            )
            QuickAddType.PROJECT -> ProjectEditorDialog(
                existing = null,
                onSave = { viewModel.saveProject(it); activeQuickDialog = null },
                onDismiss = { activeQuickDialog = null }
            )
            null -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifePilotTopBar(
    title: String,
    unreadCount: Int,
    useFa: Boolean,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = tokens.cardSurface.copy(alpha = 0.85f),
            titleContentColor = tokens.inkPrimary
        ),
        navigationIcon = {
            if (showMenuButton) {
                IconButton(onClick = onMenuClick, modifier = Modifier.testTag("open_drawer_button")) {
                    Icon(Icons.Default.Menu, contentDescription = "منوی ناوبری")
                }
            }
        },
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            IconButton(onClick = onSearchClick, modifier = Modifier.testTag("top_search_button")) {
                Icon(Icons.Default.Search, contentDescription = "جستجوی سراسری")
            }
            IconButton(onClick = onNotificationsClick, modifier = Modifier.testTag("top_notifications_button")) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(containerColor = CrimsonDanger) {
                                Text(PersianFormatters.digits(unreadCount.toString(), useFa))
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "اعلان‌ها")
                }
            }
        }
    )
}

@Composable
private fun DrawerNavigationContent(
    currentRoute: AppRoute,
    unreadCount: Int,
    useFa: Boolean,
    onSelectRoute: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("LifePilot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tokens.accentPrimary)
                Text("سیستم جامع مدیریت زندگی شخصی", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
            }
            HorizontalDivider(color = tokens.subtleDivider)
            Spacer(modifier = Modifier.height(6.dp))
        }
        items(AppRoute.entries) { route ->
            NavigationDrawerItem(
                label = { Text(route.titleFa) },
                selected = currentRoute == route,
                onClick = { onSelectRoute(route) },
                icon = { Icon(route.icon, contentDescription = route.titleFa) },
                badge = if (route == AppRoute.NOTIFICATIONS && unreadCount > 0) {
                    { GlassBadge(PersianFormatters.digits(unreadCount.toString(), useFa), CrimsonDanger) }
                } else null,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun ExpandedRtlSidebar(
    currentRoute: AppRoute,
    collapsed: Boolean,
    unreadCount: Int,
    useFa: Boolean,
    onToggleCollapse: () -> Unit,
    onSelectRoute: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val width = if (collapsed) 78.dp else 250.dp
    Surface(
        modifier = Modifier.fillMaxHeight().width(width),
        color = tokens.cardSurfaceElevated,
        tonalElevation = 4.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!collapsed) {
                        Text("LifePilot", style = MaterialTheme.typography.titleLarge, color = tokens.accentPrimary)
                    }
                    IconButton(onClick = onToggleCollapse) {
                        Icon(if (collapsed) Icons.Default.MenuOpen else Icons.Default.Menu, contentDescription = "جمع کردن منو")
                    }
                }
            }
            items(AppRoute.entries) { route ->
                val selected = currentRoute == route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) tokens.accentPrimary.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelectRoute(route) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start
                ) {
                    Icon(
                        route.icon,
                        contentDescription = route.titleFa,
                        tint = if (selected) tokens.accentPrimary else tokens.inkSecondary
                    )
                    if (!collapsed) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = route.titleFa,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) tokens.accentPrimary else tokens.inkPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (route == AppRoute.NOTIFICATIONS && unreadCount > 0) {
                            GlassBadge(PersianFormatters.digits(unreadCount.toString(), useFa), CrimsonDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifePilotRouteContent(
    route: AppRoute,
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit,
    onOpenQuickAdd: () -> Unit
) {
    when (route) {
        AppRoute.DASHBOARD -> DashboardScreen(state, viewModel, onNavigate, onOpenQuickAdd)
        AppRoute.CALENDAR -> CalendarScreen(state, viewModel, onNavigate)
        AppRoute.FINANCE -> FinanceScreen(state, viewModel, onNavigate)
        AppRoute.INSTALLMENTS -> InstallmentsScreen(state, viewModel)
        AppRoute.TASKS -> TasksScreen(state, viewModel)
        AppRoute.PROJECTS -> ProjectsScreen(state, viewModel)
        AppRoute.UNIVERSITY -> UniversityScreen(state, viewModel)
        AppRoute.MEETINGS -> MeetingsScreen(state, viewModel)
        AppRoute.NOTES -> NotesScreen(state, viewModel)
        AppRoute.HABITS -> HabitsScreen(state, viewModel)
        AppRoute.PLANNER -> SmartPlannerScreen(state, viewModel)
        AppRoute.SEARCH -> GlobalSearchScreen(state, viewModel, onNavigate)
        AppRoute.ACTIVITY -> ActivityHistoryScreen(state)
        AppRoute.NOTIFICATIONS -> NotificationsScreen(state, viewModel)
        AppRoute.REPORTS -> ReportsScreen(state)
        AppRoute.SETTINGS -> SettingsScreen(state, viewModel, onNavigate)
    }
}

@Composable
private fun AppPinLockGate(onUnlock: (String) -> Boolean) {
    val tokens = LocalGlassTokens.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    GlassCanvasBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassCard(elevated = true, modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(tokens.accentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = tokens.accentPrimary)
                    }
                    Text("قفل امنیتی LifePilot", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() }; error = false },
                        label = { Text("رمز عبور محلی را وارد کنید") },
                        isError = error,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Text("رمز وارد شده اشتباه است.", style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                    }
                    Button(
                        onClick = { if (!onUnlock(pin)) error = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("ورود")
                    }
                }
            }
        }
    }
}
