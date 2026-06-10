/**
 * 文件说明：ListDao.kt
 * 作用：定义 Room 数据库的数据访问接口，封装所有表的 CRUD 操作。
 * 备注：所有方法均为 suspend 函数，需在协程中调用。
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
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.PersonSuggestionEntity
import com.zhengyang.redbook.data.model.SearchGuessEntity
import com.zhengyang.redbook.data.model.SearchHistoryEntity
import com.zhengyang.redbook.data.model.SearchResultEntity

/**
 * 数据访问对象接口。
 * 
 * 定义了所有数据库表的操作方法，包括：
 * - note_item（笔记表）
 * - discover_category（发现页分类表）
 * - home_card（首页卡片表）
 * - following_user（关注用户表）
 * - message_row（消息行表）
 * - person_suggestion（用户推荐表）
 * - my_profile（个人资料表）
 * - interest_person（感兴趣的人表）
 * - search_history（搜索历史表）
 * - search_guess（搜索推荐表）
 * - search_result（搜索结果表）
 */
@Dao
interface ListDao {

    // ==================== 笔记表操作 ====================

    /**
     * 查询所有笔记。
     * @return 笔记列表，按默认顺序排列。
     */
    @Query("SELECT * FROM note_item")
    suspend fun getAll(): List<NoteItem>

    /**
     * 分页查询笔记。
     * @param limit 每页数量。
     * @param offset 偏移量（跳过前 offset 条）。
     * @return 分页后的笔记列表。
     */
    @Query("SELECT * FROM note_item LIMIT :limit OFFSET :offset")
    suspend fun getPagedNotes(limit: Int, offset: Int): List<NoteItem>

    /**
     * 获取笔记总数。
     * @return 笔记数量。
     */
    @Query("SELECT COUNT(*) FROM note_item")
    suspend fun getNoteCount(): Int

    /**
     * 批量插入笔记。
     * @param items 要插入的笔记列表。
     * @return 插入后的行 ID 列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteItem>): List<Long>

    /**
     * 更新笔记。
     * @param item 要更新的笔记。
     * @return 更新的行数。
     */
    @Update
    suspend fun update(item: NoteItem): Int

    /**
     * 删除笔记。
     * @param item 要删除的笔记。
     * @return 删除的行数。
     */
    @Delete
    suspend fun delete(item: NoteItem): Int

    // ==================== 发现页分类表操作 ====================

    /**
     * 查询所有发现页分类，按排序字段排序。
     * @return 分类列表，按 sortOrder 升序排列。
     */
    @Query("SELECT * FROM discover_category ORDER BY sortOrder")
    suspend fun getDiscoverCategories(): List<DiscoverCategoryEntity>

    /**
     * 批量插入分类。
     * @param items 要插入的分类列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoverCategories(items: List<DiscoverCategoryEntity>)

    /**
     * 清空所有分类。
     */
    @Query("DELETE FROM discover_category")
    suspend fun clearDiscoverCategories()

    /**
     * 获取分类总数。
     * @return 分类数量。
     */
    @Query("SELECT COUNT(*) FROM discover_category")
    suspend fun getDiscoverCategoryCount(): Int

    // ==================== 首页卡片表操作 ====================

    /**
     * 根据分区键查询首页卡片。
     * @param sectionKey 分区键（如 "discover"、"following"）。
     * @return 该分区的卡片列表，按排序字段排列。
     */
    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder")
    suspend fun getHomeCardsBySection(sectionKey: String): List<HomeCardEntity>

    /**
     * 分页查询指定分区的首页卡片。
     * @param sectionKey 分区键。
     * @param limit 每页数量。
     * @param offset 偏移量。
     * @return 分页后的卡片列表。
     */
    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder LIMIT :limit OFFSET :offset")
    suspend fun getPagedHomeCardsBySection(sectionKey: String, limit: Int, offset: Int): List<HomeCardEntity>

