package com.titos.barcodescanner.inventoryFeature


import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import kotlin.collections.ArrayList

class InventoryFragmentInside : BaseFragment(R.layout.fragment_inventory_inside), SearchView.OnQueryTextListener {

    private var onItemRemoveClick :((Int)->Unit)? = null
    private var onItemEditClick :((Int)->Unit)? = null
    private var onItemStockClick :((Int)->Unit)? = null

    private var recyclerViewScannedItems:RecyclerView? = null
    private lateinit var inventoryAdapter: InventoryAdapter
    private val inventoryList = ArrayList<InventoryFragmentOutside.InventoryDetails>()

    override fun initView() {

        inventoryAdapter = InventoryAdapter(inventoryList, requireContext())

        recyclerViewScannedItems = layoutView.findViewById(R.id.rv_mystore_scannable)
        recyclerViewScannedItems!!.apply {
            adapter = inventoryAdapter
            layoutManager = LinearLayoutManager(context)
        }

        onItemRemoveClick = {pos ->

            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        showProgress("Deleting ${inventoryList[pos].pd.name}...")
                        firebaseHelper.removeProduct(inventoryList[pos].barcode).observe(this){
                            if (it) {
                                dismissProgress()
                                /*inventoryAdapter.countryFilterList.removeAt(pos)
                                inventoryAdapter.notifyItemRemoved(pos)
                                inventoryAdapter.notifyItemRangeChanged(pos, inventoryList.size)*/
                                findNavController().navigate(R.id.myStoreFragment)
                                Snackbar.make(requireView(),"Inventory Item deleted",Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("No") { dialog, id -> dialog.cancel()
                    }

            val alert = dialogBuilder.create()
            alert.setTitle("Delete Inventory Item")
            alert.show()

        }

        onItemStockClick = {
            findNavController().navigate(R.id.action_myStoreFragment_to_stockMovementFragment, Bundle().apply{
                putParcelable("invDetails", inventoryAdapter.countryFilterList[it])
            })
        }

        onItemEditClick = { pos ->
            val bundle = Bundle()
            bundle.putString("barcode", inventoryAdapter.countryFilterList[pos].barcode)
            bundle.putBoolean("edit", true)
            findNavController().navigate(R.id.action_myStoreFragment_to_addNewProductFragment, bundle)
        }

        populateView()

        val searchView = layoutView.findViewById<SearchView>(R.id.simpleSearchView)
        searchView.setOnQueryTextListener(this)

        inventoryAdapter.onItemEditClick = onItemEditClick!!
        inventoryAdapter.onItemRemoveClick = onItemRemoveClick!!
        inventoryAdapter.onItemStockClick = onItemStockClick!!
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        inventoryAdapter.filter.filter(newText)
        return false
    }

    private fun populateView(){

        val items = arguments?.getParcelableArrayList<InventoryFragmentOutside.InventoryDetails>("inventoryList")!!

        inventoryList.addAll(items)
        inventoryAdapter.notifyDataSetChanged()

        if (items.size > 0) {
            var totalSellingPrice = 0.0
            var totalCostPrice = 0.0
            for (i in 0 until items.size) {
                totalSellingPrice += items[i].pd.sellingPrice.toDouble()
                totalCostPrice += items[i].pd.costPrice.toDouble()
            }

            layoutView.findViewById<TextView>(R.id.tv_margin).text = "${((totalSellingPrice - totalCostPrice) * 100/ totalSellingPrice).round(2)}%"
        }
    }
}
