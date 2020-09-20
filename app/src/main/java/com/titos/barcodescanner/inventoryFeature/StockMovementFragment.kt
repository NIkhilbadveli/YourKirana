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
import com.titos.barcodescanner.base.BaseFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class StockMovementFragment : BaseFragment(R.layout.fragment_stock_movement) {

    private var stockList = ArrayList<StockItem>()
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun initView() {

        val invDetails = arguments?.getParcelable<InventoryFragmentOutside.InventoryDetails>("invDetails")!!

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_stock)

        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val stcQty = layoutView.findViewById<TextView>(R.id.tv_stockQty)
        val tvName = layoutView.findViewById<TextView>(R.id.tv_name)
        tvName.text = invDetails.pd.name

        var finalQty = 0
        invDetails.pd.changes.forEach {
            val timeStamp = it.key
            val currentQty = it.value

            when {
                currentQty.take(1) == "+" -> {
                    finalQty += currentQty.substringAfter("+").toInt()
                    stockList.add(StockItem("Added: $currentQty", timeStamp, finalQty.toString()))
                }
                currentQty.take(1) == "-" -> {
                    finalQty -= currentQty.substringAfter("-").toInt()
                    stockList.add(StockItem("Sold: $currentQty", timeStamp, finalQty.toString()))
                }
                else -> {
                    finalQty = currentQty.toInt()
                    stockList.add(StockItem("Updated to: $currentQty", timeStamp, finalQty.toString()))
                }
            }
        }
        stcQty.text = finalQty.toString()
        groupAdapter.addAll(stockList.reversed())
    }
}

