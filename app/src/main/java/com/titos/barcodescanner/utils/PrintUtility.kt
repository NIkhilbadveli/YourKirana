package com.titos.barcodescanner.utils

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.titos.barcodescanner.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round


class PrintUtility(val ctx: Context, val billDetails: BillDetails) {
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager?
                    val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            printBill(UsbConnection(usbManager, usbDevice))
                        }
                    }
                }
            }
        }
    }

    init {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(ctx)
        val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager?
        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(ctx, 0, Intent(ACTION_USB_PERMISSION), 0)
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            ctx.registerReceiver(usbReceiver, filter)
            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }

    private fun printBill(printerConnection: DeviceConnection){
        try {
            val format = SimpleDateFormat("'Ordered' 'on' dd-MM-yyyy 'at' HH:mm:ss a", Locale.ROOT)
            val printer = EscPosPrinter(printerConnection, 203, 58f, 32)
            var itemsText = ""
            billDetails.billItems.forEach {
                itemsText =
                        "[L]<font size='small'>${it.name}</font>\n"+
                        "[L]<font size='small'>        ${it.quantity} * ${it.price}</font>[R]<font size='small'>${(it.price.toDouble()*it.quantity.toInt()).round(2)}</font>\n" +
                        "[L]\n"
            }

            //[C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(printer, ctx.resources.getDrawableForDensity(R.drawable.ic_store_black_24dp, DisplayMetrics.DENSITY_MEDIUM))}</img>
            val formattedText =
                    "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, ctx.getResources().getDrawableForDensity(R.drawable.logo, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                    "[L]\n" +
                    "[C]<u><font size='big'>Riki Stores</font></u>\n" +
                    "[C]<font size='small'>${format.format(Date())}</font>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[C] Name       Qty     Total\n" +
                    itemsText +
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[R]TOTAL PRICE :[R]${billDetails.orderValue}\n" +
                    "[R]TAX :[R]0.00\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<font>Customer Number: ${billDetails.contact}</font>\n" +
                    "[L]Riki Stores,\n" +
                    "[L]#68-5, 5th cross, Behind Ganapathi temple,\n" +
                    "[L]Begur Main road, Bommanahalli - 560068\n" +
                    "[L]Mobile : 9182677727\n" +
                    "[L]\n"

            printer.printFormattedText(formattedText)
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            AlertDialog.Builder(ctx)
                    .setTitle("Broken connection")
                    .setMessage(e.message)
                    .show()
        } catch (e: EscPosParserException) {
            e.printStackTrace()
            AlertDialog.Builder(ctx)
                    .setTitle("Invalid formatted text")
                    .setMessage(e.message)
                    .show()
        } catch (e: EscPosEncodingException) {
            e.printStackTrace()
            AlertDialog.Builder(ctx)
                    .setTitle("Bad selected encoding")
                    .setMessage(e.message)
                    .show()
        } catch (e: EscPosBarcodeException) {
            e.printStackTrace()
            AlertDialog.Builder(ctx)
                    .setTitle("Invalid barcode")
                    .setMessage(e.message)
                    .show()
        }
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}