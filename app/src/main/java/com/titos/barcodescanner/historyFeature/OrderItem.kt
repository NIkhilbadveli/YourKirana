package com.titos.barcodescanner.historyFeature

import android.widget.ImageView
import com.titos.barcodescanner.R

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_order.*

class OrderItem(val itemName: String, val itemQty: String, val itemPrice: String): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            tvProductName.text = itemName
            tvTotalPrice.text = "₹ ${itemPrice.toInt()*itemQty.toInt()}"
            tvProductQuantity.text = "$itemQty units x ₹ $itemPrice"
            val mImageView: ImageView = containerView.findViewById(R.id.img)

            when (position) {
                0 -> mImageView.setImageResource(R.drawable.one)
                1 -> mImageView.setImageResource(R.drawable.two)
                2 -> mImageView.setImageResource(R.drawable.three)
                3 -> mImageView.setImageResource(R.drawable.four)
                4 -> mImageView.setImageResource(R.drawable.five)
                5 -> mImageView.setImageResource(R.drawable.six)
                6 -> mImageView.setImageResource(R.drawable.seven)
                7 -> mImageView.setImageResource(R.drawable.eight)
                8 -> mImageView.setImageResource(R.drawable.nine)
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_order
}