package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.jalali.PersianFormatters
import com.example.data.local.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel,
    onNavigate: (AppRoute) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val settings = state.settings
    val useFa = settings.usePersianDigits
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameInput by remember(settings.userName) { mutableStateOf(settings.userName) }
    var pinInput by remember(settings.appLockPin) { mutableStateOf(settings.appLockPin) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var backupStatusMsg by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    backupStatusMsg = "فایل پشتیبان محلی با موفقیت ذخیره شد."
                }.onFailure {
                    backupStatusMsg = "خطا در ذخیره فایل پشتیبان."
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        viewModel.restoreBackupJson(json)
                        backupStatusMsg = "درخواست بازیابی انجام شد."
                    }
                }.onFailure {
                    backupStatusMsg = "خطا در خواندن فایل پشتیبان."
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Privacy & Profile
        item {
            GlassCard(elevated = true, accentBorderColor = EmeraldSuccess) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldSuccess)
                    Column {
                        Text("حریم خصوصی کامل محلی (Room Database)", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                        Text("هیچ داده‌ای به سرور یا API خارجی ارسال نمی‌شود. دسترسی اینترنت غیرفعال است.", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("نام نمایشی در داشبورد") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.updateSettings("تغییر نام نمایشی") { it.copy(userName = nameInput.trim()) }
                            }
                        }) { Text("ذخیره") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Appearance & Glassmorphism Controls
        item {
            GlassCard {
                Text("ظاهر و گلاسمورفیسم ۲۰۲۷", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Text("حالت نمایش", style = MaterialTheme.typography.labelLarge, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AppearanceMode.entries) { mode ->
                        FilterChip(
                            selected = settings.appearanceMode == mode,
                            onClick = { viewModel.updateSettings("تغییر تم به ${mode.labelFa}") { it.copy(appearanceMode = mode) } },
                            label = { Text(mode.labelFa) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("پالت رنگ اصلی و مکمل", style = MaterialTheme.typography.labelLarge, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccentPreset.entries.forEach { preset ->
                        val selected = settings.accentPreset == preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) tokens.accentPrimary.copy(alpha = 0.12f) else tokens.subtleDivider)
                                .clickable {
                                    viewModel.updateSettings("تغییر رنگ تاکیدی به ${preset.labelFa}") { it.copy(accentPreset = preset) }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(preset.primaryHex)))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(preset.secondaryHex)))
                                Text(preset.labelFa, style = MaterialTheme.typography.bodyMedium, color = tokens.inkPrimary)
                            }
                            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tokens.accentPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingSliderRow(
                    label = "شدت نور محیطی شیشه‌ای",
                    valueText = PersianFormatters.formatPercent(settings.glassIntensity, useFa),
                    value = settings.glassIntensity,
                    range = 0f..1f,
                    onChange = { v -> viewModel.updateSettings { it.copy(glassIntensity = v) } }
                )
                SettingSliderRow(
                    label = "شفافیت کارت‌ها",
                    valueText = PersianFormatters.formatPercent(settings.cardAlpha, useFa),
                    value = settings.cardAlpha,
                    range = 0.65f..0.96f,
                    onChange = { v -> viewModel.updateSettings { it.copy(cardAlpha = v) } }
                )
                SettingSliderRow(
                    label = "شفافیت حاشیه شیشه‌ای",
                    valueText = PersianFormatters.formatPercent(settings.borderAlpha, useFa),
                    value = settings.borderAlpha,
                    range = 0.15f..0.85f,
                    onChange = { v -> viewModel.updateSettings { it.copy(borderAlpha = v) } }
                )
                SettingSliderRow(
                    label = "گردی گوشه‌ها",
                    valueText = PersianFormatters.digits("${settings.cornerRadiusDp} dp", useFa),
                    value = settings.cornerRadiusDp.toFloat(),
                    range = 14f..32f,
                    onChange = { v -> viewModel.updateSettings { it.copy(cornerRadiusDp = v.toInt()) } }
                )
                SettingSwitchRow(
                    title = "کنتراست بالا (خوانایی حداکثری)",
                    checked = settings.highContrast,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(highContrast = c) } }
                )
                SettingSwitchRow(
                    title = "کاهش انیمیشن‌ها (دسترسی‌پذیری)",
                    checked = settings.reducedMotion,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(reducedMotion = c) } }
                )
            }
        }

        // Five Persian Font Families + Font Size
        item {
            GlassCard {
                Text("تایپوگرافی و فونت‌های فارسی (۵ خانواده)", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersianFontChoice.entries.forEach { font ->
                        val selected = settings.fontChoice == font
                        val previewFamily = fontFamilyForChoice(font)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) tokens.accentPrimary else tokens.subtleDivider,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings("تغییر فونت به ${font.displayNameFa}") { it.copy(fontChoice = font) }
                                },
                            color = if (selected) tokens.accentPrimary.copy(alpha = 0.09f) else Color.Transparent
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(font.displayNameFa, fontFamily = previewFamily, fontWeight = FontWeight.Bold, color = tokens.inkPrimary)
                                    if (selected) GlassBadge("فعال", tokens.accentPrimary)
                                }
                                Text(font.subtitleFa, fontFamily = previewFamily, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                                Text(font.previewSample, fontFamily = previewFamily, style = MaterialTheme.typography.bodyMedium, color = tokens.accentPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("اندازه فونت", style = MaterialTheme.typography.labelLarge, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScaleChoice.entries.forEach { sc ->
                        FilterChip(
                            selected = settings.fontScale == sc,
                            onClick = { viewModel.updateSettings { it.copy(fontScale = sc) } },
                            label = { Text(sc.labelFa) }
                        )
                    }
                }
            }
        }

        // Localization, Calendar & Finance
        item {
            GlassCard {
                Text("بومی‌سازی، تقویم و مالی", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                SettingSwitchRow(
                    title = "نمایش ارقام فارسی (۰-۹)",
                    checked = settings.usePersianDigits,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(usePersianDigits = c) } }
                )
                SettingSwitchRow(
                    title = "نمایش تعطیلات رسمی ایران در تقویم",
                    checked = settings.showOfficialHolidays,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(showOfficialHolidays = c) } }
                )
                SettingSwitchRow(
                    title = "نمایش شماره هفته در تقویم",
                    checked = settings.showWeekNumbers,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(showWeekNumbers = c) } }
                )
                SettingSwitchRow(
                    title = "اعلان‌های یادآور محلی",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { c -> viewModel.updateSettings { it.copy(notificationsEnabled = c) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("ارز پیش‌فرض", style = MaterialTheme.typography.labelLarge, color = tokens.inkSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.defaultCurrency == "IRT",
                        onClick = { viewModel.updateSettings { it.copy(defaultCurrency = "IRT") } },
                        label = { Text("تومان (IRT)") }
                    )
                    FilterChip(
                        selected = settings.defaultCurrency == "USD",
                        onClick = { viewModel.updateSettings { it.copy(defaultCurrency = "USD") } },
                        label = { Text("دلار (USD)") }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = { onNavigate(AppRoute.FINANCE) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مدیریت نرخ دستی دلار و تاریخچه")
                }
            }
        }

        // Dashboard Widgets Visibility & Reordering
        item {
            GlassCard {
                Text("شخصی‌سازی ویجت‌های داشبورد", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Text("نمایش، مخفی‌سازی و تغییر ترتیب کارت‌های صفحه اصلی", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                settings.widgetOrder.forEachIndexed { index, widget ->
                    val visible = widget !in settings.hiddenWidgets
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = visible,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { s ->
                                        val nextHidden = if (checked) s.hiddenWidgets - widget else s.hiddenWidgets + widget
                                        s.copy(hiddenWidgets = nextHidden)
                                    }
                                }
                            )
                            Text(widget.titleFa, style = MaterialTheme.typography.bodyMedium, color = tokens.inkPrimary)
                        }
                        Row {
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    viewModel.updateSettings { s ->
                                        val mutable = s.widgetOrder.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index - 1, item)
                                        s.copy(widgetOrder = mutable)
                                    }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "بالا") }
                            IconButton(
                                enabled = index < settings.widgetOrder.lastIndex,
                                onClick = {
                                    viewModel.updateSettings { s ->
                                        val mutable = s.widgetOrder.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index + 1, item)
                                        s.copy(widgetOrder = mutable)
                                    }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "پایین") }
                        }
                    }
                }
            }
        }

        // Security (Optional Local PIN Lock)
        item {
            GlassCard {
                Text("قفل امنیتی محلی (اختیاری)", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Text("در صورت تنظیم رمز ۴ رقمی، هنگام باز کردن برنامه درخواست می‌شود. برای غیرفعال‌سازی، خالی ذخیره کنید.", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = PersianFormatters.toLatinDigits(it).filter { c -> c.isDigit() }.take(6) },
                    label = { Text("رمز عبور عددی (خالی = بدون قفل)") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = {
                            viewModel.updateSettings("بروزرسانی قفل امنیتی برنامه") { it.copy(appLockPin = pinInput) }
                        }) { Text("ثبت") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Local Backup, Restore & Reset
        item {
            GlassCard {
                Text("پشتیبان‌گیری و مدیریت داده‌ها", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                backupStatusMsg?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { exportLauncher.launch("lifepilot_backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروجی پشتیبان")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازیابی")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پاکسازی کامل داده‌های محلی")
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDeleteDialog(
            title = "پاکسازی کامل داده‌ها",
            message = "تمام تراکنش‌ها، اقساط، وظایف، یادداشت‌ها و رویدادهای ذخیره شده روی دستگاه حذف می‌شوند. این عمل غیرقابل بازگشت است.",
            onConfirm = { viewModel.clearAllLocalData() },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val tokens = LocalGlassTokens.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tokens.inkPrimary)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = tokens.accentPrimary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = tokens.inkPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
