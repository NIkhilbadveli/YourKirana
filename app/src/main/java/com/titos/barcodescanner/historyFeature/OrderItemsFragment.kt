package com.titos.barcodescanner.historyFeature


import android.os.Bundle


import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView

import androidx.navigation.fragment.findNavController

import com.titos.barcodescanner.R

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

import java.text.SimpleDateFormat
import java.util.*



class OrderItemsFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_items_order, container, false)

        val itemsInOrder = arguments?.getStringArrayList("itemsInOrder")!!
        val orderValue = 0
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val otherDateFormat = SimpleDateFormat("dd MMM, EEE", Locale.US)

        view.findViewById<TextView>(R.id.order_date).text = "dd MMM, EEE"
        view.findViewById<TextView>(R.id.order_time).text = "00:00:00 am"
        view.findViewById<TextView>(R.id.order_value).text = "Rs. " + orderValue.toString()

        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_order_items)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
        view.findViewById<TextView>(R.id.go_back).setOnClickListener {
            findNavController().navigateUp()
        }
        for (item in itemsInOrder)
            groupAdapter.add(OrderItem(item))

        return view
    }
}
