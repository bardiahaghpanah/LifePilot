package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.JalaliDate
import com.example.core.jalali.PersianFormatters

/**
 * Ambient 2027 frosted glass background with soft architectural light pools.
 */
@Composable
fun GlassCanvasBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.canvasTop, tokens.canvasBottom)
                )
            )
            .drawBehind {
                // Soft top-right navy/sapphire ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.ambientBlobPrimary, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.10f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.85f, size.height * 0.10f)
                )
                // Warm champagne gold ambient glow bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.ambientBlobAccent, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.78f),
                        radius = size.width * 0.68f
                    ),
                    radius = size.width * 0.68f,
                    center = Offset(size.width * 0.12f, size.height * 0.78f)
                )
            },
        content = content
    )
}

/**
 * Signature frosted GlassCard with large rounded corners, soft shadow, translucent highlight border,
 * and guaranteed high-contrast legibility.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    accentBorderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(tokens.cornerRadius)
    val bgColor = if (elevated) tokens.cardSurfaceElevated else tokens.cardSurface
    val borderBrush = if (accentBorderColor != null) {
        Brush.linearGradient(
            listOf(accentBorderColor.copy(alpha = 0.55f), accentBorderColor.copy(alpha = 0.20f))
        )
    } else if (!tokens.isDark) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.95f), tokens.cardBorder)
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
        )
    }

    val baseModifier = modifier
        .shadow(
            elevation = if (tokens.isDark) 8.dp else 10.dp,
            shape = shape,
            ambientColor = DeepNavy.copy(alpha = if (tokens.isDark) 0.40f else 0.09f),
            spotColor = DeepNavy.copy(alpha = if (tokens.isDark) 0.45f else 0.11f)
        )
        .clip(shape)
        .background(bgColor)
        .border(BorderStroke(1.dp, borderBrush), shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Column(
        modifier = baseModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkSecondary
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.accentPrimary
                )
            }
        }
    }
}

@Composable
fun GlassMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color = LocalGlassTokens.current.accentPrimary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(tokens.accentPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.accentPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.inkPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.inkSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
            ) {
                Text("حذف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/**
 * Interactive Persian Jalali Date Picker Dialog with accurate leap year & Esfand support.
 */
@Composable
fun JalaliDatePickerDialog(
    initialEpochDay: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val initialDate = remember(initialEpochDay) { JalaliCalendarEngine.epochDayToJalali(initialEpochDay) }
    var viewYear by remember { mutableIntStateOf(initialDate.year) }
    var viewMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val today = remember { JalaliCalendarEngine.todayJalali() }

    val daysInMonth = JalaliCalendarEngine.daysInJalaliMonth(viewYear, viewMonth)
    val firstDayOfWeek = JalaliDate(viewYear, viewMonth, 1).dayOfWeekPersian()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "انتخاب تاریخ جلالی",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.inkSecondary
                )
                Text(
                    text = selectedDate.formatFull(tokens.usePersianDigits),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.accentPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (viewMonth == 1) {
                            viewMonth = 12; viewYear -= 1
                        } else viewMonth -= 1
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                    }
                    Text(
                        text = PersianFormatters.digits(
                            "${JalaliCalendarEngine.PERSIAN_MONTHS[viewMonth - 1]} $viewYear",
                            tokens.usePersianDigits
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        if (viewMonth == 12) {
                            viewMonth = 1; viewYear += 1
                        } else viewMonth += 1
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                    }
                }

                // Weekday header (Sat..Fri)
                Row(modifier = Modifier.fillMaxWidth()) {
                    JalaliCalendarEngine.PERSIAN_WEEKDAYS_SHORT.forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == 6) CrimsonDanger else tokens.inkSecondary
                        )
                    }
                }

                val totalCells = firstDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0..6) {
                                val dayNum = r * 7 + c - firstDayOfWeek + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = JalaliDate(viewYear, viewMonth, dayNum)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == today
                                    val isHoliday = c == 6 || JalaliCalendarEngine.holidaysForDate(cellDate).any { it.isOfficialHoliday }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> tokens.accentPrimary
                                                    isToday -> tokens.accentSecondary.copy(alpha = 0.22f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedDate = cellDate },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = PersianFormatters.digits(dayNum.toString(), tokens.usePersianDigits),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                isHoliday -> CrimsonDanger
                                                else -> tokens.inkPrimary
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        selectedDate = today
                        viewYear = today.year
                        viewMonth = today.month
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("رفتن به امروز")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate.toEpochDay()) }) {
                Text("تایید تاریخ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
