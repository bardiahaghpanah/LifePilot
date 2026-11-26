package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

private data class GlobalSearchHit(
    val id: String,
    val moduleFa: String,
    val title: String,
    val subtitle: String,
    val route: AppRoute
)

@Composable
fun GlobalSearchScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var query by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf("ALL") }

    val hits = remember(state, query, selectedModule) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else buildList {
            state.tasks.filter { it.title.contains(q, true) || it.description.contains(q, true) }.forEach {
                add(GlobalSearchHit("t_${it.id}", "وظایف", it.title, it.status, AppRoute.TASKS))
            }
            state.installments.filter { it.title.contains(q, true) || it.creditor.contains(q, true) }.forEach {
                add(GlobalSearchHit("i_${it.id}", "اقساط", it.title, it.creditor, AppRoute.INSTALLMENTS))
            }
            state.transactions.filter { it.title.contains(q, true) || it.category.contains(q, true) }.forEach {
                add(GlobalSearchHit("f_${it.id}", "مالی", it.title, PersianFormatters.formatCurrency(it.amount, it.currency, useFa), AppRoute.FINANCE))
            }
            state.projects.filter { it.title.contains(q, true) || it.description.contains(q, true) }.forEach {
                add(GlobalSearchHit("p_${it.id}", "پروژه‌ها", it.title, it.status, AppRoute.PROJECTS))
            }
            state.notes.filter { it.title.contains(q, true) || it.content.contains(q, true) || it.tags.contains(q, true) }.forEach {
                add(GlobalSearchHit("n_${it.id}", "یادداشت‌ها", it.title, it.category, AppRoute.NOTES))
            }
            state.meetings.filter { it.title.contains(q, true) || it.participants.contains(q, true) }.forEach {
                add(GlobalSearchHit("m_${it.id}", "جلسات", it.title, it.location, AppRoute.MEETINGS))
            }
            state.courses.filter { it.title.contains(q, true) || it.professor.contains(q, true) }.forEach {
                add(GlobalSearchHit("c_${it.id}", "دانشگاه", it.title, it.professor, AppRoute.UNIVERSITY))
            }
            state.universityItems.filter { it.title.contains(q, true) }.forEach {
                add(GlobalSearchHit("u_${it.id}", "دانشگاه", it.title, it.type, AppRoute.UNIVERSITY))
            }
            state.calendarEvents.filter { it.title.contains(q, true) || it.description.contains(q, true) }.forEach {
                add(GlobalSearchHit("e_${it.id}", "تقویم", it.title, it.location, AppRoute.CALENDAR))
            }
            state.habits.filter { it.title.contains(q, true) }.forEach {
                add(GlobalSearchHit("h_${it.id}", "عادت‌ها", it.title, it.scheduleType, AppRoute.HABITS))
            }
        }.filter { selectedModule == "ALL" || it.moduleFa == selectedModule }
    }

    val grouped = remember(hits) { hits.groupBy { it.moduleFa } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("global_search_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Text("جستجوی سراسری (Command Palette)", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجو در وظایف، اقساط، مالی، یادداشت‌ها، جلسات...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "پاک کردن") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("global_search_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ALL" to "همه", "وظایف" to "وظایف", "مالی" to "مالی", "اقساط" to "اقساط", "پروژه‌ها" to "پروژه‌ها", "یادداشت‌ها" to "یادداشت‌ها", "جلسات" to "جلسات", "دانشگاه" to "دانشگاه", "تقویم" to "تقویم", "عادت‌ها" to "عادت‌ها")) { (code, lbl) ->
                        FilterChip(selected = selectedModule == code, onClick = { selectedModule = code }, label = { Text(lbl) })
                    }
                }
            }
        }

        if (query.isBlank() && state.searchHistory.isNotEmpty()) {
            item {
                GlassCard {
                    Text("جستجوهای اخیر", style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.searchHistory) { h ->
                            AssistChip(onClick = { query = h }, label = { Text(h) })
                        }
                    }
                }
            }
        }

        if (query.isNotBlank() && hits.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "نتیجه‌ای یافت نشد",
                    description = "عبارت دیگری وارد کنید یا فیلتر بخش‌ها را روی «همه» بگذارید."
                )
            }
        } else {
            grouped.forEach { (moduleName, list) ->
                item(key = "hdr_$moduleName") {
                    GlassSectionHeader(title = "$moduleName (${PersianFormatters.digits(list.size.toString(), useFa)})")
                }
                items(list, key = { it.id }) { hit ->
                    GlassCard(
                        onClick = {
                            viewModel.addSearchHistory(query)
                            onNavigate(hit.route)
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hit.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                                if (hit.subtitle.isNotBlank()) {
                                    Text(hit.subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                                }
                            }
                            GlassBadge(hit.moduleFa, tokens.accentPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryScreen(
    state: LifePilotDataState
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val today = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }
    var search by remember { mutableStateOf("") }
    var moduleFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(state.activityLogs, search, moduleFilter) {
        state.activityLogs.filter { a ->
            val modOk = moduleFilter == "ALL" || a.entityType == moduleFilter
            val qOk = search.isBlank() || a.description.contains(search, true) || a.actionType.contains(search, true)
            modOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("activity_history_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Text("تاریخچه فعالیت‌ها و گزارش ممیزی", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Text("رکوردهای تاریخی غیرقابل ویرایش برای شفافیت کامل مالی و عملکردی", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("جستجو در فعالیت‌ها...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ALL" to "همه", "FINANCE" to "مالی", "INSTALLMENT" to "اقساط", "EXCHANGE_RATE" to "نرخ ارز", "TASK" to "وظایف", "PROJECT" to "پروژه", "CALENDAR" to "تقویم", "HABIT" to "عادت")) { (c, lbl) ->
                        FilterChip(selected = moduleFilter == c, onClick = { moduleFilter = c }, label = { Text(lbl) })
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.History,
                    title = "تاریخچه‌ای ثبت نشده است",
                    description = "با هر عملیات ثبت، پرداخت یا تکمیل، گزارش دقیق در این بخش ثبت می‌شود."
                )
            }
        } else {
            val grouped = filtered.groupBy { log ->
                val ep = log.timestampMillis / 86_400_000L
                when {
                    ep >= today -> "امروز"
                    ep == today - 1 -> "دیروز"
                    else -> "قدیمی‌تر"
                }
            }
            grouped.forEach { (section, list) ->
                item(key = "sec_$section") {
                    GlassSectionHeader(title = section)
                }
                items(list, key = { it.id }) { log ->
                    GlassCard(contentPadding = PaddingValues(14.dp)) {
                        Text(log.description, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                        if (log.previousValue.isNotBlank() || log.newValue.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOfNotNull(
                                    log.previousValue.takeIf { it.isNotBlank() }?.let { "قبل: $it" },
                                    log.newValue.takeIf { it.isNotBlank() }?.let { "جدید: $it" }
                                ).joinToString("  ←  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.accentPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        GlassBadge(log.entityType, tokens.accentSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.triggerLocalReminderTest()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("notifications_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("مرکز اعلان‌ها و یادآورها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text(
                            text = "${PersianFormatters.digits(state.unreadNotificationsCount.toString(), useFa)} اعلان خوانده‌نشده",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.inkSecondary
                        )
                    }
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text("خواندن همه")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.triggerLocalReminderTest()
                        }
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست اعلان محلی")
                    }
                }
            }
        }

        if (state.notifications.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = "اعلانی وجود ندارد",
                    description = "یادآورهای اقساط، جلسات و رویدادهای مهم اینجا نمایش داده می‌شوند."
                )
            }
        } else {
            items(state.notifications, key = { it.id }) { n ->
                GlassCard(
                    accentBorderColor = if (!n.isRead) tokens.accentPrimary else null,
                    onClick = { viewModel.markNotificationRead(n.id) },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                                if (!n.isRead) GlassBadge("جدید", tokens.accentPrimary)
                            }
                            Text(n.message, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                        }
                        IconButton(onClick = { viewModel.deleteNotification(n.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "حذف اعلان", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
