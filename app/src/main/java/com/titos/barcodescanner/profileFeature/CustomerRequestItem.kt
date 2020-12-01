package com.titos.barcodescanner.profileFeature

import android.content.Context
import android.text.InputType
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.titos.barcodescanner.utils.RequestDetails
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_customer_request.*

class CustomerRequestItem(val refKey: String, val requestDetails: RequestDetails, val onUpdateListener: (String, RequestDetails, Int, Int)->Unit): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            request_item_name.setText(requestDetails.name)
            request_item_qty.setText(requestDetails.qty.toString())

            check_box.isChecked = requestDetails.checked
            check_box.setOnCheckedChangeListener { _, isChecked ->
                requestDetails.checked = isChecked
                if (isChecked)
                    onUpdateListener.invoke(refKey,requestDetails, 0, position)
                else
                    onUpdateListener.invoke(refKey,requestDetails ,1, position)
            }

            subtract_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() - 1
                if (updatedQty>0){
                    requestDetails.qty = updatedQty
                    notifyChanged()
                    onUpdateListener.invoke(refKey, requestDetails,2, position)
                }
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = request_item_qty.text.toString().toInt() + 1
                requestDetails.qty = updatedQty
                notifyChanged()
                onUpdateListener.invoke(refKey, requestDetails,3, position)
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
                    requestDetails.name = request_item_name.text.toString()
                    notifyChanged()
                    onUpdateListener.invoke(refKey, requestDetails ,4, position)
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