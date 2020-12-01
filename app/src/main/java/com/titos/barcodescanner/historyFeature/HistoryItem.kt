package com.titos.barcodescanner.historyFeature

import android.util.Log
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_history.*

class HistoryItem(private val orderNumber: String, val orderTime: String, val isItNight: Boolean, val orderValue: String,val onItemRemoveClick:((Int,Int)->Unit),
                  val onItemClick:((Int)->Unit)): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            history_order_number.text = orderNumber
            history_order_time.text = orderTime
            history_order_value.text = orderValue

           /* if (isItNight) {
                //Log.d("fucked", orderTime + "....."+hour.toInt()+amPm)
                day_night_indicator.setImageResource(R.drawable.ic_nights_stay_24px)
            }*/

            delete_button.setOnClickListener { onItemRemoveClick.invoke(orderNumber.split(" ").last().toInt()-1,position) }

            containerView.setOnClickListener { onItemClick.invoke(orderNumber.split(" ").last().toInt() - 1) }
        }
    }

    override fun getLayout(): Int = R.layout.item_history
}