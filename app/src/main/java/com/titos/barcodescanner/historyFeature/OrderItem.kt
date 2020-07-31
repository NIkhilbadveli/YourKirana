package com.titos.barcodescanner.historyFeature

import com.titos.barcodescanner.R

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_order.*

class OrderItem(val itemName: String, val itemQty: String, val itemPrice: String): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            order_item_name.text = itemName
            order_item_qty.text = itemQty
            order_item_price.text = "Rs. "+itemPrice
        }
    }

    override fun getLayout(): Int = R.layout.item_order
}