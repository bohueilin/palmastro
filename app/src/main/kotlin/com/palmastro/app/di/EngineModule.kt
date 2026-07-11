package com.palmastro.app.di

import android.content.Context
import com.palmastro.app.ui.scan.ImageQualityAnalyzer
import com.palmastro.app.ui.scan.ImageQualityAnalyzerFactory
import com.palmastro.astro.AstroEngineImpl
import com.palmastro.content.ContentComposerImpl
import com.palmastro.content.GuidanceBuilder
import com.palmastro.content.SafetyFilterImpl
import com.palmastro.contracts.interfaces.*
import com.palmastro.palm.PalmFeatureExtractorImpl
import com.palmastro.scan.QualityGateImpl
import com.palmastro.scoring.DeltaEngineImpl
import com.palmastro.scoring.Ruleset
import com.palmastro.scoring.ScoringEngineImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides @Singleton fun provideQualityGate(): QualityGate = QualityGateImpl()
    @Provides @Singleton fun providePalmFeatureExtractor(): PalmFeatureExtractor = PalmFeatureExtractorImpl()
    @Provides @Singleton fun provideAstroEngine(): AstroEngine = AstroEngineImpl()

    /** Ruleset is validated at graph construction so a bad ruleset fails fast, not mid-scan. */
    @Provides @Singleton fun provideScoringEngine(): ScoringEngine =
        ScoringEngineImpl(Ruleset.default().also { it.validateOrThrow() })

    @Provides @Singleton fun provideDeltaEngine(): DeltaEngine = DeltaEngineImpl()

    // Concrete singletons are exposed alongside their interfaces because the scan
    // pipeline needs impl-only members (templatesVersion, safeFallbackPayload).
    @Provides @Singleton fun provideContentComposerImpl(): ContentComposerImpl = ContentComposerImpl()
    @Provides @Singleton fun provideContentComposer(impl: ContentComposerImpl): ContentComposer = impl
    @Provides @Singleton fun provideSafetyFilterImpl(): SafetyFilterImpl = SafetyFilterImpl()
    @Provides @Singleton fun provideSafetyFilter(impl: SafetyFilterImpl): SafetyFilter = impl

    // Guidance layer (PRD §§11–13): deterministic builder over the shared content templates.
    // TODO(integration): confirm GuidanceBuilder ctor once engine-content lands (assumed default ctor over ContentTemplates.default()).
    @Provides @Singleton fun provideGuidanceBuilder(): GuidanceBuilder = GuidanceBuilder()

    @Provides @Singleton fun provideImageQualityAnalyzerFactory(): ImageQualityAnalyzerFactory =
        ImageQualityAnalyzerFactory { context: Context, source -> ImageQualityAnalyzer(context, source) }

    @Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
