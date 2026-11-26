package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.InstallmentEntity
import com.example.data.local.InstallmentPaymentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun InstallmentsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val today = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }

    var filterTab by remember { mutableStateOf("ALL") } // ALL, OVERDUE, UPCOMING, PAID
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var payingItem by remember { mutableStateOf<Pair<InstallmentPaymentEntity, InstallmentEntity>?>(null) }
    var deletingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }

    val paymentsByInst = remember(state.installmentPayments) { state.installmentPayments.groupBy { it.installmentId } }
    val overduePayments = remember(state.installmentPayments, today) {
        state.installmentPayments.filter { it.status != "PAID" && it.dueEpochDay < today }
    }
    val remainingIrt = remember(state.installments, state.installmentPayments) {
        val irtIds = state.installments.filter { it.currency == "IRT" }.map { it.id }.toSet()
        state.installmentPayments.filter { it.installmentId in irtIds && it.status != "PAID" }
            .sumOf { (it.expectedAmount - it.paidAmount).coerceAtLeast(0.0) }
    }
    val remainingUsd = remember(state.installments, state.installmentPayments) {
        val usdIds = state.installments.filter { it.currency == "USD" }.map { it.id }.toSet()
        state.installmentPayments.filter { it.installmentId in usdIds && it.status != "PAID" }
            .sumOf { (it.expectedAmount - it.paidAmount).coerceAtLeast(0.0) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("installments_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true, accentBorderColor = tokens.accentSecondary) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("مدیریت اقساط و بدهی‌ها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text(
                            text = "هیچ قسطی بدون ثبت دستی، پرداخت‌شده تلقی نمی‌شود",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.inkSecondary
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_installment_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قسط جدید")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlassMetricCard(
                        title = "بدهی تومانی",
                        value = PersianFormatters.formatIrt(remainingIrt, useFa, false),
                        subtitle = if (remainingUsd > 0) "دلاری: ${PersianFormatters.formatUsd(remainingUsd, useFa, false)}" else "تومان باقی‌مانده",
                        icon = Icons.Default.AccountBalance,
                        tint = tokens.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    GlassMetricCard(
                        title = "اقساط معوق",
                        value = PersianFormatters.digits(overduePayments.size.toString(), useFa),
                        subtitle = "سررسید گذشته",
                        icon = Icons.Default.WarningAmber,
                        tint = if (overduePayments.isNotEmpty()) CrimsonDanger else EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ALL" to "همه طرح‌ها", "OVERDUE" to "معوق", "UPCOMING" to "پیش‌رو", "PAID" to "تسویه‌شده")) { (code, label) ->
                        FilterChip(selected = filterTab == code, onClick = { filterTab = code }, label = { Text(label) })
                    }
                }
            }
        }

        val filteredInstallments = state.installments.filter { inst ->
            val list = paymentsByInst[inst.id].orEmpty()
            when (filterTab) {
                "OVERDUE" -> list.any { it.status != "PAID" && it.dueEpochDay < today }
                "UPCOMING" -> list.any { it.status != "PAID" && it.dueEpochDay >= today }
                "PAID" -> list.isNotEmpty() && list.all { it.status == "PAID" }
                else -> true
            }
        }

        if (filteredInstallments.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.Payments,
                    title = "قسطی ثبت نشده است",
                    description = "با ثبت هر وام یا خرید قسطی، تمام سررسیدها به صورت خودکار در تقویم جلالی ایجاد می‌شوند.",
                    actionLabel = "ثبت اولین قسط",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            items(filteredInstallments, key = { it.id }) { inst ->
                val schedule = paymentsByInst[inst.id].orEmpty()
                val paidCount = schedule.count { it.status == "PAID" }
                val totalExpected = schedule.sumOf { it.expectedAmount }.coerceAtLeast(1.0)
                val totalPaid = schedule.sumOf { it.paidAmount }
                val remaining = (totalExpected - totalPaid).coerceAtLeast(0.0)
                val progress = (totalPaid / totalExpected).toFloat().coerceIn(0f, 1f)
                val isExpanded = expandedId == inst.id

                GlassCard(
                    onClick = { expandedId = if (isExpanded) null else inst.id },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(inst.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                            Text(
                                text = "طلبکار/مؤسسه: ${inst.creditor.ifBlank { "ثبت‌نشده" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                            Text(
                                text = "باقی‌مانده: ${PersianFormatters.formatCurrency(remaining, inst.currency, useFa)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (remaining <= 0.01) EmeraldSuccess else CrimsonDanger
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassBadge(
                                text = PersianFormatters.digits("$paidCount/${inst.totalCount}", useFa),
                                color = if (paidCount == inst.totalCount) EmeraldSuccess else tokens.accentPrimary
                            )
                            IconButton(onClick = { deletingInstallment = inst }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف قسط", tint = tokens.inkSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (progress >= 0.99f) EmeraldSuccess else tokens.accentPrimary,
                        trackColor = tokens.subtleDivider
                    )

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = tokens.subtleDivider)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("جدول سررسیدها (جلالی)", style = MaterialTheme.typography.labelLarge, color = tokens.inkPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            schedule.forEach { payment ->
                                val isLate = payment.status != "PAID" && payment.dueEpochDay < today
                                val statusColor = when {
                                    payment.status == "PAID" -> EmeraldSuccess
                                    payment.status == "PARTIAL" -> AmberWarning
                                    isLate -> CrimsonDanger
                                    else -> tokens.accentPrimary
                                }
                                val statusLabel = when {
                                    payment.status == "PAID" -> "پرداخت‌شده"
                                    payment.status == "PARTIAL" -> "پرداخت ناقص"
                                    isLate -> "معوق"
                                    else -> "در انتظار"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(statusColor.copy(alpha = 0.08f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "قسط ${PersianFormatters.digits(payment.installmentIndex.toString(), useFa)} — ${JalaliCalendarEngine.epochDayToJalali(payment.dueEpochDay).formatNumeric(useFa)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = tokens.inkPrimary
                                        )
                                        Text(
                                            text = "پرداختی: ${PersianFormatters.formatCurrency(payment.paidAmount, inst.currency, useFa)} از ${PersianFormatters.formatCurrency(payment.expectedAmount, inst.currency, useFa)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tokens.inkSecondary
                                        )
                                    }
                                    if (payment.status != "PAID") {
                                        FilledTonalButton(
                                            onClick = { payingItem = payment to inst },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("ثبت پرداخت")
                                        }
                                    } else {
                                        GlassBadge(statusLabel, EmeraldSuccess)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        InstallmentCreatorDialog(
            defaultCurrency = state.settings.defaultCurrency,
            onSave = { title, creditor, origAmt, cur, count, payAmt, freq, firstDue, notes ->
                viewModel.createInstallmentPlan(title, creditor, origAmt, cur, count, payAmt, freq, firstDue, notes)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    payingItem?.let { (payment, inst) ->
        RecordInstallmentPaymentDialog(
            payment = payment,
            installment = inst,
            useFa = useFa,
            onConfirm = { amt, notes, alsoRecordTx ->
                viewModel.recordInstallmentPayment(payment, inst.title, inst.currency, amt, notes, alsoRecordTx)
                payingItem = null
            },
            onDismiss = { payingItem = null }
        )
    }

    deletingInstallment?.let { inst ->
        ConfirmDeleteDialog(
            title = "حذف طرح قسط",
            message = "آیا از حذف «${inst.title}» و تمام جدول سررسیدهای آن مطمئن هستید؟",
            onConfirm = { viewModel.deleteInstallment(inst) },
            onDismiss = { deletingInstallment = null }
        )
    }
}

@Composable
fun InstallmentCreatorDialog(
    defaultCurrency: String,
    onSave: (String, String, Double, String, Int, Double, String, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    var title by remember { mutableStateOf("") }
    var creditor by remember { mutableStateOf("") }
    var totalCountStr by remember { mutableStateOf("12") }
    var paymentAmountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var firstDueEpoch by remember { mutableLongStateOf(JalaliCalendarEngine.todayEpochDay()) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد طرح قسط جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان (مثلاً وام مسکن، خرید لپ‌تاپ)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = creditor, onValueChange = { creditor = it }, label = { Text("بانک / شرکت / شخص") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = currency == "IRT", onClick = { currency = "IRT" }, label = { Text("تومان") })
                    FilterChip(selected = currency == "USD", onClick = { currency = "USD" }, label = { Text("دلار") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalCountStr,
                        onValueChange = { totalCountStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() } },
                        label = { Text("تعداد اقساط") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("مبلغ هر قسط") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MONTHLY" to "ماهانه", "WEEKLY" to "هفتگی", "BIWEEKLY" to "دو هفته", "QUARTERLY" to "فصلی").forEach { (f, lbl) ->
                        item { FilterChip(selected = frequency == f, onClick = { frequency = f }, label = { Text(lbl) }) }
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("اولین سررسید: ${JalaliCalendarEngine.epochDayToJalali(firstDueEpoch).formatNumeric(tokens.usePersianDigits)}")
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("یادداشت") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val count = (totalCountStr.toIntOrNull() ?: 1).coerceIn(1, 120)
                val payAmt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                onSave(title, creditor, payAmt * count, currency, count, payAmt, frequency, firstDueEpoch, notes)
            }) { Text("ایجاد جدول اقساط") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = firstDueEpoch,
            onDateSelected = { firstDueEpoch = it; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun RecordInstallmentPaymentDialog(
    payment: InstallmentPaymentEntity,
    installment: InstallmentEntity,
    useFa: Boolean,
    onConfirm: (Double, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val remaining = (payment.expectedAmount - payment.paidAmount).coerceAtLeast(0.0)
    var amountStr by remember { mutableStateOf(remaining.toLong().toString()) }
    var notes by remember { mutableStateOf("") }
    var recordExpense by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداخت قسط ${PersianFormatters.digits(payment.installmentIndex.toString(), useFa)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "مبلغ باقی‌مانده این قسط: ${PersianFormatters.formatCurrency(remaining, installment.currency, useFa)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("مبلغ پرداختی (کامل یا جزئی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("شماره پیگیری یا یادداشت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("ثبت خودکار به‌عنوان هزینه مالی", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = recordExpense, onCheckedChange = { recordExpense = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) onConfirm(amt, notes, recordExpense)
            }) { Text("ثبت پرداخت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
