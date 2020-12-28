package com.titos.barcodescanner.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class KhataDetails(
                var amountDue: Double = 0.0,
                var amountPaid: Double = 0.0,
                var customerName: String = "",
                var changes: Map<String, String> = mapOf()): Parcelable