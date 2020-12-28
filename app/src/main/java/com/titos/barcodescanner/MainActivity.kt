package com.titos.barcodescanner


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import am.appwise.components.ni.NoInternetDialog
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.storage.FirebaseStorage
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.titos.barcodescanner.base.BaseActivity
import com.titos.barcodescanner.utils.FirebaseHelper
import com.titos.barcodescanner.utils.UserDetails
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : BaseActivity(R.layout.activity_main) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnBluetooth: ImageButton
    private lateinit var sharedPref: SharedPreferences
    private lateinit var noInternetDialog: NoInternetDialog
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var listener: InstallStateUpdatedListener

    companion object {
        private const val MY_CAMERA_REQUEST_CODE = 100
    }

    override fun initView() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        setSupportActionBar(findViewById(R.id.toolbar_main))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), MY_CAMERA_REQUEST_CODE)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val navController: NavController = (supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController
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
        val user = FirebaseAuth.getInstance().currentUser!!

        //Adding member count to storeStats in realtime database & sharedPref
        sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE) ?: return

        //Trying to solve weird error (not able to add new product right after login but working if you switch to other fragments)
        //All the logs show that it should've been added
        if(!sharedPref.contains("firstTime")){
            navController.navigate(R.id.historyFragment)
            navController.navigate(R.id.scannerFragment)
            sharedPref.edit(){
                putBoolean("firstTime", false)
                apply()
            }
        }

        //Setting Profile Avatar
        val profileAvatar = toolbar.findViewById<AvatarView>(R.id.profile_avatar)
        val picassoLoader = PicassoLoader()

        firebaseHelper.getUserDetails(user.uid).observe(this){
            shopName = it.shopName

            sharedPref.edit {
                putString("shopName", it.shopName)
                putString("userName", it.userName)
                commit()
            }
            setShopLocation(user.uid, it)
            if (it.userName!="null") {
                picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), it.userName)
            } else
                picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), "UserName")
        }

        profileAvatar.setOnClickListener { findNavController(R.id.fragment).navigate(R.id.profileFragment) }

        //Handling firebase dynamic links
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    // Get deep link from result (may be null if no link is found)
                    var deepLink: Uri? = null
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link
                        shopName = deepLink?.getQueryParameter("shopName")!!
                        firebaseHelper.updateShopName(user.uid, shopName)
                        sharedPref.edit {
                            putString("shopName", shopName)
                            commit()
                        }
                        Snackbar.make(findViewById(android.R.id.content), "You are successfully added to $shopName", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener(this) { e -> Log.w("RikiError", "getDynamicLink:onFailure", e) }

        //Bluetooth feature
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(mReceiver, filter)

        /*btnBluetooth = toolbar.findViewById<ImageButton>(R.id.btn_bluetooth)
        btnBluetooth.setOnClickListener {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled){
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            else
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 111)
        }*/

        //Saving all products name SPAR data
        if (!sharedPref.getBoolean("alreadySaved", false))
            saveDataToCsv()

        //No Internet dialog
        //noInternetDialog = NoInternetDialog.Builder(this).build()

        //Checking for updates
        //checkForUpdates()

        //Setting up whatsapp help
        val sendIntent = Intent("android.intent.action.MAIN")

        sendIntent.action = Intent.ACTION_SEND
        sendIntent.setPackage("com.whatsapp")
        sendIntent.type = "text/plain"
        val phone = "918309572197"

        val message = "Hi, I have some questions regarding YourKirana App."
        findViewById<ImageView>(R.id.btn_help).setOnClickListener {
            try {
                sendIntent.putExtra("jid", "$phone@s.whatsapp.net")
                sendIntent.putExtra(Intent.EXTRA_TEXT, message)

                if (sendIntent.resolveActivity(packageManager) != null) {
                    startActivity(sendIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "No app installed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
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

            144 -> {
                if (resultCode != Activity.RESULT_OK) {
                    appUpdateManager.unregisterListener(listener)
                    showToast("Update cancelled!")
                }
            }
        }
    }

    private fun saveDataToCsv() {
        val sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE) ?: return

        FirebaseDatabase.getInstance().reference.child("productInfoData")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        val data = ArrayList<String>()
                        for (barcode in p0.children) {
                            data.add(barcode.child("name").value.toString())
                        }
                        val file = File(filesDir, "productData.csv")
                        csvWriter().writeAll(listOf(data), file)
                        sharedPref.edit {
                            putBoolean("alreadySaved", true)
                            commit()
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }
                })
    }

    private fun setShopLocation(uid: String, userDetails: UserDetails){
            if (userDetails.latitude==0.0 || userDetails.longitude==0.0) {
                val locationRequest = LocationRequest.create()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        if (locationResult == null) {
                            return
                        }
                        for (location in locationResult.locations) {
                            if (location != null) {
                                FirebaseHelper(shopName).updateLocation(uid, location.latitude, location.longitude)
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }
                    }
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                Log.d("fucked", location.longitude.toString())
                                FirebaseHelper(shopName).updateLocation(uid, location.latitude, location.longitude)
                            }
                            else{
                                Log.d("fucked", "Requesting for location updates")
                                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                            }
                        }
            }
    }

    //Add playstore built-in update dialog
    private fun checkForUpdates() {

        listener = InstallStateUpdatedListener{ state ->

            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Snackbar.make(findViewById(R.id.bottomNav), "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE).apply {
                    setAction("RESTART") { appUpdateManager.completeUpdate() }.show()
                }
            }
            else if (state.installStatus() == InstallStatus.DOWNLOADING){
                showProgress("Downloading Update")
            }
        }
        appUpdateManager.registerListener(listener)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Request the update.
                Log.d("fucked", "Update available")
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        144)
            } else {
                Log.d("fucked", "No Update available")
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        //noInternetDialog.onDestroy()
        //appUpdateManager.unregisterListener(listener)
    }

    /*override fun onResume() {
        super.onResume()

        appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->

                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        Snackbar.make(findViewById(R.id.bottomNav), "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE).apply {
                            setAction("RESTART") { appUpdateManager.completeUpdate() }.show()
                        }
                    }
                }
    }*/

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