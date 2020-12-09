package com.titos.barcodescanner.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.ProgressDialog


abstract class BaseFragment(private val layoutId: Int): Fragment() {
    lateinit var firebaseHelper: FirebaseHelper
    lateinit var shopName: String
    private lateinit var progressDialog: ProgressDialog
    lateinit var layoutView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        layoutView = inflater.inflate(layoutId, container, false)

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName", "Temp Store")!!
        firebaseHelper = FirebaseHelper(shopName)

        progressDialog = ProgressDialog(requireContext())

        initView()

        return layoutView
    }

    abstract fun initView()

    fun showToast(msg: String){
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    fun showProgress(msg: String){
        progressDialog.setMsg(msg)
        progressDialog.show()
    }

    fun dismissProgress(){
        progressDialog.dismiss()
    }

    fun isShowing(): Boolean { return progressDialog.isShowing() }

    fun showSnackBar(msg: String){
        Snackbar.make(layoutView, msg, Snackbar.LENGTH_SHORT).show()
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    open fun hideSoftKeyboard(activity: Activity) {
        val inputMethodManager: InputMethodManager = activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus!!.windowToken, 0)
    }
}