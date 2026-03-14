package my.novelreader.text_translator

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.novelreader.core.AppCoroutineScope
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.text_translator.domain.TranslationManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FossModule {

    @Provides
    @Singleton
    fun provideTranslationManager(
        appCoroutineScope: AppCoroutineScope,
        appPreferences: AppPreferences
    ): TranslationManager {
        // Create both managers
        val geminiManager = TranslationManagerGemini(appCoroutineScope, appPreferences)
        val googleFreeManager = TranslationManagerGoogleFree(appCoroutineScope)
        
        // Use composite to switch between them based on API key availability
        return TranslationManagerComposite(
            appCoroutineScope,
            geminiManager,
            googleFreeManager,
            appPreferences
        )
    }
}