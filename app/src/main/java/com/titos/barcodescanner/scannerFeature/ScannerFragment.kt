package com.titos.barcodescanner.scannerFeature

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.titos.barcodescanner.R
import org.w3c.dom.Text

class ScannerFragment : Fragment() {
    private var fragmentTransaction:FragmentTransaction? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val scannerView = inflater.inflate(R.layout.fragment_scanner, container, false)
        setHasOptionsMenu(true)

        val tvTotal = scannerView.findViewById<TextView>(R.id.tv_total)
        val btnTick = scannerView.findViewById<FloatingActionButton>(R.id.btn_bill)
        val btnInv = scannerView.findViewById<Button>(R.id.check_out_button)

        val fragmentManager = childFragmentManager
        fragmentTransaction = fragmentManager.beginTransaction()

        val scannerListFragment = ScannerListFragment(tvTotal, btnTick, btnInv)
        fragmentTransaction!!.add(R.id.barcodeFragment, BarcodeFragment())
        fragmentTransaction!!.add(R.id.listFragment, scannerListFragment)
        fragmentTransaction!!.commit()

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.GONE

        val switch = toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch)
        switch.visibility = View.VISIBLE
        //Setting initial mode
        switch?.isChecked = true

        switch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                scannerView.findViewById<LinearLayout>(R.id.total_price_container)!!.visibility = View.VISIBLE
                scannerView.findViewById<LinearLayout>(R.id.checkout_container)!!.visibility = View.GONE
                btnTick.visibility = View.VISIBLE
                switch.text = getString(R.string.scanner_mode)
                scannerListFragment.tvContact.visibility = View.VISIBLE
            }
            else{
                scannerView.findViewById<LinearLayout>(R.id.total_price_container)!!.visibility = View.GONE
                scannerView.findViewById<LinearLayout>(R.id.checkout_container)!!.visibility = View.VISIBLE
                btnTick.visibility = View.GONE
                switch.text = getString(R.string.inventory_mode)
                scannerListFragment.tvContact.visibility = View.GONE
            }
        }

        return scannerView
    }
}