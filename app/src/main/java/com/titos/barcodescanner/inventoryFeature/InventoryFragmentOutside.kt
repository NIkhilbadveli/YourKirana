package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.os.Bundle

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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_dashboard_outside.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryFragmentOutside : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_inventory_outside, container, false)

        val category = arrayOf("Branded Foods","Loose Items","Fridge Products","Beauty","Health and Hygiene","Home Needs")

        val viewPager = view.findViewById<ViewPager2>(R.id.pagerInventory)
        viewPager.adapter = PagerAdapter(this, category)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager){tab, position ->
            tab.text = category[position]

        }.attach()


        return view
    }

    class PagerAdapter(fm: Fragment, category: Array<String>) : FragmentStateAdapter(fm) {

        private val categoryList = category

        override fun getItemCount(): Int  = 6

        override fun createFragment(position: Int): Fragment {
            val fragment = InventoryFragmentInside()
            fragment.arguments = Bundle().apply {
                putString("inventoryCategory",  categoryList[position])
            }
            return fragment
        }
    }

}