package com.titos.barcodescanner.profileFeature

import android.content.Context
import android.text.InputType
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_customer_request.*

class CustomerRequestItem(val requestKey: String, private var checked: Boolean, var itemName: String, private var itemQty: Int): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            request_item_name.setText(itemName)
            request_item_qty.setText(itemQty.toString())

            val sharedPref = containerView.context?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            val shopName = sharedPref?.getString("shopName","shop")!!
            val requestRef = FirebaseDatabase.getInstance().reference.child("customerRequests/$shopName/$requestKey")

            check_box.isChecked = checked
            check_box.setOnCheckedChangeListener { _, isChecked ->
                requestRef.child("checked").setValue(isChecked)
                checked = isChecked
                notifyChanged()
               /* groupAdapter.removeGroupAtAdapterPosition(position)
                groupAdapter.add(this@CustomerRequestItem)*/
            }

            subtract_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() - 1
                if (updatedQty>0){
                    itemQty = updatedQty
                    requestRef.child("qty").setValue(updatedQty)
                    notifyChanged()
                }
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() + 1
                itemQty = updatedQty
                requestRef.child("qty").setValue(updatedQty)
                notifyChanged()
            }

            request_item_name.isFocusable = false
            request_item_name.isFocusableInTouchMode = false
            request_item_name.inputType = InputType.TYPE_NULL

            var edited = false
            edit_name_button.setOnClickListener {
                if (!edited){
                    request_item_name.isFocusable = true
                    request_item_name.isFocusableInTouchMode = true
                    request_item_name.inputType = InputType.TYPE_CLASS_TEXT

                    edit_name_button.setImageResource(R.drawable.ic_check_black_24dp)
                    edited = true
                }
                else{
                    requestRef.child("name").setValue(request_item_name.text.toString())
                    itemName = request_item_name.text.toString()
                    notifyChanged()
                    request_item_name.isFocusable = false
                    request_item_name.isFocusableInTouchMode = false
                    request_item_name.inputType = InputType.TYPE_NULL

                    edit_name_button.setImageResource(R.drawable.ic_edit_white_24dp)
                    edited = false
                }
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_customer_request
}