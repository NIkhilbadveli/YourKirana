package com.titos.barcodescanner.historyFeature

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper

import com.google.android.material.snackbar.Snackbar
import com.titos.barcodescanner.*
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.SwipeToAgreement
import com.titos.barcodescanner.utils.TransactionDetails
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HistoryFragment : BaseFragment(R.layout.fragment_history){

    private var onItemRemoveClick :((Int, Int)->Unit)? = null
    private var onItemClick :((Int)->Unit)? = null
    private var keys = ArrayList<String>()
    private var contactList = ArrayList<String>()
    private var orderValueList = ArrayList<String>()
    private var qtyList = ArrayList<ArrayList<String>>()
    private var barcodeList = ArrayList<ArrayList<String>>()

    override fun initView(){
        val view = layoutView

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_of_transactions)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        populateView(view, groupAdapter)

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.US)
        onItemRemoveClick = { deletePosition, pos ->
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        showProgress("Deleting transaction ${keys[deletePosition]}")
                        firebaseHelper.getTransactionDetails(keys[deletePosition]).observe(this){
                            //Adding back all the products to Inventory in this particular transaction
                            val date = Date()
                            val dateFormat = simpleDateFormat.format(date)
                            it.items.forEach { item ->
                                firebaseHelper.updateQty(dateFormat, item.key, item.value.toDouble(), 0)
                            }
                            firebaseHelper.deleteTransaction(keys[deletePosition])
                            keys.removeAt(deletePosition)

                            dismissProgress()

                            groupAdapter.removeGroupAtAdapterPosition(pos)
                            groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                            Snackbar.make(requireView(),"Transaction successfully deleted",Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("No") { dialog, id -> dialog.cancel()
                    }

            val alert = dialogBuilder.create()
            alert.setTitle("Delete Transaction")
            alert.show()
        }

        onItemClick = { pos ->
            val bundle = Bundle()
            bundle.putString("refKey", keys[pos])
            bundle.putString("orderValue", orderValueList[pos])
            bundle.putString("contact", contactList[pos])
            bundle.putStringArrayList("barcodeList", barcodeList[pos])
            bundle.putStringArrayList("qtyList", qtyList[pos])
            findNavController().navigate(R.id.action_historyFragment_to_orderItemsFragment, bundle)
        }

    }

    private fun populateView(view:View, groupAdapter:GroupAdapter<GroupieViewHolder>){
        showProgress("Please wait...")

        firebaseHelper.getAllTransactions().observe(this) { tdMap ->
            if (tdMap.isNotEmpty()){
                var id = tdMap.size
                val timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a", Locale.ENGLISH)
                val allDaySales = getAllDaySales(tdMap)
                allDaySales.forEach {ds ->
                    groupAdapter.add(HistoryHeaderItem(ds.orderDate, ds.sales))
                    for (time in tdMap.filter { it.key.split(' ').first() == ds.orderDate }
                            .toSortedMap(compareByDescending { LocalDateTime.parse(it, timeFormatter) })){
                        val orderValue = time.value.orderValue
                        val hour = time.key.split(" ")[1].split(":").first()
                        val amPm = time.key.split(" ").last()

                        var isItNight = false
                        if (amPm=="PM"&&hour.toInt()>6)
                            isItNight = true

                        groupAdapter.add(HistoryItem("Order No. $id" ,
                                time.key, isItNight, orderValue ,onItemRemoveClick!!, onItemClick!!))
                        keys.add(time.key)
                        orderValueList.add(orderValue)
                        contactList.add(time.value.contact)
                        val tempList2 = ArrayList<String>()
                        val tempList3 = ArrayList<String>()
                        for (barcode in time.value.items) {
                            tempList2.add(barcode.key)
                            tempList3.add(barcode.value)
                        }
                        barcodeList.add(tempList2)
                        qtyList.add(tempList3)
                        id--
                    }
                }

                reverseAllTheLists()

                dismissProgress()
            }
            else{
                view.findViewById<LinearLayout>(R.id.empty_view_history).visibility = View.VISIBLE
                dismissProgress()
            }
        }

    }

    private fun reverseAllTheLists() {
        keys.reverse()
        orderValueList.reverse()
        contactList.reverse()
        barcodeList.reverse()
        qtyList.reverse()
    }

    private fun getAllDaySales(tdMap: Map<String, TransactionDetails>): List<DaySales> {
        val allDaySales = ArrayList<DaySales>()
        tdMap.forEach { td ->
            if (!allDaySales.filter { it.orderDate == td.key.split(" ").first() }.any()) {
                allDaySales.add(DaySales(td.key.split(" ").first(), td.value.orderValue.toDouble()))
            }
            else
                allDaySales.first { it.orderDate == td.key.split(" ").first() }.sales += td.value.orderValue.toDouble()
        }

        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        return allDaySales.sortedByDescending { LocalDate.parse(it.orderDate, dateTimeFormatter) }
    }
}

data class DaySales(val orderDate: String, var sales: Double = 0.0)

