package com.titos.barcodescanner.inventoryFeature


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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

    private var groupAdapterScanned = GroupAdapter<GroupieViewHolder>()

    private var onItemRemoveClick :((Int)->Unit)? = null
    private var onItemEditClick :((Int)->Unit)? = null

    private var recyclerViewScannedItems:RecyclerView? = null

    private var shopName = "Temp Store"
    private lateinit var sharedPref: SharedPreferences
    private var inventoryList = ArrayList<InventoryItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view =  inflater.inflate(R.layout.fragment_inventory_inside, container, false)

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!


        recyclerViewScannedItems = view.findViewById(R.id.rv_mystore_scannable)
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

        onItemEditClick = { pos ->
            val bundle = Bundle()
            bundle.putString("barcode", inventoryList[pos].barcode)
            bundle.putBoolean("edit", true)

            //Update inventoryList using the list from callback
            val callAddToList: (ArrayList<String>)->Unit = { list -> groupAdapterScanned.notifyDataSetChanged()}
            val addNewProductFragment = AddNewProductFragment(callAddToList)
            addNewProductFragment.arguments = bundle

            val manager = parentFragmentManager
            val ft = manager.findFragmentByTag("addNewProductFragment")
            if (ft!=null)
                manager.beginTransaction().remove(ft)

            addNewProductFragment.show(manager, "addNewProductFragment")

        }

        populateView()

        // setting up swipe to refresh
        //val itemsswipetorefresh = view.findViewById<SwipeRefreshLayout>(R.id.itemsswipetorefresh)
        /*itemsswipetorefresh.setOnRefreshListener {
            inventoryList.clear()
            groupAdapterScanned.clear()
            populateView()
            itemsswipetorefresh.isRefreshing = false
        }*/

        val searchView = view.findViewById<SearchView>(R.id.simpleSearchView)
        searchView.setOnQueryTextListener(this)

        return view
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

        val prodRef = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName")
        val category = arguments?.getString("inventoryCategory")!!

        prodRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for(barcode in p0.children)
                {
                    if (category==barcode.child("category").value.toString()) {
                        val name = barcode.child("name").value.toString()
                        val sp = barcode.child("sellingPrice").value.toString()
                        val qty = barcode.child("qty").value.toString()
                        val item = InventoryItem(false, name, qty, sp, onItemRemoveClick!!, onItemEditClick!!)
                        item.barcode = barcode.key!!

                        inventoryList.add(item)
                    }
                }
                groupAdapterScanned.addAll(inventoryList)
            }
        })

    }

}
