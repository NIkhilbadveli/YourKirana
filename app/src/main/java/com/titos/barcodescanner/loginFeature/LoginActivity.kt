package com.titos.barcodescanner.loginFeature

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.*


class LoginActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123
    private val auth = FirebaseAuth.getInstance()
    private var shopName = "Temp Store"
    private lateinit var sharedPref: SharedPreferences

   /* companion object {
        init {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dialog = Dialog(this)
        val inflate = LayoutInflater.from(this).inflate(R.layout.splash_screen, null)

        sharedPref = getSharedPreferences("sharedPrefLogin", Context.MODE_PRIVATE)

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


        if (auth.currentUser!=null) {
            sharedPref.edit {
                putBoolean("alreadyLoggedIn", true)
                apply()
            }
        }

        if(auth.currentUser!=null){ //If user is signed in
            //Toast.makeText(this,"Sign In successful",Toast.LENGTH_SHORT).show()
            //getShopName(true)
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
        else {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAuthMethodPickerLayout(authMethodPickerLayout)
                            .setIsSmartLockEnabled(!BuildConfig.DEBUG)
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
                        getShopName(false)
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

                            return
                        }
                    }
            }
        }
    }

    private fun getShopName(alreadyLoggedIn: Boolean){
        val progressDialog = ProgressDialog.progressDialog(this)
        progressDialog.findViewById<TextView>(R.id.login_tv_dialog).text = "Logging in ..."

        if (!alreadyLoggedIn)
            progressDialog.show()
        val user = auth.currentUser
        val userRef = FirebaseDatabase.getInstance().reference.child("userData").child(user!!.uid)

        val view = layoutInflater.inflate(R.layout.dialog_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(false)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.child("userName").exists())
                    println("User Data exists")
                else
                {
                    userRef.child("phoneNumber").setValue(user.phoneNumber)
                    userRef.child("userName").setValue(user.displayName)
                    userRef.child("userEmail").setValue(user.email)

                }

                if(p0.child("shopName").exists()){
                    progressDialog.dismiss()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                else {
                    dialog.show()
                }
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })

        val editShopName = dialog.findViewById<EditText>(R.id.edit_shop_name)
        val addButton = dialog.findViewById<Button>(R.id.btn_add_shop)

        addButton!!.setOnClickListener {
            if(editShopName!!.text.isNotEmpty()){
                userRef.child("shopName").setValue(editShopName.text.toString())
                shopName = editShopName.text.toString()
                dialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            else
                Toast.makeText(this,"Please enter the shop name", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<Button>(R.id.btn_join_later)!!.setOnClickListener {
            userRef.child("shopName").setValue(shopName)
            dialog.dismiss()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

}