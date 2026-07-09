package com.palmastro.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.palmastro.data.dao.*
import com.palmastro.data.entities.*

@Database(
    entities = [
        UserProfileEntity::class,
        MonthlyResultEntity::class,
        DeltaEntity::class,
        EntitlementEntity::class,
        InstallIdEntity::class,
        JournalEntryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PalmAstroDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun monthlyResultDao(): MonthlyResultDao
    abstract fun deltaDao(): DeltaDao
    abstract fun entitlementDao(): EntitlementDao
    abstract fun installIdDao(): InstallIdDao
    abstract fun journalDao(): JournalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS journal (
                        id TEXT NOT NULL PRIMARY KEY,
                        monthKey TEXT NOT NULL,
                        domain TEXT,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_journal_monthKey_domain
                    ON journal (monthKey, domain)
                """.trimIndent())
            }
        }
    }
}
