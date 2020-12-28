package com.titos.barcodescanner.khataFeature

import android.content.Context
import android.os.Bundle
import android.os.Parcelable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.KhataDetails
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class KhataFragmentOutside : BaseFragment(R.layout.fragment_khata_outside), SearchView.OnQueryTextListener {
    private lateinit var groupAdapter: GroupAdapter<GroupieViewHolder>
    private lateinit var customerList : ArrayList<KhataItem>

    override fun initView() {
        showProgress("Please wait...")

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_khata_customers)
        groupAdapter = GroupAdapter()
        customerList = ArrayList()

        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        firebaseHelper.getAllKhata().observe(this) { kdList ->
            val onItemRemoveClick :((String, Int)->Unit) = { mobile, pos ->
                findNavController().navigate(R.id.action_khataFragment_to_khataFragmentInside, Bundle().apply {
                    putString("mobile", mobile)
                    putString("name", kdList[mobile]!!.customerName)
                    putDouble("amountDue", kdList[mobile]!!.amountDue)
                    putSerializable("changes", kdList[mobile]!!.changes as HashMap<String, String>)
                })
            }

            layoutView.findViewById<FloatingActionButton>(R.id.btn_new_khata).setOnClickListener {
                findNavController().navigate(R.id.action_khataFragment_to_agreementFragment, Bundle().apply {
                    putSerializable("kdMap", kdList as HashMap<String, KhataDetails>)
                })
            }

            var notPaidTotal = 0
            var paidTotal = 0
            kdList.forEach{ kd ->
                notPaidTotal += kd.value.amountDue.toInt()
                paidTotal += kd.value.amountPaid.toInt()
            }

            layoutView.findViewById<TextView>(R.id.tv_total_paid).text = "₹ $paidTotal"
            layoutView.findViewById<TextView>(R.id.tv_total_due).text = "₹ $notPaidTotal"

            if (kdList.isEmpty())
                layoutView.findViewById<TextView>(R.id.tv_empty).visibility = View.VISIBLE

            for (kd in kdList) {
                customerList.add(KhataItem(kd.key, kd.value.customerName, kd.value.amountDue.toString(), onItemRemoveClick))
            }
            groupAdapter.addAll(customerList)
            dismissProgress()
        }

        val searchView = layoutView.findViewById<SearchView>(R.id.search_bar_khata)
        searchView.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filter(newText)
        return false
    }

    private fun filter(charText: String) {
        val lowerCaseText = charText.toLowerCase(Locale.getDefault())

        if (lowerCaseText.isNotEmpty())
        {
            groupAdapter.clear()
            groupAdapter.addAll(customerList.filter { it.mobileNumber.toLowerCase(Locale.getDefault()).contains(lowerCaseText) })
        }
        else
        {
            groupAdapter.clear()
            groupAdapter.addAll(customerList)
        }

    }
}