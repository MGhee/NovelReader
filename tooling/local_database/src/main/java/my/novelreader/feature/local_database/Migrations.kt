package my.novelreader.feature.local_database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import my.novelreader.feature.local_database.migrations.MigrationsList
import my.novelreader.feature.local_database.migrations._1stKissNovelDomainChange_1_org
import my.novelreader.feature.local_database.migrations.readLightNovelDomainChange_1_today
import my.novelreader.feature.local_database.migrations.readLightNovelDomainChange_2_meme

internal fun databaseMigrations() = arrayOf(
    migration(1) {
        it.execSQL("ALTER TABLE Chapter ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    },
    migration(2) {
        it.execSQL("ALTER TABLE Book ADD COLUMN inLibrary INTEGER NOT NULL DEFAULT 0")
        it.execSQL("UPDATE Book SET inLibrary = 1")
    },
    migration(3) {
        it.execSQL("ALTER TABLE Book ADD COLUMN coverImageUrl TEXT NOT NULL DEFAULT ''")
        it.execSQL("ALTER TABLE Book ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    },
    migration(4) {
        it.execSQL("ALTER TABLE Book ADD COLUMN lastReadEpochTimeMilli INTEGER NOT NULL DEFAULT 0")
    },
    migration(5, MigrationsList::readLightNovelDomainChange_1_today),
    migration(6, MigrationsList::readLightNovelDomainChange_2_meme),
    migration(7, MigrationsList::_1stKissNovelDomainChange_1_org),
    migration(8) {
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS ChapterTranslation (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                chapterUrl TEXT NOT NULL,
                sourceLang TEXT NOT NULL,
                targetLang TEXT NOT NULL,
                originalText TEXT NOT NULL,
                translatedText TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)
        it.execSQL("""
            CREATE INDEX IF NOT EXISTS index_ChapterTranslation_chapterUrl_sourceLang_targetLang
            ON ChapterTranslation (chapterUrl, sourceLang, targetLang)
        """)
    },
    migration(9) {
        it.execSQL("ALTER TABLE Book ADD COLUMN lastSeenChaptersCount INTEGER NOT NULL DEFAULT 0")
    },
    migration(10) {
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS ReadingSession (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookUrl TEXT NOT NULL,
                startTimeEpochMilli INTEGER NOT NULL,
                endTimeEpochMilli INTEGER NOT NULL DEFAULT 0,
                chaptersRead INTEGER NOT NULL DEFAULT 0
            )
        """)
    },
    migration(11) {
        it.execSQL("ALTER TABLE Book ADD COLUMN coverSeedColor INTEGER DEFAULT NULL")
    },
    migration(12) {
        it.execSQL("CREATE INDEX IF NOT EXISTS index_Chapter_bookUrl ON Chapter (bookUrl)")
    },
    migration(13) {
        it.execSQL("ALTER TABLE Book ADD COLUMN contentType TEXT NOT NULL DEFAULT 'NOVEL'")
    },
    migration(14) {
        // Create Category table for organizing books
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS Category (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                flags INTEGER NOT NULL,
                contentType TEXT NOT NULL
            )
        """)

        // Create BookCategory junction table
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS BookCategory (
                bookUrl TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                addedEpochTimeMilli INTEGER NOT NULL,
                PRIMARY KEY (bookUrl, categoryId),
                FOREIGN KEY (bookUrl) REFERENCES Book(url) ON DELETE CASCADE,
                FOREIGN KEY (categoryId) REFERENCES Category(id) ON DELETE CASCADE
            )
        """)
        // Create indices for BookCategory
        it.execSQL("CREATE INDEX IF NOT EXISTS index_BookCategory_categoryId ON BookCategory (categoryId)")
        it.execSQL("CREATE INDEX IF NOT EXISTS index_BookCategory_bookUrl ON BookCategory (bookUrl)")
        it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_BookCategory_bookUrl_categoryId ON BookCategory (bookUrl, categoryId)")

        // Insert default category (cannot be deleted, always exists)
        it.execSQL("INSERT INTO Category (id, name, sortOrder, flags, contentType) VALUES (0, 'Default', 0, 0, 'NOVEL')")
    },
    migration(15) {
        // Add bookmarked column to Chapter table for marking favorite chapters
        it.execSQL("ALTER TABLE Chapter ADD COLUMN bookmarked INTEGER NOT NULL DEFAULT 0")
    },
    migration(16) {
        // Create SavedSearch table for saved search queries
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS SavedSearch (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sourceId TEXT NOT NULL,
                query TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                isPinned INTEGER NOT NULL
            )
        """)
        // Create index on sourceId for quick lookups
        it.execSQL("CREATE INDEX IF NOT EXISTS index_SavedSearch_sourceId ON SavedSearch (sourceId)")
    },
    migration(17) {
        // Create LibraryUpdateError table for tracking update failures
        it.execSQL("""
            CREATE TABLE IF NOT EXISTS LibraryUpdateError (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookUrl TEXT NOT NULL,
                errorMessage TEXT NOT NULL,
                errorType TEXT NOT NULL,
                attemptCount INTEGER NOT NULL DEFAULT 1,
                lastAttemptTime INTEGER NOT NULL
            )
        """)
        // Create index on bookUrl for quick lookups
        it.execSQL("CREATE INDEX IF NOT EXISTS index_LibraryUpdateError_bookUrl ON LibraryUpdateError (bookUrl)")
    },
    migration(18) {
        // Recreate Chapter so `bookmarked` and `lastReadMangaPage` carry the
        // DEFAULT 0 clauses Room now demands. Some debug installs ended up with
        // these columns but no defaults; SQLite can't add a default in place.
        val hasManga = hasColumn(it, "Chapter", "lastReadMangaPage")
        it.execSQL("""
            CREATE TABLE Chapter_new (
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                bookUrl TEXT NOT NULL,
                position INTEGER NOT NULL,
                read INTEGER NOT NULL,
                lastReadPosition INTEGER NOT NULL,
                lastReadOffset INTEGER NOT NULL,
                bookmarked INTEGER NOT NULL DEFAULT 0,
                lastReadMangaPage INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(url)
            )
        """.trimIndent())
        val mangaSelect = if (hasManga) "lastReadMangaPage" else "0"
        it.execSQL("""
            INSERT INTO Chapter_new
                (title, url, bookUrl, position, read, lastReadPosition, lastReadOffset, bookmarked, lastReadMangaPage)
            SELECT title, url, bookUrl, position, read, lastReadPosition, lastReadOffset, bookmarked, $mangaSelect
            FROM Chapter
        """.trimIndent())
        it.execSQL("DROP TABLE Chapter")
        it.execSQL("ALTER TABLE Chapter_new RENAME TO Chapter")
        it.execSQL("CREATE INDEX IF NOT EXISTS index_Chapter_bookUrl ON Chapter (bookUrl)")
    },
)

private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean =
    db.query("PRAGMA table_info($table)").use { c ->
        val nameIdx = c.getColumnIndex("name")
        while (c.moveToNext()) {
            if (c.getString(nameIdx) == column) return@use true
        }
        false
    }

internal fun migration(vi: Int, migrate: (SupportSQLiteDatabase) -> Unit) =
    object : Migration(vi, vi + 1) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate(db)
    }