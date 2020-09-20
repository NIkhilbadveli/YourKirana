package com.titos.barcodescanner.inventoryFeature

import android.content.Context
import android.os.Bundle
import android.os.Parcelable

import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.observe
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.titos.barcodescanner.*


import kotlin.collections.ArrayList
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.ProductDetails
import com.titos.barcodescanner.utils.ProgressDialog
import kotlinx.android.parcel.Parcelize

class InventoryFragmentOutside : BaseFragment(R.layout.fragment_inventory_outside) {
    private val inventoryList = ArrayList<ArrayList<InventoryDetails>>()

    override fun initView() {
        val view = layoutView

        val category = arrayOf("All", "Branded Foods","Loose Items","Fridge Products","Beauty","Health and Hygiene","Home Needs")

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        //Adding empty list for all categories
        for (i in category.indices)
            inventoryList.add(ArrayList())

        showProgress("Please wait...")

        firebaseHelper.getAllInventory().observe(this) {pdMap ->
            for (pd in pdMap) {
                val item = InventoryDetails(pd.key, pd.value)
                val matchedPos = category.indexOf(pd.value.category)
                inventoryList[matchedPos].add(item)
            }

            //Creating overall list
            for (single in inventoryList.takeLast(6))
                inventoryList[0].plusAssign(single)

            val viewPager = view.findViewById<ViewPager2>(R.id.pagerInventory)
            viewPager.adapter = PagerAdapter(this@InventoryFragmentOutside, inventoryList)

            val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
            TabLayoutMediator(tabLayout, viewPager){tab, position ->
                tab.text = category[position]

            }.attach()

            dismissProgress()
        }


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
    data class InventoryDetails(val barcode: String, val pd: ProductDetails): Parcelable
}