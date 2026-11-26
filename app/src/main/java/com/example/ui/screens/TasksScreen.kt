package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.ProjectEntity
import com.example.data.local.TaskEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

private enum class TaskViewFilter(val labelFa: String) {
    TODAY("امروز"),
    UPCOMING("پیش‌رو"),
    OVERDUE("عقب‌افتاده"),
    WEEKLY("هفتگی"),
    COMPLETED("تکمیل‌شده"),
    ALL("همه")
}

private val TASK_CATEGORIES = listOf(
    "ALL" to "همه دسته‌ها",
    "WORK" to "کاری",
    "UNIVERSITY" to "دانشگاه",
    "PERSONAL" to "شخصی",
    "PROJECTS" to "پروژه‌ها",
    "OTHER" to "سایر"
)

@Composable
fun TasksScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val today = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }

    var viewFilter by remember { mutableStateOf(TaskViewFilter.ALL) }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val filteredTasks = remember(state.tasks, viewFilter, categoryFilter, searchQuery, today) {
        state.tasks.filter { t ->
            val viewOk = when (viewFilter) {
                TaskViewFilter.TODAY -> t.deadlineEpochDay == today && t.status != "CANCELLED"
                TaskViewFilter.UPCOMING -> t.deadlineEpochDay > today && t.status != "COMPLETED" && t.status != "CANCELLED"
                TaskViewFilter.OVERDUE -> t.deadlineEpochDay < today && t.status != "COMPLETED" && t.status != "CANCELLED"
                TaskViewFilter.WEEKLY -> t.deadlineEpochDay in today..(today + 7) && t.status != "CANCELLED"
                TaskViewFilter.COMPLETED -> t.status == "COMPLETED"
                TaskViewFilter.ALL -> true
            }
            val catOk = categoryFilter == "ALL" || t.category == categoryFilter
            val qOk = searchQuery.isBlank() || t.title.contains(searchQuery, true) || t.description.contains(searchQuery, true)
            viewOk && catOk && qOk
        }.sortedWith(compareBy<TaskEntity> { it.status == "COMPLETED" }.thenBy { it.deadlineEpochDay })
    }

    val projectNames = remember(state.projects) { state.projects.associate { it.id to it.title } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("tasks_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("وظایف و کارها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        val doneCount = state.tasks.count { it.status == "COMPLETED" }
                        Text(
                            text = "${PersianFormatters.digits(doneCount.toString(), useFa)} از ${PersianFormatters.digits(state.tasks.size.toString(), useFa)} وظیفه تکمیل شده",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.inkSecondary
                        )
                    }
                    Button(
                        onClick = { editingTask = null; showEditor = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("وظیفه جدید")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در وظایف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TaskViewFilter.entries) { f ->
                        FilterChip(selected = viewFilter == f, onClick = { viewFilter = f }, label = { Text(f.labelFa) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TASK_CATEGORIES) { (code, lbl) ->
                        FilterChip(selected = categoryFilter == code, onClick = { categoryFilter = code }, label = { Text(lbl) })
                    }
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.TaskAlt,
                    title = "وظیفه‌ای در این فیلتر نیست",
                    description = "وظایف روزانه، پروژه‌ای یا دانشگاهی خود را اضافه کنید تا در تقویم جلالی و برنامه‌ریز هوشمند نمایش داده شوند.",
                    actionLabel = "ایجاد وظیفه",
                    onAction = { editingTask = null; showEditor = true }
                )
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val done = task.status == "COMPLETED"
                val overdue = !done && task.deadlineEpochDay < today
                GlassCard(
                    onClick = { editingTask = task; showEditor = true },
                    accentBorderColor = if (overdue) CrimsonDanger else null,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = done, onCheckedChange = { viewModel.toggleTaskCompletion(task) })
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (done) tokens.inkSecondary else tokens.inkPrimary,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                PriorityChip(task.priority)
                            }
                            if (task.description.isNotBlank()) {
                                Text(task.description, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary, maxLines = 2)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "سررسید: ${JalaliCalendarEngine.epochDayToJalali(task.deadlineEpochDay).formatNumeric(useFa)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overdue) CrimsonDanger else tokens.inkSecondary
                                )
                                Text(
                                    text = "• ${PersianFormatters.digits(task.estimatedMinutes.toString(), useFa)} دقیقه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.inkSecondary
                                )
                                task.linkedProjectId?.let { pid ->
                                    projectNames[pid]?.let { pName ->
                                        GlassBadge(pName, tokens.accentSecondary)
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { deletingTask = task }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف وظیفه", tint = tokens.inkSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            existing = editingTask,
            projects = state.projects,
            onSave = { task -> viewModel.saveTask(task); showEditor = false },
            onDismiss = { showEditor = false }
        )
    }

    deletingTask?.let { t ->
        ConfirmDeleteDialog(
            title = "حذف وظیفه",
            message = "آیا از حذف وظیفه «${t.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteTask(t) },
            onDismiss = { deletingTask = null }
        )
    }
}

@Composable
fun TaskEditorDialog(
    existing: TaskEntity?,
    projects: List<ProjectEntity>,
    onSave: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val today = remember { JalaliCalendarEngine.todayEpochDay() }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "PERSONAL") }
    var priority by remember { mutableStateOf(existing?.priority ?: "MEDIUM") }
    var status by remember { mutableStateOf(existing?.status ?: "PENDING") }
    var deadlineEpoch by remember { mutableLongStateOf(existing?.deadlineEpochDay ?: today) }
    var estMinStr by remember { mutableStateOf((existing?.estimatedMinutes ?: 30).toString()) }
    var linkedProjectId by remember { mutableStateOf(existing?.linkedProjectId) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ایجاد وظیفه جدید" else "ویرایش وظیفه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان وظیفه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("توضیحات") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("LOW" to "کم", "MEDIUM" to "متوسط", "HIGH" to "بالا", "URGENT" to "فوری")) { (p, lbl) ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(lbl) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TASK_CATEGORIES.drop(1)) { (c, lbl) ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(lbl) })
                    }
                }
                if (existing != null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("PENDING" to "در انتظار", "IN_PROGRESS" to "در حال انجام", "COMPLETED" to "تکمیل", "CANCELLED" to "لغو")) { (s, lbl) ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(lbl) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1.3f)) {
                        Text(JalaliCalendarEngine.epochDayToJalali(deadlineEpoch).formatNumeric(tokens.usePersianDigits))
                    }
                    OutlinedTextField(
                        value = estMinStr,
                        onValueChange = { estMinStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } },
                        label = { Text("دقیقه") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (projects.isNotEmpty()) {
                    Text("اتصال به پروژه (اختیاری)", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = linkedProjectId == null, onClick = { linkedProjectId = null }, label = { Text("بدون پروژه") })
                        }
                        items(projects) { p ->
                            FilterChip(selected = linkedProjectId == p.id, onClick = { linkedProjectId = p.id }, label = { Text(p.title) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    TaskEntity(
                        id = existing?.id ?: 0,
                        title = title.trim(),
                        description = description.trim(),
                        category = category,
                        priority = priority,
                        startEpochDay = existing?.startEpochDay ?: today,
                        deadlineEpochDay = deadlineEpoch,
                        estimatedMinutes = (estMinStr.toIntOrNull() ?: 30).coerceIn(5, 600),
                        status = status,
                        linkedProjectId = linkedProjectId
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = deadlineEpoch,
            onDateSelected = { deadlineEpoch = it; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}
