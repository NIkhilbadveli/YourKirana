package com.titos.barcodescanner.scannerFeature

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ScannerItem(var barcode: String,
                  var name: String,
                  var quantity: String,
                  var price: String,
                  var loose: Boolean,
                  var thumbnailUrl: String
):Parcelable