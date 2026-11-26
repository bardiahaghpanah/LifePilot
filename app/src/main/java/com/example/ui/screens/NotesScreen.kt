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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.jalali.PersianFormatters
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifePilotDataState
import com.example.ui.viewmodel.LifePilotViewModel

@Composable
fun NotesScreen(
    state: LifePilotDataState,
    viewModel: LifePilotViewModel
) {
    val tokens = LocalGlassTokens.current
    val useFa = state.settings.usePersianDigits
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("ACTIVE") } // ACTIVE, FAVORITE, ARCHIVED
    var isGridView by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val filtered = remember(state.notes, searchQuery, filterTab) {
        state.notes.filter { n ->
            val tabOk = when (filterTab) {
                "FAVORITE" -> n.isFavorite && !n.isArchived
                "ARCHIVED" -> n.isArchived
                else -> !n.isArchived
            }
            val qOk = searchQuery.isBlank() ||
                n.title.contains(searchQuery, true) ||
                n.content.contains(searchQuery, true) ||
                n.tags.contains(searchQuery, true)
            tabOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("notes_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("دفترچه یادداشت", style = MaterialTheme.typography.titleLarge, color = tokens.inkPrimary)
                        Text("${PersianFormatters.digits(state.notes.size.toString(), useFa)} یادداشت محلی", style = MaterialTheme.typography.bodySmall, color = tokens.inkSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "تغییر نما"
                            )
                        }
                        Button(onClick = { editingNote = null; showEditor = true }, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("یادداشت")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در عنوان، متن و برچسب‌ها...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filterTab == "ACTIVE", onClick = { filterTab = "ACTIVE" }, label = { Text("همه") })
                    FilterChip(selected = filterTab == "FAVORITE", onClick = { filterTab = "FAVORITE" }, label = { Text("علاقه‌مندی‌ها") })
                    FilterChip(selected = filterTab == "ARCHIVED", onClick = { filterTab = "ARCHIVED" }, label = { Text("آرشیو") })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassEmptyState(
                    icon = Icons.Default.NoteAlt,
                    title = "یادداشتی وجود ندارد",
                    description = "یادداشت‌های شخصی، چک‌لیست‌ها و نکات جلسات یا دانشگاه را با ذخیره خودکار بنویسید.",
                    actionLabel = "نوشتن یادداشت",
                    onAction = { editingNote = null; showEditor = true }
                )
            }
        } else if (isGridView) {
            val rows = filtered.chunked(2)
            items(rows) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { note ->
                        NoteSummaryCard(
                            note = note,
                            modifier = Modifier.weight(1f),
                            onClick = { editingNote = note; showEditor = true },
                            onTogglePin = { viewModel.saveNote(note.copy(isPinned = !note.isPinned)) },
                            onToggleFav = { viewModel.saveNote(note.copy(isFavorite = !note.isFavorite)) },
                            onDelete = { deletingNote = note }
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            items(filtered, key = { it.id }) { note ->
                NoteSummaryCard(
                    note = note,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { editingNote = note; showEditor = true },
                    onTogglePin = { viewModel.saveNote(note.copy(isPinned = !note.isPinned)) },
                    onToggleFav = { viewModel.saveNote(note.copy(isFavorite = !note.isFavorite)) },
                    onDelete = { deletingNote = note }
                )
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            existing = editingNote,
            onSave = { viewModel.saveNote(it) },
            onDismiss = { showEditor = false }
        )
    }

    deletingNote?.let { n ->
        ConfirmDeleteDialog(
            title = "حذف یادداشت",
            message = "آیا از حذف یادداشت «${n.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteNote(n) },
            onDismiss = { deletingNote = null }
        )
    }
}

@Composable
private fun NoteSummaryCard(
    note: NoteEntity,
    modifier: Modifier,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = modifier,
        accentBorderColor = if (note.isPinned) tokens.accentSecondary else null,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "سنجاق",
                        tint = if (note.isPinned) tokens.accentSecondary else tokens.inkSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onToggleFav, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = if (note.isFavorite) ChampagneGold else tokens.inkSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = tokens.inkSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = note.content.ifBlank { "بدون متن" },
            style = MaterialTheme.typography.bodySmall,
            color = tokens.inkSecondary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassBadge(note.category, tokens.accentPrimary)
            if (note.tags.isNotBlank()) {
                GlassBadge(note.tags.split(",").first().trim(), tokens.accentSecondary)
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    existing: NoteEntity?,
    onSave: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "شخصی") }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var isArchived by remember { mutableStateOf(existing?.isArchived ?: false) }

    // Auto-save whenever editing an existing note so no edits are lost
    DisposableEffect(title, content, category, tags, isArchived) {
        onDispose {
            if (existing != null && (title.isNotBlank() || content.isNotBlank())) {
                onSave(
                    existing.copy(
                        title = title.trim().ifBlank { "یادداشت بدون عنوان" },
                        content = content,
                        category = category,
                        tags = tags.trim(),
                        isArchived = isArchived
                    )
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "یادداشت جدید" else "ویرایشگر یادداشت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                // Rich formatting quick-insert toolbar
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        AssistChip(onClick = { content += "\n# عنوان بخش\n" }, label = { Text("تیتر") })
                    }
                    item {
                        AssistChip(onClick = { content += "\n• " }, label = { Text("لیست") })
                    }
                    item {
                        AssistChip(onClick = { content += "\n1. " }, label = { Text("شماره‌دار") })
                    }
                    item {
                        AssistChip(onClick = { content += "\n☐ " }, label = { Text("چک‌لیست") })
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("متن یادداشت") },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("شخصی", "کاری", "دانشگاه", "جلسه", "پروژه", "ایده")) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("برچسب‌ها (با ویرگول جدا کنید)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("انتقال به آرشیو")
                        Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    NoteEntity(
                        id = existing?.id ?: 0,
                        title = title.trim().ifBlank { "یادداشت بدون عنوان" },
                        content = content,
                        category = category,
                        tags = tags.trim(),
                        isPinned = existing?.isPinned ?: false,
                        isFavorite = existing?.isFavorite ?: false,
                        isArchived = isArchived
                    )
                )
                onDismiss()
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
