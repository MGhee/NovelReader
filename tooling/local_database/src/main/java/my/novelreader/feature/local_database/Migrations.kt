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
)

internal fun migration(vi: Int, migrate: (SupportSQLiteDatabase) -> Unit) =
    object : Migration(vi, vi + 1) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate(db)
    }