package com.titos.barcodescanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.BillDetails
import com.titos.barcodescanner.utils.PrintUtility

class BillFragment(private val billDetails: BillDetails, val context: Context) {
    private var message = ""
    init {

        message = "Ordered at ${billDetails.time}\n \n Name     Qty*Price      Total\n\n--------------------------------------------\n"
        billDetails.billItems.forEach {
            message += it.name + "\n            ${it.quantity} * ${it.price}    =    Rs. ${(it.price.toDouble()*it.quantity.toDouble()).round(2)}\n" +
                            "\n"
        }

        message += "---------------------------------------------\n\nOrder Total: ${billDetails.orderValue}"

        /*val tvBillContent = layoutView.findViewById<TextView>(R.id.tvBillContent)
        tvBillContent.text = message*/

        /*layoutView.findViewById<Button>(R.id.btnShareBill).setOnClickListener {
            shareBill()
        }

        layoutView.findViewById<Button>(R.id.btnPrintBill).setOnClickListener {
            printBill()
            showToast("Bill is being printed")
        }*/
    }

    fun shareBill(){
        val sendIntent = Intent("android.intent.action.MAIN")
        val packageManager: PackageManager = context.packageManager

        sendIntent.action = Intent.ACTION_SEND
        sendIntent.setPackage("com.whatsapp")
        sendIntent.type = "text/plain"
        val phone = "91${billDetails.contact}"

                try {
                    sendIntent.putExtra("jid", "$phone@s.whatsapp.net")
                    sendIntent.putExtra(Intent.EXTRA_TEXT, message)

                    if (sendIntent.resolveActivity(packageManager) != null) {
                        context.startActivity(sendIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "No app installed", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
    }

    private fun printBill(){
        PrintUtility(context, billDetails)
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}