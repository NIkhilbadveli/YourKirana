package com.titos.barcodescanner.scannerFeature

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ScannerItem(var scanned: Boolean,
                  var name: String,
                  var quantity: String,
                  var price: String,
                  var thumbnailUrl: String
):Parcelable