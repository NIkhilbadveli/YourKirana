package com.titos.barcodescanner.scannerFeature


import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*

import com.titos.barcodescanner.*
import com.titos.barcodescanner.R


import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList



class ScannerListFragment : Fragment() {

    private var listValues = ArrayList<ScannerItem>()
    private var barcodeList = ArrayList<String>()
    private var recyclerViewAdapter: ScannerItemAdapter? = null
    private var recyclerView:RecyclerView?=null
    private var model: MainActivity.SharedViewModel? = null
    private var databaseReference: DatabaseReference? = null

    private var tvTotal: TextView? = null
    private var emptyView: LinearLayout? = null
    private var shopName = "Temp Store"

    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_list_scanner, container, false)
        model = ViewModelProviders.of(parentFragment!!).get(MainActivity.SharedViewModel::class.java)
        
        handleSwitching(view)
        
        databaseReference = FirebaseDatabase.getInstance().reference

        model?.selected?.observe(viewLifecycleOwner, Observer { s -> searchForProduct(s) })

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!

        recyclerView = view.findViewById(R.id.list)
        tvTotal = view.findViewById(R.id.tv_total)
        emptyView = view.findViewById(R.id.empty_view_scanner)
        emptyView?.visibility = View.VISIBLE

        recyclerViewAdapter = ScannerItemAdapter(listValues)
        recyclerViewAdapter!!.apply {
            onItemClick = {pos,price,qty->
                listValues[pos].price = price
                listValues[pos].quantity = qty.toString()
                tvTotal!!.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()
            }
            onItemRemoveClick = {pos ->
                listValues.removeAt(pos)
                recyclerViewAdapter!!.notifyItemRemoved(pos)
                recyclerViewAdapter!!.notifyItemRangeChanged(pos,listValues.size)
                tvTotal!!.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()
            }
        }

        floatingActionButton = view.findViewById(R.id.btn_bill)
        recyclerView!!.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerViewAdapter
            floatingActionButton.setOnClickListener {
                if(listValues.isNotEmpty())
                    addToTransactionData()
                else
                    Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show() }

        }

        val inventoryButton = view.findViewById<Button>(R.id.check_out_button)
        inventoryButton.setOnClickListener {
            if(listValues.isNotEmpty()){
                addToInventoryData()
            }
            else
                Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun handleSwitching(view: View){
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        val switch = toolbar?.findViewById<SwitchCompat>(R.id.inventory_scanner_switch)

        //Setting initial mode
        switch?.isChecked = true

        switch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                view.findViewById<LinearLayout>(R.id.total_price_container)!!.visibility = View.VISIBLE
                view.findViewById<LinearLayout>(R.id.checkout_container)!!.visibility = View.GONE
                view.findViewById<FloatingActionButton>(R.id.btn_bill)!!.visibility = View.VISIBLE
                switch.text = getString(R.string.scanner_mode)
            }
            else{
                view.findViewById<LinearLayout>(R.id.total_price_container)!!.visibility = View.GONE
                view.findViewById<LinearLayout>(R.id.checkout_container)!!.visibility = View.VISIBLE
                view.findViewById<FloatingActionButton>(R.id.btn_bill)!!.visibility = View.GONE
                switch.text = getString(R.string.inventory_mode)
            }
        }
    }

    private fun searchForProduct(barcode: String) {

        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName).child(barcode)
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val list = ArrayList<String>()
                    list.add(barcode)
                    list.add(dataSnapshot.child("name").value.toString())
                    list.add(dataSnapshot.child("sellingPrice").value.toString())
                    list.add("dummyURL")
                    addToListView(list[0], list[1], list[2], list[3])
                }
                else{
                    showNewProductDialog(barcode)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }


    private fun showNewProductDialog(s: String) {
        model?.pauseScanner()

        val callAddToList: (ArrayList<String>)->Unit = { list ->
            model?.resumeScanner()
            if (list[0].isNotEmpty()&&list[1].isNotEmpty())
                addToListView(list[0], list[1], list[2], list[3])
        }

        val addNewProductFragment = AddNewProductFragment(callAddToList)
        val bundle = Bundle()
        bundle.putString("barcode",s)
        addNewProductFragment.arguments = bundle

        val manager = parentFragmentManager
        val ft = manager.findFragmentByTag("addNewProductFragment")
        if (ft!=null)
            manager.beginTransaction().remove(ft)

        addNewProductFragment.show(manager, "addNewProductFragment")
    }

    @SuppressLint("SetTextI18n")
    private fun addToListView(barcode: String, name: String, price: String, url: String) {

        if (!listValues.any { it.name == name }){
            listValues.add(ScannerItem(true, name, "1", price,url))
            barcodeList.add(barcode)
            recyclerViewAdapter!!.notifyItemInserted(listValues.size)
        }
        else {
            val matchedItem = listValues.first { it.name == name }
            val item = recyclerView?.getChildAt(listValues.indexOf(matchedItem))
            item?.findViewById<ImageButton>(R.id.add_quantity_button)?.performClick()
        }

        if (emptyView?.visibility==View.VISIBLE)
            emptyView?.visibility = View.GONE

        tvTotal!!.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()

    }

    var stockRef = FirebaseDatabase.getInstance().reference.child("stockMovement")

    val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    val simpleTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
    val dateFormat = simpleDateFormat.format(Date())
    val timeFormat = simpleTimeFormat.format(Date())

    private fun addToTransactionData() {

        val itemCount = recyclerView?.childCount!!


        val transactionRef = databaseReference?.child("transactionData/$shopName/$dateFormat/$timeFormat")!!

        var qty: EditText?
        var itemView: View?

        //Adding items to data
        for (i in 0 until itemCount) {
            itemView = recyclerView?.getChildAt(i)
            if (itemView != null) {
                qty = itemView.findViewById(R.id.item_quantity)
                transactionRef.child("items/${barcodeList[i]}").setValue(qty.text.toString())
                stockRef.child("$shopName/${barcodeList[i]}/$dateFormat $timeFormat").setValue(qty.text.toString())
            }
        }
        //Adding orderValue to data
        transactionRef.child("orderValue").setValue(tvTotal?.text.toString().split(' ').last())

        val snack = Snackbar.make(view!!, "Added to Transaction History!", Snackbar.LENGTH_SHORT)
        //snack.setAction("View History", getTohistory())
        snack.setAnchorView(floatingActionButton)
        //snack.show()

        val dialogview = LayoutInflater.from(context).inflate(R.layout.choice_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogview)
        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false)

        alertDialog.show()
        dialogview.findViewById<Button>(R.id.btn_choice_notpaid).setOnClickListener {
            val bundle = Bundle()
            bundle.putString("amountDue", tvTotal?.text.toString().split(' ').last())
            findNavController().navigate(R.id.action_scannerFragment_to_agreementFragment, bundle)
            alertDialog.dismiss()
            tvTotal!!.text = "Rs."
        }

        dialogview.findViewById<Button>(R.id.btn_choice_paid).setOnClickListener {
            Toast.makeText(context, "Marked as paid", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
            tvTotal!!.text = "Rs."
        }

        listValues.clear()
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun addToInventoryData(){
        val itemCount = recyclerView!!.childCount
        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName)

        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val qtyList = ArrayList<String>()
                for (barcode in barcodeList){
                    qtyList.add(p0.child(barcode).child("qty").value.toString())
                }

                for (i in 0 until itemCount) {
                    val itemView = recyclerView!!.getChildAt(i)
                    if (itemView != null) {
                        val qty = itemView.findViewById<EditText>(R.id.item_quantity)
                        val updatedQty = qtyList[i].toInt() + qty.text.toString().toInt()
                        inventoryRef.child("${barcodeList[i]}/qty").setValue(updatedQty.toString())
                        stockRef.child("$shopName/${barcodeList[i]}/$dateFormat $timeFormat").setValue(qty.text.toString())
                    }
                }
            }

        })

        val snack = Snackbar.make(requireView(), "Added to My Store!", Snackbar.LENGTH_SHORT)
        //snack.setAction("View History", getTohistory())
        snack.show()

        listValues.clear()
        recyclerViewAdapter!!.notifyDataSetChanged()
    }
}
