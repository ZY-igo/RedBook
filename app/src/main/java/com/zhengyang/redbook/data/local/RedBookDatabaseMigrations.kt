package com.zhengyang.redbook.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RedBookDatabaseMigrations {
    private val MIGRATION_2_5 = object : Migration(2, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history` (
                    `query` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`query`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `collection` (
                    `id` TEXT NOT NULL,
                    `userId` TEXT NOT NULL,
                    `noteId` TEXT NOT NULL,
                    `noteTitle` TEXT NOT NULL,
                    `noteCoverUrl` TEXT NOT NULL,
                    `noteAuthor` TEXT NOT NULL,
                    `collectedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playback_progress` (
                    `noteId` TEXT NOT NULL,
                    `positionMs` INTEGER NOT NULL,
                    `durationMs` INTEGER NOT NULL,
                    `playbackSpeed` REAL NOT NULL,
                    `lastPlayedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`noteId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `draft` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `mediaUrls` TEXT NOT NULL,
                    `mediaType` TEXT NOT NULL,
                    `location` TEXT,
                    `tags` TEXT NOT NULL,
                    `savedAt` INTEGER NOT NULL,
                    `isAutoSaved` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_5)
}
