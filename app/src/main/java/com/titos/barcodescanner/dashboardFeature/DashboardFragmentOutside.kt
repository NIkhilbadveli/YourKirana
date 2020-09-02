package com.titos.barcodescanner.dashboardFeature

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_dashboard_outside.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragmentOutside : Fragment()
{
    private var shopName = "Temp Store"
    val itemLevelDetails = ArrayList<BarcodeAndQty>()
    private lateinit var barChart: BarChart
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_dashboard_outside, container, false)

        viewPager = view.findViewById<ViewPager2>(R.id.pagerDashboard)

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

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)

        shopName = sharedPref?.getString("shopName",shopName)!!
        dashboardHelper(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun dashboardHelper(view: View)
    {

        val dateFormatter = SimpleDateFormat("dd-MM-yyyy",Locale.US)
        val otherDateFormat = SimpleDateFormat("dd MMM", Locale.US)
        val weekDays = getWeekDaySelected(dateFormatter.format(Date()))
        val startDate = otherDateFormat.format(dateFormatter.parse(weekDays[0])!!)
        val endDate = otherDateFormat.format(dateFormatter.parse(weekDays[6])!!)

        var totalSales = 0.0

        var thisWeekSales = 0.0
        val allDaySales = ArrayList<DaySales>()

        val dialog = ProgressDialog.progressDialog(requireContext())
        dialog.findViewById<TextView>(R.id.login_tv_dialog).text = "Please Wait..."

        dialog.show()
        val transactionRef = FirebaseDatabase.getInstance().reference.child("transactionData/$shopName")
        transactionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for(day in p0.children){
                    var thisDaySales = 0.0
                    for (time in day.children){
                        totalSales += time.child("orderValue").value.toString().toDouble()
                        if (weekDays.contains(day.key)){
                            thisWeekSales += time.child("orderValue").value.toString().toDouble()
                            thisDaySales += time.child("orderValue").value.toString().toDouble()
                        }
                        for (barcode in time.child("items").children) {
                            if (itemLevelDetails.filter { it.barcode == barcode.key }.any())
                                itemLevelDetails.first { it.barcode == barcode.key }.qty += barcode.value.toString().toInt()
                            else
                                itemLevelDetails.add(BarcodeAndQty(barcode.key!!,barcode.value.toString().toInt()))
                        }
                    }
                    if (weekDays.contains(day.key))
                        allDaySales.add(DaySales(day.key!!, thisDaySales))
                }

                dialog.dismiss()

                viewPager.adapter = PagerAdapter(this@DashboardFragmentOutside, itemLevelDetails)
                val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
                TabLayoutMediator(tabLayout, viewPager){tab, position ->
                    tab.text = when(position){
                        0 -> "Top 25 items"
                        1 -> "Bottom 25 items"
                        else -> "Wrong"
                    }

                }.attach()

                view.findViewById<TextView>(R.id.dbrd_total_sales_value).text = "\u20B9 $totalSales"
                view.findViewById<TextView>(R.id.dbrd_this_week_sales_title).text = "$startDate - $endDate"
                view.findViewById<TextView>(R.id.dbrd_this_week_sales_value).text = "\u20B9 $thisWeekSales"

                populateBarEntries(allDaySales)
            }
        })
    }

    private fun populateBarEntries(allDaySales: List<DaySales>)
    {

        val barEntries = ArrayList<BarEntry>()
        val barLabels = ArrayList<String>()
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy",Locale.US)
        val weekFormatter = SimpleDateFormat("E",Locale.US)
        val weekDays = getWeekDaySelected(dateFormatter.format(Date()))

        for (i in 0..6){
            barLabels.add(weekFormatter.format(dateFormatter.parse(weekDays[i])!!).first().toString())

            if (allDaySales.any{x->x.orderDate==weekDays[i]})
            {
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

    class PagerAdapter(fm: Fragment,private val allItems: ArrayList<BarcodeAndQty>) : FragmentStateAdapter(fm) {

        override fun getItemCount(): Int  = 2

        override fun createFragment(position: Int): Fragment {
            val fragment = DashboardFragmentInside()
            fragment.arguments = Bundle().apply {
                putInt("itemType", position )
                putParcelableArrayList("allItems", allItems)
            }
            return fragment
        }
    }

    private fun getWeekDaySelected(selectedDateStr:String):ArrayList<String>
    {
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

data class DaySales(val orderDate: String, val sales: Double)

@Parcelize
data class BarcodeAndQty(val barcode: String, var qty: Int):Parcelable
