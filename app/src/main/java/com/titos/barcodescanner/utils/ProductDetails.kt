package com.titos.barcodescanner.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductDetails(
                var name: String = "",
                var sellingPrice: String = "",
                var costPrice: String = "",
                var qty: Double = 0.0,
                var url: String = "https://www.google.co.in",
                var type: String = "",
                var category: String = "",
                var subCategory: String = "",
                var sold: Double = 0.0,
                var changes: Map<String, String> = mapOf()): Parcelable