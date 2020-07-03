package com.titos.barcodescanner.historyFeature

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.titos.barcodescanner.AppDatabase
import com.titos.barcodescanner.R
import com.titos.barcodescanner.TransactionTable
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class OrderItemsFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_items_order, container, false)

        val itemsInOrder = arguments?.getParcelableArrayList<TransactionTable>("itemsInOrder")!!
        val orderValue = itemsInOrder.sumByDouble { it.itemPrice }
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val otherDateFormat = SimpleDateFormat("dd MMM, EEE", Locale.US)

        view.findViewById<TextView>(R.id.order_date).text = otherDateFormat.format(simpleDateFormat.parse(itemsInOrder[0].orderDate))
        view.findViewById<TextView>(R.id.order_time).text = itemsInOrder[0].orderTime
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
