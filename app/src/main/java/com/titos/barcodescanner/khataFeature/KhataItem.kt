package com.titos.barcodescanner.khataFeature

import android.app.AlertDialog
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.*
import com.titos.barcodescanner.R
import com.titos.barcodescanner.utils.KhataDetails
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder

class KhataItem(val mobileNumber: String, private val customerName: String, private val amountDue: String, val onItemRemoveClick:((String, Int)->Unit)): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            val tvMobile = containerView.findViewById<TextView>(R.id.tv_mobile_number)
            val tvDueAmount = containerView.findViewById<TextView>(R.id.tv_due_amount)

            tvMobile.text = "$customerName (+91 $mobileNumber)"
            tvDueAmount.text = "\u20B9 $amountDue"
            val clear = containerView.findViewById<ImageView>(R.id.clear)
            clear.setOnClickListener {
                onItemRemoveClick.invoke(mobileNumber, position)
            }

            /*val builder = AlertDialog.Builder(containerView.context)
            builder.setTitle("Mark as Paid")
            builder.setMessage("Are you sure")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("Yes"){dialogInterface, which ->
                Toast.makeText(containerView.context,"Paid successfully..",Toast.LENGTH_LONG).show()
                dialogInterface.dismiss()
                onItemRemoveClick.invoke(refKey, position)
            }
            builder.setNegativeButton("No"){dialogInterface, which ->
                dialogInterface.dismiss()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)

            clear.setOnClickListener {
                alertDialog.show()
            }*/

            /*if (kd.status=="paid")
                clear.visibility = View.GONE*/
        }
    }

    override fun getLayout(): Int = R.layout.item_khata
}