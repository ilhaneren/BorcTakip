package com.borc.takip.`data`.db

import androidx.lifecycle.LiveData
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.borc.takip.`data`.model.Person
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
public class PersonDao_Impl(
  __db: RoomDatabase,
) : PersonDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPerson: EntityInsertAdapter<Person>

  private val __insertAdapterOfPerson_1: EntityInsertAdapter<Person>

  private val __deleteAdapterOfPerson: EntityDeleteOrUpdateAdapter<Person>

  private val __updateAdapterOfPerson: EntityDeleteOrUpdateAdapter<Person>
  init {
    this.__db = __db
    this.__insertAdapterOfPerson = object : EntityInsertAdapter<Person>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `persons` (`id`,`name`,`normalizedName`,`totalBalance`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Person) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.normalizedName)
        statement.bindDouble(4, entity.totalBalance)
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
      }
    }
    this.__insertAdapterOfPerson_1 = object : EntityInsertAdapter<Person>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `persons` (`id`,`name`,`normalizedName`,`totalBalance`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Person) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.normalizedName)
        statement.bindDouble(4, entity.totalBalance)
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
      }
    }
    this.__deleteAdapterOfPerson = object : EntityDeleteOrUpdateAdapter<Person>() {
      protected override fun createQuery(): String = "DELETE FROM `persons` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Person) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfPerson = object : EntityDeleteOrUpdateAdapter<Person>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `persons` SET `id` = ?,`name` = ?,`normalizedName` = ?,`totalBalance` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Person) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.normalizedName)
        statement.bindDouble(4, entity.totalBalance)
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
        statement.bindLong(7, entity.id)
      }
    }
  }

  public override suspend fun insertPerson(person: Person): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfPerson.insertAndReturnId(_connection, person)
    _result
  }

  public override suspend fun insertPersonReplace(person: Person): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPerson_1.insert(_connection, person)
  }

  public override suspend fun deletePerson(person: Person): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfPerson.handle(_connection, person)
  }

  public override suspend fun updatePerson(person: Person): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfPerson.handle(_connection, person)
  }

  public override fun getAllPersons(): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons ORDER BY totalBalance DESC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun searchPersons(query: String): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons WHERE normalizedName LIKE '%' || ? || '%' ORDER BY totalBalance DESC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDebtors(): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons WHERE totalBalance > 0 ORDER BY totalBalance DESC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun searchDebtors(query: String): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons WHERE totalBalance > 0 AND normalizedName LIKE '%' || ? || '%' ORDER BY totalBalance DESC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCreditors(): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons WHERE totalBalance < 0 ORDER BY totalBalance ASC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun searchCreditors(query: String): LiveData<List<Person>> {
    val _sql: String = "SELECT * FROM persons WHERE totalBalance < 0 AND normalizedName LIKE '%' || ? || '%' ORDER BY totalBalance ASC, updatedAt DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPersonNames(): List<String> {
    val _sql: String = "SELECT name FROM persons ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPersonsSync(): List<Person> {
    val _sql: String = "SELECT * FROM persons ORDER BY totalBalance DESC, updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPersonById(id: Long): Person? {
    val _sql: String = "SELECT * FROM persons WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: Person?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPersonByIdLive(id: Long): LiveData<Person?> {
    val _sql: String = "SELECT * FROM persons WHERE id = ?"
    return __db.invalidationTracker.createLiveData(arrayOf("persons"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: Person?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPersonByExactName(normalizedName: String): Person? {
    val _sql: String = "SELECT * FROM persons WHERE normalizedName = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, normalizedName)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: Person?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findPersonsByName(query: String): List<Person> {
    val _sql: String = "SELECT * FROM persons WHERE normalizedName LIKE '%' || ? || '%' LIMIT 5"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfNormalizedName: Int = getColumnIndexOrThrow(_stmt, "normalizedName")
        val _columnIndexOfTotalBalance: Int = getColumnIndexOrThrow(_stmt, "totalBalance")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<Person> = mutableListOf()
        while (_stmt.step()) {
          val _item: Person
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpNormalizedName: String
          _tmpNormalizedName = _stmt.getText(_columnIndexOfNormalizedName)
          val _tmpTotalBalance: Double
          _tmpTotalBalance = _stmt.getDouble(_columnIndexOfTotalBalance)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = Person(_tmpId,_tmpName,_tmpNormalizedName,_tmpTotalBalance,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateBalance(
    personId: Long,
    amount: Double,
    timestamp: Long,
  ) {
    val _sql: String = "UPDATE persons SET totalBalance = totalBalance + ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindDouble(_argIndex, amount)
        _argIndex = 2
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 3
        _stmt.bindLong(_argIndex, personId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
