package com.titos.barcodescanner.scannerFeature

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import com.titos.barcodescanner.R
import kotlinx.android.synthetic.main.add_new_product.*
import java.text.SimpleDateFormat
import java.util.*

class AddNewProductFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private var shopName = "Temp Store"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layoutView = inflater.inflate(R.layout.add_new_product, container, false)

        val spinType = layoutView.findViewById<Spinner>(R.id.spinner)
        val spinCategory = layoutView.findViewById<Spinner>(R.id.spinner1)
        val spinSubCategory = layoutView.findViewById<Spinner>(R.id.spinner2)

        sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)!!
        shopName = sharedPref.getString("shopName",shopName)!!

        val type = arrayOf<String>("units", "kgs")
        val category = arrayOf<String>("Branded Foods","Loose Items","Fridge Products","Beauty","Health and Hygiene","Home Needs")
        val branded = arrayOf<String>("Ready to Eat" , "Snacks ","Biscuits","Breakfast Cereals" , "Packaged kitchen needs")
        val loose = arrayOf<String>("Rice","Sugar","Rock Salt","Wheat","Maida","Dal & Pulses","Others (Eggs)")
        val fridge = arrayOf<String>("Milk", "Curd", "Cheese", "Panner","Cool drinks","Batter","Chocolates","Mushroom & Others")
        val beauty = arrayOf<String>("Oral care","Hair care","Skin care","Baby care")
        val home = arrayOf<String>("Laundry","Cleaning","Pooja Needs","Toiletries")
        val health = arrayOf<String>("Pads","Covid Protection","Condoms","Tissues & Gloves","OTC")
        val barcode = 5852148562

        spinType.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,type)
        spinType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {

            }
        }

        spinCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,category)

        spinCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (position==0)
                {
                    spinSubCategory.adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,branded)
                }
                if (position==1)
                {
                    spinSubCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,loose)
                }
                if (position==2)
                {
                    spinSubCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,fridge)
                }
                if (position==3)
                {
                    spinSubCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,beauty)
                }
                if (position==4)
                {
                    spinSubCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,home)
                }
                if (position==5)
                {
                    spinSubCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,health)
                }
            }
        }

        val prodInfo = FirebaseDatabase.getInstance().reference.child("inventoryData").child(shopName)

        val pName = layoutView.findViewById<EditText>(R.id.edit_name)
        val sp = layoutView.findViewById<EditText>(R.id.edit_sp)
        val cp = layoutView.findViewById<EditText>(R.id.edit_cp)
        val date = layoutView.findViewById<TextView>(R.id.edit_date)

        val date1 = Calendar.getInstance().time

        val formatter = SimpleDateFormat.getDateInstance()
        val formatedDate = formatter.format(date1)
        date.text = formatedDate

        val url: String = "https://google.com"

        val add = layoutView.findViewById<Button>(R.id.addBtn)
        add.setOnClickListener {
            prodInfo.child("name").setValue(pName.text.toString())
            prodInfo.child("sellingPrice").setValue(sp.text.toString())
            prodInfo.child("costPrice").setValue(cp.text.toString())
            prodInfo.child("url").setValue(url)
            prodInfo.child("category").setValue(spinCategory.selectedItem.toString())
            prodInfo.child("subCategory").setValue(spinSubCategory.selectedItem.toString())

        }

        imageBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    requestPermissions(permissions, PERMISSION_CODE);
                }
                else {
                    pickImageFromGallery()
                }
            }
        }


        return layoutView
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,IMAGE_PICK_CODE)

    }
    companion object{
        private val IMAGE_PICK_CODE = 1000;
        private val PERMISSION_CODE = 1001;
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when(requestCode){
            PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    pickImageFromGallery()
                }
                else {
                    val toast = Toast.makeText(requireContext(),"Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            imageBtn.setImageURI(data?.data)
        }
    }

}