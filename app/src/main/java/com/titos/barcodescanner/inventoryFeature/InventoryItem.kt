package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.view.View
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import java.util.*

class InventoryItem(var barcode: String, var itemName: String, var itemQty: String, var itemPrice:String, val onItemRemoveClick:((Int)->Unit),
                    val onItemStockClick:((Int)->Unit), val onItemEditClick: ((Int)->Unit)): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {

            mystore_item_price.text = itemPrice
            mystore_item_name.text = itemName
            mystore_item_qty.setText(itemQty)

            val sharedPref = containerView.context?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            val shopName = sharedPref?.getString("shopName","shop")!!
            val itemRef = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName/$barcode")

            mystore_item_name.setOnClickListener {
                onItemStockClick.invoke(position)
            }

            remove_inventory_item_button.setOnClickListener {
                onItemRemoveClick.invoke(position)
            }

            edit_quantity_button.setOnClickListener {
                onItemEditClick.invoke(position)
            }

            subtract_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() - 1
                if (updatedQty>0){
                    itemQty = updatedQty.toString()
                    itemRef.child("qty").setValue(itemQty)
                    notifyChanged()
                }
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() + 1
                itemQty = updatedQty.toString()
                itemRef.child("qty").setValue(itemQty)
                notifyChanged()
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_inventory
}