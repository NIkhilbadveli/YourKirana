package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Log.d

import androidx.fragment.app.Fragment

import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.titos.barcodescanner.*


import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

import java.text.SimpleDateFormat

import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.vision.L.d
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_dashboard_outside.*
import kotlinx.android.synthetic.main.item_stock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Console

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
        val stockRef = FirebaseDatabase.getInstance().reference
                .child("stockMovement/$shopName/$barcode")

        stockRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for(timeStamp in p0.children)
                {
                    val totQty = timeStamp.value.toString()
                    var tot = 0
                    val arrayStock = arrayOf(totQty)

                       if(arrayStock[0].toString() == "+")
                       {
                           tot += totQty.toInt()
                           stockList.add(StockItem("Added: $totQty",timeStamp.key!!,tot.toString()))
                       }
                       else
                       {
                           tot -= totQty.toInt()

                           stockList.add(StockItem("Sold: $totQty",timeStamp.key!!,tot.toString()))
                       }
                    stcQty.text = totQty

                }
                groupAdapter.addAll(stockList)

            }

        })

        return layoutView
    }

}