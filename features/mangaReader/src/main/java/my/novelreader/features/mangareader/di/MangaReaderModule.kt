package my.novelreader.features.mangareader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.features.mangareader.setting.ReaderPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MangaReaderModule {

    @Provides
    @Singleton
    fun provideReaderPreferences(
        appPreferences: AppPreferences,
    ): ReaderPreferences {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        return ReaderPreferences(appPreferences, scope)
    }
}
