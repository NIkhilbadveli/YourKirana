package com.titos.barcodescanner.historyFeature

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

        val etcustName = layoutView.findViewById<EditText>(R.id.custName)
        //val etcustId = layoutView.findViewById<EditText>(R.id.custId)

        val etMobileNumber = layoutView.findViewById<EditText>(R.id.etMobileNumber)
        if(arguments?.getString("contact")!="null")
            etMobileNumber.setText(arguments?.getString("contact"))

        val amountDue = arguments?.getString("amountDue")
        //val tvTakenDate = layoutView.findViewById<TextView>(R.id.tvTakenDate)
        val tvDueDate = layoutView.findViewById<TextView>(R.id.tvDueDate)
        val tvTitle = layoutView.findViewById<TextView>(R.id.tv_title)
        tvTitle.text = "Not Paid Agreement    -     \u20B9 $amountDue"

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.US)

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)

        val dpc = DatePickerDialog(
                requireContext(),
                { _: DatePicker, yr, mth, dayOfMonth ->
                    if (dayOfMonth>=day && month==mth && year==yr)
                        tvDueDate.text = "$dayOfMonth-${mth+1}-$yr"
                    else
                        Toast.makeText(context, "Please select date in the future", Toast.LENGTH_SHORT).show()
                },
                year,
                month,
                day
        )

        //calButton due
        layoutView.findViewById<ImageButton>(R.id.calButtonDue).setOnClickListener { dpc.show() }


        val khataDetails = KhataDetails()
        layoutView.findViewById<Button>(R.id.saveButton).setOnClickListener {

            if (etcustName.text.isNotEmpty() && etMobileNumber.text.isNotEmpty()
                    && tvDueDate.text!="00-00-0000" && amountDue!=null) {
                val addedTime = simpleDateFormat.format(Date())

                khataDetails.customerName=(etcustName.text.toString())
                khataDetails.mobileNumber=(etMobileNumber.text.toString())
                khataDetails.dueDate=(tvDueDate.text.toString())
                khataDetails.amountDue=(amountDue)
                khataDetails.status=("notPaid")

                firebaseHelper.addToKhata(addedTime, khataDetails)

                Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            else
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
        }

    }
}
