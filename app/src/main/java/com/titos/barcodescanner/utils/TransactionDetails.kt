package com.titos.barcodescanner.utils

data class TransactionDetails(
                var contact: String = "",
                var orderValue: String = "",
                var items: Map<String, String> = mapOf())