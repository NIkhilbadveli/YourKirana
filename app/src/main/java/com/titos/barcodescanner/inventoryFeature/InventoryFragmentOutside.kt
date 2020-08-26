package com.titos.barcodescanner.inventoryFeature

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
import com.titos.barcodescanner.profileFeature.ProfileFragment
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_dashboard_outside.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryFragmentOutside : Fragment()
{
    private val inventoryList = ArrayList<ArrayList<InventoryDetails>>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_inventory_outside, container, false)

        val category = arrayOf("All", "Branded Foods","Loose Items","Fridge Products","Beauty","Health and Hygiene","Home Needs")

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        val shopName = sharedPref.getString("shopName","Temp Store")!!

        //Adding empty list for all categories
        for (i in category.indices)
            inventoryList.add(ArrayList())

        val dialog = ProgressDialog.progressDialog(requireContext())
        dialog.findViewById<TextView>(R.id.login_tv_dialog).text = "Wait..."

        dialog.show()
        val prodRef = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName")
        prodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {


                for (barcode in p0.children) {
                    val name = barcode.child("name").value.toString()
                    val sp = barcode.child("sellingPrice").value.toString().toInt()
                    val qty = barcode.child("qty").value.toString().toInt()
                    val cp = barcode.child("costPrice").value.toString().toInt()
                    val item = InventoryDetails(barcode.key!!,name, sp, qty, cp)

                    val matchedPos = category.indexOf(barcode.child("category").value.toString())
                    inventoryList[matchedPos].add(item)
                }

                for (single in inventoryList.takeLast(6))
                    inventoryList[0].plusAssign(single)

                val viewPager = view.findViewById<ViewPager2>(R.id.pagerInventory)
                viewPager.adapter = PagerAdapter(this@InventoryFragmentOutside, inventoryList)

                val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
                TabLayoutMediator(tabLayout, viewPager){tab, position ->
                    tab.text = category[position]

                }.attach()

                dialog.dismiss()
            }
        })

        return view
    }

    class PagerAdapter(fm: Fragment, inventoryList: ArrayList<ArrayList<InventoryDetails>>) : FragmentStateAdapter(fm) {

        val invList = inventoryList
        override fun getItemCount(): Int  = 7

        override fun createFragment(position: Int): Fragment {
            val fragment = InventoryFragmentInside()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList("inventoryList", invList[position] )
            }
            return fragment
        }
    }

    @Parcelize
    data class InventoryDetails(val barcode: String, val name: String, val price: Int, val qty: Int, val cost: Int): Parcelable
}