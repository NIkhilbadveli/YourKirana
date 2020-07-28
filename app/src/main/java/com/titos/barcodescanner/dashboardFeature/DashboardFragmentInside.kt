package com.titos.barcodescanner.dashboardFeature


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.viewpager2.adapter.FragmentStateAdapter

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.titos.barcodescanner.R
import com.titos.barcodescanner.dashboardFeature.DashboardFragmentOutside

class DashboardFragmentInside : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val layoutView =  inflater.inflate(R.layout.fragment_dashboard_inside, container, false)


        return layoutView
    }


}