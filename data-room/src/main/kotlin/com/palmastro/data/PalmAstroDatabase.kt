package com.palmastro.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.palmastro.data.dao.*
import com.palmastro.data.entities.*

@Database(
    entities = [
        UserProfileEntity::class,
        MonthlyResultEntity::class,
        DeltaEntity::class,
        EntitlementEntity::class,
        InstallIdEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PalmAstroDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun monthlyResultDao(): MonthlyResultDao
    abstract fun deltaDao(): DeltaDao
    abstract fun installIdDao(): InstallIdDao
}
