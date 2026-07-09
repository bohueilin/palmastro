package com.palmastro.app.di

import com.palmastro.astro.AstroEngineImpl
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.SafetyFilterImpl
import com.palmastro.contracts.interfaces.*
import com.palmastro.palm.PalmFeatureExtractorImpl
import com.palmastro.scan.QualityGateImpl
import com.palmastro.scoring.DeltaEngineImpl
import com.palmastro.scoring.ScoringEngineImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides @Singleton fun provideQualityGate(): QualityGate = QualityGateImpl()
    @Provides @Singleton fun providePalmFeatureExtractor(): PalmFeatureExtractor = PalmFeatureExtractorImpl()
    @Provides @Singleton fun provideAstroEngine(): AstroEngine = AstroEngineImpl()
    @Provides @Singleton fun provideScoringEngine(): ScoringEngine = ScoringEngineImpl()
    @Provides @Singleton fun provideDeltaEngine(): DeltaEngine = DeltaEngineImpl()
    @Provides @Singleton fun provideContentComposer(): ContentComposer = ContentComposerImpl()
    @Provides @Singleton fun provideSafetyFilter(): SafetyFilter = SafetyFilterImpl()
}
