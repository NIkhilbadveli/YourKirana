package com.titos.barcodescanner.khataFeature

import android.view.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

import com.titos.barcodescanner.R
import android.app.DatePickerDialog

import android.widget.*
import androidx.navigation.fragment.findNavController

import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.KhataDetails
import java.text.SimpleDateFormat
import java.util.*

class AgreementFragment : BaseFragment(R.layout.fragment_agreement) {

    override fun initView() {

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        val etOptional = layoutView.findViewById<EditText>(R.id.etOptional)
        val etMobileNumber = layoutView.findViewById<EditText>(R.id.etMobileNumber)
        val etAmountDue = layoutView.findViewById<EditText>(R.id.etAmount)

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.US)

        val khataDetails = KhataDetails()
        layoutView.findViewById<Button>(R.id.saveButton).setOnClickListener {

            if (etMobileNumber.text.isNotEmpty() && etMobileNumber.text.length == 10
                    && etAmountDue.text.isNotEmpty()) {
                val addedTime = simpleDateFormat.format(Date())

                khataDetails.mobileNumber=(etMobileNumber.text.toString())
                khataDetails.amountDue=(etAmountDue.text.toString())
                khataDetails.optionalNote = etOptional.text.toString()
                khataDetails.status=("notPaid")

                firebaseHelper.addToKhata(addedTime, khataDetails)

                Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            else if (etMobileNumber.text.length !=10)
                showToast("Mobile number not correct")
            else
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
        }

    }
}
