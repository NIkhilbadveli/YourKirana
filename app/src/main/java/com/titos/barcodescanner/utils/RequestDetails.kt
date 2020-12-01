package com.titos.barcodescanner.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RequestDetails(
                var name: String = "",
                var qty: Int = 1,
                var checked: Boolean = false): Parcelable