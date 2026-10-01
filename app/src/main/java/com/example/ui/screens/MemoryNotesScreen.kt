package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.FuturisticHudCard
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryNotesScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateMemoryDialog by remember { mutableStateOf(false) }

    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.notesSearchQuery.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATA ARCHIVE",
                    color = JarvisCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = JarvisSurface,
                contentColor = JarvisCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = JarvisCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "NOTES (${notes.size})",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "MEMORY (${memories.size})",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> NotesSection(
                    notes = notes,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setNotesSearchQuery(it) },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onClearAll = { viewModel.clearAllNotes() }
                )
                1 -> MemorySection(
                    memories = memories,
                    onDeleteMemory = { viewModel.deleteMemory(it) },
                    onClearAll = { viewModel.clearAllMemories() }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) showCreateNoteDialog = true else showCreateMemoryDialog = true
            },
            containerColor = JarvisCyan,
            contentColor = Color(0xFF001E2B),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    if (showCreateNoteDialog) {
        CreateNoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onConfirm = { title, content ->
                viewModel.createNote(title, content)
                showCreateNoteDialog = false
            }
        )
    }

    if (showCreateMemoryDialog) {
        CreateMemoryDialog(
            onDismiss = { showCreateMemoryDialog = false },
            onConfirm = { category, key, value ->
                viewModel.saveMemory(category, key, value)
                showCreateMemoryDialog = false
            }
        )
    }
}

@Composable
private fun NotesSection(
    notes: List<NoteEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_search_input"),
            placeholder = { Text("Search notes...", color = JarvisTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = JarvisCyan
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisCardBorder,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes recorded. Say \"JARVIS, create a note...\" or tap +",
                    color = JarvisTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onDelete = { onDeleteNote(note.id) })
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteEntity, onDelete: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(note.timestamp))

    FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.source == "VOICE") {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Created",
                            tint = JarvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = note.title,
                        color = JarvisCyanBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                color = JarvisTextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                color = JarvisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MemorySection(
    memories: List<MemoryEntity>,
    onDeleteMemory: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RETAINED CONTEXT FOR GEMINI",
                color = JarvisTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (memories.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("CLEAR ALL", color = JarvisRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No retained facts. Say \"JARVIS, remember that I like sci-fi\"",
                    color = JarvisTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(memory = memory, onDelete = { onDeleteMemory(memory.id) })
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(memory: MemoryEntity, onDelete: () -> Unit) {
    FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JarvisSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = memory.category.uppercase(),
                        color = JarvisAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.value,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Forget Fact",
                    tint = JarvisTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurface,
        title = {
            Text("CREATE NEW NOTE", color = JarvisCyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = JarvisTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content", color = JarvisTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onConfirm(title, content) },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color(0xFF001E2B))
            ) {
                Text("SAVE NOTE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = JarvisTextMuted)
            }
        }
    )
}

@Composable
private fun CreateMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, key: String, value: String) -> Unit
) {
    var fact by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PREFERENCE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurface,
        title = {
            Text("STORE LONG-TERM MEMORY", color = JarvisAmber, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column {
                Text(
                    text = "JARVIS will inject this retained fact into the Gemini context for all future conversations.",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fact,
                    onValueChange = { fact = it },
                    label = { Text("Fact / User Preference", color = JarvisTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisAmber,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (fact.isNotBlank()) onConfirm(category, "User Fact", fact) },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber, contentColor = Color(0xFF261900))
            ) {
                Text("STORE FACT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = JarvisTextMuted)
            }
        }
    )
}
