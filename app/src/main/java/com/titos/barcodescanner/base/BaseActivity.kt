package com.titos.barcodescanner.base

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.ProgressDialog

abstract class BaseActivity(private val layoutId: Int): AppCompatActivity() {
    lateinit var firebaseHelper: FirebaseHelper
    lateinit var shopName: String
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (layoutId!=-1)
            setContentView(layoutId)

        val sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        shopName = sharedPref.getString("shopName", "Temp Store")!!
        firebaseHelper = FirebaseHelper(shopName)

        progressDialog = ProgressDialog(this)

        initView()
    }

    abstract fun initView()

    fun showToast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun showProgress(msg: String){
        progressDialog.setMsg(msg)
        progressDialog.show()
    }

    fun dismissProgress(){
        progressDialog.dismiss()
    }

    fun isShowing(): Boolean { return progressDialog.isShowing() }
}