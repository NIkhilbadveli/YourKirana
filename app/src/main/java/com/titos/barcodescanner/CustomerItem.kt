package com.titos.barcodescanner

import android.widget.TextView
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_customer_request.*

class CustomerItem(private val customerId: String, val customerName: String, private val mobileNumber: String,
val amountDue: String,val dueDate: String, val takenTime: String): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            val tvCustomerName = containerView.findViewById<TextView>(R.id.tv_customer_name)
            val tvDueDate = containerView.findViewById<TextView>(R.id.tv_due_date)
            val tvDueAmount = containerView.findViewById<TextView>(R.id.tv_due_amount)

            tvCustomerName.text = customerName
            tvDueDate.text = dueDate
            tvDueAmount.text = "\u20B9 $amountDue"
        }
    }

    override fun getLayout(): Int = R.layout.item_customer
}