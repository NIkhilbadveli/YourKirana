package com.titos.barcodescanner.scannerFeature

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_scanner.view.*

class ListItem(val context: Context, val scannerItem: ScannerItem, val onItemClick:((Int,String,Double)->Unit), val onItemRemoveClick:((Int)->Unit)): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {

            val mNameview:TextView = viewHolder.itemView.item_name
            val mQuantityview:TextView = viewHolder.itemView.item_quantity
            val mpriceview:TextView = viewHolder.itemView.item_price
            val mAddButton: ImageButton = viewHolder.itemView.add_quantity_button
            val mSubtractButton: ImageButton = viewHolder.itemView.subtract_quantity_button
            val mRemoveItemButton: ImageButton = viewHolder.itemView.remove_item_button
            val mThumbnail = viewHolder.itemView.product_thumbnail
            val etQuantity = viewHolder.itemView.et_quantity

            mNameview.text = scannerItem.name.take(35)
            mpriceview.text = (scannerItem.price.toDouble() * scannerItem.quantity.toDouble()).round(2).toString()
            mQuantityview.text = scannerItem.quantity

            val price = scannerItem.price.toDouble()

            //Setting visibilities for loose items
            if (scannerItem.loose){
                mAddButton.visibility = View.GONE
                mSubtractButton.visibility = View.GONE
                mQuantityview.visibility = View.GONE
                etQuantity.visibility = View.VISIBLE
                etQuantity.setText(scannerItem.quantity)
            }

            Glide.with(context).load(scannerItem.thumbnailUrl)
                    .override(120,120)
                    .placeholder(R.drawable.ic_broken_image_black_24dp)
                    .into(mThumbnail)

            ///look if this can be optimized by moving to the viewholder class below
            mAddButton.setOnClickListener {
                val value = mQuantityview.text.toString().toDouble() + 1
                mQuantityview.setText(value.toString())
                mpriceview.text = (value * price).round(2).toString()
                onItemClick.invoke(position,(value * price).toString(), value)
            }

            mSubtractButton.setOnClickListener {
                val value = mQuantityview.text.toString().toDouble() - 1
                if (value>0){
                    mQuantityview.text = value.toString()
                    mpriceview.text = (value * price).round(2).toString()
                    onItemClick.invoke(position, (value * price).toString(), value)
                }
            }

            etQuantity.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s!!.isNotEmpty()) {
                        val qty = s.toString().toDouble()
                        mpriceview.text = (qty * price).toString()
                        onItemClick.invoke(position, (qty * price).toString(), qty)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }
            })

            etQuantity.setOnEditorActionListener { textView, actionId, keyEvent ->
                //triggered when done editing (as clicked done on keyboard)
                if (actionId == EditorInfo.IME_ACTION_DONE||keyEvent.keyCode== KeyEvent.KEYCODE_BACK) {
                    etQuantity.clearFocus()
                }
                false
            }

            mRemoveItemButton.setOnClickListener {
                onItemRemoveClick.invoke(position)
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_scanner

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

}