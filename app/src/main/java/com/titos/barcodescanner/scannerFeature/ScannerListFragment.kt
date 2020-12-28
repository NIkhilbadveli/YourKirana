package com.titos.barcodescanner.scannerFeature

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.impl.Schedulers.schedule
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.titos.barcodescanner.MainActivity
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.dashboardFeature.BarcodeAndQty
import com.titos.barcodescanner.utils.BillDetails
import com.titos.barcodescanner.utils.PrintUtility
import com.titos.barcodescanner.utils.ProductDetails
import com.titos.barcodescanner.utils.TransactionDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class ScannerListFragment(val tvTotal: TextView, val btnTick: FloatingActionButton,
                          val btnInv: Button) : BaseFragment(R.layout.fragment_list_scanner) {

    private var listValues = ArrayList<ScannerItem>()
    private val pdMap = mutableMapOf<String, ProductDetails>()
    private lateinit var recyclerView:RecyclerView
    private lateinit var model: MainActivity.SharedViewModel
    private lateinit var scannerItemAdapter: ScannerItemAdapter
    private lateinit var onItemClick:((Int,String,Double)->Unit)
    private lateinit var onItemRemoveClick:((Int)->Unit)
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

        sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)!!

        recyclerView = view.findViewById(R.id.list)

        emptyView = view.findViewById(R.id.empty_view_scanner)
        emptyView?.visibility = View.VISIBLE

        onItemClick = { pos, price, qty->
            val recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            listValues[pos].quantity = qty.toString()
            tvTotal.text = "Rs. " + listValues.sumByDouble { (it.quantity.toDouble()*it.price.toDouble()).round(2) }.toString()
            recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
        }

        onItemRemoveClick = { pos ->
            pdMap.remove(listValues[pos].barcode)
            listValues.removeAt(pos)
            recyclerView.adapter = scannerItemAdapter

            if (listValues.isEmpty())
                emptyView?.visibility = View.VISIBLE
            tvTotal.text = "Rs. " + listValues.sumByDouble { (it.quantity.toDouble()*it.price.toDouble()).round(2) }.toString()
        }

        scannerItemAdapter = ScannerItemAdapter(listValues, requireContext(), onItemClick, onItemRemoveClick)
        scannerItemAdapter.setHasStableIds(true)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
            adapter = scannerItemAdapter
        }

        btnTick.setOnClickListener {
            if(listValues.isNotEmpty())
                addToTransactionData()
            else
                Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show()
        }

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
            dialog.dismiss()
        }
    }

    private fun searchForProduct(barcode: String) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar_main)
        val switch = toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch)
        showProgress("Searching...")
        firebaseHelper.searchBarcode(barcode).observe(this) { productDetails ->
            dismissProgress()
                if (productDetails.name.isNotEmpty()) {
                    pdMap[barcode] = productDetails
                    addToListView(barcode, productDetails.name, productDetails.sellingPrice, productDetails.type, productDetails.url)
                }
                else if (productDetails.name.isEmpty() && switch.isChecked)
                    showSnackBar("Item not found.")
                else if (productDetails.name.isEmpty() && !switch.isChecked)
                    showNewProductDialog(barcode)
        }
    }

    private fun showNewProductDialog(s: String) {
        val bundle = Bundle()
        bundle.putString("barcode", s)
        findNavController().navigate(R.id.action_scannerFragment_to_addNewProductFragment, bundle)
    }

    @SuppressLint("SetTextI18n")
    private fun addToListView(barcode: String, name: String, price: String, type:String,  url: String) {

        if (!listValues.any { it.name == name }){
            val item = ScannerItem(barcode, name, "1.0", price, false, url)
            if (type=="kgs")
                item.loose = true

            scannerItemAdapter.addItem(item)
            recyclerView.scrollToPosition(listValues.size - 1)
        }
        else {
            val matchedItem = listValues.first { it.name == name }
            //showToast("Matched Item is : ${matchedItem.name} and at ${listValues.indexOf(matchedItem)}")
            matchedItem.quantity = (matchedItem.quantity.toDouble() + 1).toString()

            recyclerView.scrollToPosition(listValues.indexOf(matchedItem))
            Timer().schedule(300){
                requireActivity().runOnUiThread {
                    val item = recyclerView.findViewHolderForAdapterPosition(listValues.indexOf(matchedItem))!!.itemView
                    if (!matchedItem.loose) {
                        item.findViewById<ImageButton>(R.id.add_quantity_button).performClick()
                    } else {
                        val etQuantity = item.findViewById<EditText>(R.id.et_quantity)
                        val updatedQty = etQuantity.text.toString().toDouble() + 1
                        etQuantity.setText(updatedQty.toString())
                    }
                }
            }
        }

        if (emptyView?.visibility==View.VISIBLE)
            emptyView?.visibility = View.GONE

        tvTotal.text = "Rs. " + listValues.sumByDouble { (it.quantity.toDouble()*it.price.toDouble()).round(2) }.toString()

    }

    private fun addToTransactionData() {
        val items = mutableMapOf<String, String>()
        val billItems = ArrayList<ScannerItem>()

        //Adding items to data
        for (i in 0 until listValues.size) {
            items[listValues[i].barcode] = listValues[i].quantity
            billItems.add(listValues[i])
        }

        firebaseHelper.addTransaction(TransactionDetails(phoneNum, tvTotal.text.toString().split(' ').last(), items), pdMap)
                .observe(this) {
                    if (it) pdMap.clear()
                }

        scannerItemAdapter.clear()

        tvTotal.text = "Rs."

        val snack = Snackbar.make(requireView(), "Added to Transaction History!", Snackbar.LENGTH_SHORT)
        snack.setAction("View History") {
            findNavController().navigate(R.id.historyFragment)
        }

        snack.setActionTextColor(Color.parseColor("#ffffff"))
        snack.show()
    }

    private fun addToInventoryData(){
        val bqList = ArrayList<BarcodeAndQty>()//Adding items to data
        for (i in 0 until listValues.size) {
            bqList.add(BarcodeAndQty(listValues[i].barcode,listValues[i].quantity.toDouble()))
        }

        firebaseHelper.addInventory(bqList, pdMap).observe(this){
            if (it) pdMap.clear()
        }

        val snack = Snackbar.make(requireView(), "Added to Inventory!", Snackbar.LENGTH_SHORT)
        snack.setActionTextColor(Color.parseColor("#ffffff"))
        snack.setAction("Check Inventory") {
            findNavController().navigate(R.id.myStoreFragment)
        }
        snack.show()
        scannerItemAdapter.clear()
    }
}
