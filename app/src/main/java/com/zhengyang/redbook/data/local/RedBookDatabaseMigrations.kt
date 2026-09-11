package com.zhengyang.redbook.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RedBookDatabaseMigrations {

    private fun tableColumns(
        db: SupportSQLiteDatabase,
        tableName: String
    ): Set<String> {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0) {
                    columns += cursor.getString(nameIndex)
                }
            }
        }
        return columns
    }

    private fun hasColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String
    ): Boolean = tableColumns(db, tableName).contains(columnName)

    private fun rebuildMyProfileTable(db: SupportSQLiteDatabase) {
        val columns = tableColumns(db, "my_profile")
        if (columns.isEmpty()) return

        val nameExpr = if ("name" in columns) "`name`" else "''"
        val avatarUrlExpr = if ("avatarUrl" in columns) "`avatarUrl`" else "NULL"
        val avatarTextExpr = if ("avatarText" in columns) "`avatarText`" else "''"
        val avatarColorHexExpr = if ("avatarColorHex" in columns) "`avatarColorHex`" else "'#FF8A9F'"
        val bioExpr = if ("bio" in columns) "`bio`" else "NULL"
        val followingCountExpr = if ("followingCount" in columns) "CAST(`followingCount` AS INTEGER)" else "0"
        val fansCountExpr = if ("fansCount" in columns) "CAST(`fansCount` AS INTEGER)" else "0"
        val likesCountExpr = if ("likesCount" in columns) "CAST(`likesCount` AS INTEGER)" else "0"
        val noteCountExpr = if ("noteCount" in columns) "CAST(`noteCount` AS INTEGER)" else "0"

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `my_profile_new` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `avatarUrl` TEXT,
                `avatarText` TEXT NOT NULL,
                `avatarColorHex` TEXT NOT NULL,
                `bio` TEXT,
                `followingCount` INTEGER NOT NULL,
                `fansCount` INTEGER NOT NULL,
                `likesCount` INTEGER NOT NULL,
                `noteCount` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `my_profile_new` (
                `id`,
                `name`,
                `avatarUrl`,
                `avatarText`,
                `avatarColorHex`,
                `bio`,
                `followingCount`,
                `fansCount`,
                `likesCount`,
                `noteCount`
            )
            SELECT
                `id`,
                $nameExpr,
                $avatarUrlExpr,
                $avatarTextExpr,
                $avatarColorHexExpr,
                $bioExpr,
                $followingCountExpr,
                $fansCountExpr,
                $likesCountExpr,
                $noteCountExpr
            FROM `my_profile`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `my_profile`")
        db.execSQL("ALTER TABLE `my_profile_new` RENAME TO `my_profile`")
    }

    private fun rebuildSearchResultTable(db: SupportSQLiteDatabase) {
        val columns = tableColumns(db, "search_result")
        if (columns.isEmpty()) return

        val idExpr = if ("id" in columns) "`id`" else "''"
        val keywordExpr = if ("keyword" in columns) "`keyword`" else "''"
        val matchTokensExpr = if ("matchTokens" in columns) "`matchTokens`" else "''"
        val filterExpr = if ("filter" in columns) "`filter`" else "''"
        val titleExpr = if ("title" in columns) "`title`" else "''"
        val subtitleExpr = if ("subtitle" in columns) "`subtitle`" else "''"
        val metaExpr = if ("meta" in columns) "`meta`" else "''"
        val badgeExpr = if ("badge" in columns) "`badge`" else "''"
        val badgeColorResNameExpr = if ("badgeColorResName" in columns) "`badgeColorResName`" else "''"
        val sortOrderExpr = if ("sortOrder" in columns) "CAST(`sortOrder` AS INTEGER)" else "0"

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `search_result_new` (
                `id` TEXT NOT NULL,
                `keyword` TEXT NOT NULL,
                `matchTokens` TEXT NOT NULL,
                `filter` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `subtitle` TEXT NOT NULL,
                `meta` TEXT NOT NULL,
                `badge` TEXT NOT NULL,
                `badgeColorResName` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`keyword`, `id`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `search_result_new` (
                `id`,
                `keyword`,
                `matchTokens`,
                `filter`,
                `title`,
                `subtitle`,
                `meta`,
                `badge`,
                `badgeColorResName`,
                `sortOrder`
            )
            SELECT
                $idExpr,
                $keywordExpr,
                $matchTokensExpr,
                $filterExpr,
                $titleExpr,
                $subtitleExpr,
                $metaExpr,
                $badgeExpr,
                $badgeColorResNameExpr,
                $sortOrderExpr
            FROM `search_result`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `search_result`")
        db.execSQL("ALTER TABLE `search_result_new` RENAME TO `search_result`")
    }

    private fun createVersion5Tables(db: SupportSQLiteDatabase) {
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

    private val MIGRATION_2_5 = object : Migration(2, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createVersion5Tables(db)
        }
    }

    private val MIGRATION_3_5 = object : Migration(3, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createVersion5Tables(db)
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createVersion5Tables(db)
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `my_profile` ADD COLUMN `avatarUrl` TEXT")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `following_user` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `subtitle` TEXT NOT NULL,
                    `avatarUrl` TEXT,
                    `avatarColorHex` TEXT NOT NULL,
                    `badge` TEXT,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `person_suggestion` (
                    `id` TEXT NOT NULL,
                    `avatarText` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `subtitle` TEXT NOT NULL,
                    `avatarBackgroundRes` INTEGER NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `interest_person` (
                    `id` TEXT NOT NULL,
                    `avatarText` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `fansText` TEXT NOT NULL,
                    `avatarBackgroundRes` INTEGER NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_guess` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `meta` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_result` (
                    `id` TEXT NOT NULL,
                    `keyword` TEXT NOT NULL,
                    `matchTokens` TEXT NOT NULL,
                    `filter` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `subtitle` TEXT NOT NULL,
                    `meta` TEXT NOT NULL,
                    `badge` TEXT NOT NULL,
                    `badgeColorResName` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`keyword`, `id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            rebuildMyProfileTable(db)
            rebuildSearchResultTable(db)

            if (!hasColumn(db, "following_user", "avatarUrl")) {
                db.execSQL("ALTER TABLE `following_user` ADD COLUMN `avatarUrl` TEXT")
            }
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 发现页频道 id 由旧的 "1".."5" 改为语义化 id（recommend/food/travel/...），
            // 旧缓存中的分类和对应 sectionKey 的卡片已不再适用，清空让下一次请求重新拉取。
            db.execSQL("DELETE FROM `discover_category`")
            db.execSQL("DELETE FROM `home_card`")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_2_5,
        MIGRATION_3_5,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )
}
