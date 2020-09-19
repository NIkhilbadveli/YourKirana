package com.titos.barcodescanner.khataFeature

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.titos.barcodescanner.utils.ProgressDialog
import com.titos.barcodescanner.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.*
import java.util.Locale.getDefault
import kotlin.collections.ArrayList

class KhataFragmentInside : Fragment(), SearchView.OnQueryTextListener {

    private  val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val customerList = ArrayList<khataItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layoutView = inflater.inflate(R.layout.fragment_khata_inside, container, false)

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!

        val status = when(arguments?.getInt("khataNumber"))
        {
            0 -> "notPaid"
            1 -> "paid"
            else -> "wrong"
        }

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_khata_customers)

        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val onItemRemoveClick :((Int)->Unit) = { pos ->
            groupAdapter.removeGroupAtAdapterPosition(pos)
        }

        val dialog = ProgressDialog(requireContext(), "Please Wait...")
        dialog.show()

        val khataRef = FirebaseDatabase.getInstance().reference.child("khataBook/$shopName")
        khataRef.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot)
            {
                if (!p0.hasChildren())
                    layoutView.findViewById<TextView>(R.id.tv_empty).visibility = View.VISIBLE

                for (timeStamp in p0.children)
                {
                    if (timeStamp.child("status").value.toString()==status)
                    {
                        val customerId = timeStamp.child("customerId").value.toString()
                        val customerName = timeStamp.child("customerName").value.toString()
                        val mobileNumber = timeStamp.child("mobileNumber").value.toString()
                        val dueDate = timeStamp.child("dueDate").value.toString()
                        val amountDue = timeStamp.child("amountDue").value.toString()


                      customerList.add(khataItem(customerId, customerName, mobileNumber, amountDue, dueDate, timeStamp.key!!,onItemRemoveClick))
                    }
                }
                groupAdapter.addAll(customerList)
                dialog.dismiss()
            }
        })

        val searchView = layoutView.findViewById<SearchView>(R.id.search_bar_khata)
        searchView.setOnQueryTextListener(this)

        return layoutView
    }

    override fun onQueryTextSubmit(query: String?): Boolean {

        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filter(newText)
        return false
    }

    private fun filter(charText: String) {
        val lowerCaseText = charText.toLowerCase(getDefault())

        if (lowerCaseText.isNotEmpty())
        {
            groupAdapter.clear()
            groupAdapter.addAll(customerList.filter { it -> it.customerName.toLowerCase(Locale.getDefault()).contains(lowerCaseText) })
        }
        else
        {
            groupAdapter.clear()
            groupAdapter.addAll(customerList)
        }

    }
}
