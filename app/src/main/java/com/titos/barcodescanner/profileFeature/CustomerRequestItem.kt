package com.titos.barcodescanner.profileFeature

import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_customer_request.*

class CustomerRequestItem(private val checked: Boolean, val itemName: String, private val itemQty: Int): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            request_item_name.setText(itemName)
            request_item_qty.setText(itemQty.toString())

            subtract_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() - 1
                if (updatedQty>0)
                    request_item_qty.setText(updatedQty.toString())
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() + 1
                request_item_qty.setText(updatedQty.toString())
            }

            var edited = false
            edit_name_button.setOnClickListener {
                if (!edited){
                    request_item_name.isClickable = true
                    request_item_name.requestFocus()
                    edit_name_button.setImageResource(R.drawable.ic_check_black_24dp)
                    edited = true
                }
                else{
                    request_item_name.isClickable = false
                    edit_name_button.setImageResource(R.drawable.ic_edit_white_24dp)
                    edited = false
                }
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_customer_request
}