package com.titos.barcodescanner.inventoryFeature


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity.apply
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.scannerFeature.AddNewProductFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.*
import kotlin.collections.ArrayList

class InventoryFragmentInside : BaseFragment(R.layout.fragment_inventory_inside), SearchView.OnQueryTextListener {

    private lateinit var groupAdapterScanned : GroupAdapter<GroupieViewHolder>

    private var onItemRemoveClick :((Int)->Unit)? = null
    private var onItemEditClick :((Int)->Unit)? = null
    private var onItemStockClick :((Int)->Unit)? = null

    private var recyclerViewScannedItems:RecyclerView? = null
    private var inventoryList = ArrayList<InventoryItem>()
    private var filteredList = ArrayList<InventoryItem>()

    override fun initView() {

        groupAdapterScanned = GroupAdapter()
        recyclerViewScannedItems = layoutView.findViewById(R.id.rv_mystore_scannable)
        recyclerViewScannedItems!!.apply {
            adapter = groupAdapterScanned
            layoutManager = LinearLayoutManager(context)
        }

        onItemRemoveClick = {pos ->

            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        groupAdapterScanned.removeGroupAtAdapterPosition(pos)
                        groupAdapterScanned.notifyItemRangeChanged(pos,groupAdapterScanned.itemCount)
                        firebaseHelper.removeProduct(filteredList[pos].inventoryDetails.barcode)
                        Snackbar.make(requireView(),"Inventory Item deleted",Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No") { dialog, id -> dialog.cancel()
                    }

            val alert = dialogBuilder.create()
            alert.setTitle("Delete Inventory Item")
            alert.show()

        }

        onItemStockClick = {
            findNavController().navigate(R.id.action_myStoreFragment_to_stockMovementFragment, Bundle().apply{
                putParcelable("invDetails", filteredList[it].inventoryDetails)
            })
        }

        onItemEditClick = { pos ->
            val bundle = Bundle()
            bundle.putString("barcode", filteredList[pos].inventoryDetails.barcode)
            bundle.putBoolean("edit", true)
            findNavController().navigate(R.id.action_myStoreFragment_to_addNewProductFragment, bundle)
        }

        populateView()

        val searchView = layoutView.findViewById<SearchView>(R.id.simpleSearchView)
        searchView.setOnQueryTextListener(this)

    }

    override fun onResume() {
        super.onResume()
        groupAdapterScanned.clear()
        inventoryList.clear()
        filteredList.clear()
        populateView()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filter(newText)
        return false
    }

    private fun filter(charText: String) {
        val lowerCaseText = charText.toLowerCase(Locale.getDefault())

        if (lowerCaseText.isNotEmpty()) {
            groupAdapterScanned.clear()
            filteredList.clear()
            filteredList = ArrayList(inventoryList.filter { it.inventoryDetails.pd.name.toLowerCase(Locale.getDefault()).contains(lowerCaseText) })
            groupAdapterScanned.addAll(filteredList)
        }
        else{
            groupAdapterScanned.clear()
            filteredList.clear()
            filteredList = inventoryList
            groupAdapterScanned.addAll(inventoryList)
        }
    }

    private fun populateView(){

        val items = arguments?.getParcelableArrayList<InventoryFragmentOutside.InventoryDetails>("inventoryList")!!

        items.forEach {
            inventoryList.add(InventoryItem(it, onItemRemoveClick!!, onItemStockClick!!, onItemEditClick!!))
        }

        filteredList = ArrayList(inventoryList)
        groupAdapterScanned.addAll(inventoryList)
        if (items.size > 0) {
            var marginSum = 0.0
            for (i in 0 until items.size) {
                marginSum += (items[i].pd.sellingPrice.toInt() - items[i].pd.costPrice.toInt()).toDouble() / items[i].pd.sellingPrice.toInt()
            }
            layoutView.findViewById<TextView>(R.id.tv_margin).text = "${(marginSum / items.size * 100).toInt()}%"
        }
    }
}
