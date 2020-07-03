package com.titos.barcodescanner.inventoryFeature


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.AppDatabase
import com.titos.barcodescanner.InventoryTable
import com.titos.barcodescanner.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


class InventoryFragment : Fragment(), SearchView.OnQueryTextListener {

    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var groupAdapterScanned = GroupAdapter<GroupieViewHolder>()
    private var db:AppDatabase? = null
    private var onItemRemoveClick :((Boolean,String, Int)->Unit)? = null
    private var onItemEditClick :((Boolean, String,String,String, Int)->Unit)? = null
    private var barcodeList = ArrayList<String>()
    private var nameList = ArrayList<String>()
    private var recyclerViewScannedItems:RecyclerView? = null
    private var recyclerView:RecyclerView? = null
    private var shopName = "Temp Store"
    private lateinit var sharedPref: SharedPreferences
    private var inventoryList = ArrayList<InventoryItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view =  inflater.inflate(R.layout.fragment_inventory, container, false)

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!

        db = AppDatabase(context!!)

        /*recyclerView = view.findViewById<RecyclerView>(R.id.rv_mystore)
        recyclerView?.apply {
            adapter = groupAdapter
            layoutManager = GridLayoutManager(context,3)
        }*/

        recyclerViewScannedItems = view.findViewById(R.id.rv_mystore_scannable)
        recyclerViewScannedItems!!.apply {
            adapter = groupAdapterScanned
            layoutManager = LinearLayoutManager(context)
        }

        onItemRemoveClick = {looseItem,itemName, pos ->
            if (looseItem){
                groupAdapter.removeGroupAtAdapterPosition(pos)
                groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                GlobalScope.launch { db?.crudMethods()?.deleteLooseItem(itemName) }
                Snackbar.make(view,"Loose Item deleted",Snackbar.LENGTH_SHORT).show()
            }
            else{
                groupAdapterScanned.removeGroupAtAdapterPosition(pos)
                groupAdapterScanned.notifyItemRangeChanged(pos,groupAdapterScanned.itemCount)
                GlobalScope.launch {
                    db?.crudMethods()?.deleteBarcodeItem(barcodeList[pos])
                    barcodeList.removeAt(pos)
                    nameList.removeAt(pos)}
                Snackbar.make(view,"Scanned Item deleted",Snackbar.LENGTH_SHORT).show()
            }
        }

        onItemEditClick = {looseItem, itemName,itemQty,itemPrice, pos -> editInventoryItem(looseItem,itemName,itemQty,itemPrice,pos)}

        populateView()

        val searchView = view.findViewById<SearchView>(R.id.simpleSearchView)
        searchView.setOnQueryTextListener(this)

