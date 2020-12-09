package com.titos.barcodescanner.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


data class ProductDetailsV2(
                var name: String = "",
                var sellingPrice: String = "",
                var url: String = "",
                var type: String = "",
                var category: String = "",
                var subCategory: String = "")