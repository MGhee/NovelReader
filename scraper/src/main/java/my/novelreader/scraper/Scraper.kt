package my.novelreader.scraper

import my.novelreader.network.NetworkClient
import my.novelreader.scraper.databases.BakaUpdates
import my.novelreader.scraper.databases.NovelUpdates
import my.novelreader.scraper.sources.AT
import my.novelreader.scraper.sources.BacaLightnovel
import my.novelreader.scraper.sources.BoxNovel
import my.novelreader.scraper.sources.IndoWebnovel
import my.novelreader.scraper.sources.LocalSource
import my.novelreader.scraper.sources.MeioNovel
import my.novelreader.scraper.sources.MoreNovel
import my.novelreader.scraper.sources.NovelHall
import my.novelreader.scraper.sources.Novelku
import my.novelreader.scraper.sources.ReadNovelFull
import my.novelreader.scraper.sources.Reddit
import my.novelreader.scraper.sources.RoyalRoad
import my.novelreader.scraper.sources.Saikai
import my.novelreader.scraper.sources.SakuraNovel
import my.novelreader.scraper.sources.Sousetsuka
import my.novelreader.scraper.sources.WbNovel
import my.novelreader.scraper.sources.WuxiaWorld
import my.novelreader.scraper.sources.ScribbleHub
import my.novelreader.scraper.sources.FreeWebNovel
import my.novelreader.scraper.sources.NovelFull
import my.novelreader.scraper.sources.AllNovel
import my.novelreader.scraper.sources.NovelBinCom
import my.novelreader.scraper.sources.NewNovel
import my.novelreader.scraper.sources.NoBadNovel
import my.novelreader.scraper.sources.WtrLab
import my.novelreader.scraper.sources.Shuba69
import my.novelreader.scraper.sources.UuKanshu
import my.novelreader.scraper.sources.Ddxss
import my.novelreader.scraper.sources.LeYueDu
import my.novelreader.scraper.sources.Twkan
import my.novelreader.scraper.sources.Ttkan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scraper @Inject constructor(
    networkClient: NetworkClient,
    localSource: LocalSource
) {
    val databasesList = setOf(
        NovelUpdates(networkClient),
        BakaUpdates(networkClient)
    )

    val sourcesList = setOf(
        localSource,
        ReadNovelFull(networkClient),
        RoyalRoad(networkClient),
        my.novelreader.scraper.sources.NovelUpdates(networkClient),
        Reddit(),
        AT(),
        Sousetsuka(),
        Saikai(networkClient),
        BoxNovel(networkClient),
        NovelHall(networkClient),
        WuxiaWorld(networkClient),
        IndoWebnovel(networkClient),
        Shuba69(networkClient),
        UuKanshu(networkClient),
        Ddxss(networkClient),
        LeYueDu(networkClient),
        Twkan(networkClient),
        Ttkan(networkClient),
        BacaLightnovel(networkClient),
        SakuraNovel(networkClient),
        MeioNovel(networkClient),
        MoreNovel(networkClient),
        Novelku(networkClient),
        WbNovel(networkClient),
        ScribbleHub(networkClient),
        FreeWebNovel(networkClient),
        NovelFull(networkClient),
        AllNovel(networkClient),
        NovelBinCom(networkClient),
        NewNovel(networkClient),
        NoBadNovel(networkClient),
        WtrLab(networkClient),
    )

    val sourcesCatalogsList = sourcesList.filterIsInstance<SourceInterface.Catalog>()
    val sourcesCatalogsLanguagesList = sourcesCatalogsList.mapNotNull { it.language }.toSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? =
        sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? =
        sourcesCatalogsList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleDatabase(url: String): DatabaseInterface? =
        databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
}
