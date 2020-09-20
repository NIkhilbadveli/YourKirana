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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.*
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.ProgressDialog
import com.titos.barcodescanner.utils.SwipeToAgreement
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
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

        val swipeHandler = object : SwipeToAgreement(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val tvOrderNumber = viewHolder.itemView.findViewById<TextView>(R.id.history_order_number)
                if(tvOrderNumber!=null) {
                    val orderNumber = tvOrderNumber.text.toString()
                    val pos = orderNumber.split(" ").last().toInt() - 1

                    val bundle = Bundle()
                    bundle.putString("amountDue", orderValueList[pos])
                    bundle.putString("contact", contactList[pos])
                    findNavController().navigate(R.id.action_historyFragment_to_agreementFragment, bundle)
                }
                else{
                    Toast.makeText(requireContext(), "Don't swipe here :)", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.historyFragment)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        populateView(view, groupAdapter)
        onItemRemoveClick = { deletePosition, pos ->
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        firebaseHelper.deleteTransaction(keys[deletePosition])
                        groupAdapter.removeGroupAtAdapterPosition(pos)
                        groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                        Snackbar.make(requireView(),"Transaction successfully deleted",Snackbar.LENGTH_SHORT).show()
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

        val dateStrToLocalDateTime: (String) -> LocalDateTime = {
            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"))
        }

        firebaseHelper.getAllTransactions().observe(this) { tdMap ->
            if (tdMap.isNotEmpty()){
                var id = 0
                val headerList = ArrayList<HistoryHeaderItem>()
                val itemList = ArrayList<HistoryItem>()

                val sortedMap = tdMap.toSortedMap(compareByDescending { it })

                for(time in sortedMap){
                    //val tempList = ArrayList<HistoryItem>()
                    /*var daySales = 0

                    for (time in day.children){*/
                    id++
                    val orderValue = time.value.orderValue
                    //daySales += orderValue.toDouble().toInt()
                    itemList.add(HistoryItem("Order No. $id" ,
                            time.key!!, orderValue ,onItemRemoveClick!!, onItemClick!!))
                    keys.add(time.key)
                    orderValueList.add(orderValue)
                    contactList.add(time.value.contact)

                    //headerList.add(HistoryHeaderItem(day.key!!, daySales.toDouble()))
                    //itemList.add(tempList)
                }
                groupAdapter.addAll(itemList)
                /*for (i in headerList.size - 1 downTo 0 ){
                    val section = Section()
                    section.add(headerList[i])
                    groupAdapter.add(section)
                    groupAdapter.addAll(itemList[i].reversed())
                }*/

                //Putting this here since we don't want to impact the loading time
                sortedMap.forEach { time ->

                        val tempList2 = ArrayList<String>()
                        val tempList3 = ArrayList<String>()
                        for (barcode in time.value.items) {
                            tempList2.add(barcode.key)
                            tempList3.add(barcode.value)
                        }
                        barcodeList.add(tempList2)
                        qtyList.add(tempList3)
                }
                dismissProgress()
            }
            else
                view.findViewById<LinearLayout>(R.id.empty_view_history).visibility = View.VISIBLE
        }

    }
}

