package com.titos.barcodescanner;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CrudMethods _crudMethods;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `transactionTable` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanned_item` INTEGER NOT NULL, `order_id` INTEGER NOT NULL, `order_date` TEXT NOT NULL, `order_time` TEXT NOT NULL, `barcode_string` TEXT NOT NULL, `item_name` TEXT NOT NULL, `item_qty` REAL NOT NULL, `item_price` REAL NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `inventoryTable` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scanned_item` INTEGER NOT NULL, `barcode_string` TEXT NOT NULL, `item_name` TEXT NOT NULL, `item_qty` REAL NOT NULL, `item_price` REAL NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '506f62395f581f899af07f0579081444')");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `transactionTable`");
        _db.execSQL("DROP TABLE IF EXISTS `inventoryTable`");
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onDestructiveMigration(_db);
          }
        }
      }

      @Override
      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
      }

      @Override
      public void onPostMigrate(SupportSQLiteDatabase _db) {
      }

      @Override
      protected RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsTransactionTable = new HashMap<String, TableInfo.Column>(9);
        _columnsTransactionTable.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("scanned_item", new TableInfo.Column("scanned_item", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("order_id", new TableInfo.Column("order_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("order_date", new TableInfo.Column("order_date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("order_time", new TableInfo.Column("order_time", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("barcode_string", new TableInfo.Column("barcode_string", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("item_name", new TableInfo.Column("item_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("item_qty", new TableInfo.Column("item_qty", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactionTable.put("item_price", new TableInfo.Column("item_price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactionTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactionTable = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactionTable = new TableInfo("transactionTable", _columnsTransactionTable, _foreignKeysTransactionTable, _indicesTransactionTable);
        final TableInfo _existingTransactionTable = TableInfo.read(_db, "transactionTable");
        if (! _infoTransactionTable.equals(_existingTransactionTable)) {
          return new RoomOpenHelper.ValidationResult(false, "transactionTable(com.titos.barcodescanner.TransactionTable).\n"
                  + " Expected:\n" + _infoTransactionTable + "\n"
                  + " Found:\n" + _existingTransactionTable);
        }
        final HashMap<String, TableInfo.Column> _columnsInventoryTable = new HashMap<String, TableInfo.Column>(6);
        _columnsInventoryTable.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventoryTable.put("scanned_item", new TableInfo.Column("scanned_item", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventoryTable.put("barcode_string", new TableInfo.Column("barcode_string", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventoryTable.put("item_name", new TableInfo.Column("item_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventoryTable.put("item_qty", new TableInfo.Column("item_qty", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInventoryTable.put("item_price", new TableInfo.Column("item_price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInventoryTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesInventoryTable = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoInventoryTable = new TableInfo("inventoryTable", _columnsInventoryTable, _foreignKeysInventoryTable, _indicesInventoryTable);
        final TableInfo _existingInventoryTable = TableInfo.read(_db, "inventoryTable");
        if (! _infoInventoryTable.equals(_existingInventoryTable)) {
          return new RoomOpenHelper.ValidationResult(false, "inventoryTable(com.titos.barcodescanner.InventoryTable).\n"
                  + " Expected:\n" + _infoInventoryTable + "\n"
                  + " Found:\n" + _existingInventoryTable);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "506f62395f581f899af07f0579081444", "a4304b316375219d50517c815f054c2e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactionTable","inventoryTable");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `transactionTable`");
      _db.execSQL("DELETE FROM `inventoryTable`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  public CrudMethods crudMethods() {
    if (_crudMethods != null) {
      return _crudMethods;
    } else {
      synchronized(this) {
        if(_crudMethods == null) {
          _crudMethods = new CrudMethods_Impl(this);
        }
        return _crudMethods;
      }
    }
  }
}
