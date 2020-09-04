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
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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
import com.google.firebase.storage.FirebaseStorage
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import java.io.File

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private var shopName = "Temp Store"
    //private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnBluetooth: ImageButton
    private lateinit var sharedPref: SharedPreferences
    private lateinit var noInternetDialog: NoInternetDialog
    companion object {

        private const val MY_CAMERA_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), MY_CAMERA_REQUEST_CODE)
            }
        }
        //FirebaseDatabase.getInstance().reference.child("appVersion").keepSynced(true)

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE) ?: return

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

        //setShopLocation(userRef)

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

        //Bluetooth feature
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

        //Checking for new updates
        checkForUpdates()

        //No Internet dialog
        noInternetDialog = NoInternetDialog.Builder(this).build()
        noInternetDialog.setCancelable(false)
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

/*    private fun setShopLocation(userDataRef:DatabaseReference){

        userDataRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if (!p0.child("latitude").exists() || !p0.child("longitude").exists()){
                    Log.d("locYourKirana","success")
                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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
    }*/

    private fun checkForUpdates() {
        val updateDialog = Dialog(this)
        updateDialog.setContentView(R.layout.dialog_update)
        updateDialog.setCanceledOnTouchOutside(false)
        FirebaseDatabase.getInstance().reference.child("appVersion").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val appVersion = packageManager.getPackageInfo(packageName, 0).versionName
                /*if (appVersion=="null") {
                    sharedPref.edit {
                        putString("appVersion", p0.value.toString())
                        commit()
                    }
                }
                else */
                if (appVersion!=p0.value.toString()){
                    updateDialog.findViewById<TextView>(R.id.tv_title).text = "New Update Available! V${p0.value.toString()}"
                    updateDialog.show()
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

        val latestRef = FirebaseStorage.getInstance().reference.child("app-release.apk")
        val localFile = File.createTempFile("app-latest", ".apk")

        var installReady = false
        updateDialog.findViewById<TextView>(R.id.btn_update_now).setOnClickListener {
            if (!installReady) {
                updateDialog.findViewById<LinearLayout>(R.id.progress_container).visibility = View.VISIBLE
                val linearLayout = updateDialog.findViewById<LinearLayout>(R.id.container_update)
                linearLayout.visibility = View.GONE
                latestRef.getFile(localFile).addOnSuccessListener {
                    Toast.makeText(this, "Download completed", Toast.LENGTH_LONG).show()
                    updateDialog.findViewById<TextView>(R.id.btn_update_now).text = "Install Now"
                    installReady = true
                    linearLayout.visibility = View.VISIBLE
                }.addOnFailureListener {
                    // Handle any errors
                }.addOnProgressListener {
                    val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                    updateDialog.findViewById<ProgressBar>(R.id.progress_bar_horizontal).progress = progress.toInt()
                    updateDialog.findViewById<TextView>(R.id.tv_progress).text = progress.toInt().toString() + "%"
                }
            }
            else{
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                     if (Build.VERSION.SDK_INT >= 24) {
                         val downloadedApk: Uri = FileProvider.getUriForFile(this, "$packageName.provider", localFile)
                         intent.setDataAndType(downloadedApk, "application/vnd.android.package-archive")
                         val resInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        for (resolveInfo in resInfoList) {
                            grantUriPermission("$packageName.provider", downloadedApk, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    } else {
                         intent.action = Intent.ACTION_VIEW
                         intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                         intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                         intent.setDataAndType(Uri.fromFile(localFile), "application/vnd.android.package-archive")
                         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                     }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    updateDialog.dismiss()
                }
                .setNegativeButton("No") { dialog, id -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle("Installing later")

        updateDialog.findViewById<TextView>(R.id.btn_maybe_later).setOnClickListener {
            if (installReady)
                alert.show()
            else
                updateDialog.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        noInternetDialog.onDestroy()
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