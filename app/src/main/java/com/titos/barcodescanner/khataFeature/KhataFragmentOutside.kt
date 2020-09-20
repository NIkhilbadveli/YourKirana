package com.titos.barcodescanner.khataFeature

import android.content.Context
import android.os.Bundle
import android.os.Parcelable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe

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
import com.titos.barcodescanner.utils.KhataDetails

class KhataFragmentOutside : BaseFragment(R.layout.fragment_khata_outside) {

    override fun initView() {
        showProgress("Please wait...")
        firebaseHelper.getAllKhata().observe(this) { kdList ->
            var notPaidTotal = 0
            var paidTotal = 0
            for (kd in kdList){
                if (kd.status=="notPaid")
                    notPaidTotal += kd.amountDue.toFloat().toInt()
                else if(kd.status=="paid")
                    paidTotal += kd.amountDue.toFloat().toInt()
            }
            layoutView.findViewById<TextView>(R.id.tv_total_paid).text = "₹ $paidTotal"
            layoutView.findViewById<TextView>(R.id.tv_total_due).text = "₹ $notPaidTotal"

            val viewPager = layoutView.findViewById<ViewPager2>(R.id.pagerKhata)
            viewPager.adapter = PagerAdapter(this, ArrayList(kdList))

            val tabLayout = layoutView.findViewById<TabLayout>(R.id.tab_layout)
            TabLayoutMediator(tabLayout, viewPager){tab, position ->
                tab.text = when(position){
                    0 -> "Not Paid"
                    1 -> "Paid"

                    else -> "Wrong"
                }

            }.attach()
            dismissProgress()
        }
    }

    class PagerAdapter(fm: Fragment, private val kdList: ArrayList<KhataDetails>) : FragmentStateAdapter(fm) {

        override fun getItemCount(): Int  = 2

        override fun createFragment(position: Int): Fragment {
            val fragment = KhataFragmentInside()
            fragment.arguments = Bundle().apply {
                putInt("khataNumber", position )
                putParcelableArrayList("kdList", kdList)
            }
            return fragment
        }
    }
}