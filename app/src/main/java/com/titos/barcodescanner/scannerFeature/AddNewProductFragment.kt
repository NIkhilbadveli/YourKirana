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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.storage.FirebaseStorage
import com.titos.barcodescanner.R
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class AddNewProductFragment(val callAddToList: ((ArrayList<String>)->Unit)) : DialogFragment() {

    private lateinit var sharedPref: SharedPreferences
    private var shopName = "Temp Store"
    private val REQUEST_IMAGE_CAPTURE = 111
    private lateinit var pName: EditText
    private lateinit var sp: EditText
    private lateinit var cp: EditText
    private lateinit var url: String
    private var barcode = "00000"
    private lateinit var layoutView: View

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        layoutView = inflater.inflate(R.layout.fragment_add_new_product, container, false)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            Log.d("tag","back button pressed")    // Handle the back button event
        }
        callback.isEnabled = true

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

        spinCategory.adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,category)

        spinCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinSubCategory.adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,subCategoryList[position])
            }
        }

        barcode = if(arguments?.getString("barcode")!=null) arguments?.getString("barcode")!! else "00000"

        val prodInfo = FirebaseDatabase.getInstance().reference.child("inventoryData/$shopName/$barcode")

        pName = layoutView.findViewById(R.id.edit_name)
        sp = layoutView.findViewById(R.id.edit_sp)
        cp = layoutView.findViewById(R.id.edit_cp)
        val etQuantity = layoutView.findViewById<EditText>(R.id.edit_quantity)

        url = "https://google.com"

        val add = layoutView.findViewById<Button>(R.id.addBtn)
        add.setOnClickListener {
            if (pName.text.isNotEmpty()&&sp.text.isNotEmpty()&&cp.text.isNotEmpty()
                    &&etQuantity.text.isNotEmpty()&&barcode!="00000") {
                prodInfo.child("name").setValue(pName.text.toString())
                prodInfo.child("sellingPrice").setValue(sp.text.toString())
                prodInfo.child("costPrice").setValue(cp.text.toString())
                prodInfo.child("qty").setValue(etQuantity.text.toString())
                prodInfo.child("url").setValue(url)
                prodInfo.child("type").setValue(spinType.selectedItem.toString())
                prodInfo.child("category").setValue(spinCategory.selectedItem.toString())
                prodInfo.child("subCategory").setValue(spinSubCategory.selectedItem.toString())
                Toast.makeText(context, "Added to Inventory", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            else
                Toast.makeText(context, "Please enter all the required values", Toast.LENGTH_SHORT).show()
        }
        layoutView.findViewById<Button>(R.id.cancelBtn).setOnClickListener { dismiss() }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        layoutView.findViewById<ImageView>(R.id.imageBtn).setOnClickListener {
            if (takePictureIntent.resolveActivity(activity?.packageManager!!) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }

        val edit = if(arguments?.getBoolean("edit")!=null) arguments?.getBoolean("edit")!! else false
        if (edit){
            add.text = "Update"
            prodInfo.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    pName.setText(p0.child("name").value.toString())
                    sp.setText(p0.child("sellingPrice").value.toString())
                    cp.setText(p0.child("costPrice").value.toString())
                    etQuantity.setText(p0.child("qty").value.toString())

                    val index = category.indexOf(p0.child("category").value.toString())
                    spinCategory.setSelection(index)
                    spinSubCategory.setSelection(subCategoryList[index].indexOf(p0.child("subCategory").value.toString()))
                }

            })
        }

        return layoutView
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

    fun getRequiredData(): ArrayList<String>{
        val list = ArrayList<String>()
        list.add(barcode)
        list.add(pName.text.toString())
        list.add(sp.text.toString())
        list.add(url)

        return list
    }

    override fun onDismiss(dialog: DialogInterface) {
        callAddToList.invoke(getRequiredData())
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