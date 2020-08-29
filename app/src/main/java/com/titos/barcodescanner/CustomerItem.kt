package com.titos.barcodescanner

import android.app.AlertDialog
import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_customer_request.*

class CustomerItem(private val customerId: String, val customerName: String, private val mobileNumber: String,
val amountDue: String,val dueDate: String, val takenTime: String,val onItemRemoveClick:((Int)->Unit)): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            val tvCustomerName = containerView.findViewById<TextView>(R.id.tv_customer_name)
            val tvDueDate = containerView.findViewById<TextView>(R.id.tv_due_date)
            val tvDueAmount = containerView.findViewById<TextView>(R.id.tv_due_amount)

            tvCustomerName.text = customerName
            tvDueDate.text = dueDate
            tvDueAmount.text = "\u20B9 $amountDue"

            val expand = containerView.findViewById<LinearLayout>(R.id.expand)
            val cust = containerView.findViewById<LinearLayout>(R.id.cardView_customer)
            val arrow = containerView.findViewById<ImageView>(R.id.right_arrow)
            val callCust = containerView.findViewById<TextView>(R.id.call)
            val sharedPref = containerView.context?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            val shopName = sharedPref?.getString("shopName","shop")!!

            val khataRef = FirebaseDatabase.getInstance().reference.child("khataBook/$shopName/$takenTime")

            cust.setOnClickListener {
                if(expand.visibility == View.GONE)
                {
                    TransitionManager.beginDelayedTransition(cust,AutoTransition())
                    expand.visibility = View.VISIBLE
                    arrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
                    val clear = containerView.findViewById<TextView>(R.id.clear)
                    callCust.text = mobileNumber
                    clear.setOnClickListener {
                        val builder = AlertDialog.Builder(containerView.context)
                        builder.setTitle("Mark as Paid")
                        builder.setMessage("Are you sure")
                        builder.setIcon(android.R.drawable.ic_dialog_alert)
                        builder.setPositiveButton("Yes"){dialogInterface, which ->
                            //Toast.makeText(containerView.context,"paid successfully..",Toast.LENGTH_LONG).show()
                            khataRef.child("status").setValue("paid")
                            dialogInterface.dismiss()
                            onItemRemoveClick.invoke(position)
                        }
                        builder.setNegativeButton("No"){dialogInterface, which ->
                            dialogInterface.dismiss()
                        }
                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.setCancelable(false)
                        alertDialog.show()
                    }
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

    override fun getLayout(): Int = R.layout.item_customer
}