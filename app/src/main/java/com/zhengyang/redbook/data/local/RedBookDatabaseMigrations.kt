/**
 * 文件说明：RedBookDatabaseMigrations.kt
 * 作用：定义数据库版本迁移策略，管理数据库结构变更。
 * 备注：当应用更新导致数据库 schema 变化时，通过迁移脚本保证数据不丢失。
 */
package com.zhengyang.redbook.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移管理器。
 *
 * 负责声明和聚合所有数据库版本之间的迁移脚本。
 * 每个迁移脚本对应一对版本号（startVersion -> endVersion），
 * 当 Room 检测到数据库版本不匹配时，会按顺序执行这些迁移脚本。
 *
 * 注意：迁移脚本必须保持幂等性，即多次执行结果一致。
 */
object RedBookDatabaseMigrations {

    /**
     * 从版本 2 升级到版本 5 的迁移脚本。
     *
     * 该迁移新增了以下数据表：
     * - search_history：搜索历史记录表，用于保存用户的搜索关键词。
     * - collection：收藏表，用于保存用户收藏的笔记信息。
     * - playback_progress：播放进度表，用于音视频笔记的播放位置记录。
     * - draft：草稿表，用于保存用户未发布的笔记草稿。
     *
     * @see SupportSQLiteDatabase 用于执行原始 SQL 语句。
     */
    private val MIGRATION_2_5 = object : Migration(2, 5) {

        /**
         * 执行数据库迁移操作。
         *
         * @param db 数据库会话对象，提供 execSQL 方法执行原生 SQL。
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            // 创建搜索历史表：存储用户搜索过的关键词。
            // query：搜索关键词文本，作为主键保证唯一性。
            // updatedAt：记录更新时间，用于判断是否需要清除过期历史。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history` (
                    `query` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`query`)
                )
                """.trimIndent()
            )

            // 创建收藏表：存储用户收藏的笔记信息。
            // id：收藏记录唯一标识。
            // userId：收藏者用户 ID。
            // noteId/noteTitle/noteCoverUrl/noteAuthor：被收藏笔记的核心信息。
            // collectedAt：收藏时间戳。
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

            // 创建播放进度表：记录音视频笔记的播放进度。
            // noteId：对应的笔记 ID，作为主键。
            // positionMs：当前播放位置（毫秒）。
            // durationMs：总时长（毫秒）。
            // playbackSpeed：播放速度倍率。
            // lastPlayedAt：最后播放时间。
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

            // 创建草稿表：存储用户未发布的笔记草稿。
            // id：草稿唯一标识。
            // title/description：笔记标题和正文描述。
            // mediaUrls/mediaType：关联的媒体资源。
            // location：发布时可选的地理位置。
            // tags：笔记标签列表（序列化存储）。
            // savedAt：最后保存时间。
            // isAutoSaved：是否自动保存（区分主动保存和自动保存）。
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

    /**
     * 从版本 5 升级到版本 6 的迁移脚本。
     *
     * 该迁移主要完成以下变更：
     * 1. 扩展 my_profile 表，新增 avatarUrl 字段。
     * 2. 新增 following_user 表：存储推荐关注的用户列表。
     * 3. 新增 person_suggestion 表：存储推荐用户列表（消息页使用）。
     * 4. 新增 interest_person 表：存储用户感兴趣的人。
     * 5. 新增 search_guess 表：存储搜索推荐词。
     * 6. 新增 search_result 表：存储搜索结果缓存。
     *
     * @see SupportSQLiteDatabase 用于执行原始 SQL 语句。
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {

        /**
         * 执行数据库迁移操作。
         *
         * @param db 数据库会话对象，提供 execSQL 方法执行原生 SQL。
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            // 为 my_profile 表新增 avatarUrl 字段。
            // 用于存储用户头像的远程 URL 地址。
            db.execSQL("ALTER TABLE `my_profile` ADD COLUMN `avatarUrl` TEXT")

            // 创建关注用户推荐表：存储首页推荐关注的用户信息。
            // id：用户唯一标识。
            // name/subtitle：用户名称和副标题（如粉丝数）。
            // avatarUrl：用户头像 URL（可选）。
            // avatarColorHex：头像背景颜色十六进制值。
            // badge：用户徽章标识（如达人标识）。
            // sortOrder：排序顺序。
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

            // 创建用户推荐表：用于消息页等位置的用户推荐展示。
            // id：用户唯一标识。
            // avatarText：头像文字（如用户名首字）。
            // name/subtitle：用户名称和副标题。
            // avatarBackgroundRes：头像背景资源 ID。
            // sortOrder：排序顺序。
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

            // 创建感兴趣的人表：存储用户可能感兴趣的其他用户。
            // id：用户唯一标识。
            // avatarText：头像文字。
            // name：用户名称。
            // fansText：粉丝数字符串描述。
            // avatarBackgroundRes：头像背景资源 ID。
            // sortOrder：排序顺序。
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

            // 创建搜索推荐表：存储搜索框的推荐关键词。
            // id：推荐词唯一标识。
            // title：推荐词文本（如"热搜话题"）。
            // meta：元数据描述（如"12万人搜索"）。
            // sortOrder：排序顺序。
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

            // 创建搜索结果缓存表：存储搜索结果以实现离线查看。
            // id：搜索结果项唯一标识。
            // keyword：搜索关键词（复合主键之一）。
            // matchTokens：匹配标记词。
            // filter：内容类型过滤（如"视频"、"图文"）。
            // title/subtitle/meta：搜索结果的标题、副标题和元数据。
            // badge/badgeColorResName：徽章及其颜色。
            // sortOrder：排序顺序。
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

    /**
     * 所有迁移脚本的集合。
     *
     * 将分散的迁移对象聚合成数组，供 Room 数据库构建器使用。
     * Room 会按照版本顺序自动选择需要执行的迁移脚本。
     *
     * 注意：这里只声明了 MIGRATION_2_5 和 MIGRATION_5_6，
     * 如果中间有跳过的版本，需要补充相应的迁移脚本。
     */
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_5, MIGRATION_5_6)
}