package com.titos.barcodescanner.inventoryFeature

import android.view.View
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_inventory.*
import java.util.*

class StockItem(val action: String, val actionTime: String, val prevQty : Int): Item() {

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            /*
            For example: action value will be something like "+6" or "-5"
            Step-1:- Take first char in action and if it is + then tvStatus should be Added : 6 otherwise it is Sale : 6. Also, assign up or down arrow
            Step-2:- Assign time to corresponding textView
            Step-3:- stockLeft = prevQty +6 or maybe prevQty -5 like that. Show this in tvStockLeft. Assign color as well

             */
        }
    }

    override fun getLayout(): Int = R.layout.item_stock
}