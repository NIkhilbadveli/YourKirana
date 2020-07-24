package com.titos.barcodescanner


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.work.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.titos.barcodescanner.profileFeature.ProfileFragment
import com.titos.barcodescanner.scannerFeature.ScannerItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private var shopName = "Temp Store"
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), MY_CAMERA_REQUEST_CODE)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val navController: NavController = Navigation.findNavController(this, R.id.fragment)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNav)
        val chipNavigationBar: ChipNavigationBar = findViewById(R.id.chip_nav)

        chipNavigationBar.setItemSelected(R.id.scannerFragment)

        chipNavigationBar.setOnItemSelectedListener { itemId ->
            bottomNavigationView.selectedItemId = itemId
        }

        //Changing chip navigation selection on change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            chipNavigationBar.setItemSelected(destination.id)
        }

        navController.saveState()
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        val user = FirebaseAuth.getInstance().currentUser

        //Adding member count to storeStats in realtime database & sharedPref
        val sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE) ?: return

        //Setting Profile Avatar
        val profileAvatar = toolbar.findViewById<AvatarView>(R.id.profile_avatar)
        val userRef = FirebaseDatabase.getInstance().reference.child("userData").child(user?.uid!!)
        val picassoLoader = PicassoLoader()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                shopName = p0.child("shopName").value.toString()
                val userName = p0.child("userName").value.toString()
                sharedPref.edit {
                    putString("shopName", shopName)
                    putString("userName", userName)
                    commit()
                }

                if (userName!="null") {
                    picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), userName)
                } else
                    picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), "UserName")
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

        profileAvatar.setOnClickListener { findNavController(R.id.fragment).navigate(R.id.profileFragment) }

        setShopLocation(userRef)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
                println("camera permission granted")
            } else {
                //Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
                println("camera permission denied")
            }
        }
    }

    private fun setShopLocation(userDataRef:DatabaseReference){

        userDataRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if (!p0.child("latitude").exists() || !p0.child("longitude").exists()){
                    Log.d("locYourKirana","success")
                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return
                    }
                    fusedLocationClient.lastLocation
                            .addOnSuccessListener { location->
                                if (location != null) {
                                    userDataRef.child("latitude").setValue(location.latitude)
                                    userDataRef.child("longitude").setValue(location.longitude)
                                }
                            }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    companion object {

        private const val MY_CAMERA_REQUEST_CODE = 100
    }

    class ViewModelForList : ViewModel() {
        val finalList = MutableLiveData<ArrayList<ScannerItem>>()

        fun sendList(list: ArrayList<ScannerItem>){
            finalList.value = list
        }
    }

    class SharedViewModel : ViewModel() {
        val selected = MutableLiveData<String>()
        val isScannerPaused = MutableLiveData<Boolean>()
        val isTooltipShown = MutableLiveData<Boolean>()

        fun select(item: String) {
            selected.value = item
        }

        fun pauseScanner(){
            isScannerPaused.value = true
        }

        fun resumeScanner(){
            isScannerPaused.value = false
        }

        fun showTooltip(){
            isTooltipShown.value = true
        }
    }
}