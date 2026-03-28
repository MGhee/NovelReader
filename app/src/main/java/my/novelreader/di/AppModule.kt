package my.novelreader.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.novelreader.App
import my.novelreader.AppNavigationRoutes
import my.novelreader.BuildConfig
import my.novelreader.core.AppInternalState
import my.novelreader.core.Toasty
import my.novelreader.core.ToastyToast
import my.novelreader.navigation.NavigationRoutes
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppNavigationRoutes(nav: AppNavigationRoutes): NavigationRoutes

    @Binds
    @Singleton
    abstract fun bindToasty(toast: ToastyToast): Toasty

    companion object {

        @Provides
        @Singleton
        fun providesApp(@ApplicationContext context: Context): App {
            return context as App
        }

        @Provides
        @Singleton
        fun providesAppInternalState(): AppInternalState = object : AppInternalState {
            override val isDebugMode = BuildConfig.DEBUG
            override val versionCode = BuildConfig.VERSION_CODE
            override val versionName = BuildConfig.VERSION_NAME
        }
    }
}