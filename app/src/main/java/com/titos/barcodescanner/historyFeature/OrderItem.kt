package com.titos.barcodescanner.historyFeature

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.titos.barcodescanner.R

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_order.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class OrderItem(val itemName: String, val itemQty: String, val itemPrice: String, val loose:Boolean): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            tvProductName.text = itemName
            if (loose) {
                tvTotalPrice.text = "₹ ${(itemPrice.toDouble() * itemQty.toDouble()).round(2)}"
                tvProductQuantity.text = "$itemQty units x ₹ $itemPrice"
            }
            else{
                tvTotalPrice.text = "₹ ${itemPrice.toDouble() * itemQty.toDouble()}"
                tvProductQuantity.text = "$itemQty units x ₹ $itemPrice"
            }

            val mImageView: ImageView = containerView.findViewById(R.id.img)
            //Setting thumbnail
            GlobalScope.launch {
                val imageUrl = getFirstImageUrl(itemName.replace('.', ' '))

                withContext(Dispatchers.Main){
                    Glide.with(containerView.context).load(imageUrl)
                            .override(120, 120)
                            .placeholder(R.drawable.ic_broken_image_black_24dp)
                            .into(mImageView)
                }
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_order

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
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

}