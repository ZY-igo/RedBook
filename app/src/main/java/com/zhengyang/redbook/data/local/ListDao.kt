/**
 * 文件说明： ListDao.kt
 * 作用： 封装本地数据访问能力，例如 Room、预置资源读取和初始化逻辑。
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

@Dao
interface ListDao {
    @Query("SELECT * FROM note_item")
    suspend fun getAll(): List<NoteItem>

    @Query("SELECT * FROM note_item LIMIT :limit OFFSET :offset")
    suspend fun getPagedNotes(limit: Int, offset: Int): List<NoteItem>

    @Query("SELECT COUNT(*) FROM note_item")
    suspend fun getNoteCount(): Int

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteItem>): List<Long>

    @Update
    suspend fun update(item: NoteItem): Int

    @Delete
    suspend fun delete(item: NoteItem): Int

    @Query("SELECT * FROM discover_category ORDER BY sortOrder")
    suspend fun getDiscoverCategories(): List<DiscoverCategoryEntity>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoverCategories(items: List<DiscoverCategoryEntity>)

    @Query("SELECT COUNT(*) FROM discover_category")
    suspend fun getDiscoverCategoryCount(): Int

    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder")
    suspend fun getHomeCardsBySection(sectionKey: String): List<HomeCardEntity>

    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder LIMIT :limit OFFSET :offset")
    suspend fun getPagedHomeCardsBySection(sectionKey: String, limit: Int, offset: Int): List<HomeCardEntity>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeCards(items: List<HomeCardEntity>)

    @Query("SELECT COUNT(*) FROM home_card")
    suspend fun getHomeCardCount(): Int

    @Query("SELECT * FROM message_row ORDER BY sortOrder")
    suspend fun getMessageRows(): List<MessageRowEntity>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageRows(items: List<MessageRowEntity>)

    @Query("SELECT COUNT(*) FROM message_row")
    suspend fun getMessageRowCount(): Int

    @Query("SELECT * FROM my_profile WHERE id = 'self' LIMIT 1")
    suspend fun getMyProfile(): MyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyProfile(item: MyProfileEntity)

    @Query("SELECT COUNT(*) FROM my_profile")
    suspend fun getMyProfileCount(): Int

    @Query("SELECT * FROM search_history ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getSearchHistory(limit: Int): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
