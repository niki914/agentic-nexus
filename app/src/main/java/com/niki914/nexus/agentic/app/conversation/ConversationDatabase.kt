package com.niki914.nexus.agentic.app.conversation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ConversationTurnEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}

fun buildConversationDatabase(context: Context): ConversationDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        ConversationDatabase::class.java,
        "conversation_history.db",
    ).build()
}