        return view
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filter(newText)
        return false
    }

    private fun filter(charText: String) {
        val lowerCaseText = charText.toLowerCase(Locale.getDefault())

        if (lowerCaseText.isNotEmpty()) {
            groupAdapterScanned.clear()
            groupAdapterScanned.addAll(inventoryList.filter { it -> it.itemName.toLowerCase(Locale.getDefault()).contains(lowerCaseText) })
        }
        else{
            groupAdapterScanned.clear()
            groupAdapterScanned.addAll(inventoryList)
        }
    }

    private fun populateView(){
        GlobalScope.launch {

            val allInventoryItems = db?.crudMethods()?.getAllInventoryItems()!!

            withContext(Dispatchers.Main){
                for (item in allInventoryItems){
                    if (item.scannedItem){
                        val itemInv = InventoryItem(false, item.itemName, item.itemQty.toInt().toString(),
                                item.itemPrice.toInt().toString(),
                                onItemRemoveClick!!,onItemEditClick!!)
                        groupAdapterScanned.add(itemInv)
                        barcodeList.add(item.barcode)
                        nameList.add(item.itemName)
                        inventoryList.add(itemInv)
                    }
                    else
                        groupAdapter.add(InventoryItem(true, item.itemName, "Rs. " + item.itemPrice.toInt().toString() + " /Kg",
                                item.itemPrice.toInt().toString(),onItemRemoveClick!!,onItemEditClick!!))

                }

                if (!sharedPref.getBoolean("inventoryTutorialCompleted",false) && !sharedPref.getBoolean("skipInvTutorial",false)){
                    val simpleTooltip = SimpleTooltip.Builder(context)
                            .anchorView(recyclerViewScannedItems)
                            .text(R.string.step_6)
                            .gravity(Gravity.BOTTOM)
                            .animated(true)
                            .transparentOverlay(false)
                            .contentView(R.layout.walkthrough, R.id.step_desc)
                            .dismissOnOutsideTouch(false)
                            .dismissOnInsideTouch(false)
                            .build()

                    simpleTooltip.findViewById<Button>(R.id.btn_next).text = "Done"
                    simpleTooltip.findViewById<TextView>(R.id.step_number).text = "Step - 6"
                    simpleTooltip.findViewById<Button>(R.id.btn_next).setOnClickListener {
                        simpleTooltip.dismiss()
                        with (sharedPref.edit()) {
                            putBoolean("inventoryTutorialCompleted", true)
                            apply()
                        }
                    }
                    simpleTooltip.findViewById<Button>(R.id.btn_skip).visibility = View.GONE
                    simpleTooltip.show()
                }

            }
        }

    }

    private fun showDialog() {
        val viewGroup = activity!!.findViewById<ViewGroup>(android.R.id.content)
        val dialogview = LayoutInflater.from(context).inflate(R.layout.add_product_dialog, viewGroup, false)

        val productPriceEditText = dialogview.findViewById<EditText>(R.id.product_price)
        productPriceEditText.hint = "Price in Rs. /Kg"

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogview)
        val alertDialog = builder.create()

        alertDialog.show()
        addToDatabaseAndUpdateList(dialogview, alertDialog)
    }

    private fun addToDatabaseAndUpdateList(view: View, alertDialog: AlertDialog) {

        val name = view.findViewById<EditText>(R.id.product_name)
        val price = view.findViewById<EditText>(R.id.product_price)

        view.findViewById<View>(R.id.product_add_button).setOnClickListener {
            if (name.text.isNotEmpty() && price.text.isNotEmpty()){

                GlobalScope.launch {
                    db?.crudMethods()?.insertInventoryItem(InventoryTable(0, false, "loose",
                            name.text.toString(),1.0,price.text.toString().toDouble()))
                }

                groupAdapter.add(InventoryItem(true, name.text.toString(), "Rs. " + price.text.toString() + " /Kg",
                        "0.0", onItemRemoveClick!!,onItemEditClick!!))
                alertDialog.dismiss()
                Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(context, "Please fill all the necessary details", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.product_cancel_button).setOnClickListener {
            alertDialog.dismiss()
            //Toast.makeText(context, "Cancelled adding", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editInventoryItem(looseItem:Boolean,itemName:String,itemQty:String,itemPrice:String, pos:Int){
        val inventoryRef = FirebaseDatabase.getInstance().reference.child("inventoryData").child(shopName)

        val viewGroup = activity!!.findViewById<ViewGroup>(android.R.id.content)
        val dialogview = LayoutInflater.from(context).inflate(R.layout.add_product_dialog, viewGroup, false)

        val productQuantityContainer = dialogview.findViewById<LinearLayout>(R.id.quantity_text_input)
        productQuantityContainer.visibility = View.VISIBLE

        val productNameEditText = dialogview.findViewById<EditText>(R.id.product_name)
        val productPriceEditText = dialogview.findViewById<EditText>(R.id.product_price)
        val productQuantityEditText = dialogview.findViewById<EditText>(R.id.product_quantity)

        productNameEditText.setText(itemName)
        productNameEditText.setTextColor(resources.getColor(R.color.textColorBlack))

        if (looseItem){
            productQuantityContainer.visibility = View.GONE
            productPriceEditText.hint = "Rs. /Kg"
        }
        else {
            productQuantityEditText.setText(itemQty)
            productPriceEditText.setText(itemPrice)
        }

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogview)
        val alertDialog = builder.create()

        alertDialog.show()

        dialogview.findViewById<View>(R.id.product_add_button).setOnClickListener {
            if (productPriceEditText.text.isNotEmpty()){

                if (looseItem){
                    val itemView = recyclerView!!.getChildAt(pos)
                    itemView.findViewById<TextView>(R.id.mystore_item_qty).text = "Rs. " + productPriceEditText.text.toString() + "/Kg"
                }
                else{
                    val itemView = recyclerViewScannedItems!!.getChildAt(pos)

                    if(productNameEditText.text.isNotEmpty()) {
                        itemView.findViewById<TextView>(R.id.mystore_item_name).text = productNameEditText.text.toString()
                        inventoryRef.child(barcodeList[pos]).child("name").setValue(productNameEditText.text.toString())
                    }

                    if(productQuantityEditText.text.isNotEmpty()) {
                        itemView.findViewById<TextView>(R.id.mystore_item_qty).text = productQuantityEditText.text.toString()
                        inventoryRef.child(barcodeList[pos]).child("quantity").setValue(productQuantityEditText.text.toString())
                    }

                    itemView.findViewById<TextView>(R.id.mystore_item_price).text = productPriceEditText.text.toString()
                    inventoryRef.child(barcodeList[pos]).child("price").setValue(productPriceEditText.text.toString())

                    //groupAdapterScanned.notifyItemChanged(pos)
                }

                GlobalScope.launch {

                    if (looseItem)
                        db?.crudMethods()?.updateLoosePrice(productNameEditText.text.toString(), productPriceEditText.text.toString().toDouble())
                    else{
                        if(productNameEditText.text.isNotEmpty())
                            db?.crudMethods()?.updateName(barcodeList[pos], productNameEditText.text.toString())

                        if(productQuantityEditText.text.isNotEmpty())
                            db?.crudMethods()?.updateQuantity(barcodeList[pos], productQuantityEditText.text.toString().toDouble())

                        db?.crudMethods()?.updatePrice(barcodeList[pos], productPriceEditText.text.toString().toDouble())
                    }
                }
                alertDialog.dismiss()
                //Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(context, "Please fill all the necessary details", Toast.LENGTH_SHORT).show()
        }

        dialogview.findViewById<View>(R.id.product_cancel_button).setOnClickListener {
            alertDialog.dismiss()
            //Toast.makeText(context, "Cancelled adding", Toast.LENGTH_SHORT).show()
        }
    }
}

@Parcelize
data class ScannedItem(val barcode: String = "", val name: String = "", val quantity: String = "", val price: String = ""): Parcelable
