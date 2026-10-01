package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JarvisRepository(private val db: JarvisDatabase) {
    val noteDao = db.noteDao()
    val memoryDao = db.memoryDao()
    val chatMessageDao = db.chatMessageDao()
    val settingDao = db.settingDao()

    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun saveNote(title: String, content: String, source: String = "VOICE", tags: String = ""): Long {
        return noteDao.insertNote(
            NoteEntity(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                source = source,
                tags = tags
            )
        )
    }

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)
    suspend fun clearNotes() = noteDao.clearAllNotes()

    // Memory
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun saveMemory(category: String, key: String, value: String, isPinned: Boolean = false): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                category = category,
                key = key,
                value = value,
                isPinned = isPinned
            )
        )
    }

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    suspend fun getFormattedMemoryContext(): String {
        val memories = memoryDao.getTopMemories(25)
        if (memories.isEmpty()) return ""
        val sb = StringBuilder("=== JARVIS LONG-TERM MEMORY ARCHIVE ===\n")
        memories.forEach { mem ->
            sb.append("- [${mem.category}] ${mem.key}: ${mem.value}\n")
        }
        sb.append("Use these retained facts when responding to the user.\n")
        return sb.toString()
    }

    // Chat History
    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun addMessage(role: String, content: String, toolName: String? = null, toolResult: String? = null): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                role = role,
                content = content,
                toolName = toolName,
                toolResult = toolResult
            )
        )
    }

    suspend fun clearMessages() = chatMessageDao.clearAllMessages()

    // Settings
    suspend fun getSetting(key: String, defaultValue: String): String {
        return settingDao.getSetting(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String) {
        settingDao.setSetting(JarvisSettingEntity(key = key, value = value))
    }

    suspend fun deleteSetting(key: String) {
        settingDao.deleteSetting(key)
    }
}
