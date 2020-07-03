package com.titos.barcodescanner.dashboardFeature

import android.content.Context
import android.os.Bundle
import android.util.Log

import androidx.fragment.app.Fragment

import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var shopName = "Temp Store"
    private var todaySales: Double = 0.0
    private var db: AppDatabase? = null
    private lateinit var barChart: BarChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        barChart = view.findViewById(R.id.barchart)

        barChart.xAxis.setDrawLabels(true)
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.setDrawAxisLine(false)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.axisLeft.isEnabled = false
        barChart.axisRight.isEnabled = false

        barChart.isDoubleTapToZoomEnabled = false
        barChart.setPinchZoom(false)
        barChart.isDragEnabled = false
        barChart.setScaleEnabled(false)
        barChart.isHighlightPerTapEnabled = false

        barChart.setBackgroundColor(resources.getColor(R.color.chartBackgroundLight))
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        db = AppDatabase(context!!)

        recyclerView = view.findViewById(R.id.list_of_top5)
        recyclerView!!.layoutManager = LinearLayoutManager(context)

        val databaseReference = FirebaseDatabase.getInstance().reference
        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)

        shopName = sharedPref?.getString("shopName",shopName)!!

        dashboardHelper()

        return view
    }

    private fun dashboardHelper() {

        val dateFormatter = SimpleDateFormat("dd-MM-yyyy",Locale.US)
        val otherDateFormat = SimpleDateFormat("dd MMM", Locale.US)
        val weekDays = getWeekDaySelected(dateFormatter.format(Date()))
        val startDate = otherDateFormat.format(dateFormatter.parse(weekDays[0])!!)
        val endDate = otherDateFormat.format(dateFormatter.parse(weekDays[6])!!)

        GlobalScope.launch (Dispatchers.IO){
            val totalSales = db?.crudMethods()?.getTotalSales()!!
            val itemLevelSales = db?.crudMethods()?.getAllItemsSales()
            val thisWeekSales = db?.crudMethods()?.getThisWeekSales(weekDays[0],weekDays[6])!!
            val allDaySales = db?.crudMethods()?.getAllDaysSales()

            withContext(Dispatchers.Main){

                dbrd_total_sales_value.text = "Rs. "+ totalSales!!.toDouble().toString()
                dbrd_this_week_sales_title.text = "$startDate - $endDate"
                dbrd_this_week_sales_value.text = "Rs. "+thisWeekSales.toString()

                if (itemLevelSales!!.size>5)
                    recyclerView!!.adapter = TopFiveItemAdapter(itemLevelSales.subList(0, 5),context!!)
                else
                    recyclerView!!.adapter = TopFiveItemAdapter(itemLevelSales,context!!)


            }
            populateBarEntries(allDaySales!!)
        }
    }

    private fun populateBarEntries(allDaySales: List<DaySales>) {

        val barEntries = ArrayList<BarEntry>()
        val barLabels = ArrayList<String>()
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy",Locale.US)
        val weekFormatter = SimpleDateFormat("E",Locale.US)
        val weekDays = getWeekDaySelected(dateFormatter.format(Date()))

        for (i in 0..6){
            barLabels.add(weekFormatter.format(dateFormatter.parse(weekDays[i])!!).first().toString())

            if (allDaySales.any{x->x.orderDate==weekDays[i]}){
                val thisDaySales = allDaySales.find { x-> x.orderDate==weekDays[i] }
                barEntries.add(BarEntry(i.toFloat(),thisDaySales!!.sales.toFloat()))
            }
            else
                barEntries.add(BarEntry(i.toFloat(),0f))
        }

        val barDataset = BarDataSet(barEntries,"Weekly Sales Values")
        barDataset.valueTextColor = resources.getColor(R.color.textColorWhite)
        barDataset.valueTextSize = 12.0f
        barDataset.setColor(resources.getColor(R.color.previousGreen))

        barChart.data = BarData(barDataset)
        barChart.barData.barWidth = 0.3f
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(barLabels)
        barChart.xAxis.textColor = resources.getColor(R.color.greyLighterShade)
        barChart.invalidate()
    }

    private fun getWeekDaySelected(selectedDateStr:String):ArrayList<String> {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("dd-MM-yyyy",Locale.US)
        val days = ArrayList<String>()
        val arr = selectedDateStr.split(("-").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

        cal.set(Calendar.YEAR, Integer.parseInt(arr[2]))
        cal.set(Calendar.MONTH, (Integer.parseInt(arr[1]) - 1))
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(arr[0]))

        val first = cal.clone() as Calendar
        first.add(Calendar.DAY_OF_WEEK, first.firstDayOfWeek - first.get(Calendar.DAY_OF_WEEK))
        first.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        for (i in 0..6)
        {
            days.add(format.format(first.time))
            first.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }
}
