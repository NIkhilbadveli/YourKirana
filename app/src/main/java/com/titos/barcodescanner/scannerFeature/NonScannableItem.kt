package com.titos.barcodescanner.scannerFeature

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.titos.barcodescanner.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.item_non_scannable.*

class NonScannableItem(val context: Context, private val itemName: String, val itemPrice: String, val itemWeight: String, val itemWeightUnit: String): Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        viewHolder.apply {
            item_name.text = itemName
            item_weight.text = itemWeight
            item_price.text = itemPrice
            item_weight_unit.text = itemWeightUnit

            button_100.setOnClickListener{
                item_price.text = (0.1*itemPrice.toDouble()).toString()
                item_weight.text = "100 "
                item_weight_unit.text = "g"
                //tv_container_weight_list.visibility = View.GONE
            }

            button_250.setOnClickListener{
                item_price.text = (0.25*itemPrice.toDouble()).toString()
                item_weight.text = "250 "
                item_weight_unit.text = "g"
                //tv_container_weight_list.visibility = View.GONE
            }

            button_500.setOnClickListener{
                item_price.text = (0.5*itemPrice.toDouble()).toString()
                item_weight.text = "500 "
                item_weight_unit.text = "g"
                //tv_container_weight_list.visibility = View.GONE
            }

            button_750.setOnClickListener{
                item_price.text = (0.75*itemPrice.toDouble()).toString()
                item_weight.text = "750 "
                item_weight_unit.text = "g"
                //tv_container_weight_list.visibility = View.GONE
            }


            val weightUnits = arrayOf("Kg    ", "g    ")
            val spinnerWeight = spinner_weight
            spinnerWeight.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, weightUnits)
            //spinnerWeight.setSelection(1)
            spinnerWeight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    if (edit_weight.text.isNotEmpty()){
                        if (position==0){
                            val weight = edit_weight.text.toString().toDouble()
                            item_price.text = ((weight/1000)*itemPrice.toDouble()).toString()
                            item_weight.text = edit_weight.text.toString()
                            item_weight_unit.text = weightUnits[position]
                        }
                        else{
                            val weight = edit_weight.text.toString().toDouble()
                            item_price.text = (weight*itemPrice.toDouble()).toString()
                            item_weight.text = edit_weight.text.toString()
                            item_weight_unit.text = weightUnits[position]
                        }
                    }
                    /*else
                        Toast.makeText(context,"Please enter some value before selecting",Toast.LENGTH_SHORT).show()*/
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Code to perform some action when nothing is selected
                }
            }
        }
    }

    override fun getLayout(): Int = R.layout.item_non_scannable
}