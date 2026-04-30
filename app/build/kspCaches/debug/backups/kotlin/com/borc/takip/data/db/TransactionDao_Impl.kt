package com.borc.takip.`data`.db

import androidx.lifecycle.LiveData
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.borc.takip.`data`.model.Transaction
import com.borc.takip.`data`.model.TransactionType
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TransactionDao_Impl(
  __db: RoomDatabase,
) : TransactionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransaction: EntityInsertAdapter<Transaction>

  private val __converters: Converters = Converters()

  private val __insertAdapterOfTransaction_1: EntityInsertAdapter<Transaction>

  private val __deleteAdapterOfTransaction: EntityDeleteOrUpdateAdapter<Transaction>
  init {
    this.__db = __db
    this.__insertAdapterOfTransaction = object : EntityInsertAdapter<Transaction>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `transactions` (`id`,`personId`,`amount`,`type`,`note`,`voiceText`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Transaction) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.personId)
        statement.bindDouble(3, entity.amount)
        val _tmp: String = __converters.fromTransactionType(entity.type)
        statement.bindText(4, _tmp)
        statement.bindText(5, entity.note)
        statement.bindText(6, entity.voiceText)
        statement.bindLong(7, entity.createdAt)
      }
    }
    this.__insertAdapterOfTransaction_1 = object : EntityInsertAdapter<Transaction>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `transactions` (`id`,`personId`,`amount`,`type`,`note`,`voiceText`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Transaction) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.personId)
        statement.bindDouble(3, entity.amount)
        val _tmp: String = __converters.fromTransactionType(entity.type)
        statement.bindText(4, _tmp)
        statement.bindText(5, entity.note)
        statement.bindText(6, entity.voiceText)
        statement.bindLong(7, entity.createdAt)
      }
    }
    this.__deleteAdapterOfTransaction = object : EntityDeleteOrUpdateAdapter<Transaction>() {
      protected override fun createQuery(): String = "DELETE FROM `transactions` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Transaction) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insertTransaction(transaction: Transaction): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfTransaction.insertAndReturnId(_connection, transaction)
    _result
  }

  public override suspend fun insertTransactionReplace(transaction: Transaction): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransaction_1.insert(_connection, transaction)
  }

  public override suspend fun insertTransactionsReplace(transactions: List<Transaction>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransaction_1.insert(_connection, transactions)
  }

  public override suspend fun deleteTransaction(transaction: Transaction): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfTransaction.handle(_connection, transaction)
  }

  public override fun getTransactionsByPerson(personId: Long): LiveData<List<Transaction>> {
    val _sql: String = "SELECT * FROM transactions WHERE personId = ? ORDER BY createdAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("transactions"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, personId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPersonId: Int = getColumnIndexOrThrow(_stmt, "personId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfVoiceText: Int = getColumnIndexOrThrow(_stmt, "voiceText")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<Transaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: Transaction
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPersonId: Long
          _tmpPersonId = _stmt.getLong(_columnIndexOfPersonId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpType: TransactionType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.toTransactionType(_tmp)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpVoiceText: String
          _tmpVoiceText = _stmt.getText(_columnIndexOfVoiceText)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = Transaction(_tmpId,_tmpPersonId,_tmpAmount,_tmpType,_tmpNote,_tmpVoiceText,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTransactionsByPersonSync(personId: Long): List<Transaction> {
    val _sql: String = "SELECT * FROM transactions WHERE personId = ? ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, personId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPersonId: Int = getColumnIndexOrThrow(_stmt, "personId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfVoiceText: Int = getColumnIndexOrThrow(_stmt, "voiceText")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<Transaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: Transaction
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPersonId: Long
          _tmpPersonId = _stmt.getLong(_columnIndexOfPersonId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpType: TransactionType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfType)
          _tmpType = __converters.toTransactionType(_tmp)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpVoiceText: String
          _tmpVoiceText = _stmt.getText(_columnIndexOfVoiceText)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = Transaction(_tmpId,_tmpPersonId,_tmpAmount,_tmpType,_tmpNote,_tmpVoiceText,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
