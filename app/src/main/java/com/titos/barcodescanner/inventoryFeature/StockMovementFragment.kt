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
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

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

        var finalQty = 0.0

        val timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a", Locale.ENGLISH)
        val sortedChanges = invDetails.pd.changes.toSortedMap(compareBy { LocalDateTime.parse(it, timeFormatter) })

        sortedChanges.forEach {
            val timeStamp = it.key
            val currentQty = it.value

            when {
                currentQty.take(1) == "+" -> {
                    finalQty += currentQty.substringAfter("+").toDouble()
                    stockList.add(StockItem("Added:     $currentQty", timeStamp, finalQty.round(2).toString()))
                }
                currentQty.take(1) == "-" -> {
                    finalQty -= currentQty.substringAfter("-").toDouble()
                    stockList.add(StockItem("Sold:      $currentQty", timeStamp, finalQty.round(2).toString()))
                }
                else -> {
                    finalQty = currentQty.toDouble()
                    stockList.add(StockItem("Updated to:        $currentQty", timeStamp, finalQty.round(2).toString()))
                }
            }
        }

        stcQty.text = finalQty.toString()
        groupAdapter.addAll(stockList.reversed())
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}

