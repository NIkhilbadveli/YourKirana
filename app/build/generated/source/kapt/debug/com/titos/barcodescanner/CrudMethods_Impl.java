package com.titos.barcodescanner;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class CrudMethods_Impl implements CrudMethods {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionTable> __insertionAdapterOfTransactionTable;

  private final EntityInsertionAdapter<InventoryTable> __insertionAdapterOfInventoryTable;

  private final EntityDeletionOrUpdateAdapter<TransactionTable> __updateAdapterOfTransactionTable;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTransaction;

  private final SharedSQLiteStatement __preparedStmtOfUpdateQuantity;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePrice;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLoosePrice;

  private final SharedSQLiteStatement __preparedStmtOfUpdateName;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBarcodeItem;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLooseItem;

  public CrudMethods_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionTable = new EntityInsertionAdapter<TransactionTable>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `transactionTable` (`id`,`scanned_item`,`order_id`,`order_date`,`order_time`,`barcode_string`,`item_name`,`item_qty`,`item_price`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, TransactionTable value) {
        stmt.bindLong(1, value.getId());
        final int _tmp;
        _tmp = value.getScannedItems() ? 1 : 0;
        stmt.bindLong(2, _tmp);
        stmt.bindLong(3, value.getOrderId());
        if (value.getOrderDate() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getOrderDate());
        }
        if (value.getOrderTime() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getOrderTime());
        }
        if (value.getBarcode() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getBarcode());
        }
        if (value.getItemName() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.getItemName());
        }
        stmt.bindDouble(8, value.getItemQty());
        stmt.bindDouble(9, value.getItemPrice());
      }
    };
    this.__insertionAdapterOfInventoryTable = new EntityInsertionAdapter<InventoryTable>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `inventoryTable` (`id`,`scanned_item`,`barcode_string`,`item_name`,`item_qty`,`item_price`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, InventoryTable value) {
        stmt.bindLong(1, value.getId());
        final int _tmp;
        _tmp = value.getScannedItem() ? 1 : 0;
        stmt.bindLong(2, _tmp);
        if (value.getBarcode() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getBarcode());
        }
        if (value.getItemName() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getItemName());
        }
        stmt.bindDouble(5, value.getItemQty());
        stmt.bindDouble(6, value.getItemPrice());
      }
    };
    this.__updateAdapterOfTransactionTable = new EntityDeletionOrUpdateAdapter<TransactionTable>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `transactionTable` SET `id` = ?,`scanned_item` = ?,`order_id` = ?,`order_date` = ?,`order_time` = ?,`barcode_string` = ?,`item_name` = ?,`item_qty` = ?,`item_price` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, TransactionTable value) {
        stmt.bindLong(1, value.getId());
        final int _tmp;
        _tmp = value.getScannedItems() ? 1 : 0;
        stmt.bindLong(2, _tmp);
        stmt.bindLong(3, value.getOrderId());
        if (value.getOrderDate() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getOrderDate());
        }
        if (value.getOrderTime() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getOrderTime());
        }
        if (value.getBarcode() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getBarcode());
        }
        if (value.getItemName() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.getItemName());
        }
        stmt.bindDouble(8, value.getItemQty());
        stmt.bindDouble(9, value.getItemPrice());
        stmt.bindLong(10, value.getId());
      }
    };
    this.__preparedStmtOfDeleteTransaction = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM transactionTable WHERE order_id=?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateQuantity = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE inventoryTable SET item_qty=? where barcode_string=?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePrice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE inventoryTable SET item_price=? where barcode_string=?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLoosePrice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE inventoryTable SET item_price=? where barcode_string='loose' AND item_name=?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateName = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE inventoryTable SET item_name=? where barcode_string=?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBarcodeItem = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM inventoryTable WHERE barcode_string=?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteLooseItem = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM inventoryTable WHERE item_name=? AND barcode_string='loose'";
        return _query;
      }
    };
  }

  @Override
  public void insertItem(final TransactionTable... item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTransactionTable.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertInventoryItem(final InventoryTable... item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfInventoryTable.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateItem(final TransactionTable... item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfTransactionTable.handleMultiple(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteTransaction(final int orderId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTransaction.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, orderId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteTransaction.release(_stmt);
    }
  }

  @Override
  public void updateQuantity(final String barcode, final double qty) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateQuantity.acquire();
    int _argIndex = 1;
    _stmt.bindDouble(_argIndex, qty);
    _argIndex = 2;
    if (barcode == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, barcode);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateQuantity.release(_stmt);
    }
  }

  @Override
  public void updatePrice(final String barcode, final double price) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePrice.acquire();
    int _argIndex = 1;
    _stmt.bindDouble(_argIndex, price);
    _argIndex = 2;
    if (barcode == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, barcode);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdatePrice.release(_stmt);
    }
  }

  @Override
  public void updateLoosePrice(final String itemName, final double price) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLoosePrice.acquire();
    int _argIndex = 1;
    _stmt.bindDouble(_argIndex, price);
    _argIndex = 2;
    if (itemName == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, itemName);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateLoosePrice.release(_stmt);
    }
  }

  @Override
  public void updateName(final String barcode, final String name) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateName.acquire();
    int _argIndex = 1;
    if (name == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, name);
    }
    _argIndex = 2;
    if (barcode == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, barcode);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateName.release(_stmt);
    }
  }

  @Override
  public void deleteBarcodeItem(final String barcode) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBarcodeItem.acquire();
    int _argIndex = 1;
    if (barcode == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, barcode);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteBarcodeItem.release(_stmt);
    }
  }

  @Override
  public void deleteLooseItem(final String itemName) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLooseItem.acquire();
    int _argIndex = 1;
    if (itemName == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, itemName);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteLooseItem.release(_stmt);
    }
  }

  @Override
  public List<TransactionTable> getAll() {
    final String _sql = "SELECT * FROM transactionTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfScannedItems = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_item");
      final int _cursorIndexOfOrderId = CursorUtil.getColumnIndexOrThrow(_cursor, "order_id");
      final int _cursorIndexOfOrderDate = CursorUtil.getColumnIndexOrThrow(_cursor, "order_date");
      final int _cursorIndexOfOrderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "order_time");
      final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode_string");
      final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "item_name");
      final int _cursorIndexOfItemQty = CursorUtil.getColumnIndexOrThrow(_cursor, "item_qty");
      final int _cursorIndexOfItemPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "item_price");
      final List<TransactionTable> _result = new ArrayList<TransactionTable>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final TransactionTable _item;
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        final boolean _tmpScannedItems;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfScannedItems);
        _tmpScannedItems = _tmp != 0;
        final int _tmpOrderId;
        _tmpOrderId = _cursor.getInt(_cursorIndexOfOrderId);
        final String _tmpOrderDate;
        _tmpOrderDate = _cursor.getString(_cursorIndexOfOrderDate);
        final String _tmpOrderTime;
        _tmpOrderTime = _cursor.getString(_cursorIndexOfOrderTime);
        final String _tmpBarcode;
        _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
        final String _tmpItemName;
        _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
        final double _tmpItemQty;
        _tmpItemQty = _cursor.getDouble(_cursorIndexOfItemQty);
        final double _tmpItemPrice;
        _tmpItemPrice = _cursor.getDouble(_cursorIndexOfItemPrice);
        _item = new TransactionTable(_tmpId,_tmpScannedItems,_tmpOrderId,_tmpOrderDate,_tmpOrderTime,_tmpBarcode,_tmpItemName,_tmpItemQty,_tmpItemPrice);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getMaxOrderId() {
    final String _sql = "SELECT MAX(order_id) FROM transactionTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getTotalSales() {
    final String _sql = "SELECT SUM(item_price), COUNT(DISTINCT order_id) FROM transactionTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getTotalOrders() {
    final String _sql = "SELECT COUNT(DISTINCT order_id) FROM transactionTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<OrderDetails> getAllOrdersDetails() {
    final String _sql = "SELECT  order_id as orderId, order_date as orderDate, order_time as orderTime, SUM(item_price) as value FROM transactionTable GROUP BY order_date, order_time";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfOrderId = CursorUtil.getColumnIndexOrThrow(_cursor, "orderId");
      final int _cursorIndexOfOrderDate = CursorUtil.getColumnIndexOrThrow(_cursor, "orderDate");
      final int _cursorIndexOfOrderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "orderTime");
      final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
      final List<OrderDetails> _result = new ArrayList<OrderDetails>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final OrderDetails _item;
        final int _tmpOrderId;
        _tmpOrderId = _cursor.getInt(_cursorIndexOfOrderId);
        final String _tmpOrderDate;
        _tmpOrderDate = _cursor.getString(_cursorIndexOfOrderDate);
        final String _tmpOrderTime;
        _tmpOrderTime = _cursor.getString(_cursorIndexOfOrderTime);
        final double _tmpValue;
        _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
        _item = new OrderDetails(_tmpOrderId,_tmpOrderDate,_tmpOrderTime,_tmpValue);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<DaySales> getAllDaysSales() {
    final String _sql = "SELECT order_date as orderDate, SUM(item_price) as sales FROM transactionTable GROUP BY order_date";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfOrderDate = CursorUtil.getColumnIndexOrThrow(_cursor, "orderDate");
      final int _cursorIndexOfSales = CursorUtil.getColumnIndexOrThrow(_cursor, "sales");
      final List<DaySales> _result = new ArrayList<DaySales>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final DaySales _item;
        final String _tmpOrderDate;
        _tmpOrderDate = _cursor.getString(_cursorIndexOfOrderDate);
        final double _tmpSales;
        _tmpSales = _cursor.getDouble(_cursorIndexOfSales);
        _item = new DaySales(_tmpOrderDate,_tmpSales);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<ItemQtyAndSales> getAllItemsSales() {
    final String _sql = "SELECT item_name as itemName, SUM(item_qty) as qty, SUM(item_price) as sales FROM transactionTable GROUP BY item_name ORDER BY SUM(item_price) DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "itemName");
      final int _cursorIndexOfQty = CursorUtil.getColumnIndexOrThrow(_cursor, "qty");
      final int _cursorIndexOfSales = CursorUtil.getColumnIndexOrThrow(_cursor, "sales");
      final List<ItemQtyAndSales> _result = new ArrayList<ItemQtyAndSales>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final ItemQtyAndSales _item;
        final String _tmpItemName;
        _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
        final double _tmpQty;
        _tmpQty = _cursor.getDouble(_cursorIndexOfQty);
        final double _tmpSales;
        _tmpSales = _cursor.getDouble(_cursorIndexOfSales);
        _item = new ItemQtyAndSales(_tmpItemName,_tmpQty,_tmpSales);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getThisWeekSales(final String startDate, final String endDate) {
    final String _sql = "SELECT SUM(item_price) as sales FROM transactionTable WHERE order_date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, startDate);
    }
    _argIndex = 2;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, endDate);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<TransactionTable> getAllItemsByOrder(final int orderNumber) {
    final String _sql = "SELECT * FROM transactionTable WHERE order_id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, orderNumber);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfScannedItems = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_item");
      final int _cursorIndexOfOrderId = CursorUtil.getColumnIndexOrThrow(_cursor, "order_id");
      final int _cursorIndexOfOrderDate = CursorUtil.getColumnIndexOrThrow(_cursor, "order_date");
      final int _cursorIndexOfOrderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "order_time");
      final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode_string");
      final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "item_name");
      final int _cursorIndexOfItemQty = CursorUtil.getColumnIndexOrThrow(_cursor, "item_qty");
      final int _cursorIndexOfItemPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "item_price");
      final List<TransactionTable> _result = new ArrayList<TransactionTable>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final TransactionTable _item;
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        final boolean _tmpScannedItems;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfScannedItems);
        _tmpScannedItems = _tmp != 0;
        final int _tmpOrderId;
        _tmpOrderId = _cursor.getInt(_cursorIndexOfOrderId);
        final String _tmpOrderDate;
        _tmpOrderDate = _cursor.getString(_cursorIndexOfOrderDate);
        final String _tmpOrderTime;
        _tmpOrderTime = _cursor.getString(_cursorIndexOfOrderTime);
        final String _tmpBarcode;
        _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
        final String _tmpItemName;
        _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
        final double _tmpItemQty;
        _tmpItemQty = _cursor.getDouble(_cursorIndexOfItemQty);
        final double _tmpItemPrice;
        _tmpItemPrice = _cursor.getDouble(_cursorIndexOfItemPrice);
        _item = new TransactionTable(_tmpId,_tmpScannedItems,_tmpOrderId,_tmpOrderDate,_tmpOrderTime,_tmpBarcode,_tmpItemName,_tmpItemQty,_tmpItemPrice);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getQuantity(final String barcode) {
    final String _sql = "SELECT item_qty FROM inventoryTable where barcode_string=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (barcode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, barcode);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getAllBarcodes() {
    final String _sql = "SELECT barcode_string FROM inventoryTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final String _item;
        _item = _cursor.getString(0);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getAllLooseItemNames() {
    final String _sql = "SELECT item_name FROM inventoryTable WHERE barcode_string='loose'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final String _item;
        _item = _cursor.getString(0);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<InventoryTable> getAllInventoryItems() {
    final String _sql = "SELECT * FROM inventoryTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfScannedItem = CursorUtil.getColumnIndexOrThrow(_cursor, "scanned_item");
      final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode_string");
      final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "item_name");
      final int _cursorIndexOfItemQty = CursorUtil.getColumnIndexOrThrow(_cursor, "item_qty");
      final int _cursorIndexOfItemPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "item_price");
      final List<InventoryTable> _result = new ArrayList<InventoryTable>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final InventoryTable _item;
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        final boolean _tmpScannedItem;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfScannedItem);
        _tmpScannedItem = _tmp != 0;
        final String _tmpBarcode;
        _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
        final String _tmpItemName;
        _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
        final double _tmpItemQty;
        _tmpItemQty = _cursor.getDouble(_cursorIndexOfItemQty);
        final double _tmpItemPrice;
        _tmpItemPrice = _cursor.getDouble(_cursorIndexOfItemPrice);
        _item = new InventoryTable(_tmpId,_tmpScannedItem,_tmpBarcode,_tmpItemName,_tmpItemQty,_tmpItemPrice);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public String getItemNameFromBarcode(final String barcode) {
    final String _sql = "SELECT item_name FROM inventoryTable WHERE barcode_string=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (barcode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, barcode);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final String _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getString(0);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
