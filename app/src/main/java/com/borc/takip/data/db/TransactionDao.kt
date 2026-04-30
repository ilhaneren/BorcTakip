package com.borc.takip.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borc.takip.data.model.Transaction

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY createdAt DESC")
    fun getTransactionsByPerson(personId: Long): LiveData<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionReplace(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionsReplace(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY createdAt DESC")
    suspend fun getTransactionsByPersonSync(personId: Long): List<Transaction>

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
