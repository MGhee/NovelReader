package my.novelreader

import android.content.Context
import android.content.Intent
import my.novelreader.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.novelreader.databaseexplorer.databaseSearch.DatabaseSearchExtras
import my.novelreader.features.chapterslist.ChaptersActivity
import my.novelreader.features.main.MainActivity
import my.novelreader.features.reader.ReaderActivity
import my.novelreader.globalsourcesearch.GlobalSourceSearchActivity
import my.novelreader.navigation.NavigationRoutes
import my.novelreader.sourceexplorer.SourceCatalogActivity
import my.novelreader.feature.local_database.BookMetadata
import my.novelreader.webview.WebViewActivity
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppNavigationRoutes @Inject constructor() : NavigationRoutes {

    override fun main(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    override fun reader(
        context: Context,
        bookUrl: String,
        chapterUrl: String,
        scrollToSpeakingItem: Boolean
    ): Intent {
        return ReaderActivity.IntentData(
            context,
            bookUrl = bookUrl,
            chapterUrl = chapterUrl,
            scrollToSpeakingItem = scrollToSpeakingItem
        )
    }

    override fun chapters(context: Context, bookMetadata: BookMetadata): Intent {
        return ChaptersActivity.IntentData(context, bookMetadata)
    }

    override fun databaseSearch(
        context: Context,
        input: String,
        databaseUrlBase: String
    ): Intent {
        return DatabaseSearchActivity.IntentData(
            context,
            DatabaseSearchExtras.Title(databaseBaseUrl = databaseUrlBase, title = input)
        )
    }

    override fun databaseSearch(
        context: Context,
        databaseBaseUrl: String
    ): Intent {
        return DatabaseSearchActivity.IntentData(
            context,
            DatabaseSearchExtras.Catalog(databaseBaseUrl = databaseBaseUrl)
        )
    }

    override fun sourceCatalog(
        context: Context,
        sourceBaseUrl: String,
    ): Intent {
        return SourceCatalogActivity
            .IntentData(context, sourceBaseUrl = sourceBaseUrl)
    }

    override fun globalSearch(
        context: Context,
        text: String,
    ): Intent {
        return GlobalSourceSearchActivity.IntentData(context, text)
    }

    override fun webView(context: Context, url: String): Intent {
        return WebViewActivity.IntentData(context, url = url)
    }
}