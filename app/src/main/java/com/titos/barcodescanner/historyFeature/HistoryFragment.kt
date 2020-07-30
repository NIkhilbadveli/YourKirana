package com.titos.barcodescanner.historyFeature

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController

import com.titos.barcodescanner.R

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import kotlin.collections.ArrayList

class HistoryFragment : Fragment(){

    private var onItemRemoveClick :((Int, Int)->Unit)? = null
    private var shopName = "Temp Store"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_of_transactions)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
        shopName = sharedPref?.getString("shopName",shopName)!!

        populateView(view, groupAdapter)
        onItemRemoveClick = {orderId, pos ->
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", DialogInterface.OnClickListener {
                        _, _ ->

                        groupAdapter.removeGroupAtAdapterPosition(pos)
                        groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                        Snackbar.make(view!!,"Transaction successfully deleted",Snackbar.LENGTH_SHORT).show()
                    })
                    .setNegativeButton("No", DialogInterface.OnClickListener {
                        dialog, id -> dialog.cancel()
                    })

            val alert = dialogBuilder.create()
            alert.setTitle("Delete Transaction")
            alert.show()
        }

        groupAdapter.setOnItemClickListener { _, _ ->

            val itemsInOrder = ArrayList<String>()
            val bundle = Bundle()
            bundle.putStringArrayList("itemsInOrder", itemsInOrder)
            findNavController().navigate(R.id.action_historyFragment_to_orderItemsFragment, bundle)
        }

        return view
    }

    private fun populateView(view:View, groupAdapter:GroupAdapter<GroupieViewHolder>){
        val transactionRef = FirebaseDatabase.getInstance().reference.child("transactionData/$shopName")

        transactionRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.hasChildren()){
                    var id = 0
                    for (day in p0.children){
                        val section = Section()
                        section.setHeader(HistoryHeaderItem(day.key!!, 0.0))
                        groupAdapter.add(section)
                        for (time in day.children){
                            id++
                            groupAdapter.add(HistoryItem("Order No. $id" ,
                                    time.key!!, "0",onItemRemoveClick!!))
                        }

                    }
                }
                else
                    view.findViewById<LinearLayout>(R.id.empty_view_history).visibility = View.VISIBLE
            }

        })
    }
}

