package my.novelreader.network

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class NetworkingModule {

    @Singleton
    @Binds
    internal abstract fun bindNetworkClient(client: ScraperNetworkClient): NetworkClient

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(client: ScraperNetworkClient): OkHttpClient = client.client
    }
}