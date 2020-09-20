package com.titos.barcodescanner.profileFeature

import android.content.Context

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.RequestDetails

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_customer_requests.*

class CustomerRequestsFragment : BaseFragment(R.layout.fragment_customer_requests) {
    private val checkedList = ArrayList<CustomerRequestItem>()
    private val uncheckedList = ArrayList<CustomerRequestItem>()
    private lateinit var onUpdateListener: (String,RequestDetails, Int, Int) -> Unit
    private val groupAdapterChecked = GroupAdapter<GroupieViewHolder>()

    override fun initView() {
        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_requests)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val recyclerViewChecked = layoutView.findViewById<RecyclerView>(R.id.rv_requests_checked)
        recyclerViewChecked.apply {
            adapter = groupAdapterChecked
            layoutManager = LinearLayoutManager(requireContext())
        }

        onUpdateListener = { refKey, rd, mode, pos ->
            //mode = 0: uncheck to check, 1: check to uncheck, 2: subtract, 3: add, 4: namechange
            firebaseHelper.updateRequest(refKey, rd)
            when(mode){
                0 -> {
                    groupAdapter.removeGroupAtAdapterPosition(pos)
                    groupAdapterChecked.add(CustomerRequestItem(refKey, rd, onUpdateListener))
                }
                1 -> {
                    groupAdapterChecked.removeGroupAtAdapterPosition(pos)
                    groupAdapter.add(CustomerRequestItem(refKey, rd, onUpdateListener))
                }
                2 -> {  }
                3 -> {  }
                4 -> {  }
            }
        }

        populateView(groupAdapter)

        var clicked = false
        val addNewRequestButton = layoutView.findViewById<Button>(R.id.add_new_request)
        addNewRequestButton.setOnClickListener {
            if (!clicked){
                add_new_request.text = "Save"
                input_container.visibility = View.VISIBLE
                clicked = true
            }
            else{
                if (product_name.text.isNotEmpty()){
                    val key = firebaseHelper.addCustomerRequest(RequestDetails(product_name.text.toString()))
                    groupAdapter.add(CustomerRequestItem(key, RequestDetails(product_name.text.toString()), onUpdateListener))
                }
                /*else
                    Toast.makeText(requireContext(),"Please Enter something", Toast.LENGTH_SHORT).show()*/

                add_new_request.text = "Add New Request"
                input_container.visibility = View.GONE
                clicked = false
            }
        }
    }

    private fun populateView(groupAdapter: GroupAdapter<GroupieViewHolder>) {
        showProgress("Please wait...")
        checkedList.clear()
        uncheckedList.clear()
        firebaseHelper.getAllRequests().observe(this) { rdList ->
            for (rd in rdList){
                if (!rd.value.checked)
                    uncheckedList.add(CustomerRequestItem(rd.key,rd.value, onUpdateListener))
                else
                    checkedList.add(CustomerRequestItem(rd.key,rd.value, onUpdateListener))
            }

            groupAdapter.addAll(uncheckedList)
            groupAdapterChecked.addAll(checkedList)
            dismissProgress()
        }
    }
}
