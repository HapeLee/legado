package io.legato.kazusa.data.dao

import androidx.room.*
import io.legato.kazusa.data.entities.RssStar
import kotlinx.coroutines.flow.Flow

@Dao
interface RssStarDao {

    @get:Query("select * from rssStars order by starTime desc")
    val all: List<RssStar>

    @Query("select `group` from rssStars group by `group` order by `group`")
    fun flowGroups(): Flow<List<String>>

    @Query("select * from rssStars where `group` = :group order by starTime desc")
    fun flowByGroup(group: String): Flow<List<RssStar>>

    @Query("select * from rssStars where origin = :origin and link = :link")
    fun get(origin: String, link: String): RssStar?

    @Query("select * from rssStars order by starTime desc")
    fun liveAll(): Flow<List<RssStar>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssStar: RssStar)

    @Update
    fun update(vararg rssStar: RssStar)

    @Query("update rssStars set origin = :origin where origin = :oldOrigin")
    fun updateOrigin(origin: String, oldOrigin: String)

    @Query("delete from rssStars where origin = :origin")
    fun delete(origin: String)

    @Query("delete from rssStars where origin = :origin and link = :link")
    fun delete(origin: String, link: String)

    @Query("delete from rssStars where `group` = :group")
    fun deleteByGroup(group: String)

    @Query("delete from rssStars")
    fun deleteAll()
}