package com.titos.barcodescanner.historyFeature


import android.content.Context
import android.os.Bundle


import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView

import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.titos.barcodescanner.R

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_items_order.*

import java.text.SimpleDateFormat
import java.util.*



class OrderItemsFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_items_order, container, false)

        val refKey = arguments?.getString("refKey")!!
        val contact = arguments?.getString("contact")!!

        val date = refKey.split('/').first()
        val time = refKey.split('/').last()
        val orderValue = arguments?.getString("orderValue")!!
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val otherDateFormat = SimpleDateFormat("dd MMM, EEE", Locale.US)

        view.findViewById<TextView>(R.id.order_date).text = otherDateFormat.format(simpleDateFormat.parse(date)!!)
        view.findViewById<TextView>(R.id.order_time).text = time
        view.findViewById<TextView>(R.id.tvCustNum).text = if (contact!="null") contact else "Not available"

        val orderVal = view.findViewById<TextView>(R.id.order_value)
        orderVal.text = "₹ $orderValue"

        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_order_items)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        view.findViewById<TextView>(R.id.go_back).setOnClickListener {
            findNavController().navigateUp()
        }

        val barcodeList = arguments?.getStringArrayList("barcodeList")!!
        val qtyList = arguments?.getStringArrayList("qtyList")!!

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","Temp Store")!!

        val inventoryRef = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName")
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (i in 0 until barcodeList.size){
                    val sp =  p0.child(barcodeList[i]).child("sellingPrice").value.toString()
                    val name = p0.child(barcodeList[i]).child("name").value.toString()

                    val total = view.findViewById<TextView>(R.id.tvItemTotalval)
                    val savings = view.findViewById<TextView>(R.id.tvDiscountVal)
                    val grandTotal = view.findViewById<TextView>(R.id.tvGrandTotalVal)
                    view.findViewById<TextView>(R.id.dealerName).text= shopName

                    val discount = 0.02*orderValue.toFloat()

                    total.text = "₹ $orderValue"
                    savings.text = "- ₹ $discount"
                    grandTotal.text = "₹ ${orderValue.toFloat() - discount}"

                    groupAdapter.add(OrderItem(name, qtyList[i], sp))
                }
            }
        })

        return view
    }
}
