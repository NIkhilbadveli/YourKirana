package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import kotlinx.android.synthetic.main.item_inventory.view.*
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

            /*subtract_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() - 1
                if (updatedQty>0){
                    itemQty = updatedQty.toString()
                    itemRef.child("qty").setValue(itemQty)
                    notifyChanged()
                }
            }

            add_quantity_button.setOnClickListener {
                val updatedQty = mystore_item_qty.text.toString().toInt() + 1
                itemQty = updatedQty.toString()
                itemRef.child("qty").setValue(itemQty)
                notifyChanged()
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

}