package com.titos.barcodescanner.scannerFeature

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.titos.barcodescanner.R



class ScannerFragment : Fragment() {
    private var fragmentTransaction:FragmentTransaction? = null

    private var scannerView:View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val fragmentManager = childFragmentManager
        fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction!!.add(R.id.barcodeFragment, BarcodeFragment())
        fragmentTransaction!!.add(R.id.listFragment, ScannerListFragment())
        fragmentTransaction!!.commit()

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar_main)
        toolbar!!.findViewById<TextView>(R.id.text_view_toolbar).visibility = View.GONE
        toolbar.findViewById<SwitchCompat>(R.id.inventory_scanner_switch).visibility = View.VISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        scannerView = inflater.inflate(R.layout.fragment_scanner, container, false)

        return scannerView
    }
}