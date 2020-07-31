package com.titos.barcodescanner.dashboardFeature

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.titos.barcodescanner.R


class TopFiveItemAdapter(private val itemQtyAndSales: List<ItemQtyAndSales>,val context: Context) : RecyclerView.Adapter<TopFiveItemAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_top_five, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = itemQtyAndSales[position]

        if (itemQtyAndSales[position].itemName.length > 12)
            holder.mNameview.text = itemQtyAndSales[position].itemName.take(12) + "..."
        else
            holder.mNameview.text = itemQtyAndSales[position].itemName.take(12)

        holder.mpriceview.text = "Rs. " + itemQtyAndSales[position].sales.toInt().toString()
        holder.mQuantityview.text = itemQtyAndSales[position].qty.toInt().toString()

        when(position){
            0 -> holder.mImageView.setImageResource(R.drawable.one)
            1 -> holder.mImageView.setImageResource(R.drawable.two)
            2 -> holder.mImageView.setImageResource(R.drawable.three)
            3 -> holder.mImageView.setImageResource(R.drawable.four)
            4 -> holder.mImageView.setImageResource(R.drawable.five)
        }
        /*if (position==0){
            holder.mQuantityview.textSize = context.resources.getDimension(R.dimen.first_item_qty)
            holder.mpriceview.textSize = context.resources.getDimension(R.dimen.first_item_sales)
        }*/
    }

    override fun getItemCount(): Int {
        return itemQtyAndSales.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mNameview: TextView = mView.findViewById(R.id.item_name_of_top5)
        val mQuantityview: TextView = mView.findViewById(R.id.item_qty_of_top5)
        val mpriceview: TextView = mView.findViewById(R.id.item_sales_of_top5)
        val mImageView: ImageView = mView.findViewById(R.id.icon_top_five)

        var mItem: ItemQtyAndSales? = null

    }
}
