package com.llsit.sipduddy.data.local.database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID
object HydrationDatabaseMigration {
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE drinks ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE drinks ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE drinks ADD COLUMN serverVersion INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drinks ADD COLUMN localVersion INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drinks ADD COLUMN pending INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE drinks ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE drinks ADD COLUMN operationId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE drinks ADD COLUMN allowRestore INTEGER NOT NULL DEFAULT 0")
            db.query("SELECT id FROM drinks").use { cursor ->
                while (cursor.moveToNext()) {
                    db.execSQL(
                        "UPDATE drinks SET remoteId=?,operationId=? WHERE id=?",
                        arrayOf<Any>(UUID.randomUUID().toString(), UUID.randomUUID().toString(), cursor.getLong(0)),
                    )
                }
            }
            db.execSQL("CREATE UNIQUE INDEX index_drinks_ownerId_remoteId ON drinks(ownerId,remoteId)")
            db.execSQL("CREATE INDEX index_drinks_ownerId_pending ON drinks(ownerId,pending)")
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_metadata (ownerId TEXT NOT NULL PRIMARY KEY,lastSuccessAt INTEGER,error TEXT,conflicts INTEGER NOT NULL)")
        }
    }
}
