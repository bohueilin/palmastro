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
    version = 3,
    exportSchema = true,
)
abstract class PalmAstroDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun monthlyResultDao(): MonthlyResultDao
    abstract fun deltaDao(): DeltaDao
    abstract fun entitlementDao(): EntitlementDao
    abstract fun installIdDao(): InstallIdDao
    abstract fun journalDao(): JournalDao

    companion object {
        /**
         * v1 -> v2: adds the journal table and the profile fields (name/gender/
         * relationshipStatus) exactly as the v2 entities declared them. The originally
         * shipped migration created journal with only 6 columns (and stray SQL
         * defaults) and never altered user_profile, so Room's post-migration schema
         * validation failed and upgrades crashed; devices that hit it rolled back and
         * stayed on v1, which this corrected migration now handles.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal` (
                        `id` TEXT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `domain` TEXT,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `name` TEXT,
                        `gender` TEXT,
                        `relationshipStatus` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_monthKey_domain`
                    ON `journal` (`monthKey`, `domain`)
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `name` TEXT")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `gender` TEXT")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `relationshipStatus` TEXT")
            }
        }

        /**
         * v2 -> v3: journal drops the copy-pasted profile fields (data preserved);
         * user_profile gains `language` (default "system" = follow device locale).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_new` (
                        `id` TEXT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `domain` TEXT,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `journal_new` (`id`, `monthKey`, `domain`, `text`, `createdAt`, `updatedAt`)
                    SELECT `id`, `monthKey`, `domain`, `text`, `createdAt`, `updatedAt` FROM `journal`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `journal`")
                db.execSQL("ALTER TABLE `journal_new` RENAME TO `journal`")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_monthKey_domain`
                    ON `journal` (`monthKey`, `domain`)
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'system'")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
