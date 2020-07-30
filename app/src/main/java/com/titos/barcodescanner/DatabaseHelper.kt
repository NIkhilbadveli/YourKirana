package com.titos.barcodescanner

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.firebase.database.*
import com.titos.barcodescanner.scannerFeature.AddNewProductFragment
import com.titos.barcodescanner.scannerFeature.ScannerListFragment

class DatabaseHelper(val shopName: String, val manager: FragmentManager,
                     val model: MainActivity.SharedViewModel, val callback: ScannerListFragment.DataSharing) {
    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun searchForProduct(barcode: String) {

        val inventoryRef = databaseReference.child("inventoryData").child(shopName).child(barcode)
        inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val list = ArrayList<String>()
                    list.add(barcode)
                    list.add(dataSnapshot.child("name").value.toString())
                    list.add(dataSnapshot.child("sellingPrice").value.toString())
                    list.add("dummyURL")
                    callback.callAddToList(list)
                }
                else{
                    showNewProductDialog(barcode)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    fun showNewProductDialog(s: String) {
        model.pauseScanner()

        val addNewProductFragment = AddNewProductFragment()
        val bundle = Bundle()
        bundle.putString("barcode",s)
        addNewProductFragment.arguments = bundle

        val ft = manager.findFragmentByTag("addNewProductFragment")
        if (ft!=null)
            manager.beginTransaction().remove(ft)

        addNewProductFragment.show(manager, "addNewProductFragment")

        addNewProductFragment.onDismiss(object : DialogInterface{
            override fun dismiss() {
                model.resumeScanner()
                val list = ArrayList<String>()
                list.addAll( addNewProductFragment.getRequiredData())

                if (list[0].isNotEmpty()&&list[1].isNotEmpty())
                    callback.callAddToList(list)
            }

            override fun cancel() {
                model.resumeScanner()
            }

        })
    }
}