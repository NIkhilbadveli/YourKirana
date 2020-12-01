package com.titos.barcodescanner.historyFeature

import android.widget.TextView
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import java.text.SimpleDateFormat
import java.util.*


class HistoryHeaderItem(private val orderDate: String, val daySales: Double): Item(){
    private val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val otherDateFormat = SimpleDateFormat("MMM yyyy, EEE", Locale.US)

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val date = simpleDateFormat.parse(orderDate)

        val tvDate =  viewHolder.itemView.findViewById<TextView>(R.id.secion_header_date)
        val tvDay =  viewHolder.itemView.findViewById<TextView>(R.id.secion_header_day)
        val tvDaySales =  viewHolder.itemView.findViewById<TextView>(R.id.secion_header_day_sales)

        tvDate.text = orderDate.split("-").first()
        tvDay.text = otherDateFormat.format(date)
        tvDaySales.text = daySales.toInt().toString()
    }

    override fun getLayout(): Int = R.layout.item_header_history
}