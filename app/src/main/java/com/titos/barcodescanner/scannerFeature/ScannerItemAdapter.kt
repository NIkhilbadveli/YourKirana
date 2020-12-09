package com.titos.barcodescanner.scannerFeature

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.titos.barcodescanner.R
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.IOException


class ScannerItemAdapter(val scannerItemList: ArrayList<ScannerItem>, val context: Context,
                         var onItemClick: ((Int, String, Double) -> Unit),
                         var onItemRemoveClick: ((Int) -> Unit)) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    fun addItem(item: ScannerItem) {
        scannerItemList.add(item)
        notifyItemInserted(scannerItemList.size - 1)
    }

    fun clear() {
        scannerItemList.clear()
        notifyDataSetChanged()
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
    
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_scanner, parent, false))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(position: Int) {
            val mNameview = itemView.findViewById<TextView>(R.id.item_name)
            val mQuantityview = itemView.findViewById<TextView>(R.id.item_quantity)
            val mpriceview = itemView.findViewById<TextView>(R.id.item_price)
            val mAddButton = itemView.findViewById<ImageButton>(R.id.add_quantity_button)
            val mSubtractButton = itemView.findViewById<ImageButton>(R.id.subtract_quantity_button)
            val mRemoveItemButton = itemView.findViewById<ImageButton>(R.id.remove_item_button)
            val mThumbnail = itemView.findViewById<ImageView>(R.id.product_thumbnail)
            val etQuantity = itemView.findViewById<EditText>(R.id.et_quantity)

            mNameview.text = scannerItemList[position].name.take(35)
            mQuantityview.text = scannerItemList[position].quantity
            mpriceview.text = (scannerItemList[position].price.toDouble() * scannerItemList[position].quantity.toDouble()).round(2).toString()

            val price = scannerItemList[position].price.toDouble()

            //Setting visibilities for loose items
            if (scannerItemList[position].loose) {
                mAddButton.visibility = View.GONE
                mSubtractButton.visibility = View.GONE
                mQuantityview.visibility = View.GONE
                etQuantity.visibility = View.VISIBLE
                etQuantity.setText(scannerItemList[position].quantity)
            }

            //Setting thumbnail
            GlobalScope.launch {
                val imageUrl = getFirstImageUrl(scannerItemList[position].name)

                withContext(Dispatchers.Main){
                    Glide.with(context).load(imageUrl)
                            .override(120, 120)
                            .placeholder(R.drawable.ic_broken_image_black_24dp)
                            .into(mThumbnail)
                }
            }

            ///look if this can be optimized by moving to the viewholder class below
            mAddButton.setOnClickListener {
                val value = mQuantityview.text.toString().toDouble() + 1
                mQuantityview.text = value.toString()
                mpriceview.text = (value * price).round(2).toString()
                onItemClick.invoke(position, (value * price).toString(), value)
            }

            mSubtractButton.setOnClickListener {
                val value = mQuantityview.text.toString().toDouble() - 1
                if (value > 0) {
                    mQuantityview.text = value.toString()
                    mpriceview.text = (value * price).round(2).toString()
                    onItemClick.invoke(position, (value * price).toString(), value)
                }
            }

            //On long press the add button or subract button
            val editText = EditText(context)
            editText.inputType = InputType.TYPE_CLASS_NUMBER

            val builder = AlertDialog.Builder(context)
                    .setTitle("Enter Quantity")
                    .setView(editText)
            builder.setPositiveButton("Ok") { dialog, whichButton ->
                val qty = editText.text.toString()
                if (qty.isNotEmpty()) {
                    val value = qty.toDouble()
                    mQuantityview.text = value.toString()
                    mpriceview.text = (value * price).round(2).toString()
                    onItemClick.invoke(position, (value * price).toString(), value)
                }
            }
            builder.setNegativeButton("Cancel") { dialog, whichButton ->}
            val alertDialog = builder.create()

            mAddButton.setOnLongClickListener {
                editText.setText(mQuantityview.text.toString())
                alertDialog.show()
                return@setOnLongClickListener true
            }

            mSubtractButton.setOnLongClickListener {
                editText.setText(mQuantityview.text.toString())
                alertDialog.show()
                return@setOnLongClickListener true
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
                if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                    etQuantity.clearFocus()
                }
                false
            }

            mRemoveItemButton.setOnClickListener {
                onItemRemoveClick.invoke(position)
            }
        }
    }

    private fun getFirstImageUrl(itemName: String): String {
        val width = 140
        val height = 300
        val webURL = ("https://www.google.com/search?tbm=isch&q="
                + itemName
                + "&tbs=isz:ex,iszw:"
                + width
                + ",iszh:"
                + height)

        var url = "https://www.google.co.in"
        try {
            val doc = Jsoup.connect(webURL)
                    .userAgent("Mozilla")
                    .get()
            val img = doc.getElementsByTag("img")
            url = if(img.size>1) img[1].absUrl("src") else "https://www.google.co.in"
            /*for (el in img) {
                val src: String = el.absUrl("src")
                println("src attribute is: $src")
            }*/
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return url
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(position)
    }

    override fun getItemCount(): Int {
        return scannerItemList.size
    }
}
