package com.titos.barcodescanner.utils

import android.os.Parcelable
import com.titos.barcodescanner.scannerFeature.ScannerItem
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BillDetails(
                var contact: String = "",
                var orderValue: String = "",
                var billItems: ArrayList<ScannerItem> = ArrayList()): Parcelable