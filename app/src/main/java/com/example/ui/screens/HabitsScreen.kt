package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.HabitEntity
import com.example.data.repository.LifePilotDomainLogic
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun HabitsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val todayEpoch = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var deletingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    val visibleHabits = remember(state.habits, showArchived) {
        state.habits.filter { if (showArchived) it.isArchived else !it.isArchived }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("habits_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("پیگیری عادت‌ها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text("روزهای غیربرنامه‌ریزی‌شده زنجیره شما را قطع نمی‌کنند", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                    Button(onClick = { showCreateDialog = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عادت جدید")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showArchived, onClick = { showArchived = false }, label = { Text("فعال") })
                    FilterChip(selected = showArchived, onClick = { showArchived = true }, label = { Text("آرشیوشده") })
                }
            }
        }

        if (visibleHabits.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "عادتی ثبت نشده است",
                    description = "عادت‌های روزانه یا هفتگی (ورزش، مطالعه، زبان) بسازید و نقشه حرارتی ۲۸ روزه خود را ببینید.",
                    actionLabel = "ایجاد عادت",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            items(visibleHabits, key = { it.id }) { habit ->
                val stats = remember(habit, state.habitLogs, todayEpoch) {
                    LifePilotDomainLogic.calculateHabitStats(habit, state.habitLogs, todayEpoch)
                }
                val todayCount = state.habitLogs.find { it.habitId == habit.id && it.dateEpochDay == todayEpoch }?.completedCount ?: 0
                val habitColor = Color(habit.colorHex)

                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(habit.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                                if (habit.isPaused) GlassBadge("متوقف", AmberWarning)
                            }
                            Text(
                                text = "زنجیره فعلی: ${PersianFormatters.digits(stats.currentStreak.toString(), useFa)} روز • رکورد: ${PersianFormatters.digits(stats.longestStreak.toString(), useFa)} • ماهانه: ${PersianFormatters.formatPercent(stats.last30DaysCompletionRate, useFa)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                        }
                        Row {
                            IconButton(onClick = { viewModel.saveHabit(habit.copy(isPaused = !habit.isPaused)) }) {
                                Icon(if (habit.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "توقف/ادامه")
                            }
                            IconButton(onClick = { viewModel.saveHabit(habit.copy(isArchived = !habit.isArchived)) }) {
                                Icon(Icons.Default.Archive, contentDescription = "آرشیو")
                            }
                            IconButton(onClick = { deletingHabit = habit }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Daily +/- interactive counter (supports undo accidental taps)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پیشرفت امروز: ${PersianFormatters.digits("$todayCount / ${habit.targetCount}", useFa)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (todayCount >= habit.targetCount) EmeraldSuccess else tokens.inkPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedIconButton(
                                onClick = { viewModel.adjustHabitProgress(habit, todayEpoch, -1) },
                                enabled = todayCount > 0
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش")
                            }
                            FilledIconButton(
                                onClick = { viewModel.adjustHabitProgress(habit, todayEpoch, 1) }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // 28-Day Jalali Calendar Heatmap (4 rows x 7 days)
                    Text("نقشه حرارتی ۲۸ روز اخیر", style = MaterialTheme.typography.labelSmall, color = tokens.inkSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val logMap = remember(state.habitLogs, habit.id) {
                        state.habitLogs.filter { it.habitId == habit.id }.associate { it.dateEpochDay to it.completedCount }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (week in 0..3) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (dayOffset in 0..6) {
                                    val ep = todayEpoch - (27 - (week * 7 + dayOffset))
                                    val cnt = logMap[ep] ?: 0
                                    val ratio = (cnt.toFloat() / habit.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val cellColor = when {
                                        ratio >= 1f -> habitColor
                                        ratio > 0f -> habitColor.copy(alpha = 0.45f)
                                        else -> tokens.subtleDivider
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(cellColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        HabitCreatorDialog(
            onSave = { viewModel.saveHabit(it); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }

    deletingHabit?.let { h ->
        ConfirmDeleteDialog(
            title = "حذف عادت",
            message = "آیا از حذف عادت «${h.title}» و تمام تاریخچه آن مطمئن هستید؟",
            onConfirm = { viewModel.deleteHabit(h) },
            onDismiss = { deletingHabit = null }
        )
    }
}

@Composable
fun HabitCreatorDialog(
    onSave: (HabitEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf("DAILY") }
    var daysMask by remember { mutableIntStateOf(0b1111111) }
    var targetStr by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد عادت جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("نام عادت (مثلاً مطالعه، ورزش)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = scheduleType == "DAILY", onClick = { scheduleType = "DAILY"; daysMask = 0b1111111 }, label = { Text("روزانه") })
                    FilterChip(selected = scheduleType == "WEEKLY", onClick = { scheduleType = "WEEKLY" }, label = { Text("روزهای منتخب") })
                }
                if (scheduleType == "WEEKLY") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        JalaliCalendarEngine.PERSIAN_WEEKDAYS_SHORT.forEachIndexed { idx, lbl ->
                            val selected = ((daysMask shr idx) and 1) == 1
                            FilterChip(
                                selected = selected,
                                onClick = { daysMask = daysMask xor (1 shl idx) },
                                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } },
                    label = { Text("هدف روزانه (تعداد)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    HabitEntity(
                        title = title.trim(),
                        scheduleType = scheduleType,
                        scheduledDaysMask = if (scheduleType == "DAILY") 0b1111111 else daysMask.takeIf { it != 0 } ?: 0b1111111,
                        targetCount = (targetStr.toIntOrNull() ?: 1).coerceIn(1, 50)
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
