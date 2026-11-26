package com.example.ui.screens

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
import com.example.data.local.UniversityCourseEntity
import com.example.data.local.UniversityItemEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun UniversityScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var tab by remember { mutableStateOf(0) } // 0=Courses, 1=Assignments & Exams
    var showAddCourse by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }

    val totalCredits = state.courses.sumOf { it.credits }
    val pendingAcademic = state.universityItems.count { it.status != "COMPLETED" }
    val courseMap = remember(state.courses) { state.courses.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("university_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("مدیریت دانشگاه", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text(
                            text = "${PersianFormatters.digits(state.courses.size.toString(), useFa)} درس (${PersianFormatters.digits(totalCredits.toString(), useFa)} واحد) • ${PersianFormatters.digits(pendingAcademic.toString(), useFa)} مورد پیش‌رو",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.inkSecondary
                        )
                    }
                    Button(
                        onClick = { if (tab == 0) showAddCourse = true else showAddItem = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (tab == 0) "درس جدید" else "تکلیف/امتحان")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("دروس و برنامه هفتگی") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("امتحانات و تکالیف") })
                }
            }
        }

        if (tab == 0) {
            if (state.courses.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = Icons.Default.School,
                        title = "درسی ثبت نشده است",
                        description = "دروس ترم و زمان کلاس‌های هفتگی را ثبت کنید تا به صورت خودکار در تقویم جلالی تکرار شوند.",
                        actionLabel = "افزودن درس",
                        onAction = { showAddCourse = true }
                    )
                }
            } else {
                items(state.courses, key = { it.id }) { course ->
                    GlassCard(contentPadding = PaddingValues(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(course.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                                    GlassBadge("${PersianFormatters.digits(course.credits.toString(), useFa)} واحد", tokens.accentPrimary)
                                }
                                Text(
                                    text = "استاد: ${course.professor.ifBlank { "نامشخص" }} • کلاس: ${course.room.ifBlank { "—" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.inkSecondary
                                )
                                Text(
                                    text = "${JalaliCalendarEngine.PERSIAN_WEEKDAYS[course.dayOfWeek.coerceIn(0, 6)]} • ${JalaliCalendarEngine.formatMinuteOfDay(course.startMinuteOfDay, useFa)} تا ${JalaliCalendarEngine.formatMinuteOfDay(course.endMinuteOfDay, useFa)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.accentPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteCourse(course) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف درس", tint = tokens.inkSecondary)
                            }
                        }
                    }
                }
            }
        } else {
            if (state.universityItems.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = Icons.Default.Assignment,
                        title = "تکلیف یا امتحانی ثبت نشده",
                        description = "تمارین، امتحانات و جلسات مطالعه خود را ثبت کنید تا در تقویم نمایش داده شوند.",
                        actionLabel = "ثبت مورد دانشگاهی",
                        onAction = { showAddItem = true }
                    )
                }
            } else {
                items(state.universityItems, key = { it.id }) { item ->
                    val done = item.status == "COMPLETED"
                    val typeFa = when (item.type) {
                        "EXAM" -> "امتحان"
                        "SUBMISSION" -> "تحویل پروژه"
                        "STUDY" -> "مطالعه"
                        else -> "تکلیف"
                    }
                    GlassCard(contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = done, onCheckedChange = { viewModel.toggleUniversityItem(item) })
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    GlassBadge(typeFa, if (item.type == "EXAM") CrimsonDanger else SapphireInfo)
                                }
                                Text(
                                    text = "${courseMap[item.courseId]?.title ?: "عمومی"} • ${JalaliCalendarEngine.epochDayToJalali(item.dueEpochDay).formatFull(useFa)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.inkSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteUniversityItem(item) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = tokens.inkSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCourse) {
        var title by remember { mutableStateOf("") }
        var professor by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("") }
        var creditsStr by remember { mutableStateOf("3") }
        var dow by remember { mutableIntStateOf(0) }
        var startH by remember { mutableStateOf("9") }
        var endH by remember { mutableStateOf("11") }

        AlertDialog(
            onDismissRequest = { showAddCourse = false },
            title = { Text("افزودن درس دانشگاهی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("نام درس") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = professor, onValueChange = { professor = it }, label = { Text("نام استاد") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("کلاس/ساختمان") }, singleLine = true, modifier = Modifier.weight(1.4f))
                        OutlinedTextField(value = creditsStr, onValueChange = { creditsStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } }, label = { Text("واحد") }, singleLine = true, modifier = Modifier.weight(0.8f))
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items((0..6).toList()) { d ->
                            FilterChip(selected = dow == d, onClick = { dow = d }, label = { Text(JalaliCalendarEngine.PERSIAN_WEEKDAYS[d]) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startH, onValueChange = { startH = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } }, label = { Text("ساعت شروع") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endH, onValueChange = { endH = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } }, label = { Text("ساعت پایان") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sh = (startH.toIntOrNull() ?: 9).coerceIn(6, 22)
                    val eh = (endH.toIntOrNull() ?: (sh + 2)).coerceIn(sh + 1, 23)
                    viewModel.saveCourse(
                        UniversityCourseEntity(
                            title = title.trim(),
                            professor = professor.trim(),
                            room = room.trim(),
                            credits = (creditsStr.toIntOrNull() ?: 3).coerceIn(1, 6),
                            dayOfWeek = dow,
                            startMinuteOfDay = sh * 60,
                            endMinuteOfDay = eh * 60
                        )
                    )
                    showAddCourse = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAddCourse = false }) { Text("انصراف") } }
        )
    }

    if (showAddItem) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("ASSIGNMENT") }
        var courseId by remember { mutableStateOf(state.courses.firstOrNull()?.id ?: 0L) }
        var dueEpoch by remember { mutableLongStateOf(JalaliCalendarEngine.todayEpochDay() + 3) }
        var showDp by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddItem = false },
            title = { Text("ثبت تکلیف یا امتحان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("ASSIGNMENT" to "تکلیف", "EXAM" to "امتحان", "SUBMISSION" to "پروژه", "STUDY" to "مطالعه")) { (c, lbl) ->
                            FilterChip(selected = type == c, onClick = { type = c }, label = { Text(lbl) })
                        }
                    }
                    if (state.courses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.courses) { c ->
                                FilterChip(selected = courseId == c.id, onClick = { courseId = c.id }, label = { Text(c.title) })
                            }
                        }
                    }
                    OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("تاریخ: ${JalaliCalendarEngine.epochDayToJalali(dueEpoch).formatFull(useFa)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveUniversityItem(
                        UniversityItemEntity(courseId = courseId, title = title.trim(), type = type, dueEpochDay = dueEpoch)
                    )
                    showAddItem = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAddItem = false }) { Text("انصراف") } }
        )
        if (showDp) {
            JalaliDatePickerDialog(dueEpoch, onDateSelected = { dueEpoch = it; showDp = false }, onDismiss = { showDp = false })
        }
    }
}
