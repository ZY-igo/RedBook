package com.zhengyang.redbook.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zhengyang.redbook.data.model.NoteItem

@Dao
interface ListDao {
    @Query("SELECT * FROM note_item")
    suspend fun getAll(): List<NoteItem>

    @Insert
    suspend fun insertAll(items: List<NoteItem>)

    @Update
    suspend fun update(item: NoteItem)

    @Delete
    suspend fun delete(item: NoteItem)
}
