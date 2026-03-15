package com.palmastro.app.di

import android.content.Context
import androidx.room.Room
import com.palmastro.data.PalmAstroDatabase
import com.palmastro.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PalmAstroDatabase =
        Room.databaseBuilder(context, PalmAstroDatabase::class.java, "palmastro.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: PalmAstroDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideMonthlyResultDao(db: PalmAstroDatabase): MonthlyResultDao = db.monthlyResultDao()
    @Provides fun provideDeltaDao(db: PalmAstroDatabase): DeltaDao = db.deltaDao()
    @Provides fun provideInstallIdDao(db: PalmAstroDatabase): InstallIdDao = db.installIdDao()
}
