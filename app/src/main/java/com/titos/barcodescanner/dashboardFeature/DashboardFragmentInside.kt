package com.titos.barcodescanner.dashboardFeature


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.viewpager2.adapter.FragmentStateAdapter

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.R
import com.titos.barcodescanner.dashboardFeature.DashboardFragmentOutside
import com.titos.barcodescanner.historyFeature.OrderItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class DashboardFragmentInside : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val layoutView =  inflater.inflate(R.layout.fragment_dashboard_inside, container, false)

        val allItems = arguments?.getParcelableArrayList<BarcodeAndQty>("allItems")!!
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_top_five)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
        recyclerView.isNestedScrollingEnabled = false

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","Temp Store")!!

        val transactionRef = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName")
        transactionRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val topFiveList = ArrayList<TopFiveItem>()
                for (i in 0 until allItems.size){
                    val sales = p0.child(allItems[i].barcode).child("sellingPrice").value.toString().toInt()*allItems[i].qty
                    topFiveList.add(TopFiveItem(p0.child(allItems[i].barcode).child("name").value.toString(),
                            allItems[i].qty.toString(), sales.toString()))
                }

                when(arguments?.getInt("itemType")){
                    0 -> {groupAdapter.addAll(topFiveList.sortedByDescending { it.sales.toInt() }.take(25))}
                    1 -> {groupAdapter.addAll(topFiveList.sortedByDescending { it.sales.toInt() }.takeLast(25))}
                }
            }
        })

        return layoutView
    }


}