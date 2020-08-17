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
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item


class TopFiveItem(val name: String, val qty: String, val sales: String) : Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.apply {
            val mNameview: TextView = containerView.findViewById(R.id.item_name_of_top5)
            val mQuantityview: TextView = containerView.findViewById(R.id.item_qty_of_top5)
            val mpriceview: TextView = containerView.findViewById(R.id.item_sales_of_top5)
            val mImageView: ImageView = containerView.findViewById(R.id.icon_top_five)

            if (name.length > 12)
                mNameview.text = name.take(12) + "..."
            else
                mNameview.text = name.take(12)

            mpriceview.text = "₹ " + sales
            mQuantityview.text = qty

            when (position) {
                0 -> mImageView.setImageResource(R.drawable.one)
                1 -> mImageView.setImageResource(R.drawable.two)
                2 -> mImageView.setImageResource(R.drawable.three)
                3 -> mImageView.setImageResource(R.drawable.four)
                4 -> mImageView.setImageResource(R.drawable.five)
            }
            /*if (position==0){
            holder.mQuantityview.textSize = context.resources.getDimension(R.dimen.first_item_qty)
            holder.mpriceview.textSize = context.resources.getDimension(R.dimen.first_item_sales)
            }*/
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_top_five
    }

}
