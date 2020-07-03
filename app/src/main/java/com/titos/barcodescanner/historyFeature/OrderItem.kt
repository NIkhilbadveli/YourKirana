package com.titos.barcodescanner.historyFeature

import com.titos.barcodescanner.R
import com.titos.barcodescanner.TransactionTable
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_order.*

class OrderItem(val orderItem: TransactionTable): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            order_item_name.text = orderItem.itemName
            order_item_qty.text = orderItem.itemQty.toString()
            order_item_price.text = "Rs. "+orderItem.itemPrice.toString()
        }
    }

    override fun getLayout(): Int = R.layout.item_order
}