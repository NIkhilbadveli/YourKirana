package com.titos.barcodescanner.inventoryFeature

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import kotlinx.android.synthetic.main.item_stock.*
import java.util.*

class StockItem(private val action: String, private val actionTime: String, private val prevQty : String): Item() {

    @SuppressLint("ResourceAsColor")
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            /*
            For example: action value will be something like "+6" or "-5"
            Step-1:- Take first char in action and if it is + then tvStatus should be Added : 6
                    otherwise it is Sale : 6. Also, assign up or down arrow
            Step-2:- Assign time to corresponding textView
            Step-3:- stockLeft = prevQty +6 or maybe prevQty -5 like that. Show this in tvStockLeft. Assign color as well

             */
            val stockLeft = containerView.findViewById<TextView>(R.id.tv_stockleft)

            stockLeft.text = prevQty
            tvStatus.text = action
            tv_dateandtime.text = actionTime

            if(action.contains("Added"))
            {
                iv_arrows.setImageResource(R.drawable.up_arrow)
            }
            else if(action.contains("Updated"))
            {
                iv_arrows.visibility = View.GONE
            }
            else {
                iv_arrows.setImageResource(R.drawable.down_arrow)
            }

        }
    }

    override fun getLayout(): Int = R.layout.item_stock
}