package com.titos.barcodescanner.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import com.titos.barcodescanner.R
import java.util.*
import kotlin.concurrent.schedule

class ProgressDialog(context: Context) {

    var dialog: Dialog = Dialog(context)
    constructor(ct: Context, msg: String) : this(ct)  {
        this.setMsg(msg)
    }

    init {
        val inflate = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        dialog.setContentView(inflate)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))

    }

    fun show(){
        dialog.show()
        cancelAfterTimeout()
    }

    fun setMsg(msg: String){
        dialog.findViewById<TextView>(R.id.login_tv_dialog).text = msg
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

    fun isShowing(): Boolean{ return dialog.isShowing }
}