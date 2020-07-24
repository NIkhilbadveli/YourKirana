package com.titos.barcodescanner.scannerFeature

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.titos.barcodescanner.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Year
import java.util.*
import java.util.zip.DataFormatException

class AgreementFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val layoutView = inflater.inflate(R.layout.fragment_agreement, container, false)
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.VISIBLE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.GONE

        val database = FirebaseDatabase.getInstance().reference
        val etcustName = layoutView.findViewById<EditText>(R.id.custName)
        val etcustId = layoutView.findViewById<EditText>(R.id.custId)
        val etMobileNumber = layoutView.findViewById<EditText>(R.id.etMobileNumber)

        //val tvTakenDate = layoutView.findViewById<TextView>(R.id.tvTakenDate)
        val tvDueDate = layoutView.findViewById<TextView>(R.id.tvDueDate)

        val simpleDateFormat = SimpleDateFormat("dd-mm-yyyy hh:mm:ss", Locale.US)
        val addedTime = simpleDateFormat.format(Date())

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)

        //calButton due
        layoutView.findViewById<ImageButton>(R.id.calButtonDue).setOnClickListener {
            val dpc = DatePickerDialog(
                    requireContext(),
                    DatePickerDialog.OnDateSetListener { view: DatePicker, year, month, dayOfMonth ->

                        tvDueDate.text = "$dayOfMonth-${month+1}-$year"
                    },
                    year,
                    month,
                    day
            )
            dpc.show()
        }

        val amountDue = arguments?.getString("amountDue")
        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!

        layoutView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            if (etcustId.text.isNotEmpty() && etcustName.text.isNotEmpty() && etMobileNumber.text.isNotEmpty()
                    && tvDueDate.text!="00-00-0000" && amountDue!=null) {

                database.child("khataBook/$shopName/$addedTime/customerId").setValue(etcustId.text.toString())
                database.child("khataBook/$shopName/$addedTime/customerName").setValue(etcustName.text.toString())
                database.child("khataBook/$shopName/$addedTime/mobileNumber").setValue(etMobileNumber.text.toString())
                database.child("khataBook/$shopName/$addedTime/dueDate").setValue(tvDueDate.text.toString())
                database.child("khataBook/$shopName/$addedTime/amountDue").setValue(amountDue)
                database.child("khataBook/$shopName/$addedTime/status").setValue("notPaid")
                Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            else
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
        }

        return layoutView
    }
}
