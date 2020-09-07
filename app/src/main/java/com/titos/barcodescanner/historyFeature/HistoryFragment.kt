package com.titos.barcodescanner.historyFeature

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper

import com.titos.barcodescanner.R

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.ProgressDialog
import com.titos.barcodescanner.SwipeToAgreement
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import kotlinx.android.synthetic.main.item_history.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class HistoryFragment : Fragment(){

    private var onItemRemoveClick :((Int, Int)->Unit)? = null
    private var onItemClick :((Int)->Unit)? = null
    private var shopName = "Temp Store"
    private var keys = ArrayList<String>()
    private var contactList = ArrayList<String>()
    private var orderValueList = ArrayList<String>()
    private var qtyList = ArrayList<ArrayList<String>>()
    private var barcodeList = ArrayList<ArrayList<String>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

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

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
        shopName = sharedPref?.getString("shopName",shopName)!!
        val transactionRef = FirebaseDatabase.getInstance().reference.child("transactionData/$shopName")
        populateView(view, groupAdapter)
        onItemRemoveClick = { deletePosition, pos ->
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        transactionRef.child(keys[deletePosition]).removeValue()
                        groupAdapter.removeGroupAtAdapterPosition(pos)
                        groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                        Snackbar.make(view!!,"Transaction successfully deleted",Snackbar.LENGTH_SHORT).show()
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

        return view
    }

    private fun populateView(view:View, groupAdapter:GroupAdapter<GroupieViewHolder>){

        val dialog = ProgressDialog(requireContext(), "Please Wait...")
        dialog.show()

        val transactionRef = FirebaseDatabase.getInstance().reference.child("transactionData/$shopName")
        val dateStrToLocalDate: (String) -> LocalDate = {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }

        transactionRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.hasChildren()){
                    var id = 0
                    val headerList = ArrayList<HistoryHeaderItem>()
                    val itemList = ArrayList<ArrayList<HistoryItem>>()
                    val daySnapshot = ArrayList<DataSnapshot>()
                    for (day in p0.children){
                        daySnapshot.add(day)
                    }

                    val daySnapshotSorted = daySnapshot.sortedByDescending { dateStrToLocalDate(it.key!!)}.reversed()

                    for(day in daySnapshotSorted){
                        var daySales = 0
                        val tempList = ArrayList<HistoryItem>()
                        for (time in day.children){
                            id++
                            val orderValue = time.child("orderValue").value.toString()
                            daySales += orderValue.toDouble().toInt()
                            tempList.add(HistoryItem("Order No. $id" ,
                                    time.key!!, orderValue ,onItemRemoveClick!!, onItemClick!!))
                            keys.add("${day.key}/${time.key}")
                            orderValueList.add(orderValue)
                            contactList.add(time.child("contact").value.toString())
                        }
                        headerList.add(HistoryHeaderItem(day.key!!, daySales.toDouble()))
                        itemList.add(tempList)
                    }

                    for (i in headerList.size - 1 downTo 0 ){
                        val section = Section()
                        section.add(headerList[i])
                        groupAdapter.add(section)
                        groupAdapter.addAll(itemList[i].reversed())
                    }

                    //Putting this here since we don't want to impact the loading time
                    daySnapshotSorted.forEach { day ->
                        for (time in day.children) {
                            val tempList2 = ArrayList<String>()
                            val tempList3 = ArrayList<String>()
                            for (barcode in time.child("items").children) {
                                tempList2.add(barcode.key!!)
                                tempList3.add(barcode.value.toString())
                            }
                            barcodeList.add(tempList2)
                            qtyList.add(tempList3)
                        }
                    }

                    dialog.dismiss()
                }
                else
                    view.findViewById<LinearLayout>(R.id.empty_view_history).visibility = View.VISIBLE
            }

        })
    }
}

