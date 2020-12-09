package com.titos.barcodescanner.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.titos.barcodescanner.dashboardFeature.BarcodeAndQty
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FirebaseHelper(val shopName: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val simpleTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)

    //Handling inventory stuff
    fun addOrUpdateProduct(barcode: String, pd: ProductDetails, mode: Int){
        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        firestore.collection("stores/$shopName/inventoryData")
                .document(barcode)
                .set(pd)
                .addOnSuccessListener {
                    updateQty("$dateFormat $timeFormat", barcode, pd.qty ,mode)
                    Log.d("TAG", "addProduct: $barcode")
                }
                .addOnFailureListener { Log.d("TAG", "failedToAdd: $barcode") }

        //Saving to common database only if it is from eldorado account
        if (FirebaseAuth.getInstance().currentUser!!.email=="eldorado.studios1@gmail.com") {
            val pdV2 = ProductDetailsV2(pd.name, pd.sellingPrice, pd.url, pd.type, pd.category, pd.subCategory)
            firestore.collection("productData")
                    .document(barcode)
                    .set(pdV2)
        }
    }

    fun getNewBarcode(): String{
        return firestore.collection("stores/$shopName/inventoryData").document().id
    }

    fun getProductDetails(barcode: String): LiveData<ProductDetails>{
        val pd = MutableLiveData<ProductDetails>()
        firestore.collection("stores/$shopName/inventoryData")
                .document(barcode)
                .get()
                .addOnSuccessListener {
                    Log.d("TAG", "retrievedProduct: $barcode")
                    pd.value = it.toObject(ProductDetails::class.java)
                }
                .addOnFailureListener {e -> Log.d("TAG", "failedToRetrieve: $e") }
        return pd
    }

    fun getMultipleProductDetails(barcodeList: ArrayList<String>):LiveData<Map<String, ProductDetails>>{
        val ldProductDetails = MutableLiveData<Map<String, ProductDetails>>()
        firestore.collection("stores/$shopName/inventoryData")
                .whereIn(FieldPath.documentId(), barcodeList)
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String, ProductDetails>()
                    for (doc in it.documents){
                        map[doc.id] = doc.toObject(ProductDetails::class.java)!!
                    }

                    ldProductDetails.value = map
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetTransactions") }

        return ldProductDetails
    }

    fun getNameToBarcodeMap(): LiveData<Map<String, String>>{
        val ldProductDetails = MutableLiveData<Map<String, String>>()
        firestore.collection("productData")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String, String>()
                    for (doc in it.documents){
                        map[doc.getString("name")!!] = doc.id
                    }
                    firestore.collection("stores/$shopName/inventoryData")
                            .get()
                            .addOnSuccessListener { qs ->
                                for (doc in qs.documents){
                                    if (!map.containsKey(doc.getString("name")!!))
                                        map[doc.getString("name")!!] = doc.id
                                }
                                ldProductDetails.value = map
                            }
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetTransactions") }

        return ldProductDetails
    }

    fun removeProduct(barcode: String){
        firestore.collection("stores/$shopName/inventoryData")
                .document(barcode)
                .delete()
                .addOnSuccessListener { Log.d("TAG", "deleteSuccess: $barcode") }
    }

    fun getAllInventory(): LiveData<Map<String,ProductDetails>>{
        val ldProductDetails = MutableLiveData<Map<String,ProductDetails>>()
        firestore.collection("stores/$shopName/inventoryData")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,ProductDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(ProductDetails::class.java)!!

                    ldProductDetails.value = map
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetTransactions") }

        return ldProductDetails
    }

    //Handling transaction stuff
    fun addInventory(barcodeList: List<BarcodeAndQty>, pdMap: Map<String, ProductDetails>){
        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        firestore.collection("stores/$shopName/inventoryData")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,ProductDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(ProductDetails::class.java)!!

                    //Adding products to inventory if they are not present while billing
                    //Updating changes in inventory
                    barcodeList.forEach { bq ->
                        if (map.containsKey(bq.barcode))
                            updateQty("$dateFormat $timeFormat", bq.barcode, bq.qty, 0)
                        else{
                            val pd = pdMap[bq.barcode] ?: error("")
                            pd.qty = 0.0
                            pd.costPrice = pd.sellingPrice
                            addOrUpdateProduct(bq.barcode, pd, 3)
                            updateQty("$dateFormat $timeFormat", bq.barcode, bq.qty, 0)
                        }
                    }
                }
    }

    //Handling transaction stuff
    fun addTransaction(transactionDetails: TransactionDetails, pdMap: Map<String, ProductDetails>){
        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        //adding to Transaction data
        firestore.collection("stores/$shopName/transactionData")
                .document("$dateFormat $timeFormat")
                .set(transactionDetails)
                .addOnSuccessListener { Log.d("TAG", "addTransaction: $dateFormat $timeFormat") }
                .addOnFailureListener { Log.d("TAG", "failedToAdd: $dateFormat $timeFormat") }

        //Updating changes in inventory
        firestore.collection("stores/$shopName/inventoryData")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,ProductDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(ProductDetails::class.java)!!

                    //Adding products to inventory if they are not present while billing
                    transactionDetails.items.forEach { td ->
                        if (map.containsKey(td.key))
                            updateQty("$dateFormat $timeFormat", td.key, td.value.toDouble(), 1)
                        else{
                            val pd = pdMap[td.key] ?: error("")
                            pd.qty = 0.0
                            pd.costPrice = pd.sellingPrice
                            addOrUpdateProduct(td.key, pd, 3)
                            updateQty("$dateFormat $timeFormat", td.key, td.value.toDouble(), 1)
                        }
                    }
                }

        //Adding this transaction to corresponding customer
        addTransactionToCustomer(transactionDetails.contact, "$dateFormat $timeFormat")
    }

    fun getAllTransactions(): LiveData<Map<String,TransactionDetails>>{
        val ldTransactionDetails = MutableLiveData<Map<String,TransactionDetails>>()

        firestore.collection("stores/$shopName/transactionData")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,TransactionDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(TransactionDetails::class.java)!!

                    ldTransactionDetails.value = map
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetTransactions") }

        return ldTransactionDetails
    }

    //Handling user related stuff
    fun isUserOld(uid: String): LiveData<Boolean>{
        val alreadyExists = MutableLiveData<Boolean>() //whether shop already exists
        val doc = firestore.collection("users")
                .document(uid)
        doc.get().addOnSuccessListener {
            alreadyExists.value = it.exists()
        }

        return alreadyExists
    }

    fun addNewUser(uid: String, userDetails: UserDetails){
        firestore.collection("users")
                .document(uid)
                .set(userDetails)
                .addOnSuccessListener { Log.d("TAG", "addedNewUser: $uid") }
                .addOnFailureListener {e -> Log.d("TAG", "failedToAdd: $e") }

        //Adding this user to the newly created store
        firestore.collection("stores")
                .document(userDetails.shopName)
                .set(mapOf("createdBy" to userDetails.userName))
    }

    fun checkIfShopAlreadyExists(shopName: String): MutableLiveData<Boolean>{
        val ldBoolean = MutableLiveData<Boolean>()
        firestore.collection("stores")
                .document(shopName)
                .get()
                .addOnSuccessListener {
                        ldBoolean.value = !it.exists()
                }

        return ldBoolean
    }

    fun updateUserName(uid: String, userName: String, shopName: String){
        firestore.collection("users")
                .document(uid)
                .update("userName", userName)
        updateShopName(uid, shopName)
    }

    fun updateLocation(uid: String, latitude: Double, longitude: Double){
        firestore.collection("users")
                .document(uid)
                .update(mapOf("latitude" to latitude, "longitude" to longitude))
    }

    fun getUserDetails(uid: String): LiveData<UserDetails>{
        val ud = MutableLiveData<UserDetails>()
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener {
                    Log.d("TAG", "retrievedUser: $uid")
                    ud.value = it.toObject(UserDetails::class.java)
                }
                .addOnFailureListener {e -> Log.d("TAG", "failedToRetrieve: $e") }
        return ud
    }

    fun updateShopName(uid: String, shopName: String){
        firestore.collection("users")
                .document(uid)
                .update("shopName", shopName)
    }

    //Handling khata related stuff
    fun addToKhata(time: String, khataDetails: KhataDetails){
        firestore.document("stores/$shopName")
                .collection("khataBook")
                .document(time)
                .set(khataDetails)
                .addOnSuccessListener { Log.d("TAG", "addedToKhata: $time") }
                .addOnFailureListener { Log.d("TAG", "failedToAdd: $it") }
    }

    fun getAllKhata(): LiveData<Map<String, KhataDetails>>{
        val ldKhataDetails = MutableLiveData<Map<String, KhataDetails>>()
        firestore.collection("stores/$shopName/khataBook")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,KhataDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(KhataDetails::class.java)!!

                    ldKhataDetails.value = map
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetKhata") }

        return ldKhataDetails
    }

    fun updateKhataStatus(time: String){
        firestore.collection("stores/$shopName/khataBook")
                .document(time)
                .update("status", "paid")
                .addOnSuccessListener { Log.d("TAG", "UpdatedKhata: $time") }
                .addOnFailureListener { Log.d("TAG", "failedToAdd: $it") }
    }

    //Handling scannerlistfragment stuff
    fun searchBarcode(barcode: String): LiveData<ProductDetails>{
        val productDetails = MutableLiveData<ProductDetails>() //whether barcode already exists
        firestore.collection("stores/$shopName/inventoryData")
                .document(barcode)
                .get()
                .addOnSuccessListener {
                    //Checking in inventory
                    if (it.exists())
                        productDetails.value = it.toObject(ProductDetails::class.java)
                    else {
                        //Checking in the common product database
                        firestore.collection("productData")
                                .document(barcode)
                                .get()
                                .addOnSuccessListener { ds->
                                    if (ds.exists())
                                        productDetails.value = ds.toObject(ProductDetails::class.java)
                                    else
                                        productDetails.value = ProductDetails()
                                }
                    }
                }

        return productDetails
    }

    //mode = 0: plus, 1: minus, 2: update, 3: new, 4: don't trigger
    fun updateQty(time: String, barcode: String, qty: Double, mode: Int) {
        val doc = firestore.collection("stores/$shopName/inventoryData/")
                .document(barcode)

        val data = mutableMapOf<String, String>()
        when (mode) {
            0 -> {
                data[time] = "+$qty"
                doc.update("qty", FieldValue.increment(qty))
                        .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                        .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
                doc.set(mapOf("changes" to data), SetOptions.merge())
            }
            1 -> {
                data[time] = "-$qty"
                doc.get().addOnSuccessListener {
                    val currentQty = it.get("qty").toString().toDouble()
                    if (currentQty-qty>=0){
                        doc.update("qty", FieldValue.increment(0 - qty))
                                .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                                .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
                    }
                    else{
                        doc.update("qty", 0.0)
                                .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                                .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
                    }
                }

                //Adding this qty to sold
                doc.update("sold", FieldValue.increment(qty))
                        .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                        .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
                doc.set(mapOf("changes" to data), SetOptions.merge())
            }
            2 -> {
                data[time] = qty.toString()
                doc.update("qty", qty)
                        .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                        .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
                doc.set(mapOf("changes" to data), SetOptions.merge())
            }
            3 -> {
                data[time] = "+$qty"
                doc.set(mapOf("changes" to data), SetOptions.merge())
            }
            4 -> {}
        }
    }

    //Getting all the details of a particular transaction
    fun getTransactionDetails(time: String): LiveData<TransactionDetails>{
        val ldMap = MutableLiveData<TransactionDetails>()
        firestore.collection("stores/$shopName/transactionData")
                .document(time)
                .get()
                .addOnSuccessListener {
                    ldMap.value = it.toObject(TransactionDetails::class.java)
                }
                .addOnFailureListener { Log.d("TAG", "failedToGet: $it") }

        return ldMap
    }

    fun deleteTransaction(time: String){
        firestore.collection("stores/$shopName/transactionData")
                .document(time)
                .delete()
    }

    private fun addTransactionToCustomer(phone: String, time: String){
        val doc =  firestore.collection("stores/$shopName/customerData")
                .document(phone)
        val data = mutableMapOf<String, String>()
        data[doc.id] = time
        doc.set(mapOf("entries" to data), SetOptions.merge())
    }

    //Customer Requests
    fun addCustomerRequest(requestDetails: RequestDetails): String{
        val doc = firestore.collection("stores/$shopName/customerRequests")
                .document()

        doc.set(requestDetails)

        return doc.id
    }

    fun updateRequest(docId: String, requestDetails: RequestDetails){
        val doc = firestore.collection("stores/$shopName/customerRequests")
                .document(docId)
        doc.set(requestDetails)
    }

    fun getAllRequests(): LiveData<Map<String, RequestDetails>>{
        val ldRequestDetails = MutableLiveData<Map<String, RequestDetails>>()
        firestore.collection("stores/$shopName/customerRequests")
                .get()
                .addOnSuccessListener {
                    val map = mutableMapOf<String,RequestDetails>()
                    for (doc in it.documents)
                        map[doc.id] = doc.toObject(RequestDetails::class.java)!!

                    ldRequestDetails.value = map
                }
                .addOnFailureListener { Log.d("TAG", "failedToGetRequests") }

        return ldRequestDetails
    }


}
