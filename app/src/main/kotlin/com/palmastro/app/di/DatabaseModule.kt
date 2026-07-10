package com.palmastro.app.di

import android.content.Context
import androidx.room.Room
import com.palmastro.app.security.DatabaseKeyManager
import com.palmastro.data.PalmAstroDatabase
import com.palmastro.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PalmAstroDatabase {
        // Load the SQLCipher native libraries before any encrypted DB access
        // (SupportFactory also does this defensively; being explicit is cheap).
        SQLiteDatabase.loadLibs(context)
        val passphrase = DatabaseKeyManager.getOrCreateDatabaseKey(context)
        return Room.databaseBuilder(context, PalmAstroDatabase::class.java, "palmastro.db")
            .openHelperFactory(SupportFactory(passphrase))
            .addMigrations(*PalmAstroDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides fun provideUserProfileDao(db: PalmAstroDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideMonthlyResultDao(db: PalmAstroDatabase): MonthlyResultDao = db.monthlyResultDao()
    @Provides fun provideDeltaDao(db: PalmAstroDatabase): DeltaDao = db.deltaDao()
    @Provides fun provideEntitlementDao(db: PalmAstroDatabase): EntitlementDao = db.entitlementDao()
    @Provides fun provideInstallIdDao(db: PalmAstroDatabase): InstallIdDao = db.installIdDao()
    @Provides fun provideJournalDao(db: PalmAstroDatabase): JournalDao = db.journalDao()
}
