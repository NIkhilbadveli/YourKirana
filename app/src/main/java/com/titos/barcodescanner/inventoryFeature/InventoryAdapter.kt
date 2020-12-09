package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import kotlinx.android.synthetic.main.item_inventory.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
class InventoryAdapter(var inventoryList: MutableList<InventoryFragmentOutside.InventoryDetails>, val context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    var countryFilterList = ArrayList<InventoryFragmentOutside.InventoryDetails>()
    lateinit var onItemEditClick: (Int) -> Unit
    lateinit var onItemRemoveClick: (Int) -> Unit
    lateinit var onItemStockClick: (Int) -> Unit

    init {
        countryFilterList = inventoryList as ArrayList<InventoryFragmentOutside.InventoryDetails>
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_inventory, parent, false))
    }

    override fun getItemCount(): Int {
        return countryFilterList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(position: Int) {
            val inventoryDetails = countryFilterList[position]
            itemView.mystore_item_price.text = inventoryDetails.pd.sellingPrice
            itemView.mystore_item_name.text = inventoryDetails.pd.name
            val mThumbnail = itemView.inventory_imageview

            if(inventoryDetails.pd.type=="units")
                itemView.mystore_item_qty.text = inventoryDetails.pd.qty.toInt().toString() + " units"
            else if(inventoryDetails.pd.type=="kgs")
                itemView.mystore_item_qty.text = inventoryDetails.pd.qty.toString() + " kgs"

            itemView.mystore_item_name.setOnClickListener {
                onItemStockClick.invoke(position)
            }

            itemView.remove_inventory_item_button.setOnClickListener {
                onItemRemoveClick.invoke(position)
            }

            itemView.edit_quantity_button.setOnClickListener {
                onItemEditClick.invoke(position)
            }

            /*GlobalScope.launch {
                val imageUrl = if (!inventoryList[position].pd.url.contains("https://www.google."))
                    inventoryList[position].pd.url
                else
                    getFirstImageUrl(inventoryList[position].pd.name)

                withContext(Dispatchers.Main){
                    Glide.with(context).load(imageUrl)
                            .override(120, 120)
                            .placeholder(R.drawable.ic_broken_image_black_24dp)
                            .into(mThumbnail)
                }
            }*/
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                if (charSearch.isEmpty()) {
                    countryFilterList = inventoryList as ArrayList<InventoryFragmentOutside.InventoryDetails>
                } else {
                    val resultList = ArrayList<InventoryFragmentOutside.InventoryDetails>()
                    for (row in inventoryList) {
                        if (row.pd.name.toLowerCase(Locale.ROOT).contains(constraint.toString().toLowerCase(Locale.ROOT))) {
                            resultList.add(row)
                        }
                    }
                    countryFilterList = resultList
                }
                val filterResults = FilterResults()
                filterResults.values = countryFilterList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                countryFilterList = results?.values as ArrayList<InventoryFragmentOutside.InventoryDetails>
                notifyDataSetChanged()
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
            url = img[1].absUrl("src")
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