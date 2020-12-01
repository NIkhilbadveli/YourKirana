package com.titos.barcodescanner.khataFeature

import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.utils.KhataDetails
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.Locale.getDefault
import kotlin.collections.ArrayList

class KhataFragmentInside : BaseFragment(R.layout.fragment_khata_inside), SearchView.OnQueryTextListener {

    private  val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val customerList = ArrayList<KhataItem>()

    override fun initView() {

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

        val onItemRemoveClick :((String, Int)->Unit) = { time, pos ->
            firebaseHelper.updateKhataStatus(time)
            groupAdapter.removeGroupAtAdapterPosition(pos)
        }

        val kdList = arguments?.getSerializable("kdList")!! as HashMap<String, KhataDetails>

                if (kdList.isEmpty())
                    layoutView.findViewById<TextView>(R.id.tv_empty).visibility = View.VISIBLE

                for (kd in kdList) {
                    if (kd.value.status==status)
                        customerList.add(KhataItem(kd.key, kd.value, onItemRemoveClick))
                }
                groupAdapter.addAll(customerList)

        val searchView = layoutView.findViewById<SearchView>(R.id.search_bar_khata)
        searchView.setOnQueryTextListener(this)

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
            groupAdapter.addAll(customerList.filter { it.kd.customerName.toLowerCase(getDefault()).contains(lowerCaseText) })
        }
        else
        {
            groupAdapter.clear()
            groupAdapter.addAll(customerList)
        }

    }
}
