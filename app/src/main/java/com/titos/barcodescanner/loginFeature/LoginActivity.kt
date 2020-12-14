package com.titos.barcodescanner.loginFeature

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.observe
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.*
import com.titos.barcodescanner.base.BaseActivity
import com.titos.barcodescanner.utils.UserDetails


class LoginActivity : BaseActivity(-1) {
    private val RC_SIGN_IN = 123
    private val auth = FirebaseAuth.getInstance()

    companion object {
        init {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
    }

    override fun initView() {

        val dialog = Dialog(this)
        val inflate = LayoutInflater.from(this).inflate(R.layout.splash_screen, null)

        dialog.setContentView(inflate)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.WHITE))

        val authMethodPickerLayout = AuthMethodPickerLayout
                .Builder(R.layout.activity_login)
                .setGoogleButtonId(R.id.google_signin_button)
                .setEmailButtonId(R.id.email_signin_button)
                .setFacebookButtonId(R.id.facebook_signin_button)
                .setPhoneButtonId(R.id.otp_signin_button)
                .build()


        if(auth.currentUser!=null){ //If user is signed in
            //Toast.makeText(this,"Sign In successful",Toast.LENGTH_SHORT).show()
            handleLogin(false)
        }
        else {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAuthMethodPickerLayout(authMethodPickerLayout)
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build(),
                                    AuthUI.IdpConfig.EmailBuilder().build(),
                                    AuthUI.IdpConfig.FacebookBuilder().build(),
                                    AuthUI.IdpConfig.PhoneBuilder().setDefaultCountryIso("in").setWhitelistedCountries(listOf("+91")).build()))
                            .setTheme(R.style.AppThemeFirebaseAuth)
                            .build(),
                    RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                val response = IdpResponse.fromResultIntent(data)
                if (resultCode == Activity.RESULT_OK) {
                    handleLogin(true)
                    //Toast.makeText(this,"Sign In successful",Toast.LENGTH_SHORT).show()

                    return
                } else {
                    if (response == null) {
                        //If no response from the Server
                        return
                    }
                    if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                        //If there was a network problem the user's phone

                        return
                    }
                    if (response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                        //If the error cause was unknown

                        Toast.makeText(this, response.error!!.errorCode, Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
    }

    private fun handleLogin(show: Boolean){
        showProgress("Logging in ...")
        val user = auth.currentUser!!

        val view = layoutInflater.inflate(R.layout.dialog_bottom_sheet, null)
        val dialog = Dialog(this, R.style.WideDialog)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(false)

        val userDetails = UserDetails()
        if (user.phoneNumber!=null) userDetails.phoneNumber = user.phoneNumber.toString()
        if (user.displayName!=null) userDetails.userName = user.displayName.toString()
        if (user.email!=null) userDetails.userEmail = user.email.toString()

        firebaseHelper.isUserOld(user.uid).observe(this){ old ->
            if (isShowing())
                dismissProgress()

            if (old){
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
            else{
                dialog.show()
            }
        }

        val editShopName = dialog.findViewById<EditText>(R.id.edit_shop_name)
        val addButton = dialog.findViewById<Button>(R.id.btn_add_shop)
        val tvError = dialog.findViewById<TextView>(R.id.tv_error)!!

        addButton!!.setOnClickListener {
            val shopName = editShopName!!.text.trim().toString()
            if(shopName.isNotEmpty()){
                showProgress("Checking for availability...")
                firebaseHelper.checkIfShopAlreadyExists(shopName).observe(this){ available ->
                    dismissProgress()
                    if (available) {
                        tvError.visibility = View.GONE
                        userDetails.shopName = shopName
                        firebaseHelper.addNewUser(user.uid, userDetails)

                        dialog.dismiss()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    else
                        tvError.visibility = View.VISIBLE
                }
            }
            else
                Toast.makeText(this, "Please enter the shop name", Toast.LENGTH_SHORT).show()
        }
        dialog.setOnKeyListener { arg0, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                showToast("Please enter the shop name!")
            }
            true
        }
        /*dialog.findViewById<Button>(R.id.btn_join_later)!!.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }*/
    }

}