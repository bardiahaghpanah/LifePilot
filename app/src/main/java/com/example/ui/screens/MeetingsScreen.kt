package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.MeetingEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun MeetingsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var showEditor by remember { mutableStateOf(false) }
    var editingMeeting by remember { mutableStateOf<MeetingEntity?>(null) }
    var deletingMeeting by remember { mutableStateOf<MeetingEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("meetings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("جلسات و قرارها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text("متصل به تقویم جلالی و برنامه‌ریز هوشمند", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                    Button(onClick = { editingMeeting = null; showEditor = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جلسه جدید")
                    }
                }
            }
        }

        if (state.meetings.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.Groups,
                    title = "جلسه‌ای ثبت نشده است",
                    description = "قرارهای کاری، جلسات آنلاین و یادداشت‌های آماده‌سازی را مدیریت کنید.",
                    actionLabel = "ثبت جلسه",
                    onAction = { editingMeeting = null; showEditor = true }
                )
            }
        } else {
            items(state.meetings, key = { it.id }) { m ->
                GlassCard(onClick = { editingMeeting = m; showEditor = true }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(m.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                            Text(
                                text = "${JalaliCalendarEngine.epochDayToJalali(m.dateEpochDay).formatFull(useFa)} • ${JalaliCalendarEngine.formatMinuteOfDay(m.startMinuteOfDay, useFa)} تا ${JalaliCalendarEngine.formatMinuteOfDay(m.endMinuteOfDay, useFa)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.accentPrimary
                            )
                            if (m.location.isNotBlank()) {
                                Text("مکان: ${m.location}", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                            }
                            if (m.participants.isNotBlank()) {
                                Text("شرکت‌کنندگان: ${m.participants}", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                            }
                            if (m.notes.isNotBlank()) {
                                Text("یادداشت آماده‌سازی: ${m.notes}", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                            }
                        }
                        IconButton(onClick = { deletingMeeting = m }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف جلسه", tint = tokens.inkSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { viewModel.createFollowUpTaskFromMeeting(m) },
                            label = { Text("ساخت وظیفه پیگیری") },
                            leadingIcon = { Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        if (m.onlineLink.isNotBlank()) {
                            GlassBadge("آنلاین", SapphireInfo)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        MeetingEditorDialog(
            existing = editingMeeting,
            onSave = { viewModel.saveMeeting(it); showEditor = false },
            onDismiss = { showEditor = false }
        )
    }

    deletingMeeting?.let { m ->
        ConfirmDeleteDialog(
            title = "حذف جلسه",
            message = "آیا از حذف جلسه «${m.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteMeeting(m) },
            onDismiss = { deletingMeeting = null }
        )
    }
}

@Composable
fun MeetingEditorDialog(
    existing: MeetingEntity?,
    onSave: (MeetingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var onlineLink by remember { mutableStateOf(existing?.onlineLink ?: "") }
    var participants by remember { mutableStateOf(existing?.participants ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var dateEpoch by remember { mutableLongStateOf(existing?.dateEpochDay ?: JalaliCalendarEngine.todayEpochDay()) }
    var startH by remember { mutableStateOf(((existing?.startMinuteOfDay ?: 600) / 60).toString()) }
    var endH by remember { mutableStateOf(((existing?.endMinuteOfDay ?: 660) / 60).toString()) }
    var showDp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "زمان‌بندی جلسه جدید" else "ویرایش جلسه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان جلسه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("تاریخ: ${JalaliCalendarEngine.epochDayToJalali(dateEpoch).formatFull(tokens.usePersianDigits)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startH, onValueChange = { startH = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } }, label = { Text("ساعت شروع") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endH, onValueChange = { endH = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } }, label = { Text("ساعت پایان") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("مکان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = onlineLink, onValueChange = { onlineLink = it }, label = { Text("لینک جلسه آنلاین (اختیاری)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = participants, onValueChange = { participants = it }, label = { Text("شرکت‌کنندگان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("یادداشت‌های جلسه") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val sh = (startH.toIntOrNull() ?: 10).coerceIn(0, 23)
                val eh = (endH.toIntOrNull() ?: (sh + 1)).coerceIn(sh, 23)
                onSave(
                    MeetingEntity(
                        id = existing?.id ?: 0,
                        title = title.trim(),
                        dateEpochDay = dateEpoch,
                        startMinuteOfDay = sh * 60,
                        endMinuteOfDay = eh * 60,
                        location = location.trim(),
                        onlineLink = onlineLink.trim(),
                        participants = participants.trim(),
                        notes = notes.trim()
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
    if (showDp) {
        JalaliDatePickerDialog(dateEpoch, onDateSelected = { dateEpoch = it; showDp = false }, onDismiss = { showDp = false })
    }
}
