package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle

import androidx.fragment.app.Fragment

import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.titos.barcodescanner.*


import kotlin.collections.ArrayList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class StockMovementFragment : Fragment()
{
    private lateinit var sharedPref: SharedPreferences
    private var shopName = "Temp Store"
    private var barcode = "00000"
    private var stockList = ArrayList<StockItem>()
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val layoutView = inflater.inflate(R.layout.fragment_stock_movement, container, false)

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!
        barcode = if(arguments?.getString("barcode")!=null) arguments?.getString("barcode")!! else "00000"

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_stock)

        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val stcQty = layoutView.findViewById<TextView>(R.id.tv_stockQty)
        val tvName = layoutView.findViewById<TextView>(R.id.tv_name)
        tvName.text = arguments?.getString("name")!!

        val stockRef = FirebaseDatabase.getInstance().reference
                .child("stockMovement/$shopName/$barcode")

        stockRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                var finalQty = 0
                for(timeStamp in p0.children) {
                    val currentQty = timeStamp.value.toString()

                    when {
                        currentQty.take(1) == "+" -> {
                            finalQty += currentQty.substringAfter("+").toInt()
                            stockList.add(StockItem("Added: $currentQty",timeStamp.key!!, finalQty.toString()))
                        }
                        currentQty.take(1) == "-" -> {
                            finalQty -= currentQty.substringAfter("-").toInt()
                            stockList.add(StockItem("Sold: $currentQty",timeStamp.key!!, finalQty.toString()))
                        }
                        else -> {
                            finalQty = currentQty.toInt()
                            stockList.add(StockItem("Updated to: $currentQty",timeStamp.key!!, finalQty.toString()))
                        }
                    }
                }
                stcQty.text = finalQty.toString()
                groupAdapter.addAll(stockList.reversed())

            }

        })

        return layoutView
    }

}