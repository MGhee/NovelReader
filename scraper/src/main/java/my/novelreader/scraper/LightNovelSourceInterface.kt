package my.novelreader.scraper

/**
 * Marker interface for light-novel sources. Used by the Finder UI to group these sources
 * under their own tab and by the data layer to trigger per-volume cover refreshes after a
 * chapter (= one EPUB volume) is parsed.
 */
interface LightNovelSourceInterface : SourceInterface.Catalog
