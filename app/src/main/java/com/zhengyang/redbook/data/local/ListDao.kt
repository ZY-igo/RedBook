package com.zhengyang.redbook.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.model.PersonSuggestionEntity

@Dao
interface ListDao {
    @Query("SELECT * FROM note_item")
    suspend fun getAll(): List<NoteItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteItem>): List<Long>

    @Update
    suspend fun update(item: NoteItem): Int

    @Delete
    suspend fun delete(item: NoteItem): Int

    @Query("SELECT * FROM discover_category ORDER BY sortOrder")
    suspend fun getDiscoverCategories(): List<DiscoverCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoverCategories(items: List<DiscoverCategoryEntity>)

    @Query("SELECT COUNT(*) FROM discover_category")
    suspend fun getDiscoverCategoryCount(): Int

    @Query("SELECT * FROM home_card WHERE sectionKey = :sectionKey ORDER BY sortOrder")
    suspend fun getHomeCardsBySection(sectionKey: String): List<HomeCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeCards(items: List<HomeCardEntity>)

    @Query("SELECT COUNT(*) FROM home_card")
    suspend fun getHomeCardCount(): Int

    @Query("SELECT * FROM following_user ORDER BY sortOrder")
    suspend fun getFollowingUsers(): List<FollowingUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowingUsers(items: List<FollowingUserEntity>)

    @Query("SELECT COUNT(*) FROM following_user")
    suspend fun getFollowingUserCount(): Int

    @Query("SELECT * FROM message_row ORDER BY sortOrder")
    suspend fun getMessageRows(): List<MessageRowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageRows(items: List<MessageRowEntity>)

    @Query("SELECT COUNT(*) FROM message_row")
    suspend fun getMessageRowCount(): Int

    @Query("SELECT * FROM person_suggestion ORDER BY sortOrder")
    suspend fun getPersonSuggestions(): List<PersonSuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonSuggestions(items: List<PersonSuggestionEntity>)

    @Query("SELECT COUNT(*) FROM person_suggestion")
    suspend fun getPersonSuggestionCount(): Int

    @Query("SELECT * FROM my_profile WHERE id = 'self' LIMIT 1")
    suspend fun getMyProfile(): MyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyProfile(item: MyProfileEntity)

    @Query("SELECT COUNT(*) FROM my_profile")
    suspend fun getMyProfileCount(): Int

    @Query("SELECT * FROM interest_person ORDER BY sortOrder")
    suspend fun getInterestPeople(): List<InterestPersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestPeople(items: List<InterestPersonEntity>)

    @Query("SELECT COUNT(*) FROM interest_person")
    suspend fun getInterestPersonCount(): Int
}
