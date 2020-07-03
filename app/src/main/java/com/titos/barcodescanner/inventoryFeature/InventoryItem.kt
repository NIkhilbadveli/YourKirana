package com.titos.barcodescanner.inventoryFeature

import android.view.View
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import java.util.*

class InventoryItem(val looseItem: Boolean,val itemName: String, val itemQty: String, val itemPrice:String, val onItemRemoveClick:((Boolean,String,Int)->Unit),
                    val onItemEditClick: ((Boolean,String,String,String, Int)->Unit)): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            if (looseItem)
                tv_in_stock.visibility = View.GONE
            else{
                inventory_price_container.visibility = View.VISIBLE
                mystore_item_price.text = itemPrice
            }

            mystore_item_name.text = itemName.take(15)
            mystore_item_qty.setText(itemQty)

            remove_inventory_item_button.setOnClickListener {
                onItemRemoveClick.invoke(looseItem,itemName,  position)
            }

            edit_quantity_button.setOnClickListener {
                onItemEditClick.invoke(looseItem,itemName,itemQty,itemPrice,position)
            }

            subtract_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() - 1
                if (updatedQty>0)
                    mystore_item_qty.setText(updatedQty.toString())
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() + 1
                mystore_item_qty.setText(updatedQty.toString())
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_inventory
}