package io.legato.kazusa.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.legato.kazusa.constant.AppPattern
import io.legato.kazusa.data.entities.RssSource
import io.legato.kazusa.utils.cnCompare
import io.legato.kazusa.utils.splitNotBlank
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Dao
interface RssSourceDao {

    @Query("select * from rssSources where sourceUrl = :key")
    fun getByKey(key: String): RssSource?

    @Query("select * from rssSources where sourceUrl in (:sourceUrls)")
    fun getRssSources(vararg sourceUrls: String): List<RssSource>

    @get:Query("SELECT * FROM rssSources order by customOrder")
    val all: List<RssSource>

    @get:Query("select count(sourceUrl) from rssSources")
    val size: Int

    @Query("SELECT * FROM rssSources order by customOrder")
    fun flowAll(): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources
        where sourceName like '%' || :key || '%' 
        or sourceUrl like '%' || :key || '%' 
        or sourceGroup like '%' || :key || '%'
        or sourceComment like '%' || :key || '%'
        order by customOrder"""
    )
    fun flowSearch(key: String): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where (sourceGroup = :key
        or sourceGroup like :key || ',%' 
        or sourceGroup like  '%,' || :key
        or sourceGroup like  '%,' || :key || ',%')
        order by customOrder"""
    )
    fun flowGroupSearch(key: String): Flow<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1 order by customOrder")
    fun flowEnabled(): Flow<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 0 order by customOrder")
    fun flowDisabled(): Flow<List<RssSource>>

    @Query("select * from rssSources where loginUrl is not null and loginUrl != ''")
    fun flowLogin(): Flow<List<RssSource>>

    @Query("select * from rssSources where sourceGroup is null or sourceGroup = '' or sourceGroup like '%未分组%'")
    fun flowNoGroup(): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where enabled = 1 
        and (sourceName like '%' || :searchKey || '%' 
            or sourceGroup like '%' || :searchKey || '%' 
            or sourceUrl like '%' || :searchKey || '%'
            or sourceComment like '%' || :searchKey || '%') 
        order by customOrder"""
    )
    fun flowEnabled(searchKey: String): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where enabled = 1 and (sourceGroup = :searchKey
        or sourceGroup like :searchKey || ',%' 
        or sourceGroup like  '%,' || :searchKey
        or sourceGroup like  '%,' || :searchKey || ',%') 
        order by customOrder"""
    )
    fun flowEnabledByGroup(searchKey: String): Flow<List<RssSource>>

    @Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> ''")
    fun flowGroupsUnProcessed(): Flow<List<String>>

    @Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> '' and enabled = 1")
    fun flowEnabledGroupsUnProcessed(): Flow<List<String>>

    @get:Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> ''")
    val allGroupsUnProcessed: List<String>

    @get:Query("select min(customOrder) from rssSources")
    val minOrder: Int

    @get:Query("select max(customOrder) from rssSources")
    val maxOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssSource: RssSource)

    @Update
    fun update(vararg rssSource: RssSource)

    @Delete
    fun delete(vararg rssSource: RssSource)

    @Query("delete from rssSources where sourceUrl = :sourceUrl")
    fun delete(sourceUrl: String)

    @Query("delete from rssSources where sourceGroup like 'legado'")
    fun deleteDefault()

    @get:Query("select * from rssSources where sourceGroup is null or sourceGroup = ''")
    val noGroup: List<RssSource>

    @Query("select * from rssSources where sourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<RssSource>

    @Query("select exists(select 1 from rssSources where sourceUrl = :key)")
    fun has(key: String): Boolean

    @Query("update rssSources set enabled = :enable where sourceUrl = :sourceUrl")
    fun enable(sourceUrl: String, enable: Boolean)

    private fun dealGroups(list: List<String>): List<String> {
        val groups = linkedSetOf<String>()
        list.forEach {
            it.splitNotBlank(AppPattern.splitGroupRegex).forEach { group ->
                groups.add(group)
            }
        }
        return groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }
    }

    fun allGroups(): List<String> = dealGroups(allGroupsUnProcessed)

    fun flowGroups(): Flow<List<String>> {
        return flowGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }

    fun flowEnabledGroups(): Flow<List<String>> {
        return flowEnabledGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }

}
