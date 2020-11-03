package com.titos.barcodescanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe

import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.BillDetails
import com.titos.barcodescanner.utils.KhataDetails
import com.titos.barcodescanner.utils.PrintUtility

class BillFragment : BaseFragment(R.layout.fragment_bill) {
    private var message = ""
    override fun initView() {
        val billDetails = requireArguments().getParcelable<BillDetails>("billDetails")!!

        message = "Hi, how are you? We're from Riki Stores, your local convenience store"

        val tvBillContent = layoutView.findViewById<TextView>(R.id.tvBillContent)
        tvBillContent.text = message

        layoutView.findViewById<Button>(R.id.btnShareBill).setOnClickListener {
            shareBill(billDetails)
        }

        layoutView.findViewById<Button>(R.id.btnPrintBill).setOnClickListener {
            printBill(billDetails)
            showToast("Bill is being printed")
        }
    }

    private fun shareBill(billDetails: BillDetails){
        val sendIntent = Intent("android.intent.action.MAIN")
        val packageManager: PackageManager = requireContext().packageManager

        sendIntent.action = Intent.ACTION_SEND
        sendIntent.setPackage("com.whatsapp.w4b")
        sendIntent.type = "text/plain"
        val phone = "91${billDetails.contact}"

                try {
                    sendIntent.putExtra("jid", "$phone@s.whatsapp.net")
                    sendIntent.putExtra(Intent.EXTRA_TEXT, message)

                    if (sendIntent.resolveActivity(packageManager) != null) {
                        requireContext().startActivity(sendIntent)
                    }
                } catch (e: Exception) {
                    showToast("No app installed")
                    e.printStackTrace()
                }
    }

    private fun printBill(billDetails: BillDetails){
        PrintUtility(requireContext(), billDetails)
    }
}