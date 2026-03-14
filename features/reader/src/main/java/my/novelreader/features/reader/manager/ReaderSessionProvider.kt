package my.novelreader.features.reader.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import my.novelreader.data.AppRepository
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.features.reader.ReaderRepository
import my.novelreader.features.reader.ui.ReaderViewHandlersActions
import my.novelreader.feature.local_database.DAOs.ChapterTranslationDao
import my.novelreader.text_translator.domain.TranslationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReaderSessionProvider @Inject constructor(
    private val appRepository: AppRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
    private val translationManager: TranslationManager,
    private val readerRepository: ReaderRepository,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
    private val chapterTranslationDao: ChapterTranslationDao,
) {
    fun create(
        bookUrl: String,
        initialChapterUrl: String,
    ): ReaderSession = ReaderSession(
        bookUrl = bookUrl,
        initialChapterUrl = initialChapterUrl,
        appRepository = appRepository,
        translationManager = translationManager,
        appPreferences = appPreferences,
        context = context,
        readerRepository = readerRepository,
        readerViewHandlersActions = readerViewHandlersActions,
        chapterTranslationDao = chapterTranslationDao,
    )
}
