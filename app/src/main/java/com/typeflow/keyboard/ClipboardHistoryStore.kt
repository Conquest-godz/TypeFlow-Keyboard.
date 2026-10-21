package com.typeflow.keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local, device-only clipboard history. Entries are not capped by a product-defined
 * count or word limit; available storage and SQLite's platform limits still apply.
 */
class ClipboardHistoryStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CLIPS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clip_created_at ON $TABLE_CLIPS(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Preserve existing history during upgrades; migrations will be added as schema evolves.
    }

    @Synchronized
    fun save(text: String): Long {
        if (text.isBlank()) return -1L
        val db = writableDatabase
        val duplicate = db.rawQuery(
            "SELECT id FROM $TABLE_CLIPS WHERE text = ? ORDER BY created_at DESC LIMIT 1",
            arrayOf(text)
        )
        val existingId = duplicate.use { if (it.moveToFirst()) it.getLong(0) else null }
        if (existingId != null) {
            val values = ContentValues().apply { put("created_at", System.currentTimeMillis()) }
            db.update(TABLE_CLIPS, values, "id = ?", arrayOf(existingId.toString()))
            return existingId
        }
        val values = ContentValues().apply {
            put("text", text)
            put("created_at", System.currentTimeMillis())
        }
        return db.insertOrThrow(TABLE_CLIPS, null, values)
    }

    @Synchronized
    fun recent(limit: Int = 100): List<Clip> {
        val result = mutableListOf<Clip>()
        readableDatabase.rawQuery(
            "SELECT id, text, created_at FROM $TABLE_CLIPS ORDER BY created_at DESC, id DESC LIMIT ?",
            arrayOf(limit.coerceAtLeast(1).toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += Clip(cursor.getLong(0), cursor.getString(1), cursor.getLong(2))
            }
        }
        return result
    }

    @Synchronized
    fun delete(id: Long): Int =
        writableDatabase.delete(TABLE_CLIPS, "id = ?", arrayOf(id.toString()))

    @Synchronized
    fun clear() {
        writableDatabase.delete(TABLE_CLIPS, null, null)
    }

    data class Clip(val id: Long, val text: String, val createdAt: Long)

    private companion object {
        const val DATABASE_NAME = "typeflow_clipboard.db"
        const val DATABASE_VERSION = 1
        const val TABLE_CLIPS = "clipboard_history"
    }
}
