package com.titos.barcodescanner.scannerFeature

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.titos.barcodescanner.R

class ScannerItemAdapter(private val mValues: ArrayList<ScannerItem>) : RecyclerView.Adapter<ScannerItemAdapter.ViewHolder>() {
    var onItemClick:((Int,String,Int)->Unit)?= null //For increase/decrease quantity
    var onItemRemoveClick:((Int)->Unit)? = null
    var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scanner, parent, false)

        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mNameview.text = mValues[position].name.take(35)
        holder.mpriceview.text = mValues[position].price
        holder.mQuantityview.text = mValues[position].quantity

        val price = mValues[position].price.toDouble()

        if (!mValues[position].scanned){
            holder.mAddButton.visibility = View.GONE
            holder.mSubtractButton.visibility = View.GONE
        }

        Glide.with(context!!).load(mValues[position].thumbnailUrl)
                .override(120,120)
                .placeholder(R.drawable.ic_broken_image_black_24dp)
                .into(holder.mThumbnail)

        ///look if this can be optimized by moving to the viewholder class below
        holder.mAddButton.setOnClickListener {
            val value = Integer.parseInt(holder.mQuantityview.text.toString()) + 1
            holder.mQuantityview.setText(value.toString())
            holder.mpriceview.text = (value * price).toString()
            onItemClick?.invoke(position,(value * price).toString(),value)
        }

        holder.mSubtractButton.setOnClickListener {
            val value = Integer.parseInt(holder.mQuantityview.text.toString()) - 1
            if (value>=0){
                holder.mQuantityview.text = value.toString()
                holder.mpriceview.text = (value * price).toString()
                onItemClick?.invoke(position,(value * price).toString(),value)
            }
        }

        holder.mQuantityview.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if (mValues[position].scanned && s!!.isNotEmpty()) {
                    val qty = Integer.parseInt(s.toString())
                    holder.mpriceview.text = (qty * price).toString()
                    onItemClick?.invoke(position,(qty * price).toString(),qty)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        holder.mQuantityview.setOnEditorActionListener { textView, actionId, keyEvent ->
            //triggered when done editing (as clicked done on keyboard)
            if (actionId == EditorInfo.IME_ACTION_DONE||keyEvent.keyCode==KeyEvent.KEYCODE_BACK) {
                holder.mQuantityview.clearFocus()
            }
            false
        }

        holder.mRemoveItemButton.setOnClickListener {
            onItemRemoveClick?.invoke(position)
        }

    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mNameview:TextView = mView.findViewById(R.id.item_name)
        val mQuantityview:TextView = mView.findViewById(R.id.item_quantity)
        val mpriceview:TextView = mView.findViewById(R.id.item_price)
        val mAddButton:ImageButton = mView.findViewById(R.id.add_quantity_button)
        val mSubtractButton:ImageButton = mView.findViewById(R.id.subtract_quantity_button)
        val mRemoveItemButton:ImageButton = mView.findViewById(R.id.remove_item_button)
        val mLinearLayout = mView.findViewById<LinearLayout>(R.id.name_price_container)
        val mThumbnail = mView.findViewById<ImageView>(R.id.product_thumbnail)
        var mItem: ScannerItem? = null
    }
}