    /**
     * 批量插入首页卡片。
     * @param items 要插入的卡片列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeCards(items: List<HomeCardEntity>)

    /**
     * 删除指定分区的所有卡片。
     * @param sectionKey 分区键。
     */
    @Query("DELETE FROM home_card WHERE sectionKey = :sectionKey")
    suspend fun deleteHomeCardsBySection(sectionKey: String)

    /**
     * 获取首页卡片总数。
     * @return 卡片数量。
     */
    @Query("SELECT COUNT(*) FROM home_card")
    suspend fun getHomeCardCount(): Int

    // ==================== 关注用户表操作 ====================

    /**
     * 查询所有关注用户，按排序字段排序。
     * @return 关注用户列表。
     */
    @Query("SELECT * FROM following_user ORDER BY sortOrder")
    suspend fun getFollowingUsers(): List<FollowingUserEntity>

    /**
     * 批量插入关注用户。
     * @param items 要插入的用户列表。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowingUsers(items: List<FollowingUserEntity>)

    /**
     * 清空所有关注用户。
     */
    @Query("DELETE FROM following_user")
    suspend fun clearFollowingUsers()

    // ==================== 消息行表操作 ====================

    /**
     * 查询所有消息行，按排序字段排序。
     * @return 消息行列表。
     */
    @Query("SELECT * FROM message_row ORDER BY sortOrder")
    suspend fun getMessageRows(): List<MessageRowEntity>

    /**
     * 批量插入消息行。
     * @param items 要插入的消息行列表。
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageRows(items: List<MessageRowEntity>)

    /**
     * 清空所有消息行。
     */
    @Query("DELETE FROM message_row")
    suspend fun clearMessageRows()

    /**
     * 获取消息行总数。
     * @return 消息行数量。
     */
    @Query("SELECT COUNT(*) FROM message_row")
    suspend fun getMessageRowCount(): Int

    // ==================== 用户推荐表操作 ====================

    /**
     * 查询所有用户推荐，按排序字段排序。
     * @return 用户推荐列表。
     */
    @Query("SELECT * FROM person_suggestion ORDER BY sortOrder")
    suspend fun getPersonSuggestions(): List<PersonSuggestionEntity>

    /**
     * 批量插入用户推荐。
     * @param items 要插入的推荐列表。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonSuggestions(items: List<PersonSuggestionEntity>)

    /**
     * 清空所有用户推荐。
     */
    @Query("DELETE FROM person_suggestion")
    suspend fun clearPersonSuggestions()

    // ==================== 个人资料表操作 ====================

    /**
     * 查询个人资料（单条记录）。
     * @return 个人资料实体，可能为 null（未登录时）。
     */
    @Query("SELECT * FROM my_profile LIMIT 1")
    suspend fun getMyProfile(): MyProfileEntity?

    /**
     * 插入或更新个人资料。
     * @param item 个人资料实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyProfile(item: MyProfileEntity)

    /**
     * 获取个人资料记录数（0 或 1）。
     * @return 记录数。
     */
    @Query("SELECT COUNT(*) FROM my_profile")
    suspend fun getMyProfileCount(): Int

    // ==================== 感兴趣的人表操作 ====================

    /**
     * 查询所有感兴趣的人，按排序字段排序。
     * @return 感兴趣的人列表。
     */
    @Query("SELECT * FROM interest_person ORDER BY sortOrder")
    suspend fun getInterestPeople(): List<InterestPersonEntity>

    /**
     * 批量插入感兴趣的人。
     * @param items 要插入的列表。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestPeople(items: List<InterestPersonEntity>)

    /**
     * 清空所有感兴趣的人。
     */
    @Query("DELETE FROM interest_person")
    suspend fun clearInterestPeople()

    // ==================== 搜索历史表操作 ====================

