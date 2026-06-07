/**
 * 文件说明：RedBookDatabaseMigrations.kt
 * 作用：集中定义数据库版本升级所需的迁移逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移管理类
 *
 * 负责集中维护数据库版本升级规则，保证用户升级应用时，
 * 已有本地表结构与历史数据可以安全迁移到最新版本。
 */
object RedBookDatabaseMigrations {

    /**
     * 数据库版本 2 -> 5 的迁移。
     *
     * 本次迁移补充了搜索历史、收藏、播放进度和草稿相关表，
     * 为搜索页、本地收藏、视频续播和发布草稿功能提供持久化支持。
     *
     * @param db Room 提供的底层 SQLite 数据库实例。
     */
    private val MIGRATION_2_5 = object : Migration(2, 5) {
        /**
         * 执行数据库表结构迁移
         *
         * 按目标版本需要创建缺失表，并保持已有旧版本数据不被破坏。
         *
         * @param db Room 提供的底层 SQLite 数据库实例。
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            // 创建搜索历史表，用于保存最近搜索词并按更新时间排序展示。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history` (
                    `query` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`query`)
                )
                """.trimIndent()
            )

            // 创建收藏表，用于离线保存用户收藏过的笔记快照。
            // 表内冗余标题、封面和作者信息，避免列表展示时依赖额外联表或远端请求。
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

            // 创建播放进度表，用于视频详情页记录续播位置和倍速设置。
            // 以 noteId 为主键覆盖写入，保证每条视频仅保留最近一次有效进度。
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

            // 创建草稿表，用于保存发布流程中的自动保存和手动保存内容。
            // mediaUrls 和 tags 依赖 RoomConverters 做列表与字符串之间的转换。
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

    /** 所有迁移规则集合，供 Room 初始化数据库时统一注册。 */
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_5)
}
