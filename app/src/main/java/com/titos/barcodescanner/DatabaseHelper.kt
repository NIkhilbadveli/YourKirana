package com.titos.barcodescanner

import android.content.Context
import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "transactionTable")
data class TransactionTable(
        @PrimaryKey(autoGenerate = true) var id: Int = -1,

        @ColumnInfo(name = "scanned_item") var scannedItems: Boolean = true,
        @ColumnInfo(name = "order_id") var orderId: Int = -1,
        @ColumnInfo(name = "order_date") var orderDate: String = "",
        @ColumnInfo(name = "order_time") var orderTime: String = "",
        @ColumnInfo(name = "barcode_string") var barcode: String = "",
        @ColumnInfo(name = "item_name") var itemName: String = "",
        @ColumnInfo(name = "item_qty") var itemQty: Double = -1.0,
        @ColumnInfo(name = "item_price") var itemPrice: Double = -1.0
):Parcelable

@Entity(tableName = "inventoryTable")
data class InventoryTable(
        @PrimaryKey(autoGenerate = true) var id: Int,

        @ColumnInfo(name = "scanned_item") var scannedItem: Boolean,
        @ColumnInfo(name = "barcode_string") var barcode: String,
        @ColumnInfo(name = "item_name") var itemName: String,
        @ColumnInfo(name = "item_qty") var itemQty: Double,
        @ColumnInfo(name = "item_price") var itemPrice: Double
)

@Dao
interface CrudMethods {
    @Query("SELECT * FROM transactionTable")
    fun getAll(): List<TransactionTable>

    @Query("SELECT MAX(order_id) FROM transactionTable")
    fun getMaxOrderId(): Int

    @Insert
    fun insertItem(vararg item: TransactionTable)

    @Update
    fun updateItem(vararg item: TransactionTable)

    @Query("DELETE FROM transactionTable WHERE order_id=:orderId")
    fun deleteTransaction(orderId: Int)

    @Query("SELECT SUM(item_price), COUNT(DISTINCT order_id) FROM transactionTable")
    fun getTotalSales(): Double

    @Query("SELECT COUNT(DISTINCT order_id) FROM transactionTable")
    fun getTotalOrders(): Int

    @Query("SELECT  order_id as orderId, order_date as orderDate, order_time as orderTime, SUM(item_price) as value FROM transactionTable GROUP BY order_date, order_time")
    fun getAllOrdersDetails(): List<OrderDetails>

    @Query("SELECT order_date as orderDate, SUM(item_price) as sales FROM transactionTable GROUP BY order_date")
    fun getAllDaysSales(): List<DaySales>

    @Query("SELECT item_name as itemName, SUM(item_qty) as qty, SUM(item_price) as sales FROM transactionTable GROUP BY item_name ORDER BY SUM(item_price) DESC")
    fun getAllItemsSales(): List<ItemQtyAndSales>

    @Query("SELECT SUM(item_price) as sales FROM transactionTable WHERE order_date BETWEEN :startDate AND :endDate")
    fun getThisWeekSales(startDate: String,endDate:String): Double

    @Query("SELECT * FROM transactionTable WHERE order_id=:orderNumber")
    fun getAllItemsByOrder(orderNumber: Int): List<TransactionTable>

    @Insert
    fun insertInventoryItem(vararg  item: InventoryTable)

    @Query("SELECT item_qty FROM inventoryTable where barcode_string=:barcode")
    fun getQuantity(barcode: String): Double

    @Query("UPDATE inventoryTable SET item_qty=:qty where barcode_string=:barcode")
    fun updateQuantity(barcode: String, qty: Double)

    @Query("UPDATE inventoryTable SET item_price=:price where barcode_string=:barcode")
    fun updatePrice(barcode: String, price: Double)

    @Query("UPDATE inventoryTable SET item_price=:price where barcode_string='loose' AND item_name=:itemName")
    fun updateLoosePrice(itemName: String, price: Double)

    @Query("UPDATE inventoryTable SET item_name=:name where barcode_string=:barcode")
    fun updateName(barcode: String, name: String)

    @Query("SELECT barcode_string FROM inventoryTable")
    fun getAllBarcodes(): List<String>

    @Query("SELECT item_name FROM inventoryTable WHERE barcode_string='loose'")
    fun getAllLooseItemNames(): List<String>

    @Query("SELECT * FROM inventoryTable")
    fun getAllInventoryItems(): List<InventoryTable>

    @Query("SELECT item_name FROM inventoryTable WHERE barcode_string=:barcode")
    fun getItemNameFromBarcode(barcode: String): String

    @Query("DELETE FROM inventoryTable WHERE barcode_string=:barcode")
    fun deleteBarcodeItem(barcode: String)

    @Query("DELETE FROM inventoryTable WHERE item_name=:itemName AND barcode_string='loose'")
    fun deleteLooseItem(itemName: String)
}

@Database(entities = [TransactionTable::class, InventoryTable::class], version = 1,exportSchema=false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun crudMethods(): CrudMethods

    companion object {
        @Volatile private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context)= instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also { instance = it}
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context,
                AppDatabase::class.java, "mystore-data.db")
                .build()
    }
}

data class OrderDetails(val orderId: Int,val orderDate: String, val orderTime: String, val value: Double)

data class DaySales(val orderDate: String,val sales: Double)

data class ItemQtyAndSales(val itemName: String,val qty:Double, val sales: Double)
