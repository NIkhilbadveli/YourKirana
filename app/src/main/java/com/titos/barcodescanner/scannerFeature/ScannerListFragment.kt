package com.titos.barcodescanner.scannerFeature


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*

import com.titos.barcodescanner.*
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.dashboardFeature.BarcodeAndQty
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.TransactionDetails


import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ScannerListFragment(val tvTotal: TextView,val btnTick: FloatingActionButton,
                          val btnInv: Button) : BaseFragment(R.layout.fragment_list_scanner) {

    private var listValues = ArrayList<ScannerItem>()
    private var barcodeList = ArrayList<String>()
    private var recyclerViewAdapter: ScannerItemAdapter? = null
    private var recyclerView:RecyclerView?=null
    private lateinit var model: MainActivity.SharedViewModel

    private var emptyView: LinearLayout? = null

    private lateinit var sharedPref: SharedPreferences

    private var phoneNum = "1234567890"
    private lateinit var dialog: Dialog
    lateinit var tvContact: TextView

    override fun initView() {

        val view = layoutView
        model = ViewModelProvider(requireParentFragment()).get(MainActivity.SharedViewModel::class.java)
        
        handleSwitching(view)

        model.selected.observe(viewLifecycleOwner, Observer { s -> searchForProduct(s) })

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!

        recyclerView = view.findViewById(R.id.list)

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

        //floatingActionButton = view.findViewById(R.id.btn_bill)
        recyclerView!!.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerViewAdapter
            btnTick.setOnClickListener {
                if(listValues.isNotEmpty())
                    addToTransactionData()
                else
                    Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show()
            }
        }

        //val inventoryButton = view.findViewById<Button>(R.id.check_out_button)
        btnInv.setOnClickListener {
            if(listValues.isNotEmpty()){
                addToInventoryData()
            }
            else
                Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show()
        }

        tvContact = view.findViewById(R.id.tv_contact)
        tvContact.setOnClickListener {
            dialog.show()
            model.pauseScanner()
        }

    }

    private fun handleSwitching(view: View){

        val viewGroup = view.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_phone, viewGroup, false)
        dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

        dialog.setCanceledOnTouchOutside(false)

        val etPhone = dialogView.findViewById<EditText>(R.id.et_number)
        dialogView.findViewById<Button>(R.id.btn_add_phone).setOnClickListener {
            if (etPhone.text.isNotEmpty()&&etPhone.text.toString().length==10){
                phoneNum = etPhone.text.toString()
                view.findViewById<TextView>(R.id.tv_contact).text = phoneNum
                dialog.dismiss()
                model.resumeScanner()
            }
            else if (etPhone.text.isNotEmpty() && etPhone.text.toString().length!=10)
                Toast.makeText(context, "Please enter valid phone number", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, "Don't leave it empty :)", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            model.resumeScanner()
            dialog.cancel()
        }
    }

    private fun searchForProduct(barcode: String) {
        firebaseHelper.searchBarcode(barcode).observe(this) { productDetails ->
                if (productDetails.name.isNotEmpty())
                    addToListView(barcode, productDetails.name, productDetails.sellingPrice, "dummyURL")
                else
                    showNewProductDialog(barcode)
        }
    }

    private fun showNewProductDialog(s: String) {
        val bundle = Bundle()
        bundle.putString("barcode",s)
        findNavController().navigate(R.id.action_scannerListFragment_to_addNewProductFragment, bundle)
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

        tvTotal.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()

    }

    private fun addToTransactionData() {

        val itemCount = recyclerView?.childCount!!

        var qty: EditText?
        var itemView: View?

        val items = mutableMapOf<String, String>()

        //Adding items to data
        for (i in 0 until itemCount) {
            itemView = recyclerView?.getChildAt(i)
            if (itemView != null) {
                qty = itemView.findViewById(R.id.item_quantity)
                items[barcodeList[i]] = qty.text.toString()
            }
        }

        if (phoneNum.length==10)
            firebaseHelper.addTransaction(TransactionDetails(phoneNum, tvTotal.text.toString().split(' ').last(), items))
        else
            showToast("Please Enter full Mobile number!")


        val snack = Snackbar.make(requireView(), "Added to Transaction History!", Snackbar.LENGTH_SHORT)
        snack.setAction("View History") {
            findNavController().navigate(R.id.historyFragment)
        }

        snack.setActionTextColor(Color.parseColor("#ffffff"))
        snack.show()

        listValues.clear()
        tvTotal.text = "Rs."
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun addToInventoryData(){
        val itemCount = recyclerView!!.childCount

        val bqList = ArrayList<BarcodeAndQty>()
        for (i in 0 until itemCount) {
            val itemView = recyclerView!!.getChildAt(i)
            if (itemView != null) {
                val qty = itemView.findViewById<EditText>(R.id.item_quantity)
                bqList.add(BarcodeAndQty(barcodeList[i], qty.text.toString().toInt()))
            }
        }

        firebaseHelper.addInventory(bqList)

        val snack = Snackbar.make(requireView(), "Added to Inventory!", Snackbar.LENGTH_SHORT)
        snack.setActionTextColor(Color.parseColor("#ffffff"))
        snack.setAction("Check Inventory") {
            findNavController().navigate(R.id.myStoreFragment)
        }
        snack.show()

        listValues.clear()
        recyclerViewAdapter!!.notifyDataSetChanged()
    }
}
