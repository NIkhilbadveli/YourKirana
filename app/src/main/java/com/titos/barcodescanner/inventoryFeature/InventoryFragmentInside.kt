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
import com.titos.barcodescanner.scannerFeature.AddNewProductFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.*
import kotlin.collections.ArrayList


class InventoryFragmentInside : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var groupAdapterScanned : GroupAdapter<GroupieViewHolder>

    private var onItemRemoveClick :((Int)->Unit)? = null
    private var onItemEditClick :((Int)->Unit)? = null
    private var onItemStockClick :((Int)->Unit)? = null

    private var recyclerViewScannedItems:RecyclerView? = null
    private var inventoryList = ArrayList<InventoryItem>()
    private lateinit var layoutView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        layoutView =  inflater.inflate(R.layout.fragment_inventory_inside, container, false)

        groupAdapterScanned = GroupAdapter()
        recyclerViewScannedItems = layoutView.findViewById(R.id.rv_mystore_scannable)
        recyclerViewScannedItems!!.apply {
            adapter = groupAdapterScanned
            layoutManager = LinearLayoutManager(context)
        }

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        val shopName = sharedPref.getString("shopName","Temp Store")!!

        onItemRemoveClick = {pos ->

            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        groupAdapterScanned.removeGroupAtAdapterPosition(pos)
                        groupAdapterScanned.notifyItemRangeChanged(pos,groupAdapterScanned.itemCount)

                        FirebaseDatabase.getInstance().reference
                                .child("inventoryData/$shopName/${inventoryList[pos].barcode}").removeValue()
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
                putString("barcode",inventoryList[it].barcode)
                putString("name", inventoryList[it].itemName)
            })
        }

        onItemEditClick = { pos ->
            val bundle = Bundle()
            bundle.putString("barcode", inventoryList[pos].barcode)
            bundle.putBoolean("edit", true)

            //Update inventoryList using the list from callback
            val callAddToList: (ArrayList<String>)->Unit = { list ->
                inventoryList[pos].barcode = list[0]
                inventoryList[pos].itemName = list[1]
                inventoryList[pos].itemPrice = list[2]
                inventoryList[pos].itemQty = list[3]
                groupAdapterScanned.notifyItemChanged(pos)
            }
            val addNewProductFragment = AddNewProductFragment(callAddToList)
            addNewProductFragment.arguments = bundle

            val manager = parentFragmentManager
            val ft = manager.findFragmentByTag("addNewProductFragment")
            if (ft!=null)
                manager.beginTransaction().remove(ft)

            addNewProductFragment.show(manager, "addNewProductFragment")

        }

        populateView()

        val searchView = layoutView.findViewById<SearchView>(R.id.simpleSearchView)
        searchView.setOnQueryTextListener(this)

        return layoutView
    }

    override fun onResume() {
        super.onResume()
        groupAdapterScanned.clear()
        inventoryList.clear()
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
            groupAdapterScanned.addAll(inventoryList.filter { it.itemName.toLowerCase(Locale.getDefault()).contains(lowerCaseText) })
        }
        else{
            groupAdapterScanned.clear()
            groupAdapterScanned.addAll(inventoryList)
        }
    }

    private fun populateView(){

        val items = arguments?.getParcelableArrayList<InventoryFragmentOutside.InventoryDetails>("inventoryList")!!


            items.forEach {
                inventoryList.add(InventoryItem(it.barcode, it.name, it.qty.toString(), it.price.toString(), onItemRemoveClick!!, onItemStockClick!!, onItemEditClick!!))
            }

            groupAdapterScanned.addAll(inventoryList)

            if (items.size > 0) {
                var marginSum = 0.0
                for (i in 0 until items.size) {
                    marginSum += (items[i].price - items[i].cost).toDouble() / items[i].price
                }
                layoutView.findViewById<TextView>(R.id.tv_margin).text = "${(marginSum / items.size * 100).toInt()}%"
            }

    }

}
