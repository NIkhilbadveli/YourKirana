package com.titos.barcodescanner.scannerFeature

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.storage.FirebaseStorage
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.ProductDetails
import kotlinx.android.synthetic.main.fragment_add_new_product.view.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class AddNewProductFragment : BaseFragment(R.layout.fragment_add_new_product) {

    private val REQUEST_IMAGE_CAPTURE = 111
    private lateinit var pName: AutoCompleteTextView
    private lateinit var sp: EditText
    private lateinit var cp: EditText
    private lateinit var etQuantity: EditText
    private lateinit var url: String
    private var barcode = "00000"

    override fun initView() {

        val spinType = layoutView.findViewById<Spinner>(R.id.spinner)
        val spinCategory = layoutView.findViewById<Spinner>(R.id.spinner1)
        val spinSubCategory = layoutView.findViewById<Spinner>(R.id.spinner2)

        val type = arrayOf<String>("units", "kgs")
        val category = arrayOf<String>("Branded Foods","Loose Items","Fridge Products","Beauty","Health and Hygiene","Home Needs")

        val branded = arrayOf<String>("Ready to Eat" , "Snacks ","Biscuits","Breakfast Cereals" , "Packaged kitchen needs")
        val loose = arrayOf<String>("Rice","Sugar","Rock Salt","Wheat","Maida","Dal & Pulses","Others (Eggs)")
        val fridge = arrayOf<String>("Milk", "Curd", "Cheese", "Panner","Cool drinks","Batter","Chocolates","Mushroom & Others")
        val beauty = arrayOf<String>("Oral care","Hair care","Skin care","Baby care")
        val health = arrayOf<String>("Pads","Covid Protection","Condoms","Tissues & Gloves","OTC")
        val home = arrayOf<String>("Laundry","Cleaning","Pooja Needs","Toiletries")

        val subCategoryList = ArrayList<Array<String>>()
        subCategoryList.add(branded)
        subCategoryList.add(loose)
        subCategoryList.add(fridge)
        subCategoryList.add(beauty)
        subCategoryList.add(health)
        subCategoryList.add(home)

        spinType.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,type)

        spinType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position==1) {
                    spinCategory.setSelection(1)
                    spinCategory.isEnabled = false
                }
                else
                    spinCategory.isEnabled = true
            }
        }

        spinCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,category)

        spinCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinSubCategory.adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,subCategoryList[position])
            }
        }

        barcode = if(arguments?.getString("barcode")!=null) arguments?.getString("barcode")!! else "00000"

        val prodInfo = ProductDetails()

        pName = layoutView.findViewById(R.id.edit_name)
        val productData = csvReader().readAll(File(activity?.filesDir, "productData.csv"))
        val adapter = ArrayAdapter(requireContext(), R.layout.item_text,R.id.text1, productData[0])
        pName.setAdapter(adapter)

        sp = layoutView.findViewById(R.id.edit_sp)
        cp = layoutView.findViewById(R.id.edit_cp)
        etQuantity = layoutView.findViewById(R.id.edit_quantity)

        url = "https://google.com"

        val edit = if(arguments?.getBoolean("edit")!=null) arguments?.getBoolean("edit")!! else false

        val add = layoutView.findViewById<Button>(R.id.addBtn)
        add.setOnClickListener {

            if (pName.text.isNotEmpty()&&sp.text.isNotEmpty()&&cp.text.isNotEmpty()
                    &&etQuantity.text.isNotEmpty()&&barcode!="00000") {
                if(sp.text.toString().toInt()>=cp.text.toString().toInt()) {

                    prodInfo.name=(pName.text.toString())
                    prodInfo.sellingPrice=(sp.text.toString())
                    prodInfo.costPrice=(cp.text.toString())
                    prodInfo.qty=(etQuantity.text.toString().toInt())
                    prodInfo.url=(url)
                    prodInfo.type=(spinType.selectedItem.toString())
                    prodInfo.category=(spinCategory.selectedItem.toString())
                    prodInfo.subCategory=(spinSubCategory.selectedItem.toString())

                    if(edit)
                        firebaseHelper.addOrUpdateProduct(barcode, prodInfo, 2)
                    else {
                        firebaseHelper.addOrUpdateProduct(barcode, prodInfo, 0)
                        Toast.makeText(context, "Added to Inventory", Toast.LENGTH_SHORT).show()
                    }
                    findNavController().navigateUp()
                }
                else
                    Toast.makeText(context, "Selling Price should be greater than cost price", Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(context, "Please enter all the required values", Toast.LENGTH_SHORT).show()
        }

        layoutView.findViewById<Button>(R.id.cancelBtn).setOnClickListener { findNavController().navigateUp() }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        layoutView.findViewById<ImageView>(R.id.imageBtn).setOnClickListener {
            if (takePictureIntent.resolveActivity(activity?.packageManager!!) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }

        if (edit){
            add.text = "Update"
            firebaseHelper.getProductDetails(barcode).observe(this) { p0->
                pName.setText(p0.name)
                sp.setText(p0.sellingPrice)
                cp.setText(p0.costPrice)
                etQuantity.setText(p0.qty.toString())

                val index = category.indexOf(p0.category)
                spinCategory.setSelection(index)
                spinSubCategory.setSelection(subCategoryList[index].indexOf(p0.subCategory))
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data?.extras
            val imageBitmap = extras!!["data"] as Bitmap?
            layoutView.findViewById<ImageView>(R.id.imageBtn).setImageBitmap(imageBitmap)
            uploadImageAndUpdateUrl(imageBitmap!!)
            detectAndProcessTxt(imageBitmap)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun detectAndProcessTxt(imageBitmap: Bitmap?){
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap!!)
        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        var detectedText = ""

        textRecognizer.processImage(firebaseVisionImage)
                .addOnSuccessListener {
                    detectedText = it.text.replace(System.getProperty("line.separator")!!," ")
                    pName.setText(detectedText)
                }
                .addOnFailureListener { Toast.makeText(context,"Failed to detect any text... add manually",Toast.LENGTH_SHORT).show() }
    }

    private fun uploadImageAndUpdateUrl(bitmap: Bitmap){

        val file = createTempFile(barcode, ".jpg")
        val fOut: OutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
        fOut.flush()
        fOut.close()
        MediaStore.Images.Media.insertImage(activity?.contentResolver, file.absolutePath, file.name, file.name)

        val imageRef = FirebaseStorage.getInstance().reference.child( "$shopName/${Uri.fromFile(file).lastPathSegment}")
        val uploadTask = imageRef.putFile(Uri.fromFile(file))

        //Toast.makeText(requireContext(),"Started uploading",Toast.LENGTH_SHORT).show()

        uploadTask.addOnSuccessListener {
            Toast.makeText(requireContext(),"Successfully uploaded",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { Toast.makeText(requireContext(),it.toString(),Toast.LENGTH_SHORT).show() }

        imageRef.downloadUrl.addOnSuccessListener {
            url = it.toString()
        }.addOnFailureListener { Toast.makeText(requireContext(),it.toString(),Toast.LENGTH_SHORT).show() }
    }
}