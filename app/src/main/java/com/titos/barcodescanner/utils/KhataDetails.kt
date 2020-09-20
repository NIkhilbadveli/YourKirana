package com.titos.barcodescanner.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class KhataDetails(
                var amountDue: String = "",
                var customerId: String = "",
                var customerName: String = "",
                var dueDate: String = "",
                var mobileNumber: String = "",
                var status: String = ""): Parcelable