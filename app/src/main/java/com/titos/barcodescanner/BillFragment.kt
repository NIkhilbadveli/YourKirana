package com.titos.barcodescanner

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.TextView
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.BillDetails
import com.titos.barcodescanner.utils.PrintUtility

class BillFragment : BaseFragment(R.layout.fragment_bill) {
    private var message = ""
    override fun initView() {
        val billDetails = requireArguments().getParcelable<BillDetails>("billDetails")!!
        message = "Name     Qty*RikiPrice      Total\n\n--------------------------------------------\n"
        billDetails.billItems.forEach {
            message += it.name + "\n            ${it.quantity} * ${it.price}    =    Rs. ${(it.price.toDouble()*it.quantity.toDouble()).round(2)}\n" +
                            "\n"
        }

        message += "---------------------------------------------\n\nOrder Total: ${billDetails.orderValue}"

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