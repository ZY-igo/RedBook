/**
 * 文件说明：ListDao.kt
 * 作用：定义本地聚合数据访问接口，负责多个业务列表与轻量信息表的读写。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.SearchHistoryEntity

/**
 * 本地列表数据访问对象
 *
 * 负责首页卡片、分类、消息入口、个人资料和搜索历史等本地表的读写，
 * 当前项目的仓储层通过该接口访问 Room 数据库。
 */
@Dao
interface ListDao {

    /**
     * 读取全部笔记缓存。
     *
     * @return 本地保存的全部笔记实体
     */
    @Query("SELECT * FROM note_item")
    suspend fun getAll(): List<NoteItem>

    /**
     * 分页读取笔记缓存。
     *
     * @param limit 单次读取数量
     * @param offset 偏移量
     * @return 当前分页范围内的笔记实体列表
     */
    @Query("SELECT * FROM note_item LIMIT :limit OFFSET :offset")
    suspend fun getPagedNotes(limit: Int, offset: Int): List<NoteItem>

    /**
     * 统计本地笔记缓存数量。
     *
     * @return 当前缓存中的笔记总数
     */
    @Query("SELECT COUNT(*) FROM note_item")
    suspend fun getNoteCount(): Int

    /**
     * 批量插入或替换笔记缓存。
     *
     * @param items 需要写入的笔记实体列表
     * @return Room 返回的插入结果主键列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteItem>): List<Long>

    /**
     * 更新单条笔记记录。
     *
     * @param item 需要更新的笔记实体。
     * @return 受影响的行数。
     */
    @Update
    suspend fun update(item: NoteItem): Int

    /**
     * 删除单条笔记记录。
     *
     * @param item 需要删除的笔记实体。
     * @return 受影响的行数。
     */
    @Delete
    suspend fun delete(item: NoteItem): Int

    /**
     * 读取全部发现页分类，并按排序值升序返回。
     *
     * @return 分类实体列表。
     */
    @Query("SELECT * FROM discover_category ORDER BY sortOrder")
    suspend fun getDiscoverCategories(): List<DiscoverCategoryEntity>

    /**
     * 批量插入或替换发现页分类。
     *
     * @param items 需要写入的分类实体列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoverCategories(items: List<DiscoverCategoryEntity>)

    /**
     * 统计发现页分类数量。
     *
     * @return 分类记录总数。
     */
    @Query("SELECT COUNT(*) FROM discover_category")
    suspend fun getDiscoverCategoryCount(): Int

    /**
     * 按分区读取首页卡片。
     *
     * @param sectionKey 分区标识。
     * @return 当前分区下的全部卡片，按排序值升序返回。
     */
    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder")
    suspend fun getHomeCardsBySection(sectionKey: String): List<HomeCardEntity>

    /**
     * 按分区分页读取首页卡片。
     *
     * @param sectionKey 分区标识。
     * @param limit 单次读取数量。
     * @param offset 偏移量。
     * @return 当前分页范围内的首页卡片列表。
     */
    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder LIMIT :limit OFFSET :offset")
    suspend fun getPagedHomeCardsBySection(sectionKey: String, limit: Int, offset: Int): List<HomeCardEntity>

    /**
     * 批量插入或替换首页卡片。
     *
     * @param items 需要写入的首页卡片实体列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeCards(items: List<HomeCardEntity>)

    /**
     * 统计首页卡片缓存数量。
     *
     * @return 本地首页卡片总数。
     */
    @Query("SELECT COUNT(*) FROM home_card")
    suspend fun getHomeCardCount(): Int

    /**
     * 读取消息页入口列表。
     *
     * @return 按排序值升序排列的消息入口实体列表。
     */
    @Query("SELECT * FROM message_row ORDER BY sortOrder")
    suspend fun getMessageRows(): List<MessageRowEntity>

    /**
     * 批量插入或替换消息页入口。
     *
     * @param items 需要写入的消息入口实体列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageRows(items: List<MessageRowEntity>)

    /**
     * 统计消息入口数量。
     *
     * @return 消息入口记录总数。
     */
    @Query("SELECT COUNT(*) FROM message_row")
    suspend fun getMessageRowCount(): Int

    /**
     * 读取当前用户的个人资料。
     *
     * @return 当前用户资料；若尚未写入则返回 `null`。
     */
    @Query("SELECT * FROM my_profile WHERE id = 'self' LIMIT 1")
    suspend fun getMyProfile(): MyProfileEntity?

    /**
     * 插入或替换当前用户资料。
     *
     * @param item 需要保存的个人资料实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyProfile(item: MyProfileEntity)

    /**
     * 统计个人资料记录数量。
     *
     * @return 个人资料表中的记录总数。
     */
    @Query("SELECT COUNT(*) FROM my_profile")
    suspend fun getMyProfileCount(): Int

    /**
     * 按最近使用时间倒序读取搜索历史。
     *
     * @param limit 返回条数上限。
     * @return 搜索历史列表。
     */
    @Query("SELECT * FROM search_history ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getSearchHistory(limit: Int): List<SearchHistoryEntity>

    /**
     * 插入或覆盖一条搜索历史记录。
     *
     * @param item 需要保存的搜索历史实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryEntity)

    /**
     * 清空全部搜索历史。
     */
    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
