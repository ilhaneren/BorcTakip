package com.borc.takip.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _personDao: Lazy<PersonDao> = lazy {
    PersonDao_Impl(this)
  }

  private val _transactionDao: Lazy<TransactionDao> = lazy {
    TransactionDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "f12fc721e7bb37721da478fb60b2d01d", "614c67ec918cbfbcc54e3e163db7bfd3") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `persons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `totalBalance` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personId` INTEGER NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `note` TEXT NOT NULL, `voiceText` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_personId` ON `transactions` (`personId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f12fc721e7bb37721da478fb60b2d01d')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `persons`")
        connection.execSQL("DROP TABLE IF EXISTS `transactions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsPersons: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPersons.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPersons.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPersons.put("normalizedName", TableInfo.Column("normalizedName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPersons.put("totalBalance", TableInfo.Column("totalBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPersons.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPersons.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPersons: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPersons: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPersons: TableInfo = TableInfo("persons", _columnsPersons, _foreignKeysPersons, _indicesPersons)
        val _existingPersons: TableInfo = read(connection, "persons")
        if (!_infoPersons.equals(_existingPersons)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |persons(com.borc.takip.data.model.Person).
              | Expected:
              |""".trimMargin() + _infoPersons + """
              |
              | Found:
              |""".trimMargin() + _existingPersons)
        }
        val _columnsTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("personId", TableInfo.Column("personId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("amount", TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("note", TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("voiceText", TableInfo.Column("voiceText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysTransactions.add(TableInfo.ForeignKey("persons", "CASCADE", "NO ACTION", listOf("personId"), listOf("id")))
        val _indicesTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTransactions.add(TableInfo.Index("index_transactions_personId", false, listOf("personId"), listOf("ASC")))
        val _infoTransactions: TableInfo = TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions)
        val _existingTransactions: TableInfo = read(connection, "transactions")
        if (!_infoTransactions.equals(_existingTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transactions(com.borc.takip.data.model.Transaction).
              | Expected:
              |""".trimMargin() + _infoTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingTransactions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "persons", "transactions")
  }

  public override fun clearAllTables() {
    super.performClear(true, "persons", "transactions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PersonDao::class, PersonDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TransactionDao::class, TransactionDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun personDao(): PersonDao = _personDao.value

  public override fun transactionDao(): TransactionDao = _transactionDao.value
}
