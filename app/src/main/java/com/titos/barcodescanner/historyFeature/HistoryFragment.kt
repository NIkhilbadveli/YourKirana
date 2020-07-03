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
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import com.titos.barcodescanner.AppDatabase
import com.titos.barcodescanner.OrderDetails
import com.titos.barcodescanner.R

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import kotlin.collections.ArrayList

class HistoryFragment : Fragment(){
    private var db: AppDatabase? = null
    private var onItemRemoveClick :((Int, Int)->Unit)? = null
    private var shopName = "Temp Store"
    private var orderDetailsList = ArrayList<OrderDetails>()
    private var todaySales: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        db = AppDatabase(context!!)

        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_of_transactions)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        val databaseReference = FirebaseDatabase.getInstance().reference
        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)

        shopName = sharedPref?.getString("shopName",shopName)!!

        populateView(view, groupAdapter)
        onItemRemoveClick = {orderId, pos ->
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage("Are you sure?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", DialogInterface.OnClickListener {
                        _, _ ->
                        GlobalScope.launch { db?.crudMethods()?.deleteTransaction(orderId) }
                        groupAdapter.removeGroupAtAdapterPosition(pos)
                        groupAdapter.notifyItemRangeChanged(pos,groupAdapter.itemCount)
                        Snackbar.make(view!!,"Transaction successfully deleted",Snackbar.LENGTH_SHORT).show()
                    })
                    .setNegativeButton("No", DialogInterface.OnClickListener {
                        dialog, id -> dialog.cancel()
                    })

            val alert = dialogBuilder.create()
            alert.setTitle("Delete Transaction")
            alert.show()
        }

        groupAdapter.setOnItemClickListener { _, itemview ->
            val tvOrderNumber = itemview.findViewById<TextView>(R.id.history_order_number)
            val orderNumber = tvOrderNumber.text.split(" ").last().toInt()
            GlobalScope.launch(Dispatchers.IO) {
                val itemsInOrder = db?.crudMethods()?.getAllItemsByOrder(orderNumber)
                val bundle = Bundle()
                bundle.putParcelableArrayList("itemsInOrder", itemsInOrder as java.util.ArrayList<out Parcelable>)
                withContext(Dispatchers.Main){
                    findNavController().navigate(R.id.action_historyFragment_to_orderItemsFragment, bundle)
                }
            }
        }

        return view
    }

    private fun populateView(view:View, groupAdapter:GroupAdapter<GroupieViewHolder>){
        GlobalScope.launch (Dispatchers.IO){
            val dateStrToLocalDate: (OrderDetails) -> LocalDate = {
                LocalDate.parse(it.orderDate, DateTimeFormatter.ofPattern("dd-MM-yyyy")) }
            val allOrderDetails = (db?.crudMethods()?.getAllOrdersDetails()!!).sortedByDescending { it.orderId }
            val allDaysSales = db?.crudMethods()?.getAllDaysSales()!!
                   /* DaySales(SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()),todaySales)*/

            val allDates=ArrayList<String>()

            withContext(Dispatchers.Main){

                if(allOrderDetails.isEmpty())
                    view.findViewById<LinearLayout>(R.id.empty_view_history).visibility = View.VISIBLE

                allOrderDetails.forEach {
                    val section = Section()
                    if(!allDates.contains(it.orderDate))
                    {   val daySales = allDaysSales.single { day -> day.orderDate == it.orderDate }.sales
                        section.setHeader(HistoryHeaderItem(it.orderDate,daySales))
                        groupAdapter.add(section)
                        allDates.add(it.orderDate)
                    }
                    groupAdapter.add(HistoryItem("Order No. " + it.orderId.toString(), it.orderTime, it.value.toInt().toString(),onItemRemoveClick!!))
                }
            }
        }
    }
}

