package com.titos.barcodescanner.historyFeature

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


import android.widget.TextView
import androidx.lifecycle.observe

import androidx.navigation.fragment.findNavController


import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

import java.text.SimpleDateFormat
import java.util.*

class OrderItemsFragment : BaseFragment(R.layout.fragment_items_order){

    override fun initView() {

        val view = layoutView

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

        firebaseHelper.getMultipleProductDetails(barcodeList).observe(this) {pdMap ->
            for(pd in pdMap) {
                val sp = pd.value.sellingPrice
                val name = pd.value.name

                val total = view.findViewById<TextView>(R.id.tvItemTotalval)
                val savings = view.findViewById<TextView>(R.id.tvDiscountVal)
                val grandTotal = view.findViewById<TextView>(R.id.tvGrandTotalVal)
                view.findViewById<TextView>(R.id.dealerName).text = shopName

                val discount = 0.02 * orderValue.toDouble().round(2)

                total.text = "₹ $orderValue"
                savings.text = "- ₹ 0.0"
                grandTotal.text = "₹ ${orderValue.toFloat()}"

                val loose = pd.value.type == "kgs"
                groupAdapter.add(OrderItem(name, qtyList[barcodeList.indexOf(pd.key)], sp, loose))
            }
        }
    }

}
