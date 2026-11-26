package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.ProjectEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun ProjectsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var expandedProjectId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var deletingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var addingMilestoneFor by remember { mutableStateOf<ProjectEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("projects_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("کار و پروژه‌ها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text("پیشرفت هر پروژه از وظایف و نقاط عطف واقعی محاسبه می‌شود", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                    Button(onClick = { editingProject = null; showCreateDialog = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پروژه جدید")
                    }
                }
            }
        }

        if (state.projects.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.Workspaces,
                    title = "پروژه‌ای تعریف نشده است",
                    description = "پروژه‌های کاری و شخصی خود را همراه با نقاط عطف و ددلاین در تقویم جلالی مدیریت کنید.",
                    actionLabel = "ایجاد پروژه",
                    onAction = { editingProject = null; showCreateDialog = true }
                )
            }
        } else {
            items(state.projects, key = { it.id }) { proj ->
                val progress = state.projectProgress(proj.id)
                val projMilestones = state.milestones.filter { it.projectId == proj.id }
                val projTasks = state.tasks.filter { it.linkedProjectId == proj.id }
                val isExpanded = expandedProjectId == proj.id

                GlassCard(onClick = { expandedProjectId = if (isExpanded) null else proj.id }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(proj.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                                PriorityChip(proj.priority)
                            }
                            if (proj.description.isNotBlank()) {
                                Text(proj.description, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                            }
                            Text(
                                text = "تحویل: ${JalaliCalendarEngine.epochDayToJalali(proj.endEpochDay).formatNumeric(useFa)} • پیشرفت: ${PersianFormatters.formatPercent(progress, useFa)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.accentPrimary
                            )
                        }
                        Row {
                            IconButton(onClick = { editingProject = proj; showCreateDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "ویرایش پروژه", tint = tokens.inkSecondary)
                            }
                            IconButton(onClick = { deletingProject = proj }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف پروژه", tint = tokens.inkSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = tokens.accentPrimary,
                        trackColor = tokens.subtleDivider
                    )

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = tokens.subtleDivider)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("نقاط عطف (${PersianFormatters.digits(projMilestones.size.toString(), useFa)})", style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = { addingMilestoneFor = proj }) { Text("+ نقطه عطف") }
                        }
                        projMilestones.forEach { ms ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(checked = ms.isCompleted, onCheckedChange = { viewModel.toggleMilestone(ms) })
                                Text(
                                    text = "${ms.title} (${JalaliCalendarEngine.epochDayToJalali(ms.dueEpochDay).formatShort(useFa)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (ms.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteMilestone(ms) }) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (projTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("وظایف متصل (${PersianFormatters.digits(projTasks.size.toString(), useFa)})", style = MaterialTheme.typography.labelLarge)
                            projTasks.forEach { t ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = t.status == "COMPLETED", onCheckedChange = { viewModel.toggleTaskCompletion(t) })
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditorDialog(
            existing = editingProject,
            onSave = { p -> viewModel.saveProject(p); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }

    addingMilestoneFor?.let { proj ->
        var msTitle by remember { mutableStateOf("") }
        var msEpoch by remember { mutableLongStateOf(proj.endEpochDay) }
        var showDp by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { addingMilestoneFor = null },
            title = { Text("نقطه عطف برای «${proj.title}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = msTitle, onValueChange = { msTitle = it }, label = { Text("عنوان نقطه عطف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("تاریخ: ${JalaliCalendarEngine.epochDayToJalali(msEpoch).formatNumeric(useFa)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addMilestone(proj.id, msTitle, msEpoch); addingMilestoneFor = null }) { Text("افزودن") }
            },
            dismissButton = { TextButton(onClick = { addingMilestoneFor = null }) { Text("انصراف") } }
        )
        if (showDp) {
            JalaliDatePickerDialog(msEpoch, onDateSelected = { msEpoch = it; showDp = false }, onDismiss = { showDp = false })
        }
    }

    deletingProject?.let { p ->
        ConfirmDeleteDialog(
            title = "حذف پروژه",
            message = "آیا از حذف پروژه «${p.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteProject(p) },
            onDismiss = { deletingProject = null }
        )
    }
}

@Composable
fun ProjectEditorDialog(
    existing: ProjectEntity?,
    onSave: (ProjectEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val today = remember { JalaliCalendarEngine.todayEpochDay() }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: "MEDIUM") }
    var status by remember { mutableStateOf(existing?.status ?: "ACTIVE") }
    var endEpoch by remember { mutableLongStateOf(existing?.endEpochDay ?: (today + 30)) }
    var showDp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ایجاد پروژه جدید" else "ویرایش پروژه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("نام پروژه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("توضیحات") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("PLANNING" to "برنامه‌ریزی", "ACTIVE" to "فعال", "ON_HOLD" to "متوقف", "COMPLETED" to "تکمیل")) { (s, lbl) ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(lbl) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("LOW" to "کم", "MEDIUM" to "متوسط", "HIGH" to "بالا", "URGENT" to "فوری")) { (p, lbl) ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(lbl) })
                    }
                }
                OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("ددلاین پروژه: ${JalaliCalendarEngine.epochDayToJalali(endEpoch).formatFull(tokens.usePersianDigits)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    ProjectEntity(
                        id = existing?.id ?: 0,
                        title = title.trim(),
                        description = desc.trim(),
                        startEpochDay = existing?.startEpochDay ?: today,
                        endEpochDay = endEpoch,
                        priority = priority,
                        status = status
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
    if (showDp) {
        JalaliDatePickerDialog(endEpoch, onDateSelected = { endEpoch = it; showDp = false }, onDismiss = { showDp = false })
    }
}
