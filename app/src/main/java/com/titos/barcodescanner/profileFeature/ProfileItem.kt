package com.titos.barcodescanner.profileFeature

import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_profile.*

class ProfileItem(private val imageId: Int, val itemName: String): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            profile_item_icon.setImageResource(imageId)
            profile_item_name.text = itemName
        }
    }

    override fun getLayout(): Int = R.layout.item_profile
}