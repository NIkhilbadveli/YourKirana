package com.titos.barcodescanner.khataFeature

import android.app.Activity
import android.view.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

import com.titos.barcodescanner.R
import android.app.DatePickerDialog
import android.content.Intent
import android.util.Log

import android.widget.*
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.deepakkumardk.kontactpickerlib.KontactPicker
import com.deepakkumardk.kontactpickerlib.model.KontactPickerItem
import com.deepakkumardk.kontactpickerlib.model.SelectionMode
import com.deepakkumardk.kontactpickerlib.model.SelectionTickView

import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.KhataDetails
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AgreementFragment : BaseFragment(R.layout.fragment_agreement) {

    private lateinit var etMobileNumber: AutoCompleteTextView
    private lateinit var etCustomerName: AutoCompleteTextView

    override fun initView() {
        val kdMap = arguments?.getSerializable("kdMap") as HashMap<String, KhataDetails>

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        val etOptional = layoutView.findViewById<EditText>(R.id.etOptional)
        etMobileNumber = layoutView.findViewById(R.id.etMobileNumber)
        val etAmountDue = layoutView.findViewById<EditText>(R.id.etAmount)
        etCustomerName = layoutView.findViewById(R.id.etCustomerName)

        val adapter = ArrayAdapter(requireContext(), R.layout.item_text, R.id.text1, kdMap.keys.toList())
        etMobileNumber.setAdapter(adapter)
        etMobileNumber.setOnItemClickListener { _, _, pos, _ ->
            val number = adapter.getItem(pos)!!
            etCustomerName.setText(kdMap[number]!!.customerName)
        }

        val nameList = ArrayList<String>()
        kdMap.forEach { nameList.add(it.value.customerName) }
        val adapter1 = ArrayAdapter(requireContext(), R.layout.item_text, R.id.text1, nameList)
        etCustomerName.setAdapter(adapter1)
        etCustomerName.setOnItemClickListener { _, _, pos, _ ->
            val name = adapter1.getItem(pos)!!
            val number = kdMap.filterValues { it.customerName == name }.keys

            if (number.isNotEmpty())
                etMobileNumber.setText(number.first())
        }


        val item = KontactPickerItem().apply {
            debugMode = true
            selectionTickView = SelectionTickView.LargeView
            selectionMode = SelectionMode.Single
        }
        layoutView.findViewById<ImageButton>(R.id.btnContacts).setOnClickListener {
            KontactPicker().startPickerForResult(this, item, 3000)  //RequestCode
        }

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.US)

        val khataDetails = KhataDetails()
        layoutView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            etAmountDue.clearFocus()
            etMobileNumber.clearFocus()
            etCustomerName.clearFocus()
            etOptional.clearFocus()
            if (etMobileNumber.text.isNotEmpty() && etMobileNumber.text.length == 10
                    && etAmountDue.text.isNotEmpty()) {
                val addedTime = simpleDateFormat.format(Date())

                khataDetails.amountDue=(etAmountDue.text.toString().toDouble())
                khataDetails.customerName = etCustomerName.text.toString()

                firebaseHelper.addToKhata(addedTime, etMobileNumber.text.toString(), etOptional.text.toString(), khataDetails).observe(this){
                    if (it){
                        //Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    else
                        showToast("Error occurred in firestore.")
                }
            }
            else if (etMobileNumber.text.length !=10)
                showToast("Mobile number not correct")
            else
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 3000) {
            val list = KontactPicker.getSelectedKontacts(data)!!  //ArrayList<MyContacts>
            if (list.isNotEmpty()){
                if (list[0].contactNumber!!.contains("+91"))
                    etMobileNumber.setText(list[0].contactNumber!!.replace("+91", ""))
                else
                    etMobileNumber.setText(list[0].contactNumber)

                etCustomerName.setText(list[0].contactName)
            }
        }
    }
}
