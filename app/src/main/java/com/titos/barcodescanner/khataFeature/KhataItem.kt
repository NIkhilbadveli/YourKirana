package com.titos.barcodescanner.khataFeature

import android.app.AlertDialog
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.titos.barcodescanner.R
import com.titos.barcodescanner.utils.KhataDetails
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder

class KhataItem(val kd: KhataDetails, val onItemRemoveClick:((Int)->Unit)): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            val tvCustomerName = containerView.findViewById<TextView>(R.id.tv_customer_name)
            val tvDueDate = containerView.findViewById<TextView>(R.id.tv_due_date)
            val tvDueAmount = containerView.findViewById<TextView>(R.id.tv_due_amount)

            tvCustomerName.text = kd.customerName
            tvDueDate.text = kd.dueDate
            tvDueAmount.text = "\u20B9 ${kd.amountDue}"

            val expand = containerView.findViewById<LinearLayout>(R.id.expand)
            val cust = containerView.findViewById<LinearLayout>(R.id.cardView_customer)
            val arrow = containerView.findViewById<ImageView>(R.id.right_arrow)
            val callCust = containerView.findViewById<TextView>(R.id.call)

            val clear = containerView.findViewById<TextView>(R.id.clear)

            val builder = AlertDialog.Builder(containerView.context)
            builder.setTitle("Mark as Paid")
            builder.setMessage("Are you sure")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("Yes"){dialogInterface, which ->
                //Toast.makeText(containerView.context,"Paid successfully..",Toast.LENGTH_LONG).show()
                dialogInterface.dismiss()
                onItemRemoveClick.invoke(position)
            }
            builder.setNegativeButton("No"){dialogInterface, which ->
                dialogInterface.dismiss()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)

            clear.setOnClickListener {
                alertDialog.show()
            }

            if (kd.status=="paid")
                clear.visibility = View.GONE

            cust.setOnClickListener {
                if(expand.visibility == View.GONE)
                {
                    TransitionManager.beginDelayedTransition(cust,AutoTransition())
                    arrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
                    expand.visibility = View.VISIBLE
                    callCust.text = kd.mobileNumber
                }
                else
                {
                    TransitionManager.beginDelayedTransition(expand,AutoTransition())
                    expand.visibility = View.GONE
                    arrow.setImageResource(R.drawable.ic_keyboard_arrow_right_black_24dp)
                }
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_khata
}