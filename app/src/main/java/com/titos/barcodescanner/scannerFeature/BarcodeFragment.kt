package com.titos.barcodescanner.scannerFeature

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.ViewfinderView
import com.titos.barcodescanner.MainActivity
import com.titos.barcodescanner.R

import java.util.*
import kotlin.concurrent.schedule

class BarcodeFragment : Fragment() {
    private var barcode_view: View? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private var viewfinderView: ViewfinderView? = null
    private var lastText: String? = null
    private var qrScan: IntentIntegrator? = null
    private var model: MainActivity.SharedViewModel? = null

    private var confirmCounter = 0
    private var modelPreviousText: String? = null
    private var volumeLevel: Int = 100
    private var isFlashOn: Boolean = false

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text != null && result.text == lastText) {
                confirmCounter++

                if (confirmCounter == 2) {
                    if (modelPreviousText != lastText){
                        model!!.select(lastText!!)
                        modelPreviousText = lastText
                        beepAndVibratePhone()
                        Timer().schedule(2000) {
                            modelPreviousText = ""
                        }
                    }
                    confirmCounter = 0
                }
            }

            lastText = result.text
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        barcode_view = inflater.inflate(R.layout.fragment_barcode, container, false)
        model = ViewModelProviders.of(requireParentFragment()).get(MainActivity.SharedViewModel::class.java)
        barcodeScannerView = barcode_view!!.findViewById(R.id.zxing_barcode_scanner)
        viewfinderView = barcode_view!!.findViewById(R.id.zxing_viewfinder_view)

        //intializing scan object
        qrScan = IntentIntegrator(activity)
        qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES)
        qrScan!!.setOrientationLocked(false)
        qrScan!!.setPrompt("Place barcode inside")

        barcodeScannerView!!.initializeFromIntent(qrScan!!.createScanIntent())
        barcodeScannerView!!.decodeContinuous(callback)

        model?.isScannerPaused?.observe(viewLifecycleOwner, Observer { paused ->
            if (paused)
                barcodeScannerView!!.pause()
            else
                barcodeScannerView!!.resume()
        })

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)!!
        val volumeOnOffButton = barcode_view!!.findViewById<ImageButton>(R.id.volume_on_off)

        //setting according to sharedPref
        if (sharedPref.getBoolean("volumeOnOff",true)){
            with (sharedPref.edit()) {
                putBoolean("volumeOnOff", true)
                commit()
            }
        }
        else{
            volumeOnOffButton.setImageResource(R.drawable.ic_volume_off_black_24dp)
            volumeLevel = 0
        }

        volumeOnOffButton.setOnClickListener {
            if (volumeLevel==100){
                volumeOnOffButton.setImageResource(R.drawable.ic_volume_off_black_24dp)
                volumeLevel = 0
                with (sharedPref.edit()) {
                    putBoolean("volumeOnOff", false)
                    commit()
                }
            }
            else{
                volumeOnOffButton.setImageResource(R.drawable.ic_volume_on_black_24dp)
                volumeLevel = 100
                with (sharedPref.edit()) {
                    putBoolean("volumeOnOff", true)
                    commit()
                }
            }
        }

        val flashOnOffButton = barcode_view!!.findViewById<ImageButton>(R.id.flash_on_off)
        if (!context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)!!)
            flashOnOffButton.visibility = View.GONE

        flashOnOffButton.setOnClickListener {
            if (isFlashOn){
                flashOnOffButton.setImageResource(R.drawable.ic_flash_off_black_24dp)
                barcodeScannerView!!.setTorchOff()
                isFlashOn = false
            }
            else{
                flashOnOffButton.setImageResource(R.drawable.ic_flash_on_black_24dp)
                barcodeScannerView!!.setTorchOn()
                isFlashOn = true
            }
        }

        return barcode_view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModelStore.clear()
    }

    override fun onResume() {
        super.onResume()
        barcodeScannerView!!.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeScannerView!!.pause()
    }

    fun beepAndVibratePhone() {
        /*val audioManager:AudioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        *//*val volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)*100/15*/

        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC,volumeLevel)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,1000)

        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(150)
        }
    }

}
