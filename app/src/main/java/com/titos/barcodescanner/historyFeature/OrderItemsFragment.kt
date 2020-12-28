package com.titos.barcodescanner.historyFeature

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


import android.widget.TextView
import androidx.lifecycle.observe

import androidx.navigation.fragment.findNavController
import com.titos.barcodescanner.BillFragment


import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.scannerFeature.ScannerItem
import com.titos.barcodescanner.utils.BillDetails

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

        val barcodeList = arguments?.getStringArrayList("barcodeList")!!
        val qtyList = arguments?.getStringArrayList("qtyList")!!

        val billDetails = BillDetails()
        billDetails.orderValue = orderValue
        billDetails.contact = contact
        billDetails.time = refKey

        for (i in 0 until barcodeList.size) {
            val bd = barcodeList[i]
            firebaseHelper.getProductDetails(bd).observe(this) {pd ->
                val sp = pd.sellingPrice
                val name = pd.name
                val loose = pd.type == "kgs"
                groupAdapter.add(OrderItem(name, qtyList[barcodeList.indexOf(bd)], sp, loose))
                billDetails.billItems.add(ScannerItem(bd, name, qtyList[barcodeList.indexOf(bd)], sp, loose, "https://www.google.co.in"))
                if (billDetails.billItems.size == barcodeList.size) {
                    val billFragment = BillFragment(billDetails, requireContext())
                    view.findViewById<TextView>(R.id.btnShareBill).setOnClickListener {
                        billFragment.shareBill()
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.dealerName).text = shopName
        val grandTotal = view.findViewById<TextView>(R.id.tvGrandTotalVal)
        grandTotal.text = "₹ ${orderValue.toFloat()}"
    }

}
