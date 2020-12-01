package com.titos.barcodescanner.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
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
    fun addOrUpdateProduct(barcode: String, productDetails: ProductDetails, mode: Int){
        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        firestore.collection("stores/$shopName/inventoryData")
                .document(barcode)
                .set(productDetails)
                .addOnSuccessListener { Log.d("TAG", "addProduct: $barcode") }
                .addOnFailureListener { Log.d("TAG", "failedToAdd: $barcode") }

        updateQty("$dateFormat $timeFormat", barcode, productDetails.qty ,mode)
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
    fun addInventory(barcodeList: List<BarcodeAndQty>){
        val date = Date()
        val dateFormat = simpleDateFormat.format(date)
        val timeFormat = simpleTimeFormat.format(date)

        //Updating changes in inventory
        barcodeList.forEach {
            updateQty("$dateFormat $timeFormat", it.barcode, it.qty, 0)
        }
    }

    //Handling transaction stuff
    fun addTransaction(transactionDetails: TransactionDetails){
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
        transactionDetails.items.forEach {
            updateQty("$dateFormat $timeFormat", it.key, it.value.toDouble(), 1)
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

    }

    fun updateUserName(uid: String, userName: String, shopName: String){
        firestore.collection("users")
                .document(uid)
                .update("userName", userName)
        updateShopName(uid, shopName)
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
                    if (it.exists())
                        productDetails.value = it.toObject(ProductDetails::class.java)
                    else
                        productDetails.value = ProductDetails()
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
                doc.update("qty", FieldValue.increment(0 - qty))
                        .addOnSuccessListener { Log.d("TAG", "updateQty: $barcode") }
                        .addOnFailureListener { Log.d("TAG", "failedToUpdate: $it") }
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

    fun deleteTransaction(time: String): LiveData<Boolean>{
        val ldStatus = MutableLiveData<Boolean>()
        firestore.collection("stores/$shopName/transactionData")
                .document(time)
                .delete()
                .addOnSuccessListener {
                    Log.d("TAG", "deleteSuccess: $time")
                    ldStatus.value = true
                }
        return  ldStatus
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
