package com.titos.barcodescanner.scannerFeature


import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.*
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
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.titos.barcodescanner.*
import com.titos.barcodescanner.R
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip

import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class ScannerListFragment : Fragment() {

    private var listValues = ArrayList<ScannerItem>()
    private var barcodeList = ArrayList<String>()
    private var recyclerViewAdapter: ScannerItemAdapter? = null
    private var recyclerView:RecyclerView?=null
    private var model: MainActivity.SharedViewModel? = null
    private var databaseReference: DatabaseReference? = null
    private var db: AppDatabase? = null
    private var tvTotal: TextView? = null
    private var emptyView: LinearLayout? = null
    private var shopName = "Temp Store"
    private var memberCount = 1
    private val REQUEST_IMAGE_CAPTURE = 111
    private var nameEditText: EditText? = null
    private var onlineBarcodeList = ArrayList<String>()
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var sharedPref: SharedPreferences
    private var viewTargets = arrayOfNulls<View>(7)
    private lateinit var extendedToolTip: ExtendedToolTip
    private var isButtonClicked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_list_scanner, container, false)
        model = ViewModelProviders.of(parentFragment!!).get(MainActivity.SharedViewModel::class.java)
        model?.selected?.observe(viewLifecycleOwner, Observer { s -> showDialog(s) })
        
        handleSwitching(view)
        
        databaseReference = FirebaseDatabase.getInstance().reference

        sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!
        memberCount = sharedPref.getInt("memberCount",memberCount)

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
            onItemEditClick = {pos,name,price-> editProductInfo(pos,name,price) }
        }

        floatingActionButton = view.findViewById(R.id.btn_bill)
        recyclerView!!.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerViewAdapter
            floatingActionButton.setOnClickListener {
                if(listValues.isNotEmpty())
                    addToTransactionData(this)
                else
                    Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show() }

        }

        val inventoryButton = view.findViewById<Button>(R.id.check_out_button)
        inventoryButton.setOnClickListener {
            if(listValues.isNotEmpty()){
                addToInventoryData()
                isButtonClicked = true
                if(extendedToolTip.simpleTooltip.isShowing)
                    extendedToolTip.next()
            }
            else
                Toast.makeText(context, "Please scan at least one item :)", Toast.LENGTH_SHORT).show()
        }

        db = AppDatabase(context!!)

        /*val listModel:MainActivity.ViewModelForList = ViewModelProviders.of(parentFragment!!).get(MainActivity.ViewModelForList::class.java)
        listModel.finalList.observe(viewLifecycleOwner,Observer{
            if (emptyView?.visibility==View.VISIBLE)
                emptyView?.visibility = View.GONE

            listValues.addAll(it)
            it.forEach { barcodeList.add("loose") }
            tvTotal!!.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()
            recyclerViewAdapter!!.notifyItemInserted(listValues.size)
        })*/

        databaseReference?.child("inventoryData")?.child(shopName)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (barcode in dataSnapshot.children){
                    onlineBarcodeList.add(barcode.value.toString())
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        val switch = toolbar?.findViewById<SwitchCompat>(R.id.inventory_scanner_switch)
        viewTargets[0] = (switch)
        viewTargets[1] = (activity?.findViewById(R.id.barcodeFragment))

        viewTargets[3] = inventoryButton
        viewTargets[4] = activity?.findViewById(R.id.chip_nav)

        extendedToolTip = ExtendedToolTip(viewTargets)

        if (!sharedPref.getBoolean("inventoryTutorialCompleted",false) && !sharedPref.getBoolean("skipInvTutorial",false) )
            extendedToolTip.show()

        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (extendedToolTip.simpleTooltip.isShowing){
                if (extendedToolTip.stepNum<4)
                    extendedToolTip.dismiss()

                if (destination.id==R.id.myStoreFragment)
                    extendedToolTip.dismiss()
                else if (extendedToolTip.stepNum==5)
                    Toast.makeText(context,"Please click on the My Store icon", Toast.LENGTH_SHORT).show()
            }
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
                if (extendedToolTip.simpleTooltip.isShowing)
                    extendedToolTip.next()
            }
        }
    }
    
    private fun showDialog(s: String) {
        val productRef = databaseReference!!.child("productInfoData").child(s)

        productRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    addToListView(dataSnapshot.key as String,dataSnapshot.child("name").value.toString(),
                            dataSnapshot.child("price").value.toString(), dataSnapshot.child("URL").value.toString())
                } else {
                    tryWithInventoryData(s)

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun tryWithInventoryData(barcode: String){

        val dialogview = LayoutInflater.from(context).inflate(R.layout.add_product_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogview)
        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false)

        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName).child(barcode)
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    addToListView(barcode,dataSnapshot.child("name").value.toString(), dataSnapshot.child("price").value.toString(),"dummyURL")
                }
                else{
                    alertDialog.show()
                    addToDatabaseAndUpdateList(barcode, dialogview, alertDialog)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun addToDatabaseAndUpdateList(s: String, view: View, alertDialog: AlertDialog) {
        model?.pauseScanner()

        nameEditText = view.findViewById(R.id.product_name)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val price = view.findViewById<EditText>(R.id.product_price)

        val cameraButton = view.findViewById<ImageButton>(R.id.open_camera_button)
        cameraButton.visibility = View.VISIBLE

        /*if (takePictureIntent.resolveActivity(activity?.packageManager!!) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }*/

        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName)

        view.findViewById<View>(R.id.product_add_button).setOnClickListener {
            if(nameEditText?.text!!.isNotEmpty() && price.text.isNotEmpty()){
                inventoryRef.child(s).
                        setValue(ScannedItem(s,nameEditText?.text.toString(), "1", price.text.toString()))

                addToListView(s,nameEditText?.text.toString(), price.text.toString(),"dummyURL")

                model?.resumeScanner()
                alertDialog.dismiss()
                Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(context, "Please enter the name and price of the product", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.product_cancel_button).setOnClickListener {
            model?.resumeScanner()
            alertDialog.dismiss()
            //Toast.makeText(context, "Cancelled adding", Toast.LENGTH_SHORT).show()
        }

        cameraButton.setOnClickListener {
            if (takePictureIntent.resolveActivity(activity?.packageManager!!) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val extras = data?.extras
            val imageBitmap = extras!!["data"] as Bitmap?
            detectAndProcessTxt(imageBitmap)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun detectAndProcessTxt(imageBitmap: Bitmap?){
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap!!)
        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        var detectedText = ""

        textRecognizer.processImage(firebaseVisionImage)
                .addOnSuccessListener {
                    detectedText = it.text.replace(System.getProperty("line.separator")!!," ")
                    nameEditText?.setText(detectedText)
                }
                .addOnFailureListener { Toast.makeText(context,"Failed to detect any text... add manually",Toast.LENGTH_SHORT).show() }
    }

    private fun addToListView(barcode: String, name: String, price: String, url: String) {

        if (!listValues.any { it.name == name }){
            listValues.add(ScannerItem(true, name, "1", price,url))
            barcodeList.add(barcode)
            recyclerViewAdapter!!.notifyItemInserted(listValues.size)
            Timer().schedule(500) {
                viewTargets[2] = recyclerView!!.getChildAt(0)
                activity?.runOnUiThread {
                    if (extendedToolTip.stepNum==2 && !extendedToolTip.skipTutorial)
                        extendedToolTip.next()
                }
            }
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

    private fun addToTransactionData(rvTransaction: RecyclerView) {

        val itemCount = rvTransaction.childCount
        val itemList = mutableListOf<TransactionTable>()

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val simpleTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
        val dateFormat = simpleDateFormat.format(Date())
        val timeFormat = simpleTimeFormat.format(Date())

        var name: TextView?
        var qty: EditText?
        var price: TextView?
        var itemView: View?

        for (i in 0 until itemCount) {
            itemView = rvTransaction.getChildAt(i)
            if (itemView != null) {

                name = itemView.findViewById(R.id.item_name)
                qty = itemView.findViewById(R.id.item_quantity)
                price = itemView.findViewById(R.id.item_price)
                itemList.add(TransactionTable(0, true, 0, dateFormat, timeFormat,barcodeList[i],
                        name.text.toString(), convertToKg(qty), price.text.toString().toDouble()))
            }
        }

        val oneDayTransactions = databaseReference!!.child("oneDayTransactions").child(shopName)

        GlobalScope.launch {
                val maxOrderId = db?.crudMethods()?.getMaxOrderId()
                val currentOrderId = maxOrderId!! + 1

                for(item in itemList){
                    val updatedQty = db?.crudMethods()?.getQuantity(item.barcode)!! - item.itemQty
                    item.orderId = currentOrderId

                    if (!onlineBarcodeList.contains(item.barcode))
                        databaseReference?.child("inventoryData")?.child(shopName)?.child(item.barcode)?.setValue(
                                ScannedItem(item.barcode, item.itemName, item.itemQty.toString(), item.itemPrice.toString()))

                    if(updatedQty >= 0.0){
                        db?.crudMethods()?.updateQuantity(item.barcode,updatedQty)
                        databaseReference!!.child("inventoryData").child(shopName).child(item.barcode).child("quantity").setValue(updatedQty)
                    }

                    if (memberCount < 2)
                        db?.crudMethods()?.insertItem(item)
                    else
                        oneDayTransactions.child(item.orderId.toString()).child(oneDayTransactions.push().key!!).setValue(item)
                }
            }

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
        }

        dialogview.findViewById<Button>(R.id.btn_choice_paid).setOnClickListener {
            Toast.makeText(context, "Marked as paid", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }

        listValues.clear()
        recyclerViewAdapter!!.notifyDataSetChanged()
        tvTotal!!.text = "Rs."
    }

    private fun addToInventoryData(){
        val itemCount = recyclerView!!.childCount
        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName)

        GlobalScope.launch {
            val allBarcodes = db?.crudMethods()?.getAllBarcodes()
            var showToast = 0
            var name: TextView?
            var qty: EditText?
            var price: TextView?
            var itemView: View?

            for (i in 0 until itemCount) {
                itemView = recyclerView!!.getChildAt(i)
                if (itemView != null) {
                    name = itemView.findViewById(R.id.item_name)
                    qty = itemView.findViewById(R.id.item_quantity)
                    price = itemView.findViewById(R.id.item_price)

                    val unitPrice = (price.text.toString().toDouble()/convertToKg(qty)).toInt()
                    inventoryRef.child(barcodeList[i]).setValue(ScannedItem(barcodeList[i], name.text.toString(),
                            qty.text.toString(), unitPrice.toString()))

                    if (allBarcodes!!.contains(barcodeList[i])){
                        db?.crudMethods()?.updateQuantity(barcodeList[i], convertToKg(qty))
                        db?.crudMethods()?.updatePrice(barcodeList[i], unitPrice.toDouble())
                        showToast++
                    }
                    else{
                        db?.crudMethods()?.insertInventoryItem(InventoryTable(0,true,barcodeList[i],
                                            name.text.toString(),convertToKg(qty),unitPrice.toDouble()))
                    }
                }
            }
            withContext(Dispatchers.Main){
                if (showToast>0){
                    Toast.makeText(context, "$showToast items are overwritten as they already exist",Toast.LENGTH_SHORT).show()
                }
            }
        }

        val snack = Snackbar.make(view!!, "Added to My Store!", Snackbar.LENGTH_SHORT)
        //snack.setAction("View History", getTohistory())
        snack.setAnchorView(floatingActionButton)
        snack.show()

        listValues.clear()
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun editProductInfo(pos:Int,name:String,price: String){
        val viewGroup = activity!!.findViewById<ViewGroup>(android.R.id.content)
        val dialogview = LayoutInflater.from(context).inflate(R.layout.add_product_dialog, viewGroup, false)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogview)
        val alertDialog = builder.create()
        val inventoryRef = databaseReference!!.child("inventoryData").child(shopName)

        val nameEditText = dialogview.findViewById<EditText>(R.id.product_name)
        val priceEditText = dialogview.findViewById<EditText>(R.id.product_price)
        nameEditText.setText(name)
        priceEditText.setText(price)

        alertDialog.show()

        dialogview.findViewById<View>(R.id.product_add_button).setOnClickListener {
            inventoryRef.child(barcodeList[pos]).child("name").setValue(nameEditText.text.toString())
            inventoryRef.child(barcodeList[pos]).child("price").setValue(priceEditText.text.toString())

            val itemView = recyclerView!!.getChildAt(pos)
            itemView.findViewById<TextView>(R.id.item_name).text = nameEditText.text.toString()
            itemView.findViewById<TextView>(R.id.item_price).text = priceEditText.text.toString()

            listValues[pos].name = nameEditText.text.toString()
            listValues[pos].price = priceEditText.text.toString()
            tvTotal!!.text = "Rs. " + listValues.sumByDouble { it.price.toDouble() }.toString()

            GlobalScope.launch {
                db?.crudMethods()?.updateName(barcodeList[pos], nameEditText.text.toString())
                db?.crudMethods()?.updatePrice(barcodeList[pos], priceEditText.text.toString().toDouble())
            }

            alertDialog.dismiss()
            Toast.makeText(context, "Updated in database", Toast.LENGTH_SHORT).show()
        }

        dialogview.findViewById<View>(R.id.product_cancel_button).setOnClickListener {
            alertDialog.dismiss()
            //Toast.makeText(context, "Cancelled adding", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertToKg(qty: EditText): Double{
        val txt = qty.text.toString()
        val value = txt.split(" ").first()
        val unit = txt.split(" ").last()

        if (unit=="Kg")
            return value.toDouble()
        else if(unit=="g")
            return value.toDouble()/1000
        else
            return value.toDouble()
    }

    inner class ExtendedToolTip(targets: Array<View?>){
        private val steps = listOf(R.string.step_1, R.string.step_2, R.string.step_3, R.string.step_4, R.string.step_5, R.string.step_6)
        var stepNum = 0
        private val simpleTooltipBuilder = SimpleTooltip.Builder(context)
                .anchorView(targets.first())
                .text(steps.first())
                .gravity(Gravity.BOTTOM)
                .animated(true)
                .transparentOverlay(false)
                .contentView(R.layout.walkthrough, R.id.step_desc)
                .dismissOnOutsideTouch(false)
                .dismissOnInsideTouch(false)
        private val viewTargets = targets
        var simpleTooltip = simpleTooltipBuilder.build()
        var skipTutorial = false

        init {
            simpleTooltip.findViewById<Button>(R.id.btn_next).setOnClickListener {
                next()
            }
            simpleTooltip.findViewById<Button>(R.id.btn_skip).setOnClickListener {
                showSkipTutorialDialog()
            }
        }

        fun show(){
            stepNum++
            simpleTooltip.show()
        }

        fun dismiss(){
            simpleTooltip.dismiss()
        }

        fun next(){
            if (viewTargets[stepNum]!=null && stepNum<4 && !(viewTargets.first() as SwitchCompat).isChecked){
                doNext()
            }
            else if (stepNum==1){
                Toast.makeText(context,"Please click on the switch!", Toast.LENGTH_SHORT).show()
            }
            else if (stepNum==2){
                Toast.makeText(context,"Please scan some item or press skip if you don't have any product", Toast.LENGTH_SHORT).show()
            }
            else if (stepNum==4){
                if (isButtonClicked)
                    doNext()
                else
                    Toast.makeText(context,"Please click on this button!", Toast.LENGTH_SHORT).show()
            }
            else if (stepNum==5)
                Toast.makeText(context,"Have you clicked on the My Store icon?", Toast.LENGTH_SHORT).show()

        }

        private fun doNext(){
            simpleTooltip.dismiss()
            val simpleTooltipBuilder = SimpleTooltip.Builder(context)
                    .anchorView(viewTargets[stepNum])
                    .text(steps[stepNum])
                    .gravity(Gravity.BOTTOM)
                    .animated(true)
                    .transparentOverlay(false)
                    .contentView(R.layout.walkthrough, R.id.step_desc)
                    .dismissOnOutsideTouch(false)
                    .dismissOnInsideTouch(false)

            if (stepNum>=3)
                simpleTooltipBuilder.gravity(Gravity.TOP)

            val showStepNum = stepNum+1
            simpleTooltip = simpleTooltipBuilder.build()
            simpleTooltip.findViewById<TextView>(R.id.step_number).text = "Step - $showStepNum"
            simpleTooltip.findViewById<Button>(R.id.btn_next).setOnClickListener {
                next()
            }
            simpleTooltip.findViewById<Button>(R.id.btn_skip).setOnClickListener {
                showSkipTutorialDialog()
            }
            this.show()
        }

        private fun showSkipTutorialDialog(){
            val dialogBuilder = AlertDialog.Builder(context)

            dialogBuilder.setMessage("Do you want to skip the tutorial?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        with (sharedPref.edit()) {
                            putBoolean("skipInvTutorial", true)
                            commit()
                        }
                        skipTutorial = true
                        simpleTooltip.dismiss()
                        Toast.makeText(context,"App tutorial is skipped", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No") { dialog, id -> dialog.dismiss()
                    }

            val alert = dialogBuilder.create()
            alert.setTitle("Skip Tutorial")
            alert.show()
        }

    }

    @Parcelize
    data class ScannedItem(val barcode:String, val name: String, val quantity: String, val price: String): Parcelable
}
