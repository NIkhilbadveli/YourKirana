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
import kotlinx.coroutines.withContext


import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class ScannerListFragment(val tvTotal: TextView,val btnTick: FloatingActionButton,val btnInv: Button) : Fragment() {

    private var listValues = ArrayList<ScannerItem>()
    private var barcodeList = ArrayList<String>()
    private var recyclerViewAdapter: ScannerItemAdapter? = null
    private var recyclerView:RecyclerView?=null
    private lateinit var model: MainActivity.SharedViewModel
    private var databaseReference: DatabaseReference? = null

    private var emptyView: LinearLayout? = null
    private var shopName = "Temp Store"

    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var sharedPref: SharedPreferences
    private var allItemsCurrentQty: MutableMap<String, Int> = mutableMapOf()
    private lateinit var inventoryRef: DatabaseReference

    private var phoneNum = "1234567890"
    private lateinit var dialog: Dialog
    lateinit var tvContact: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_list_scanner, container, false)
        model = ViewModelProviders.of(requireParentFragment()).get(MainActivity.SharedViewModel::class.java)
        
        handleSwitching(view)
        
        databaseReference = FirebaseDatabase.getInstance().reference

        model.selected.observe(viewLifecycleOwner, Observer { s -> searchForProduct(s) })

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!

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

        inventoryRef = databaseReference!!.child("inventoryData").child(shopName)
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (barcode in p0.children){
                    allItemsCurrentQty[barcode.key!!] = barcode.child("qty").value.toString().toInt()
                }
            }

        })

        tvContact = view.findViewById(R.id.tv_contact)
        tvContact.setOnClickListener {
            dialog.show()
            model.pauseScanner()
        }

        return view
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
            /*if (list[0].isNotEmpty()&&list[1].isNotEmpty())
                addToListView(list[0], list[1], list[2], list[3])*/
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

    private fun addToTransactionData() {

        val itemCount = recyclerView?.childCount!!

        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        val transactionRef = databaseReference?.child("transactionData/$shopName/$dateFormat/$timeFormat")!!

        var qty: EditText?
        var itemView: View?

        //Adding items to data
        for (i in 0 until itemCount) {
            itemView = recyclerView?.getChildAt(i)
            if (itemView != null) {
                qty = itemView.findViewById(R.id.item_quantity)
                val updatedQty = allItemsCurrentQty[barcodeList[i]]!! - qty.text.toString().toInt()
                inventoryRef.child("${barcodeList[i]}/qty").setValue(updatedQty.toString())
                transactionRef.child("items/${barcodeList[i]}").setValue(qty.text.toString())
                stockRef.child("$shopName/${barcodeList[i]}/$dateFormat $timeFormat").setValue("-${qty.text}")
            }
        }

        //Adding orderValue to data
        transactionRef.child("orderValue").setValue(tvTotal?.text.toString().split(' ').last())

        //Adding customer data
        if (phoneNum!="1234567890"&&phoneNum.length==10) {
            transactionRef.child("contact").setValue(phoneNum)
            val key = databaseReference!!.push().key
            databaseReference!!.child("customerData/$phoneNum/$key").setValue("$dateFormat, $timeFormat")
        }

        val snack = Snackbar.make(requireView(), "Added to Transaction History!", Snackbar.LENGTH_SHORT)
        snack.setAction("View History") {
            findNavController().navigate(R.id.historyFragment)
        }

        snack.setActionTextColor(Color.parseColor("#ffffff"))
        snack.show()

        listValues.clear()
        tvTotal!!.text = "Rs."
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun addToInventoryData(){
        val itemCount = recyclerView!!.childCount
        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName)

        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        for (i in 0 until itemCount) {
            val itemView = recyclerView!!.getChildAt(i)
            if (itemView != null) {
                val qty = itemView.findViewById<EditText>(R.id.item_quantity)
                val updatedQty = allItemsCurrentQty[barcodeList[i]]!! + qty.text.toString().toInt()
                inventoryRef.child("${barcodeList[i]}/qty").setValue(updatedQty.toString())
                stockRef.child("$shopName/${barcodeList[i]}/$dateFormat $timeFormat").setValue("+${qty.text}")
            }
        }
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
