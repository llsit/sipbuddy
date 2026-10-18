package com.llsit.sipduddy.data.sync
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.local.database.HydrationDatabaseMigration
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class HydrationMigrationTest {
    @Test fun migrationPreservesLegacyGuestHistoryAndAssignsStableUniqueIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-sync-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE drinks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,drink TEXT NOT NULL,amountMl INTEGER NOT NULL,timestamp INTEGER NOT NULL)")
                        db.execSQL("CREATE INDEX index_drinks_timestamp ON drinks(timestamp)")
                        db.execSQL("INSERT INTO drinks(id,drink,amountMl,timestamp) VALUES(17,'WATER',250,100),(18,'TEA',400,101)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        error("unexpected upgrade")
                    }
                }).build(),
        )
        helper.writableDatabase
        helper.close()
        val db = Room.databaseBuilder(context, HydrationDatabase::class.java, name).addMigrations(HydrationDatabaseMigration.FROM_1_TO_2).build()
        try {
            val rows = db.drinks().observeEntries(0, 200).first()
            assertEquals(setOf(17L, 18L), rows.map { it.id }.toSet())
            assertEquals(650L, db.drinks().getTotalMl(0, 200))
            assertTrue(rows.all { it.ownerId.isEmpty() && it.pending && it.serverVersion == 0L })
            assertEquals(2, rows.map { UUID.fromString(it.remoteId) }.toSet().size)
            assertTrue(db.drinks().observeEntries(0, 200, "A").first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
