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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.Navigation
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

        val user = FirebaseAuth.getInstance().currentUser

        //Adding member count to storeStats in realtime database & sharedPref
        val sharedPref = getSharedPreferences("sharedPref",Context.MODE_PRIVATE) ?: return

        val userDataRef = FirebaseDatabase.getInstance().reference.child("userData")
        userDataRef.child(user!!.uid).child("shopName").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                shopName = p0.value.toString()
                var memberCount = 1
                val searchQuery = userDataRef.orderByChild("shopName").equalTo(shopName)
                searchQuery.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(users: DataSnapshot) {
                        if (users.exists()){
                            FirebaseDatabase.getInstance().reference.child("storeStats").
                                    child(shopName).child("memberCount").setValue(users.childrenCount)

                            with (sharedPref.edit()) {
                                putString("shopName", shopName)
                                putInt("memberCount",users.childrenCount.toInt())
                                commit()
                            }

                            memberCount = users.childrenCount.toInt()
                        }
                    }
                    override fun onCancelled(p0: DatabaseError) {
                    }
                })

                //Scheduling daily backup job
                createDailyBackupJob()

                /*//Merging oneDayTransactions with offline data
                createHourlyMergeJob()*/
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

        setShopLocation(userDataRef)
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
        val user = FirebaseAuth.getInstance().currentUser
        userDataRef.child(user!!.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if (!p0.child("latitude").exists() || !p0.child("longitude").exists()){
                    Log.d("locYourKirana","success")
                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return
                    }
                    fusedLocationClient.lastLocation
                            .addOnSuccessListener { location->
                                if (location != null) {
                                    userDataRef.child(user.uid).child("latitude").setValue(location.latitude)
                                    userDataRef.child(user.uid).child("longitude").setValue(location.longitude)
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

    private fun createDailyBackupJob(){
        //Backup job using WorkManager
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val uploadWorkRequest =
                PeriodicWorkRequestBuilder<BackupWorker>(12, TimeUnit.HOURS)
                        .setInputData(workDataOf("shopName" to shopName))
                        .setConstraints(constraints)
                        .build()

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("dailyBackupJob",ExistingPeriodicWorkPolicy.KEEP,uploadWorkRequest)

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadWorkRequest.id)
                .observe(this, Observer { workInfo ->
                    if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Log.d("dailyBackup","success")
                        /*val backupRef = userRef.child("backupTimes")
                        backupRef.child(backupRef.key!!).setValue(LocalDateTime.now())*/
                    }
                })
    }

    private fun createHourlyMergeJob(){

        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val mergeWorkRequest =
                PeriodicWorkRequestBuilder<MergeWorker>(1, TimeUnit.HOURS)
                        .setInputData(workDataOf("shopName" to shopName))
                        .setConstraints(constraints)
                        .build()

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("mergingJob",ExistingPeriodicWorkPolicy.KEEP,mergeWorkRequest)

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

    class BackupWorker(appContext: Context, workerParams: WorkerParameters)
        : Worker(appContext, workerParams) {
        val context = appContext

        override fun doWork(): Result {
            val shopName = inputData.getString("shopName")
            backupDataToFirebase(shopName!!)

            return Result.success()
        }

        private fun backupDataToFirebase(shopName: String){
            createZip()
            val storageRef = FirebaseStorage.getInstance().reference

            val filePath = context.getString(R.string.file_path)

            val file = Uri.fromFile(File("$filePath/db.zip"))
            val userRef = storageRef.child( "$shopName/${file.lastPathSegment}")
            val uploadTask = userRef.putFile(file)

            Log.d("backup","Backup started")

            uploadTask.addOnFailureListener {
                Log.d("backup","Backup failed")
            }.addOnSuccessListener {
                Log.d("backup","Backup completed")
            }
        }

        private fun createZip(){
            val path = context.getString(R.string.database_path)
            val filePath = context.getString(R.string.file_path)

            val files: Array<String> = arrayOf("$path/mystore-data.db", "$path/mystore-data.db-shm","$path/mystore-data.db-wal")
            ZipOutputStream(BufferedOutputStream(FileOutputStream("$filePath/db.zip"))).use { out ->
                val data = ByteArray(1024)
                for (file in files) {
                    FileInputStream(file).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            val entry = ZipEntry(file)
                            out.putNextEntry(entry)
                            while (true) {
                                val readBytes = origin.read(data)
                                if (readBytes == -1) {
                                    break
                                }
                                out.write(data, 0, readBytes)
                            }
                        }
                    }
                }
            }
        }
    }

    class MergeWorker(appContext: Context, workerParams: WorkerParameters)
        : Worker(appContext, workerParams) {
        val context = appContext
        private var transactionList = ArrayList<TransactionTable>()

        override fun doWork(): Result {
            val shopName = inputData.getString("shopName")
            mergeOnlineWithOffline(shopName!!)

            return Result.success()
        }

        private fun mergeOnlineWithOffline(shopName: String){
            val db = AppDatabase(context)

            val oneDay = FirebaseDatabase.getInstance().reference.child("oneDayTransactions").child(shopName)
            oneDay.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(transactions: DataSnapshot) {
                    if (transactions.exists()){
                        for (orderId in transactions.children){
                            for (details in orderId.children){
                                val transaction:TransactionTable = details.getValue(TransactionTable::class.java)!!

                                transactionList.add(transaction)
                            }
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })

            //Adding to local database
            GlobalScope.launch {
                for (item in transactionList)
                    db.crudMethods().insertItem(item)
            }

            //removing transactions from realtime database
            oneDay.removeValue()
        }
    }


}