package com.titos.barcodescanner.khataFeature

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import com.titos.barcodescanner.R


class KhataAdapter(val context: Context, val changeList: ArrayList<String>, val map: HashMap<String, String>):
        BaseAdapter() {

    override fun getCount(): Int {
        return changeList.size
    }

    override fun getItem(p0: Int): Any {
        return changeList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.item_khata_changes, parent, false)

        rowView.findViewById<TextView>(R.id.tv_time).text = changeList[position]
        rowView.findViewById<TextView>(R.id.tv_change).text = map[changeList[position]]

        return rowView
    }
}