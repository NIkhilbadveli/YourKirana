package com.titos.barcodescanner.khataFeature

import android.app.Activity
import android.view.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

import com.titos.barcodescanner.R
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class KhataFragmentInside : BaseFragment(R.layout.fragment_khata_inside) {

    override fun initView() {
        val mobile = arguments?.getString("mobile")!!
        val name = arguments?.getString("name")!!
        val amountDue = arguments?.getDouble("amountDue")!!
        val changes = arguments?.getSerializable("changes")!! as HashMap<String, String>

        val tvDetails = layoutView.findViewById<TextView>(R.id.tv_details)
        tvDetails.text = "Details for $name ($mobile)"

        val sendIntent = Intent("android.intent.action.MAIN")
        val packageManager: PackageManager = requireContext().packageManager

        sendIntent.action = Intent.ACTION_SEND
        sendIntent.setPackage("com.whatsapp")
        sendIntent.type = "text/plain"
        val phone = "91$mobile"

        val message = "This is a reminder for paying the amount due (Rs. $amountDue). Kindly clear the due as soon as possible."
        layoutView.findViewById<ImageView>(R.id.btn_whatsapp).setOnClickListener {
            try {
                sendIntent.putExtra("jid", "$phone@s.whatsapp.net")
                sendIntent.putExtra(Intent.EXTRA_TEXT, message)

                if (sendIntent.resolveActivity(packageManager) != null) {
                    requireContext().startActivity(sendIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "No app installed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        val timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a", Locale.ENGLISH)

        val listView = layoutView.findViewById<ListView>(R.id.lv_changes)
        val changeList = ArrayList<String>()
        changes.forEach { changeList.add(it.key)}
        changeList.sortByDescending { LocalDateTime.parse(it, timeFormatter) }

        listView.adapter  = KhataAdapter(requireContext(), changeList, changes )

        val etAmountPaid = layoutView.findViewById<EditText>(R.id.etAmountPaid)
        layoutView.findViewById<Button>(R.id.btnSettle).setOnClickListener {
            etAmountPaid.clearFocus()
            if (etAmountPaid.text.isNotEmpty()) {
                firebaseHelper.updateKhataStatus(mobile, etAmountPaid.text.toString().toDouble())
                showSnackBar("Amount settled is \u20B9 ${etAmountPaid.text}")
                findNavController().navigateUp()
            }
            else
                showToast("Please enter a value!")
        }
    }

}
