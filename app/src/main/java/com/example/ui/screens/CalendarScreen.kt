package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.JalaliDate
import com.example.core.jalali.PersianFormatters
import com.example.data.local.CalendarEventEntity
import com.example.data.repository.LifePilotDomainLogic
import com.example.data.repository.UnifiedCalendarItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

private enum class CalendarViewMode(val labelFa: String) {
    MONTH("ماهانه"),
    WEEK("هفتگی"),
    DAY("روزانه"),
    AGENDA("فهرست"),
    YEAR("سالانه")
}

private val CALENDAR_CATEGORY_LABELS = listOf(
    "ALL" to "همه",
    "WORK" to "کاری",
    "UNIVERSITY" to "دانشگاه",
    "MEETINGS" to "جلسات",
    "PERSONAL" to "شخصی",
    "INSTALLMENTS" to "اقساط",
    "TASKS" to "وظایف",
    "PROJECTS" to "پروژه‌ها",
    "EXAMS" to "امتحانات",
    "HOLIDAY" to "مناسبت‌ها"
)

@Composable
fun CalendarScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val today = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayJalali(state.settings.timeZoneId) }

    var viewMode by remember {
        mutableStateOf(
            CalendarViewMode.entries.find { it.name == state.settings.calendarDefaultView } ?: CalendarViewMode.MONTH
        )
    }
    var selectedDate by remember { mutableStateOf(today) }
    var activeCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val selectedEpoch = selectedDate.toEpochDay()
    val dayItemsRaw = remember(state, selectedEpoch) { state.unifiedItemsForDay(selectedEpoch) }
    val dayItems = remember(dayItemsRaw, activeCategory, searchQuery) {
        dayItemsRaw.filter { item ->
            val catOk = activeCategory == "ALL" || item.category == activeCategory
            val qOk = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true)
            catOk && qOk
        }
    }
    val dayConflicts = remember(state, selectedEpoch) {
        LifePilotDomainLogic.detectConflicts(state.fixedBlocksForDay(selectedEpoch))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View Switcher & Navigation Header
        item {
            GlassCard(elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedDate = when (viewMode) {
                            CalendarViewMode.YEAR -> selectedDate.plusYears(-1)
                            CalendarViewMode.WEEK -> selectedDate.plusDays(-7)
                            CalendarViewMode.DAY -> selectedDate.plusDays(-1)
                            else -> selectedDate.plusMonths(-1)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "قبلی")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (viewMode == CalendarViewMode.YEAR)
                                PersianFormatters.digits("سال ${selectedDate.year}", useFa)
                            else selectedDate.formatMonthYear(useFa),
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.inkPrimary
                        )
                        val greg = JalaliCalendarEngine.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day)
                        Text(
                            text = "${selectedDate.formatFull(useFa)} • ${greg.formatNumeric()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.inkSecondary
                        )
                    }
                    IconButton(onClick = {
                        selectedDate = when (viewMode) {
                            CalendarViewMode.YEAR -> selectedDate.plusYears(1)
                            CalendarViewMode.WEEK -> selectedDate.plusDays(7)
                            CalendarViewMode.DAY -> selectedDate.plusDays(1)
                            else -> selectedDate.plusMonths(1)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "بعدی")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CalendarViewMode.entries) { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            label = { Text(mode.labelFa) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { selectedDate = today },
                            label = { Text("امروز") },
                            leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                editingEvent = null
                                showCreateDialog = true
                            },
                            label = { Text("رویداد جدید") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در رویدادها...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CALENDAR_CATEGORY_LABELS) { (code, label) ->
                        FilterChip(
                            selected = activeCategory == code,
                            onClick = { activeCategory = code },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Main Calendar Grid / View
        item {
            when (viewMode) {
                CalendarViewMode.MONTH -> JalaliMonthGridCard(
                    state = state,
                    selectedDate = selectedDate,
                    today = today,
                    useFa = useFa,
                    showWeekNumbers = state.settings.showWeekNumbers,
                    onSelectDate = { selectedDate = it }
                )
                CalendarViewMode.WEEK -> JalaliWeekViewCard(
                    state = state,
                    selectedDate = selectedDate,
                    today = today,
                    useFa = useFa,
                    onSelectDate = { selectedDate = it }
                )
                CalendarViewMode.YEAR -> JalaliYearOverviewCard(
                    year = selectedDate.year,
                    selectedMonth = selectedDate.month,
                    useFa = useFa,
                    onMonthSelected = { month ->
                        selectedDate = JalaliDate(selectedDate.year, month, 1)
                        viewMode = CalendarViewMode.MONTH
                    }
                )
                CalendarViewMode.DAY, CalendarViewMode.AGENDA -> {
                    // Handled in items list below with expanded range for AGENDA
                }
            }
        }

        // Conflict Detection Banner
        if (dayConflicts.isNotEmpty() && viewMode != CalendarViewMode.YEAR) {
            item {
                GlassCard(accentBorderColor = CrimsonDanger) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "تداخل زمانی", tint = CrimsonDanger)
                        Column {
                            Text("تداخل زمانی شناسایی شد!", style = MaterialTheme.typography.titleSmall, color = CrimsonDanger)
                            dayConflicts.forEach { c ->
                                Text(
                                    text = "«${c.first.title}» با «${c.second.title}» هم‌پوشانی دارد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.inkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Agenda or Selected Day Items
        if (viewMode == CalendarViewMode.AGENDA) {
            item {
                GlassSectionHeader(
                    title = "فهرست ۱۴ روز آینده",
                    subtitle = "شروع از ${selectedDate.formatShort(useFa)}"
                )
            }
            val agendaDays = (0L..13L).map { selectedEpoch + it }
            agendaDays.forEach { ep ->
                val itemsForEp = state.unifiedItemsForDay(ep).filter {
                    (activeCategory == "ALL" || it.category == activeCategory) &&
                        (searchQuery.isBlank() || it.title.contains(searchQuery, true))
                }
                if (itemsForEp.isNotEmpty()) {
                    item(key = "agenda_$ep") {
                        val j = JalaliCalendarEngine.epochDayToJalali(ep)
                        GlassCard {
                            Text(
                                text = j.formatFull(useFa),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsForEp.forEach { item ->
                                    UnifiedCalendarItemRow(
                                        item = item,
                                        useFa = useFa,
                                        onClick = {
                                            handleCalendarItemClick(item, state, onNavigate) { ev ->
                                                editingEvent = ev
                                                showCreateDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (viewMode != CalendarViewMode.YEAR) {
            item {
                GlassSectionHeader(
                    title = "برنامه ${selectedDate.formatFull(useFa)}",
                    subtitle = if (JalaliCalendarEngine.isJalaliLeapYear(selectedDate.year))
                        "سال کبیسه (اسفند ۳۰ روزه)" else "اسفند ۲۹ روزه",
                    actionLabel = "+ رویداد",
                    onActionClick = {
                        editingEvent = null
                        showCreateDialog = true
                    }
                )
            }
            if (dayItems.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = Icons.Default.EventAvailable,
                        title = "روز آرام و خالی",
                        description = "برای این تاریخ رویداد، قسط یا جلسه‌ای ثبت نشده است.",
                        actionLabel = "ثبت رویداد جدید",
                        onAction = {
                            editingEvent = null
                            showCreateDialog = true
                        }
                    )
                }
            } else {
                items(dayItems, key = { it.id }) { item ->
                    GlassCard(
                        onClick = {
                            handleCalendarItemClick(item, state, onNavigate) { ev ->
                                editingEvent = ev
                                showCreateDialog = true
                            }
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        UnifiedCalendarItemRow(item = item, useFa = useFa, onClick = null)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CalendarEventEditorDialog(
            initialDateEpochDay = editingEvent?.startEpochDay ?: selectedEpoch,
            existing = editingEvent,
            onSave = { ev ->
                viewModel.saveCalendarEvent(ev)
                showCreateDialog = false
            },
            onDelete = editingEvent?.let { ev ->
                {
                    viewModel.deleteCalendarEvent(ev)
                    showCreateDialog = false
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

private fun handleCalendarItemClick(
    item: UnifiedCalendarItem,
    state: LifePilotDataState,
    onNavigate: (AppRoute) -> Unit,
    onEditEvent: (CalendarEventEntity) -> Unit
) {
    when (item.sourceType) {
        "EVENT" -> state.calendarEvents.find { it.id == item.sourceId }?.let(onEditEvent)
        "INSTALLMENT" -> onNavigate(AppRoute.INSTALLMENTS)
        "TASK" -> onNavigate(AppRoute.TASKS)
        "PROJECT" -> onNavigate(AppRoute.PROJECTS)
        "MEETING" -> onNavigate(AppRoute.MEETINGS)
        "COURSE", "EXAM", "ASSIGNMENT" -> onNavigate(AppRoute.UNIVERSITY)
    }
}

@Composable
private fun UnifiedCalendarItemRow(
    item: UnifiedCalendarItem,
    useFa: Boolean,
    onClick: (() -> Unit)?
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(38.dp)
                .clip(CircleShape)
                .background(Color(item.colorHex))
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.isHoliday) CrimsonDanger else tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.isHoliday) GlassBadge("تعطیل/مناسبت", CrimsonDanger)
                if (item.isOverdue) GlassBadge("معوق", CrimsonDanger)
            }
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!item.isAllDay && item.startMinute != null) {
            val endTxt = item.endMinute?.let { " - ${JalaliCalendarEngine.formatMinuteOfDay(it, useFa)}" } ?: ""
            GlassBadge(
                text = JalaliCalendarEngine.formatMinuteOfDay(item.startMinute, useFa) + endTxt,
                color = Color(item.colorHex)
            )
        } else {
            GlassBadge("تمام روز", Color(item.colorHex))
        }
    }
}

@Composable
private fun JalaliMonthGridCard(
    state: LifePilotDataState,
    selectedDate: JalaliDate,
    today: JalaliDate,
    useFa: Boolean,
    showWeekNumbers: Boolean,
    onSelectDate: (JalaliDate) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val daysInMonth = JalaliCalendarEngine.daysInJalaliMonth(selectedDate.year, selectedDate.month)
    val firstDow = JalaliDate(selectedDate.year, selectedDate.month, 1).dayOfWeekPersian()
    val rows = (firstDow + daysInMonth + 6) / 7

    GlassCard(contentPadding = PaddingValues(12.dp)) {
        // Weekday header
        Row(modifier = Modifier.fillMaxWidth()) {
            if (showWeekNumbers) {
                Text(
                    text = "ه",
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.inkSecondary
                )
            }
            JalaliCalendarEngine.PERSIAN_WEEKDAYS_SHORT.forEachIndexed { idx, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (idx == 6) CrimsonDanger else tokens.inkSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rows) {
                val firstDayInRow = (r * 7 - firstDow + 1).coerceIn(1, daysInMonth)
                val rowWeekNum = JalaliDate(selectedDate.year, selectedDate.month, firstDayInRow).weekOfYear()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showWeekNumbers) {
                        Text(
                            text = PersianFormatters.digits(rowWeekNum.toString(), useFa),
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.inkSecondary.copy(alpha = 0.7f)
                        )
                    }
                    for (c in 0..6) {
                        val dayNum = r * 7 + c - firstDow + 1
                        if (dayNum in 1..daysInMonth) {
                            val cellDate = JalaliDate(selectedDate.year, selectedDate.month, dayNum)
                            val ep = cellDate.toEpochDay()
                            val cellItems = remember(state, ep) { state.unifiedItemsForDay(ep) }
                            val isSelected = cellDate == selectedDate
                            val isToday = cellDate == today
                            val isHoliday = c == 6 || cellItems.any { it.isHoliday }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> tokens.accentPrimary
                                            isToday -> tokens.accentSecondary.copy(alpha = 0.22f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = if (isToday && !isSelected) tokens.accentSecondary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectDate(cellDate) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = PersianFormatters.digits(dayNum.toString(), useFa),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        isHoliday -> CrimsonDanger
                                        else -> tokens.inkPrimary
                                    }
                                )
                                if (cellItems.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        cellItems.take(3).forEach { ci ->
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else Color(ci.colorHex))
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JalaliWeekViewCard(
    state: LifePilotDataState,
    selectedDate: JalaliDate,
    today: JalaliDate,
    useFa: Boolean,
    onSelectDate: (JalaliDate) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val dow = selectedDate.dayOfWeekPersian()
    val startOfWeek = selectedDate.plusDays(-dow.toLong())

    GlassCard(contentPadding = PaddingValues(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0..6) {
                val d = startOfWeek.plusDays(i.toLong())
                val isSelected = d == selectedDate
                val isToday = d == today
                val count = state.unifiedItemsForDay(d.toEpochDay()).size
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> tokens.accentPrimary
                                isToday -> tokens.accentSecondary.copy(alpha = 0.2f)
                                else -> tokens.subtleDivider
                            }
                        )
                        .clickable { onSelectDate(d) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = JalaliCalendarEngine.PERSIAN_WEEKDAYS_SHORT[i],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else if (i == 6) CrimsonDanger else tokens.inkSecondary
                    )
                    Text(
                        text = PersianFormatters.digits(d.day.toString(), useFa),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else tokens.inkPrimary
                    )
                    if (count > 0) {
                        Text(
                            text = PersianFormatters.digits(count.toString(), useFa),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else tokens.accentPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JalaliYearOverviewCard(
    year: Int,
    selectedMonth: Int,
    useFa: Boolean,
    onMonthSelected: (Int) -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard {
        Text(
            text = PersianFormatters.digits(
                "نمای سالانه $year (${if (JalaliCalendarEngine.isJalaliLeapYear(year)) "کبیسه" else "عادی"})",
                useFa
            ),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.inkPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..3) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..2) {
                        val m = row * 3 + col + 1
                        val days = JalaliCalendarEngine.daysInJalaliMonth(year, m)
                        val isCurrent = m == selectedMonth
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onMonthSelected(m) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) tokens.accentPrimary else tokens.subtleDivider
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = JalaliCalendarEngine.PERSIAN_MONTHS[m - 1],
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent) Color.White else tokens.inkPrimary
                                )
                                Text(
                                    text = PersianFormatters.digits("$days روز", useFa),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) Color.White.copy(alpha = 0.85f) else tokens.inkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventEditorDialog(
    initialDateEpochDay: Long,
    existing: CalendarEventEntity? = null,
    onSave: (CalendarEventEntity) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "PERSONAL") }
    var startEpoch by remember { mutableLongStateOf(existing?.startEpochDay ?: initialDateEpochDay) }
    var isAllDay by remember { mutableStateOf(existing?.isAllDay ?: false) }
    var startHour by remember { mutableStateOf(((existing?.startMinuteOfDay ?: 540) / 60).toString()) }
    var endHour by remember { mutableStateOf(((existing?.endMinuteOfDay ?: 600) / 60).toString()) }
    var recurrence by remember { mutableStateOf(existing?.recurrenceRule ?: "NONE") }
    var isCustomHoliday by remember { mutableStateOf(existing?.isCustomHoliday ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val recurrenceOptions = listOf(
        "NONE" to "بدون تکرار",
        "DAILY" to "روزانه",
        "WEEKLY" to "هفتگی",
        "MONTHLY" to "ماهانه",
        "YEARLY" to "سالانه"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن رویداد جدید" else "ویرایش رویداد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان رویداد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مکان (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تاریخ: ${JalaliCalendarEngine.epochDayToJalali(startEpoch).formatFull(tokens.usePersianDigits)}")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("تمام روز")
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { startHour = PersianFormatters.toLatinDigits(it).filter { ch -> ch.isDigit() } },
                            label = { Text("ساعت شروع") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { endHour = PersianFormatters.toLatinDigits(it).filter { ch -> ch.isDigit() } },
                            label = { Text("ساعت پایان") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("تکرار رویداد", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(recurrenceOptions) { (code, label) ->
                        FilterChip(
                            selected = recurrence == code,
                            onClick = { recurrence = code },
                            label = { Text(label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("مناسبت یا تعطیلی شخصی")
                    Switch(checked = isCustomHoliday, onCheckedChange = { isCustomHoliday = it })
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val sh = (startHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                val eh = (endHour.toIntOrNull() ?: (sh + 1)).coerceIn(sh, 23)
                onSave(
                    CalendarEventEntity(
                        id = existing?.id ?: 0,
                        title = title.trim(),
                        description = description.trim(),
                        category = category,
                        startEpochDay = startEpoch,
                        endEpochDay = startEpoch,
                        startMinuteOfDay = sh * 60,
                        endMinuteOfDay = eh * 60,
                        isAllDay = isAllDay,
                        location = location.trim(),
                        recurrenceRule = recurrence,
                        isCustomHoliday = isCustomHoliday,
                        colorHex = if (isCustomHoliday) 0xFFC62828 else 0xFF13294B
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("حذف", color = CrimsonDanger) }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = startEpoch,
            onDateSelected = {
                startEpoch = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
