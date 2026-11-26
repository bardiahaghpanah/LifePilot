package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.repository.LifePilotDomainLogic
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun SmartPlannerScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    val todayEpoch = remember(state.settings.timeZoneId) { JalaliCalendarEngine.todayEpochDay(state.settings.timeZoneId) }
    var targetEpoch by remember { mutableLongStateOf(todayEpoch) }
    val targetJalali = remember(targetEpoch) { JalaliCalendarEngine.epochDayToJalali(targetEpoch) }

    val fixedBlocks = remember(state, targetEpoch) { state.fixedBlocksForDay(targetEpoch) }
    val conflicts = remember(fixedBlocks) { LifePilotDomainLogic.detectConflicts(fixedBlocks) }
    val eligibleTasks = remember(state.tasks, state.rejectedProposalTaskIds) {
        state.tasks.filter { it.id !in state.rejectedProposalTaskIds }
    }
    val proposals = remember(fixedBlocks, eligibleTasks, targetEpoch) {
        LifePilotDomainLogic.buildSmartTaskProposals(fixedBlocks, eligibleTasks, targetEpoch)
    }

    val totalDayTasks = state.tasks.filter { it.deadlineEpochDay == targetEpoch && it.status != "CANCELLED" }
    val completedDayTasks = totalDayTasks.count { it.status == "COMPLETED" }
    val dayCompletionRatio = if (totalDayTasks.isEmpty()) 0f else completedDayTasks.toFloat() / totalDayTasks.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("planner_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true, accentBorderColor = tokens.accentSecondary) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { targetEpoch -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "روز قبل")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("برنامه‌ریز هوشمند روزانه", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text(targetJalali.formatFull(useFa), style = MaterialTheme.typography.titleSmall, color = tokens.accentPrimary)
                    }
                    IconButton(onClick = { targetEpoch += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "روز بعد")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "برنامه‌ریز با تحلیل جلسات، کلاس‌ها و وظایف واقعی شما، زمان‌های آزاد را پیدا می‌کند و هرگز جلسات یا سررسیدها را خودکار جابه‌جا نمی‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkSecondary
                )
            }
        }

        // Conflicts Warning
        if (conflicts.isNotEmpty()) {
            item {
                GlassCard(accentBorderColor = CrimsonDanger) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonDanger)
                        Column {
                            Text("تداخل زمانی در برنامه ثابت", style = MaterialTheme.typography.titleSmall, color = CrimsonDanger)
                            conflicts.forEach { c ->
                                Text("«${c.first.title}» و «${c.second.title}» تداخل دارند.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Fixed Commitments Timeline
        item {
            GlassSectionHeader(
                title = "تعهدات ثابت (قفل‌شده)",
                subtitle = "جلسات، کلاس‌ها و رویدادهای ثبت‌شده در این روز"
            )
        }
        if (fixedBlocks.isEmpty()) {
            item {
                GlassCard {
                    Text("هیچ تعهد زمان‌دار ثابتی در این روز وجود ندارد — کل روز برای زمان‌بندی آزاد است.", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
                }
            }
        } else {
            items(fixedBlocks) { b ->
                GlassCard(contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(b.colorHex)))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.title, style = MaterialTheme.typography.titleSmall, color = tokens.inkPrimary)
                            Text(
                                text = "${JalaliCalendarEngine.formatMinuteOfDay(b.startMinute, useFa)} تا ${JalaliCalendarEngine.formatMinuteOfDay(b.endMinute, useFa)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.inkSecondary
                            )
                        }
                        Icon(Icons.Default.Lock, contentDescription = "قفل‌شده", tint = tokens.inkSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Proposed Task Time Blocks (User Approves or Rejects!)
        item {
            GlassSectionHeader(
                title = "پیشنهاد زمان‌بندی وظایف در اوقات آزاد",
                subtitle = "برای ثبت واقعی در تقویم، هر پیشنهاد را تایید کنید",
                actionLabel = if (state.rejectedProposalTaskIds.isNotEmpty()) "بازنشانی" else null,
                onActionClick = { viewModel.resetRejectedProposals() }
            )
        }
        if (proposals.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "پیشنهاد جدیدی وجود ندارد",
                    description = "همه وظایف باز زمان‌بندی شده‌اند یا وظیفه بازی در فهرست ندارید."
                )
            }
        } else {
            items(proposals, key = { it.task.id }) { prop ->
                GlassCard(accentBorderColor = tokens.accentPrimary, contentPadding = PaddingValues(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(prop.task.title, style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                            Text(
                                text = "بازه پیشنهادی: ${JalaliCalendarEngine.formatMinuteOfDay(prop.suggestedStartMinute, useFa)} تا ${JalaliCalendarEngine.formatMinuteOfDay(prop.suggestedEndMinute, useFa)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.accentPrimary
                            )
                            Text(prop.reasonFa, style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                        }
                        PriorityChip(prop.task.priority)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.approveTaskProposal(prop, targetEpoch) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تایید و ثبت در تقویم")
                        }
                        OutlinedButton(
                            onClick = { viewModel.rejectTaskProposal(prop.task.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("رد")
                        }
                    }
                }
            }
        }

        // End of Day Review Card
        item {
            GlassCard {
                Text("جمع‌بندی و مرور پایان روز", style = MaterialTheme.typography.titleMedium, color = tokens.inkPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("وظایف تکمیل‌شده این روز:", style = MaterialTheme.typography.bodyMedium, color = tokens.inkSecondary)
                    Text(
                        text = PersianFormatters.digits("$completedDayTasks از ${totalDayTasks.size}", useFa),
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldSuccess
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { dayCompletionRatio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = EmeraldSuccess,
                    trackColor = tokens.subtleDivider
                )
            }
        }
    }
}
