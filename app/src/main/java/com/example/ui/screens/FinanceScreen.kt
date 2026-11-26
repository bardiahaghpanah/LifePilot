package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.FinancialTransactionEntity
import com.example.data.repository.LifePilotDomainLogic
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

private enum class FinanceTab(val labelFa: String) {
    OVERVIEW("خلاصه مالی"),
    TRANSACTIONS("تراکنش‌ها"),
    EXCHANGE_RATE("نرخ دستی دلار")
}

@Composable
fun FinanceScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var selectedTab by remember { mutableStateOf(FinanceTab.OVERVIEW) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var defaultTxType by remember { mutableStateOf("EXPENSE") }
    var deletingTx by remember { mutableStateOf<FinancialTransactionEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().testTag("finance_screen")) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FinanceTab.entries) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab.labelFa) }
                )
            }
            item {
                AssistChip(
                    onClick = { onNavigate(AppRoute.INSTALLMENTS) },
                    label = { Text("اقساط و بدهی") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        when (selectedTab) {
            FinanceTab.OVERVIEW -> FinanceOverviewTab(
                state = state,
                useFa = useFa,
                onAddIncome = { defaultTxType = "INCOME"; showAddTxDialog = true },
                onAddExpense = { defaultTxType = "EXPENSE"; showAddTxDialog = true },
                onOpenRateTab = { selectedTab = FinanceTab.EXCHANGE_RATE }
            )
            FinanceTab.TRANSACTIONS -> FinanceTransactionsTab(
                state = state,
                useFa = useFa,
                onAddTx = { defaultTxType = "EXPENSE"; showAddTxDialog = true },
                onRequestDelete = { deletingTx = it }
            )
            FinanceTab.EXCHANGE_RATE -> ManualExchangeRateTab(
                state = state,
                useFa = useFa,
                viewModel = viewModel
            )
        }
    }

    if (showAddTxDialog) {
        TransactionEditorDialog(
            defaultCurrency = state.settings.defaultCurrency,
            defaultType = defaultTxType,
            currentRate = state.currentUsdRate?.rateIrtPerUsd,
            onSave = { title, desc, amt, cur, type, cat, epoch, status, rateOverride, notes ->
                viewModel.addTransaction(title, desc, amt, cur, type, cat, epoch, status, rateOverride, notes)
                showAddTxDialog = false
            },
            onDismiss = { showAddTxDialog = false }
        )
    }

    deletingTx?.let { tx ->
        ConfirmDeleteDialog(
            title = "حذف تراکنش",
            message = "آیا از حذف تراکنش «${tx.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteTransaction(tx) },
            onDismiss = { deletingTx = null }
        )
    }
}

@Composable
private fun FinanceOverviewTab(
    state: LifePilotDataState,
    useFa: Boolean,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenRateTab: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val remainingIrtDebt = remember(state.installments, state.installmentPayments) {
        val irtIds = state.installments.filter { it.currency == "IRT" }.map { it.id }.toSet()
        state.installmentPayments.filter { it.installmentId in irtIds && it.status != "PAID" }
            .sumOf { (it.expectedAmount - it.paidAmount).coerceAtLeast(0.0) }
    }
    val remainingUsdDebt = remember(state.installments, state.installmentPayments) {
        val usdIds = state.installments.filter { it.currency == "USD" }.map { it.id }.toSet()
        state.installmentPayments.filter { it.installmentId in usdIds && it.status != "PAID" }
            .sumOf { (it.expectedAmount - it.paidAmount).coerceAtLeast(0.0) }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true, accentBorderColor = tokens.accentSecondary) {
                Text("موجودی‌های تفکیک‌شده (بدون اختلاط ارز)", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    GlassMetricCard(
                        title = "تراز تومان (IRT)",
                        value = PersianFormatters.formatIrt(state.irtBalance, useFa, showUnit = false),
                        subtitle = "درآمد: ${PersianFormatters.formatIrt(state.irtIncome, useFa, false)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    GlassMetricCard(
                        title = "تراز دلار (USD)",
                        value = PersianFormatters.formatUsd(state.usdBalance, useFa, showUnit = false),
                        subtitle = "درآمد: ${PersianFormatters.formatUsd(state.usdIncome, useFa, false)}",
                        icon = Icons.Default.AttachMoney,
                        tint = tokens.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val rate = state.currentUsdRate
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.accentSecondary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ارزش تجمیعی تخمینی (تومان)", style = MaterialTheme.typography.labelLarge, color = tokens.inkPrimary)
                            TextButton(onClick = onOpenRateTab, contentPadding = PaddingValues(0.dp)) {
                                Text("تغییر نرخ دستی")
                            }
                        }
                        Text(
                            text = state.estimatedCombinedIrt?.let { PersianFormatters.formatIrt(it, useFa) }
                                ?: "برای برآورد تجمیعی، ابتدا نرخ دستی دلار را ثبت کنید.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPrimary
                        )
                        if (rate != null) {
                            Text(
                                text = "بر اساس نرخ دستی ۱ دلار = ${PersianFormatters.formatIrt(rate.rateIrtPerUsd, useFa)} (تخمینی — مجزا از موجودی اصلی)",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAddIncome,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت درآمد")
                    }
                    Button(
                        onClick = onAddExpense,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت هزینه")
                    }
                }
            }
        }

        // Remaining Installments Debt Card
        item {
            GlassCard {
                Text("بدهی اقساط باقی‌مانده", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تومانی:", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
                    Text(PersianFormatters.formatIrt(remainingIrtDebt, useFa), style = MaterialTheme.typography.titleSmall, color = CrimsonDanger)
                }
                if (remainingUsdDebt > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("دلاری:", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
                        Text(PersianFormatters.formatUsd(remainingUsdDebt, useFa), style = MaterialTheme.typography.titleSmall, color = CrimsonDanger)
                    }
                }
            }
        }

        // Category Spending Breakdown (IRT Expenses)
        item {
            val irtByCat = remember(state.transactions) {
                state.transactions.filter { it.type == "EXPENSE" && it.currency == "IRT" }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                    .take(6)
            }
            GlassCard {
                Text("دسته‌بندی هزینه‌های تومانی", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                if (irtByCat.isEmpty()) {
                    Text("هنوز هزینه تومانی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
                } else {
                    val maxVal = irtByCat.maxOf { it.value }.coerceAtLeast(1.0)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        irtByCat.forEach { (cat, sum) ->
                            val frac = (sum / maxVal).toFloat().coerceIn(0.05f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat, style = MaterialTheme.typography.bodyMedium, color = tokens.inkPrimary)
                                    Text(PersianFormatters.formatIrt(sum, useFa), style = MaterialTheme.typography.labelLarge, color = tokens.accentPrimary)
                                }
                                Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                    drawRoundRect(
                                        color = tokens.subtleDivider,
                                        cornerRadius = CornerRadius(8f, 8f),
                                        size = size
                                    )
                                    drawRoundRect(
                                        color = tokens.accentPrimary,
                                        cornerRadius = CornerRadius(8f, 8f),
                                        topLeft = Offset(size.width * (1f - frac), 0f),
                                        size = Size(size.width * frac, size.height)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceTransactionsTab(
    state: LifePilotDataState,
    useFa: Boolean,
    onAddTx: () -> Unit,
    onRequestDelete: (FinancialTransactionEntity) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var currencyFilter by remember { mutableStateOf("ALL") }
    var typeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(state.transactions, currencyFilter, typeFilter) {
        state.transactions.filter { tx ->
            (currencyFilter == "ALL" || tx.currency == currencyFilter) &&
                (typeFilter == "ALL" || tx.type == typeFilter)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to "همه", "IRT" to "تومان", "USD" to "دلار").forEach { (c, lbl) ->
                        FilterChip(selected = currencyFilter == c, onClick = { currencyFilter = c }, label = { Text(lbl) })
                    }
                }
                FilledTonalButton(onClick = onAddTx) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تراکنش")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "همه انواع", "INCOME" to "درآمد", "EXPENSE" to "هزینه").forEach { (t, lbl) ->
                    FilterChip(selected = typeFilter == t, onClick = { typeFilter = t }, label = { Text(lbl) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "تراکنشی یافت نشد",
                    description = "درآمدها و هزینه‌های تومانی و دلاری خود را با دقت تاریخی ثبت کنید.",
                    actionLabel = "ثبت اولین تراکنش",
                    onAction = onAddTx
                )
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                GlassCard(contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tx.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                                GlassBadge(tx.category, tokens.accentPrimary)
                            }
                            Text(
                                text = JalaliCalendarEngine.epochDayToJalali(tx.dateEpochDay).formatFull(useFa),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                            if (tx.currency == "USD" && tx.historicalRateUsed != null && tx.convertedAmountIrt != null) {
                                Text(
                                    text = "نرخ ثبت‌شده در آن تاریخ: ${PersianFormatters.formatIrt(tx.historicalRateUsed, useFa)} • معادل تاریخی: ${PersianFormatters.formatIrt(tx.convertedAmountIrt, useFa)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.accentSecondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (tx.type == "INCOME") "+ " else "− ") + PersianFormatters.formatCurrency(tx.amount, tx.currency, useFa),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "INCOME") EmeraldSuccess else CrimsonDanger
                            )
                            IconButton(onClick = { onRequestDelete(tx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف تراکنش", tint = tokens.inkSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualExchangeRateTab(
    state: LifePilotDataState,
    useFa: Boolean,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    var newRateInput by remember { mutableStateOf("") }
    var rateNotes by remember { mutableStateOf("") }
    var calcAmount by remember { mutableStateOf("") }
    var usdToIrtMode by remember { mutableStateOf(true) }
    var selectedRateId by remember(state.exchangeRates) { mutableStateOf(state.currentUsdRate?.id) }
    var customCalcRate by remember { mutableStateOf("") }

    val activeCalcRate = customCalcRate.toDoubleOrNull()?.takeIf { it > 0 }
        ?: state.exchangeRates.find { it.id == selectedRateId }?.rateIrtPerUsd
        ?: state.currentUsdRate?.rateIrtPerUsd
        ?: 0.0

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true, accentBorderColor = tokens.accentSecondary) {
                Text("مدیریت کاملاً دستی نرخ دلار (USD ⇄ IRT)", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "نرخ ارز هرگز به‌صورت خودکار یا از API خارجی دریافت نمی‌شود. تراکنش‌های قدیمی همواره نرخ تاریخی خود را حفظ می‌کنند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                state.currentUsdRate?.let { curr ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tokens.accentPrimary.copy(alpha = 0.09f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("نرخ فعلی ثبت‌شده:", style = MaterialTheme.typography.labelMedium, color = tokens.inkSecondary)
                            Text(
                                text = "۱ دلار = ${PersianFormatters.formatIrt(curr.rateIrtPerUsd, useFa)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.accentPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = newRateInput,
                    onValueChange = { newRateInput = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("نرخ جدید (تومان به ازای ۱ دلار، مثلاً 270000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateNotes,
                    onValueChange = { rateNotes = it },
                    label = { Text("یادداشت اختیاری (منبع یا توضیح)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val parsed = newRateInput.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            viewModel.saveManualExchangeRate(parsed, rateNotes)
                            newRateInput = ""
                            rateNotes = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_usd_rate_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ذخیره نرخ جدید در تاریخچه")
                }
            }
        }

        // Manual Currency Converter
        item {
            GlassCard {
                Text("مبدل دقیق دلار و تومان", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = usdToIrtMode, onClick = { usdToIrtMode = true }, label = { Text("دلار به تومان") })
                    FilterChip(selected = !usdToIrtMode, onClick = { usdToIrtMode = false }, label = { Text("تومان به دلار") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = calcAmount,
                    onValueChange = { calcAmount = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (usdToIrtMode) "مبلغ به دلار (USD)" else "مبلغ به تومان (IRT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customCalcRate,
                    onValueChange = { customCalcRate = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("نرخ دلخواه (خالی = نرخ انتخابی فعلی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val inputVal = calcAmount.toDoubleOrNull() ?: 0.0
                if (inputVal > 0 && activeCalcRate > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val resultText = if (usdToIrtMode) {
                        PersianFormatters.formatIrt(LifePilotDomainLogic.convertUsdToIrt(inputVal, activeCalcRate), useFa)
                    } else {
                        PersianFormatters.formatUsd(LifePilotDomainLogic.convertIrtToUsd(inputVal, activeCalcRate), useFa)
                    }
                    Text("نتیجه تبدیل: $resultText", style = MaterialTheme.typography.titleLarge, color = EmeraldSuccess)
                }
            }
        }

        // Rate History
        item {
            GlassSectionHeader(title = "تاریخچه نرخ‌های ثبت‌شده", subtitle = "برای انتخاب در مبدل روی هر ردیف بزنید")
        }
        if (state.exchangeRates.isEmpty()) {
            item {
                Text("هنوز نرخی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
            }
        } else {
            items(state.exchangeRates, key = { it.id }) { r ->
                val isSelected = r.id == selectedRateId
                GlassCard(
                    accentBorderColor = if (isSelected) tokens.accentSecondary else null,
                    contentPadding = PaddingValues(12.dp),
                    onClick = {
                        selectedRateId = r.id
                        customCalcRate = ""
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("۱ دلار = ${PersianFormatters.formatIrt(r.rateIrtPerUsd, useFa)}", style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                            if (r.notes.isNotBlank()) {
                                Text(r.notes, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                            }
                        }
                        if (isSelected) GlassBadge("فعال در مبدل", tokens.accentSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionEditorDialog(
    defaultCurrency: String,
    defaultType: String = "EXPENSE",
    currentRate: Double?,
    onSave: (String, String, Double, String, String, String, Long, String, Double?, String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var type by remember { mutableStateOf(defaultType) }
    var category by remember { mutableStateOf(if (defaultType == "INCOME") "حقوق و درآمد" else "خوراک و خرید") }
    var dateEpoch by remember { mutableLongStateOf(JalaliCalendarEngine.todayEpochDay()) }
    var rateOverrideStr by remember { mutableStateOf(currentRate?.toLong()?.toString() ?: "") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (type == "INCOME") {
        listOf("حقوق و درآمد", "پروژه و فریلنس", "سرمایه‌گذاری", "هدیه", "سایر")
    } else {
        listOf("خوراک و خرید", "حمل‌ونقل", "مسکن و قبوض", "اقساط و بدهی", "دانشگاه و آموزش", "سلامت", "تفریح", "سایر")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "INCOME") "ثبت درآمد جدید" else "ثبت هزینه جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("هزینه") })
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("درآمد") })
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(selected = currency == "IRT", onClick = { currency = "IRT" }, label = { Text("تومان") })
                    FilterChip(selected = currency == "USD", onClick = { currency = "USD" }, label = { Text("دلار") })
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (currency == "USD") "مبلغ (دلار)" else "مبلغ (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (currency == "USD") {
                    OutlinedTextField(
                        value = rateOverrideStr,
                        onValueChange = { rateOverrideStr = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("نرخ تاریخی دلار برای این تراکنش (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("تاریخ: ${JalaliCalendarEngine.epochDayToJalali(dateEpoch).formatFull(tokens.usePersianDigits)}")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت اختیاری") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                onSave(title, "", amt, currency, type, category, dateEpoch, "PAID", rateOverrideStr.toDoubleOrNull(), notes)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = dateEpoch,
            onDateSelected = { dateEpoch = it; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}
