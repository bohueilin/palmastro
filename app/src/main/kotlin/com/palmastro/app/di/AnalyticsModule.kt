package com.palmastro.app.di

import android.content.Context
import com.palmastro.analytics.AnalyticsEmitterImpl
import com.palmastro.app.analytics.FirebaseAnalyticsSink
import com.palmastro.contracts.interfaces.AnalyticsEmitter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideAnalyticsEmitter(@ApplicationContext context: Context): AnalyticsEmitter {
        FirebaseAnalyticsSink.init(context)
        return AnalyticsEmitterImpl { name, props ->
            FirebaseAnalyticsSink.send(name, props)
        }
    }
}
