package com.titos.barcodescanner.loginFeature

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
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
import com.google.firebase.storage.FirebaseStorage
import com.titos.barcodescanner.BuildConfig
import com.titos.barcodescanner.MainActivity
import com.titos.barcodescanner.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.URLDecoder
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.concurrent.schedule
import kotlin.properties.Delegates


class LoginActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123
    private val auth = FirebaseAuth.getInstance()
    private var shopName = "Temp Store"
    private var BUFFER_SIZE = 2048

    companion object {
        init {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        if(auth.currentUser != null){ //If user is signed in
            //Toast.makeText(this,"Sign In successful",Toast.LENGTH_SHORT).show()
            getShopName(true)

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
        //dialog.show()
        /*Timer().schedule(1000) {
            dialog.dismiss()
            runOnUiThread {

            }
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){

            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                getShopName(false)
                //Toast.makeText(this,"Sign In successful",Toast..LENGTH_SHORT).show()

                return
            }
            else {
                if(response == null){
                    //If no response from the Server
                    return
                }
                if(response.error!!.errorCode == ErrorCodes.NO_NETWORK){
                    //If there was a network problem the user's phone

                    return
                }
                if(response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR){
                    //If the error cause was unknown

                    return
                }
            }
        }
    }

    private fun parseIntent(intent: Intent):String {
        val longURL = URLDecoder.decode(intent.data.toString(),"UTF-8")
        if(longURL.contains("shopName")){
            return longURL.split("=").last()
        }
        else
            return "invalid URL"
    }

    private fun getShopName(alreadyLoggedIn: Boolean){
        val progressDialog = ProgressDialog.progressDialog(this)
        if (!alreadyLoggedIn)
            progressDialog.show()
        val user = auth.currentUser
        val userRef = FirebaseDatabase.getInstance().reference.child("userData").child(user!!.uid)


        val view = layoutInflater.inflate(R.layout.bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        var shopNamePresent = false
        val shopNameChangeBuilder = AlertDialog.Builder(this)
        shopNameChangeBuilder.setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes", DialogInterface.OnClickListener {
                    dial, id ->
                    userRef.child("shopName").setValue(parseIntent(intent))
                    //Deleting old shop transactions
                    Toast.makeText(this@LoginActivity,"Shop Name changed",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                })
                .setNegativeButton("No", DialogInterface.OnClickListener {
                    dial, id ->
                    dial.cancel()
                    if(!shopNamePresent)
                        dialog.show()
                })
        val alert = shopNameChangeBuilder.create()
        alert.setTitle("Add " + parseIntent(intent))

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
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })

        userRef.child("shopName").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    shopName = dataSnapshot.value.toString()
                    shopNamePresent = true

                    if(!getDatabasePath("mystore-data.db").exists())
                        restoreDataFromFirebase()

                    if(parseIntent(intent)!="invalid URL"){
                        progressDialog.dismiss()
                        alert.show()
                    }
                    else {
                        progressDialog.dismiss()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
                else {
                    if(parseIntent(intent)!="invalid URL"){
                        progressDialog.dismiss()
                        alert.show()
                    }
                    else {
                        progressDialog.dismiss()
                        dialog.show()
                    }

                }
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })

        val editShopName = dialog.findViewById<EditText>(R.id.edit_shop_name)
        val addButton = dialog.findViewById<Button>(R.id.add_shop_name)

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
    }

    private fun restoreDataFromFirebase(){

        val filePath = getString( R.string.file_path)
        val databasePath = getString(R.string.database_path)
        val unzippedData = File("$filePath/$databasePath")

        val dbFile = File("$filePath/db.zip")

        val dbRef = FirebaseStorage.getInstance().reference.child("$shopName/db.zip")
        Log.d("shopName",shopName)
        dbRef.metadata.addOnSuccessListener {
            Toast.makeText(this,"Restoring data from a backup",Toast.LENGTH_SHORT).show()

            dbRef.getFile(dbFile).addOnSuccessListener {
                unzip("$filePath/db.zip",filePath)
                unzippedData.copyRecursively(File(databasePath))
                Toast.makeText(this,"Restoring done",Toast.LENGTH_SHORT).show()
            }.addOnFailureListener{
                Log.v("download_error",it.message!!)
            }
        }.addOnFailureListener{
            Log.v("no_backup",it.message!!)
        }

    }

    class ProgressDialog {
        companion object {
            fun progressDialog(context: Context): Dialog {
                val dialog = Dialog(context)
                val inflate = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
                inflate.findViewById<TextView>(R.id.login_tv_dialog).text = "Logging in ..."

                dialog.setContentView(inflate)
                dialog.setCancelable(false)
                dialog.window!!.setBackgroundDrawable(
                        ColorDrawable(Color.TRANSPARENT))
                return dialog
            }
        }
    }

    @Throws(IOException::class)
    fun unzip(zipFile: String?, location: String) {
        var location = location
        var size by Delegates.notNull<Int>()

        val buffer = ByteArray(BUFFER_SIZE)
        try {
            if (!location.endsWith(File.separator)) {
                location += File.separator
            }
            val f = File(location)
            if (!f.isDirectory) {
                f.mkdirs()
            }
            val zin = ZipInputStream(BufferedInputStream(FileInputStream(zipFile), BUFFER_SIZE))
            try {
                var ze: ZipEntry? = null
                while (zin.getNextEntry().also({ ze = it }) != null) {
                    val path = location + ze!!.getName()
                    val unzipFile = File(path)
                    if (ze!!.isDirectory()) {
                        if (!unzipFile.isDirectory) {
                            unzipFile.mkdirs()
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        val parentDir = unzipFile.parentFile
                        if (null != parentDir) {
                            if (!parentDir.isDirectory) {
                                parentDir.mkdirs()
                            }
                        }

                        // unzip the file
                        val out = FileOutputStream(unzipFile, false)
                        val fout = BufferedOutputStream(out, BUFFER_SIZE)
                        try {
                            while (zin.read(buffer, 0, BUFFER_SIZE).also({ size = it }) != -1) {
                                fout.write(buffer, 0, size)
                            }
                            zin.closeEntry()
                        } finally {
                            fout.flush()
                            fout.close()
                        }
                    }
                }
            } finally {
                zin.close()
            }
        } catch (e: Exception) {
            Log.e("loginActivity", "Unzip exception", e)
        }
    }
}