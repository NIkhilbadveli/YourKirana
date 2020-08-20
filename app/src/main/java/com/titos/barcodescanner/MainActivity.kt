package com.titos.barcodescanner


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.titos.barcodescanner.scannerFeature.ScannerItem
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private var shopName = "Temp Store"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnBluetooth: ImageButton

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

        //Handling firebase dynamic links
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    // Get deep link from result (may be null if no link is found)
                    var deepLink: Uri? = null
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link
                        shopName = deepLink?.getQueryParameter("shopName")!!
                        userRef.child("shopName").setValue(shopName)
                        sharedPref.edit {
                            putString("shopName", shopName)
                            commit()
                        }
                        Toast.makeText(this, "You are successfully added to $shopName", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener(this) { e -> Log.w("RikiError", "getDynamicLink:onFailure", e) }

        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(mReceiver, filter)

        btnBluetooth = toolbar.findViewById<ImageButton>(R.id.btn_bluetooth)
        btnBluetooth.setOnClickListener {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled){
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            else
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 111)
        }

        //Saving all products name
        if (!sharedPref.getBoolean("alreadySaved", false))
            saveDataToCsv()
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

    //The BroadcastReceiver that listens for bluetooth broadcasts
    val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action;
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                btnBluetooth.setImageResource(R.drawable.bluetooth)
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                btnBluetooth.setImageResource(R.drawable.bluetooth_disconnected)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            111 -> if (resultCode == Activity.RESULT_OK)
                    startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))

        }
    }

    private fun saveDataToCsv() {
        val sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE) ?: return
        val filePath = getString( R.string.file_path)
        FirebaseDatabase.getInstance().reference.child("productInfoData")
                .addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val data = ArrayList<String>()
                for (barcode in p0.children){
                    data.add(barcode.child("name").value.toString())
                }
                val file = File(filesDir, "productData.csv")
                csvWriter().writeAll(listOf(data), file)
                sharedPref.edit{
                    putBoolean("alreadySaved", true)
                    commit()
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
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

        fun select(item: String) {
            selected.value = item
        }

        fun pauseScanner(){
            isScannerPaused.value = true
        }

        fun resumeScanner(){
            isScannerPaused.value = false
        }

    }



}