    /**
     * 查询搜索历史，按更新时间倒序排列。
     * @param limit 返回条数限制。
     * @return 搜索历史列表（最新的在前）。
     */
    @Query("SELECT * FROM search_history ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getSearchHistory(limit: Int): List<SearchHistoryEntity>

    /**
     * 插入搜索历史（自动去重，同 query 会替换）。
     * @param item 搜索历史实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryEntity)

    /**
     * 清空所有搜索历史。
     */
    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // ==================== 搜索推荐表操作 ====================

    /**
     * 查询搜索推荐词，按排序字段排序。
     * @return 搜索推荐列表。
     */
    @Query("SELECT * FROM search_guess ORDER BY sortOrder")
    suspend fun getSearchGuessItems(): List<SearchGuessEntity>

    /**
     * 批量插入搜索推荐词。
     * @param items 要插入的推荐词列表。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchGuessItems(items: List<SearchGuessEntity>)

    /**
     * 清空所有搜索推荐词。
     */
    @Query("DELETE FROM search_guess")
    suspend fun clearSearchGuessItems()

    // ==================== 搜索结果表操作 ====================

    /**
     * 根据关键词查询搜索结果。
     * @param keyword 搜索关键词。
     * @return 该关键词对应的搜索结果列表。
     */
    @Query("SELECT * FROM search_result WHERE keyword = :keyword ORDER BY sortOrder")
    suspend fun getSearchResults(keyword: String): List<SearchResultEntity>

    /**
     * 批量插入搜索结果。
     * @param items 要插入的结果列表。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(items: List<SearchResultEntity>)

    /**
     * 清空指定关键词的搜索结果。
     * @param keyword 搜索关键词。
     */
    @Query("DELETE FROM search_result WHERE keyword = :keyword")
    suspend fun clearSearchResults(keyword: String)

    // ==================== 事务性替换操作 ====================

    /**
     * 替换所有分类（先清空再插入）。
     * @param items 新的分类列表。
     */
    @Transaction
    suspend fun replaceDiscoverCategories(items: List<DiscoverCategoryEntity>) {
        clearDiscoverCategories()
        insertDiscoverCategories(items)
    }

    /**
     * 替换指定分区的首页卡片（先清空再插入）。
     * @param sectionKey 分区键。
     * @param items 新的卡片列表。
     */
    @Transaction
    suspend fun replaceHomeCardsBySection(sectionKey: String, items: List<HomeCardEntity>) {
        deleteHomeCardsBySection(sectionKey)
        insertHomeCards(items)
    }

    /**
     * 替换所有关注用户（先清空再插入）。
     * @param items 新的关注用户列表。
     */
    @Transaction
    suspend fun replaceFollowingUsers(items: List<FollowingUserEntity>) {
        clearFollowingUsers()
        insertFollowingUsers(items)
    }

    /**
     * 替换所有消息行（先清空再插入）。
     * @param items 新的消息行列表。
     */
    @Transaction
    suspend fun replaceMessageRows(items: List<MessageRowEntity>) {
        clearMessageRows()
        insertMessageRows(items)
    }

    /**
     * 替换所有用户推荐（先清空再插入）。
     * @param items 新的推荐列表。
     */
    @Transaction
    suspend fun replacePersonSuggestions(items: List<PersonSuggestionEntity>) {
        clearPersonSuggestions()
        insertPersonSuggestions(items)
    }

    /**
     * 替换所有感兴趣的人（先清空再插入）。
     * @param items 新的列表。
     */
    @Transaction
    suspend fun replaceInterestPeople(items: List<InterestPersonEntity>) {
        clearInterestPeople()
        insertInterestPeople(items)
    }

    /**
     * 替换所有搜索推荐词（先清空再插入）。
     * @param items 新的推荐词列表。
     */
    @Transaction
    suspend fun replaceSearchGuessItems(items: List<SearchGuessEntity>) {
        clearSearchGuessItems()
        insertSearchGuessItems(items)
    }

    /**
     * 替换指定关键词的搜索结果（先清空再插入）。
     * @param keyword 搜索关键词。
     * @param items 新的结果列表。
     */
    @Transaction
    suspend fun replaceSearchResults(keyword: String, items: List<SearchResultEntity>) {
        clearSearchResults(keyword)
        insertSearchResults(items)
    }
}