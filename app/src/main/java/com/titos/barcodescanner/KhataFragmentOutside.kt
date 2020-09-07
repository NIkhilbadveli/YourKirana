package com.titos.barcodescanner


import android.content.Context
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class KhataFragmentOutside : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layoutView = inflater.inflate(R.layout.fragment_khata_outside, container, false)
        val viewPager = layoutView.findViewById<ViewPager2>(R.id.pagerKhata)
        viewPager.adapter = PagerAdapter(this)

        val tabLayout = layoutView.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager){tab, position ->
            tab.text = when(position){
                0 -> "Not Paid"
                1 -> "Paid"

                else -> "Wrong"
            }

        }.attach()

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!
        val khataRef = FirebaseDatabase.getInstance().reference.child("khataBook/$shopName")
        khataRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                var notPaidTotal = 0
                var paidTotal = 0
                for (timeStamp in p0.children){
                    if (timeStamp.child("status").value.toString()=="notPaid")
                        notPaidTotal += timeStamp.child("amountDue").value.toString().toFloat().toInt()
                    else if(timeStamp.child("status").value.toString()=="paid")
                        paidTotal += timeStamp.child("amountDue").value.toString().toFloat().toInt()
                }
                layoutView.findViewById<TextView>(R.id.tv_total_paid).text = "₹ $paidTotal"
                layoutView.findViewById<TextView>(R.id.tv_total_due).text = "₹ $notPaidTotal"
            }

        })

        return layoutView
    }

    class PagerAdapter(fm: Fragment) : FragmentStateAdapter(fm) {

        override fun getItemCount(): Int  = 2

        override fun createFragment(position: Int): Fragment {
            val fragment = KhataFragmentInside()
            fragment.arguments = Bundle().apply {

                putInt("khataNumber", position )
            }
            return fragment
        }
    }
}