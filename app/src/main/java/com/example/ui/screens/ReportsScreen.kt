package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.repository.LifePilotDomainLogic
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState

@Composable
fun ReportsScreen(state: LifePilotDataState) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val context = LocalContext.current
    val today = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }
    var rangeDays by remember { mutableIntStateOf(30) } // 7, 30, 365

    val startEpoch = today - rangeDays
    val periodTxs = remember(state.transactions, startEpoch) {
        state.transactions.filter { it.dateEpochDay >= startEpoch }
    }
    val periodIrtIncome = periodTxs.filter { it.currency == "IRT" && it.type == "INCOME" }.sumOf { it.amount }
    val periodIrtExpense = periodTxs.filter { it.currency == "IRT" && it.type == "EXPENSE" }.sumOf { it.amount }
    val periodUsdIncome = periodTxs.filter { it.currency == "USD" && it.type == "INCOME" }.sumOf { it.amount }
    val periodUsdExpense = periodTxs.filter { it.currency == "USD" && it.type == "EXPENSE" }.sumOf { it.amount }

    val completedTasks = state.tasks.count { it.status == "COMPLETED" }
    val overdueTasks = state.tasks.count { it.status != "COMPLETED" && it.status != "CANCELLED" && it.deadlineEpochDay < today }
    val totalTaskMinutes = state.tasks.filter { it.status == "COMPLETED" }.sumOf { it.estimatedMinutes }

    val avgHabitCompletion = remember(state.habits, state.habitLogs, today) {
        val active = state.habits.filter { !it.isArchived }
        if (active.isEmpty()) 0f
        else active.map { LifePilotDomainLogic.calculateHabitStats(it, state.habitLogs, today).last30DaysCompletionRate }.average().toFloat()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("گزارش‌ها و تحلیل عملکرد", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text("محاسبه‌شده ۱۰۰٪ از داده‌های واقعی پایگاه‌داده", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                    FilledTonalButton(onClick = {
                        val summary = buildString {
                            appendLine("گزارش عملکرد LifePilot (${JalaliCalendarEngine.todayJalali().formatFull(useFa)})")
                            appendLine("درآمد تومان ($rangeDays روز): ${PersianFormatters.formatIrt(periodIrtIncome, useFa)}")
                            appendLine("هزینه تومان ($rangeDays روز): ${PersianFormatters.formatIrt(periodIrtExpense, useFa)}")
                            appendLine("درآمد دلار ($rangeDays روز): ${PersianFormatters.formatUsd(periodUsdIncome, useFa)}")
                            appendLine("هزینه دلار ($rangeDays روز): ${PersianFormatters.formatUsd(periodUsdExpense, useFa)}")
                            appendLine("وظایف تکمیل‌شده: $completedTasks | عقب‌افتاده: $overdueTasks")
                            appendLine("میانگین پایبندی عادت‌ها: ${PersianFormatters.formatPercent(avgHabitCompletion, useFa)}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "گزارش LifePilot")
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(intent, "خروجی گزارش"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروجی")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7 to "۷ روزه", 30 to "۳۰ روزه", 365 to "سالانه").forEach { (d, lbl) ->
                        FilterChip(selected = rangeDays == d, onClick = { rangeDays = d }, label = { Text(lbl) })
                    }
                }
            }
        }

        // Financial Bar Chart (Income vs Expense IRT)
        item {
            GlassCard {
                Text("نمودار درآمد در برابر هزینه (تومان)", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                val maxV = maxOf(periodIrtIncome, periodIrtExpense, 1.0)
                val incRatio = (periodIrtIncome / maxV).toFloat().coerceIn(0.04f, 1f)
                val expRatio = (periodIrtExpense / maxV).toFloat().coerceIn(0.04f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("درآمد دوره", style = MaterialTheme.typography.bodyMedium)
                        Text(PersianFormatters.formatIrt(periodIrtIncome, useFa), style = MaterialTheme.typography.titleSmall, color = EmeraldSuccess)
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                        drawRoundRect(tokens.subtleDivider, cornerRadius = CornerRadius(12f, 12f), size = size)
                        drawRoundRect(EmeraldSuccess, cornerRadius = CornerRadius(12f, 12f), topLeft = Offset(size.width * (1f - incRatio), 0f), size = Size(size.width * incRatio, size.height))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("هزینه دوره", style = MaterialTheme.typography.bodyMedium)
                        Text(PersianFormatters.formatIrt(periodIrtExpense, useFa), style = MaterialTheme.typography.titleSmall, color = CrimsonDanger)
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                        drawRoundRect(tokens.subtleDivider, cornerRadius = CornerRadius(12f, 12f), size = size)
                        drawRoundRect(CrimsonDanger, cornerRadius = CornerRadius(12f, 12f), topLeft = Offset(size.width * (1f - expRatio), 0f), size = Size(size.width * expRatio, size.height))
                    }
                    if (periodUsdIncome > 0 || periodUsdExpense > 0) {
                        HorizontalDivider(color = tokens.subtleDivider)
                        Text(
                            text = "ارزی (USD) — درآمد: ${PersianFormatters.formatUsd(periodUsdIncome, useFa)} • هزینه: ${PersianFormatters.formatUsd(periodUsdExpense, useFa)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.accentPrimary
                        )
                    }
                }
            }
        }

        // Productivity Metrics
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GlassMetricCard(
                    title = "وظایف تکمیل‌شده",
                    value = PersianFormatters.digits(completedTasks.toString(), useFa),
                    subtitle = "${PersianFormatters.digits(totalTaskMinutes.toString(), useFa)} دقیقه زمان ثبت‌شده",
                    icon = Icons.Default.TaskAlt,
                    tint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "وظایف عقب‌افتاده",
                    value = PersianFormatters.digits(overdueTasks.toString(), useFa),
                    subtitle = "نیازمند رسیدگی",
                    icon = Icons.Default.Schedule,
                    tint = if (overdueTasks > 0) CrimsonDanger else tokens.accentPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GlassMetricCard(
                    title = "پایبندی عادت‌ها",
                    value = PersianFormatters.formatPercent(avgHabitCompletion, useFa),
                    subtitle = "میانگین ۳۰ روز اخیر",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = tokens.accentSecondary,
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "پروژه‌های فعال",
                    value = PersianFormatters.digits(state.projects.size.toString(), useFa),
                    subtitle = "${PersianFormatters.digits(state.notes.size.toString(), useFa)} یادداشت ثبت‌شده",
                    icon = Icons.Default.Workspaces,
                    tint = tokens.accentPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
