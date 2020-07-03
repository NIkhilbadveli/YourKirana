package com.titos.barcodescanner.profileFeature

import android.content.Context

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.titos.barcodescanner.R

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_customer_requests.*



class CustomerRequestsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val layoutView = inflater.inflate(R.layout.fragment_customer_requests, container, false)

        val sharedPref = activity?.getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!
        val requestRef = FirebaseDatabase.getInstance().reference.child("customerRequests").child(shopName)

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_requests)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                for (request in p0.children){
                    val name = request.child("name").value.toString()
                    val qty = request.child("qty").value.toString().toInt()
                    val checked = request.child("checked").value.toString().toBoolean()
                    groupAdapter.add(CustomerRequestItem(checked,name,qty))
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        var clicked = false
        val addNewRequestButton = layoutView.findViewById<Button>(R.id.add_new_request)
        addNewRequestButton.setOnClickListener {
            if (!clicked){
                add_new_request.text = "Save"
                input_container.visibility = View.VISIBLE
                clicked = true
            }
            else{
                if (product_name.text.isNotEmpty()){
                    val key = requestRef.push().key!!
                    requestRef.child(key).child("name").setValue(product_name.text.toString())
                    requestRef.child(key).child("qty").setValue(1)
                    requestRef.child(key).child("checked").setValue(false)
                    groupAdapter.add(CustomerRequestItem(false,product_name.text.toString(),1))
                }
                /*else
                    Toast.makeText(requireContext(),"Please Enter something", Toast.LENGTH_SHORT).show()*/

                add_new_request.text = "Add New Request"
                input_container.visibility = View.GONE
                clicked = false
            }
        }

        return layoutView
    }
}
