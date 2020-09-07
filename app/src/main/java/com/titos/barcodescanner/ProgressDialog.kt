package com.titos.barcodescanner

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import java.util.*
import kotlin.concurrent.schedule

class ProgressDialog(context: Context, txt: String) {
    var dialog: Dialog = Dialog(context)

    init {
        val inflate = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        dialog.setContentView(inflate)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))
        dialog.findViewById<TextView>(R.id.login_tv_dialog).text = txt
    }

    fun show(){
        dialog.show()
        cancelAfterTimeout()
    }

    private fun cancelAfterTimeout(){
        Timer().schedule(10000){
            if (dialog.isShowing)
                dialog.cancel()
        }
    }

    fun dismiss(){
        dialog.dismiss()
    }
}