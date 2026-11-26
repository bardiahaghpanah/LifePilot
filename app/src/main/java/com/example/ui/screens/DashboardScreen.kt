package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.DashboardWidgetId
import com.example.data.repository.LifePilotDomainLogic
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit,
    onOpenQuickAdd: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val tz = state.settings.timeZoneId
    val todayJalali = remember(tz) { JalaliCalendarEngine.todayJalali(tz) }
    val todayEpoch = remember(todayJalali) { todayJalali.toEpochDay() }

    var liveClock by remember { mutableStateOf(JalaliCalendarEngine.currentTimeFormatted(tz, useFa)) }
    LaunchedEffect(tz, useFa) {
        while (true) {
            liveClock = JalaliCalendarEngine.currentTimeFormatted(tz, useFa)
            delay(20_000L)
        }
    }

    val visibleWidgets = remember(state.settings.widgetOrder, state.settings.hiddenWidgets) {
        state.settings.widgetOrder.filterNot { it in state.settings.hiddenWidgets }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Greeting + Jalali Date + Live Clock Card
        item(key = "hero_header") {
            GlassCard(
                elevated = true,
                accentBorderColor = tokens.accentSecondary,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${JalaliCalendarEngine.currentGreeting(tz)}، ${state.settings.userName}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = tokens.inkPrimary
                        )
                        Text(
                            text = todayJalali.formatFull(useFa),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.accentPrimary
                        )
                        val greg = JalaliCalendarEngine.epochDayToGregorian(todayEpoch)
                        Text(
                            text = "هفته ${PersianFormatters.digits(todayJalali.weekOfYear().toString(), useFa)} سال • ${greg.formatNumeric()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.inkSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.accentPrimary.copy(alpha = 0.10f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = liveClock,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = tokens.accentPrimary
                            )
                            Text(
                                text = "تهران",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.inkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(
                        label = "برنامه‌ریز هوشمند",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppRoute.PLANNER) }
                    )
                    QuickActionPill(
                        label = "جستجو",
                        icon = Icons.Default.Search,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppRoute.SEARCH) }
                    )
                    QuickActionPill(
                        label = "افزودن سریع",
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenQuickAdd
                    )
                }
            }
        }

        // Customizable Widgets in user-selected order
        visibleWidgets.forEach { widgetId ->
            item(key = widgetId.name) {
                when (widgetId) {
                    DashboardWidgetId.FINANCE -> DashboardFinanceWidget(state, useFa, onNavigate)
                    DashboardWidgetId.AGENDA -> DashboardAgendaWidget(state, todayEpoch, useFa, onNavigate)
                    DashboardWidgetId.TASKS -> DashboardTasksWidget(state, todayEpoch, useFa, viewModel, onNavigate)
                    DashboardWidgetId.INSTALLMENTS -> DashboardInstallmentsWidget(state, todayEpoch, useFa, onNavigate)
                    DashboardWidgetId.UNIVERSITY -> DashboardUniversityWidget(state, todayEpoch, useFa, onNavigate)
                    DashboardWidgetId.PROJECTS -> DashboardProjectsWidget(state, useFa, onNavigate)
                    DashboardWidgetId.HABITS -> DashboardHabitsWidget(state, todayEpoch, useFa, viewModel, onNavigate)
                    DashboardWidgetId.NOTES -> DashboardNotesWidget(state, onNavigate)
                    DashboardWidgetId.ACTIVITY -> DashboardActivityWidget(state, onNavigate)
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = tokens.accentPrimary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tokens.accentPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardFinanceWidget(
    state: LifePilotDataState,
    useFa: Boolean,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(onClick = { onNavigate(AppRoute.FINANCE) }) {
        GlassSectionHeader(
            title = "خلاصه مالی",
            subtitle = "تومان و دلار به‌صورت مجزا",
            actionLabel = "مدیریت مالی",
            onActionClick = { onNavigate(AppRoute.FINANCE) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.accentPrimary.copy(alpha = 0.07f))
                    .padding(12.dp)
            ) {
                Text("تراز تومان (IRT)", style = MaterialTheme.typography.labelSmall, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PersianFormatters.formatIrt(state.irtBalance, useFa),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.irtBalance >= 0) EmeraldSuccess else CrimsonDanger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.accentSecondary.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text("تراز دلار (USD)", style = MaterialTheme.typography.labelSmall, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PersianFormatters.formatUsd(state.usdBalance, useFa),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.usdBalance >= 0) tokens.accentPrimary else CrimsonDanger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val rate = state.currentUsdRate
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(tokens.subtleDivider)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (rate != null) "نرخ دستی دلار: ${PersianFormatters.formatIrt(rate.rateIrtPerUsd, useFa)}"
                    else "نرخ دستی دلار هنوز ثبت نشده است",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.inkPrimary
                )
                if (state.estimatedCombinedIrt != null) {
                    Text(
                        text = "برآورد تجمیعی (تخمینی): ${PersianFormatters.formatIrt(state.estimatedCombinedIrt!!, useFa)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.inkSecondary
                    )
                }
            }
            Icon(Icons.Default.CurrencyExchange, contentDescription = "نرخ ارز", tint = tokens.accentSecondary)
        }
    }
}

@Composable
private fun DashboardAgendaWidget(
    state: LifePilotDataState,
    todayEpoch: Long,
    useFa: Boolean,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val items = remember(state, todayEpoch) { state.unifiedItemsForDay(todayEpoch) }
    GlassCard {
        GlassSectionHeader(
            title = "برنامه و قرارهای امروز",
            subtitle = "${PersianFormatters.digits(items.size.toString(), useFa)} مورد در تقویم امروز",
            actionLabel = "تقویم",
            onActionClick = { onNavigate(AppRoute.CALENDAR) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (items.isEmpty()) {
            Text(
                text = "برای امروز رویداد، جلسه یا سررسیدی ثبت نشده است.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.inkSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(5).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(item.colorHex).copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(item.colorHex))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!item.isAllDay && item.startMinute != null) {
                            GlassBadge(
                                text = JalaliCalendarEngine.formatMinuteOfDay(item.startMinute, useFa),
                                color = Color(item.colorHex)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTasksWidget(
    state: LifePilotDataState,
    todayEpoch: Long,
    useFa: Boolean,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val activeTasks = remember(state.tasks, todayEpoch) {
        state.tasks.filter { it.status != "CANCELLED" && it.deadlineEpochDay <= todayEpoch + 1 }
            .sortedBy { it.status == "COMPLETED" }
    }
    val overdueCount = remember(state.tasks, todayEpoch) {
        state.tasks.count { it.status != "COMPLETED" && it.status != "CANCELLED" && it.deadlineEpochDay < todayEpoch }
    }

    GlassCard {
        GlassSectionHeader(
            title = "وظایف امروز و فوری",
            subtitle = if (overdueCount > 0) "${PersianFormatters.digits(overdueCount.toString(), useFa)} وظیفه عقب‌افتاده" else "مدیریت کارهای روزانه",
            actionLabel = "همه وظایف",
            onActionClick = { onNavigate(AppRoute.TASKS) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (activeTasks.isEmpty()) {
            Text("وظیفه‌ای برای امروز ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                activeTasks.take(5).forEach { task ->
                    val done = task.status == "COMPLETED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleTaskCompletion(task) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { viewModel.toggleTaskCompletion(task) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (done) tokens.inkSecondary else tokens.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = JalaliCalendarEngine.epochDayToJalali(task.deadlineEpochDay).formatShort(useFa),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!done && task.deadlineEpochDay < todayEpoch) CrimsonDanger else tokens.inkSecondary
                            )
                        }
                        PriorityChip(task.priority)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardInstallmentsWidget(
    state: LifePilotDataState,
    todayEpoch: Long,
    useFa: Boolean,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val unpaid = remember(state.installmentPayments) {
        state.installmentPayments.filter { it.status != "PAID" }
    }
    val overdue = remember(unpaid, todayEpoch) { unpaid.filter { it.dueEpochDay < todayEpoch } }
    val upcoming = remember(unpaid, todayEpoch) { unpaid.filter { it.dueEpochDay >= todayEpoch }.take(3) }
    val instMap = remember(state.installments) { state.installments.associateBy { it.id } }

    GlassCard(onClick = { onNavigate(AppRoute.INSTALLMENTS) }) {
        GlassSectionHeader(
            title = "اقساط و بدهی‌ها",
            subtitle = "${PersianFormatters.digits(unpaid.size.toString(), useFa)} قسط باقی‌مانده • ${PersianFormatters.digits(overdue.size.toString(), useFa)} معوق",
            actionLabel = "مشاهده",
            onActionClick = { onNavigate(AppRoute.INSTALLMENTS) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (unpaid.isEmpty()) {
            Text("قسط پرداخت‌نشده‌ای وجود ندارد.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            val toShow = (overdue.take(2) + upcoming).distinctBy { it.id }.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                toShow.forEach { p ->
                    val parent = instMap[p.installmentId]
                    val isLate = p.dueEpochDay < todayEpoch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${parent?.title ?: "قسط"} (شماره ${PersianFormatters.digits(p.installmentIndex.toString(), useFa)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.inkPrimary
                            )
                            Text(
                                text = "سررسید: ${JalaliCalendarEngine.epochDayToJalali(p.dueEpochDay).formatNumeric(useFa)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLate) CrimsonDanger else tokens.inkSecondary
                            )
                        }
                        GlassBadge(
                            text = PersianFormatters.formatCurrency(
                                (p.expectedAmount - p.paidAmount).coerceAtLeast(0.0),
                                parent?.currency ?: "IRT",
                                useFa
                            ),
                            color = if (isLate) CrimsonDanger else tokens.accentSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardUniversityWidget(
    state: LifePilotDataState,
    todayEpoch: Long,
    useFa: Boolean,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val todayDow = JalaliCalendarEngine.epochDayToPersianDayOfWeek(todayEpoch)
    val todayClasses = remember(state.courses, todayDow) { state.courses.filter { it.dayOfWeek == todayDow } }
    val upcomingExams = remember(state.universityItems, todayEpoch) {
        state.universityItems.filter { it.status != "COMPLETED" && it.dueEpochDay >= todayEpoch }.take(2)
    }

    GlassCard(onClick = { onNavigate(AppRoute.UNIVERSITY) }) {
        GlassSectionHeader(
            title = "دانشگاه و کلاس‌ها",
            subtitle = "${PersianFormatters.digits(state.courses.size.toString(), useFa)} درس فعال",
            actionLabel = "دانشگاه",
            onActionClick = { onNavigate(AppRoute.UNIVERSITY) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (todayClasses.isEmpty() && upcomingExams.isEmpty()) {
            Text("کلاس یا تکلیف نزدیکی برای امروز ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            todayClasses.forEach { c ->
                Text(
                    text = "• ${c.title} (${JalaliCalendarEngine.formatMinuteOfDay(c.startMinuteOfDay, useFa)} تا ${JalaliCalendarEngine.formatMinuteOfDay(c.endMinuteOfDay, useFa)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.inkPrimary
                )
            }
            upcomingExams.forEach { u ->
                Text(
                    text = "• ${u.title} — ${JalaliCalendarEngine.epochDayToJalali(u.dueEpochDay).formatShort(useFa)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.accentPrimary
                )
            }
        }
    }
}

@Composable
private fun DashboardProjectsWidget(
    state: LifePilotDataState,
    useFa: Boolean,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val activeProjects = remember(state.projects) { state.projects.filter { it.status != "COMPLETED" }.take(3) }
    GlassCard(onClick = { onNavigate(AppRoute.PROJECTS) }) {
        GlassSectionHeader(
            title = "پیشرفت پروژه‌ها",
            actionLabel = "پروژه‌ها",
            onActionClick = { onNavigate(AppRoute.PROJECTS) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (activeProjects.isEmpty()) {
            Text("پروژه فعالی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeProjects.forEach { p ->
                    val progress = state.projectProgress(p.id)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                            Text(PersianFormatters.formatPercent(progress, useFa), style = MaterialTheme.typography.labelMedium, color = tokens.accentPrimary)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = tokens.accentPrimary,
                            trackColor = tokens.subtleDivider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHabitsWidget(
    state: LifePilotDataState,
    todayEpoch: Long,
    useFa: Boolean,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val activeHabits = remember(state.habits) { state.habits.filter { !it.isArchived && !it.isPaused }.take(4) }
    GlassCard {
        GlassSectionHeader(
            title = "عادت‌های امروز",
            actionLabel = "عادت‌ها",
            onActionClick = { onNavigate(AppRoute.HABITS) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (activeHabits.isEmpty()) {
            Text("هنوز عادتی تعریف نکرده‌اید.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeHabits.forEach { habit ->
                    val logCount = state.habitLogs.find { it.habitId == habit.id && it.dateEpochDay == todayEpoch }?.completedCount ?: 0
                    val done = logCount >= habit.targetCount
                    val stats = LifePilotDomainLogic.calculateHabitStats(habit, state.habitLogs, todayEpoch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                            Text(
                                text = "زنجیره فعلی: ${PersianFormatters.digits(stats.currentStreak.toString(), useFa)} روز • ${PersianFormatters.digits("$logCount/${habit.targetCount}", useFa)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustHabitProgress(habit, todayEpoch, if (done) -1 else 1) }
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Add,
                                contentDescription = "ثبت عادت",
                                tint = if (done) EmeraldSuccess else tokens.accentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardNotesWidget(
    state: LifePilotDataState,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val recentNotes = remember(state.notes) { state.notes.filter { !it.isArchived }.take(3) }
    GlassCard(onClick = { onNavigate(AppRoute.NOTES) }) {
        GlassSectionHeader(
            title = "یادداشت‌های اخیر",
            actionLabel = "دفترچه",
            onActionClick = { onNavigate(AppRoute.NOTES) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (recentNotes.isEmpty()) {
            Text("یادداشتی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recentNotes.forEach { n ->
                    Text(
                        text = "• ${n.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardActivityWidget(
    state: LifePilotDataState,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val recent = remember(state.activityLogs) { state.activityLogs.take(4) }
    GlassCard(onClick = { onNavigate(AppRoute.ACTIVITY) }) {
        GlassSectionHeader(
            title = "فعالیت‌های اخیر",
            actionLabel = "تاریخچه",
            onActionClick = { onNavigate(AppRoute.ACTIVITY) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (recent.isEmpty()) {
            Text("هنوز فعالیتی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recent.forEach { a ->
                    Text(
                        text = "• ${a.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: String) {
    val (label, color) = when (priority) {
        "URGENT" -> "فوری" to CrimsonDanger
        "HIGH" -> "بالا" to AmberWarning
        "LOW" -> "کم" to EmeraldSuccess
        else -> "متوسط" to SapphireInfo
    }
    GlassBadge(text = label, color = color)
}
