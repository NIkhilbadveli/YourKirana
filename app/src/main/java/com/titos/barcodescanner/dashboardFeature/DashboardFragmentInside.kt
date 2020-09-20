package com.titos.barcodescanner.dashboardFeature

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
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
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.dashboardFeature.DashboardFragmentOutside
import com.titos.barcodescanner.historyFeature.OrderItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class DashboardFragmentInside : BaseFragment(R.layout.fragment_dashboard_inside) {

    override fun initView() {
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_top_five)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
        recyclerView.isNestedScrollingEnabled = false

        firebaseHelper.getAllInventory().observe(this) { pdList ->
            val topFiveList = ArrayList<TopFiveItem>()
            for (pd in pdList){
                val sales = pd.value.sellingPrice.toInt()*pd.value.sold
                topFiveList.add(TopFiveItem(pd.value.name, pd.value.sold.toString(), sales.toString()))
            }

            when(arguments?.getInt("itemType")){
                0 -> {groupAdapter.addAll(topFiveList.sortedByDescending { it.sales.toInt() }.take(25))}
                1 -> {groupAdapter.addAll(topFiveList.sortedByDescending { it.sales.toInt() }.takeLast(25))}
            }
        }
    }
}