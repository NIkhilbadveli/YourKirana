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

class StockMovementFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_stock_movement, container, false)




        return view
    }

}