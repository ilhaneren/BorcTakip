package com.borc.takip.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borc.takip.data.model.Person

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY totalBalance DESC, updatedAt DESC")
    fun getAllPersons(): LiveData<List<Person>>

    @Query("SELECT * FROM persons WHERE normalizedName LIKE '%' || :query || '%' ORDER BY totalBalance DESC, updatedAt DESC")
    fun searchPersons(query: String): LiveData<List<Person>>

    // Borçlular: totalBalance > 0 (bize borçlu)
    @Query("SELECT * FROM persons WHERE totalBalance > 0 ORDER BY totalBalance DESC, updatedAt DESC")
    fun getDebtors(): LiveData<List<Person>>

    @Query("SELECT * FROM persons WHERE totalBalance > 0 AND normalizedName LIKE '%' || :query || '%' ORDER BY totalBalance DESC, updatedAt DESC")
    fun searchDebtors(query: String): LiveData<List<Person>>

    // Alacaklılar: totalBalance < 0 (biz borçluyuz)
    @Query("SELECT * FROM persons WHERE totalBalance < 0 ORDER BY totalBalance ASC, updatedAt DESC")
    fun getCreditors(): LiveData<List<Person>>

    @Query("SELECT * FROM persons WHERE totalBalance < 0 AND normalizedName LIKE '%' || :query || '%' ORDER BY totalBalance ASC, updatedAt DESC")
    fun searchCreditors(query: String): LiveData<List<Person>>

    @Query("SELECT name FROM persons ORDER BY updatedAt DESC")
    suspend fun getAllPersonNames(): List<String>

    @Query("SELECT * FROM persons ORDER BY totalBalance DESC, updatedAt DESC")
    suspend fun getAllPersonsSync(): List<Person>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Long): Person?

    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonByIdLive(id: Long): LiveData<Person?>

    @Query("SELECT * FROM persons WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getPersonByExactName(normalizedName: String): Person?

    @Query("SELECT * FROM persons WHERE normalizedName LIKE '%' || :query || '%' LIMIT 5")
    suspend fun findPersonsByName(query: String): List<Person>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerson(person: Person): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonReplace(person: Person)

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("UPDATE persons SET totalBalance = totalBalance + :amount, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updateBalance(
        personId: Long,
        amount: Double,
        timestamp: Long = System.currentTimeMillis()
    )
}
