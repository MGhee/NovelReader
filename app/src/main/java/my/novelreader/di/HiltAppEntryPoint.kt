package my.novelreader.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.novelreader.tooling.application_workers.setup.AppWorkerFactory

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltAppEntryPoint {
    fun workerFactory(): AppWorkerFactory
}