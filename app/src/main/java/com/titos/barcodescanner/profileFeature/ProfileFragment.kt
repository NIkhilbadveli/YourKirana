package com.titos.barcodescanner.profileFeature


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ShareCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.titos.barcodescanner.ProgressDialog

import com.titos.barcodescanner.R
import com.titos.barcodescanner.loginFeature.LoginActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder


class ProfileFragment : Fragment() {

    private lateinit var layoutView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        layoutView = inflater.inflate(R.layout.fragment_profile, container, false)

        val profileAvatar = layoutView.findViewById<AvatarView>(R.id.profile_avatar)
        val user = FirebaseAuth.getInstance().currentUser!!

        val picassoLoader = PicassoLoader()

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!
        val userName = sharedPref.getString("userName","UserName")!!

        layoutView.findViewById<TextView>(R.id.profile_name).text = userName.split(' ').joinToString(" ") { it.capitalize() }
        layoutView.findViewById<TextView>(R.id.store_name).text = shopName.split(' ').joinToString(" ") { it.capitalize() }
        picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), userName)

        val recyclerView = layoutView.findViewById<RecyclerView>(R.id.rv_profile)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        groupAdapter.add(ProfileItem(R.drawable.icons8_administrator_male_48px_2, "Edit Profile"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_share_48px, "Share App"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_logout_rounded_left_48px, "Logout"))

        val dialog = ProgressDialog.progressDialog(requireContext())
        groupAdapter.setOnItemClickListener { _, itemView ->
            when (recyclerView.getChildAdapterPosition(itemView)) {
                0 -> editProfile(user)
                1 -> shareApp()
                2 -> logoutFromApp(dialog)
            }
        }

        val customerRequestsButton = layoutView.findViewById<Button>(R.id.customer_requests)
        customerRequestsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_customerRequestsFragment)
        }

        val shortTask = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse("https://www.rikistores.com?shopName=$shopName"))
                .setDomainUriPrefix("https://titos.page.link")
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder("com.titos.rikistores").build())
                .buildShortDynamicLink()

        val downloadLink = "https://firebasestorage.googleapis.com/v0/b/barcode-scanner-b4a04.appspot.com/o/app-release.apk"
        shortTask.addOnSuccessListener {
            val msg = "$userName invited you to join his store $shopName on Rikistores app.\n" +
                    "Download link ----> $downloadLink\n \n " +
                    "Steps to follow after installing: \n " +
                    "1) Login with any of the providers\n " +
                    "2) Tap on 'Join Later' when asked for a shop name\n " +
                    "3) Now close the app if opened & select the below link for joining" +
                    "\n \n Click here to join ----> " + it.shortLink

            layoutView.findViewById<ImageView>(R.id.btn_add_user).setOnClickListener {
                ShareCompat.IntentBuilder.from(requireActivity())
                        .setType("text/plain")
                        .setChooserTitle("Send the store link via...")
                        .setText(msg)
                        .startChooser()
            }
        }.addOnFailureListener{ }

        return layoutView
    }

    private fun editProfile(user:FirebaseUser){
        val userRef = FirebaseDatabase.getInstance().reference.child("userData").child(user.uid)
        val viewGroup = layoutView.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, viewGroup, false)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val dpName = p0.child("userName").value.toString()
                val storeName = p0.child("shopName").value.toString()
                if (dpName!="null"&&storeName!="null") {
                    dialogView.findViewById<TextView>(R.id.et_dp_name).text = dpName
                    dialogView.findViewById<TextView>(R.id.et_store_name).text = storeName
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)!!
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.product_add_button).setOnClickListener {
            val userName = dialogView.findViewById<TextView>(R.id.et_dp_name).text
            val shopName = dialogView.findViewById<TextView>(R.id.et_store_name).text
            if (userName.isNotEmpty()&&shopName.isNotEmpty()){
                userRef.child("userName").setValue(userName.toString())
                userRef.child("shopName").setValue(shopName.toString())
                sharedPref.edit {
                    putString("shopName", shopName.toString())
                    putString("userName", userName.toString())
                    commit()
                }

                layoutView.findViewById<TextView>(R.id.profile_name).text = userName.toString().split(' ').joinToString(" ") { it.capitalize() }
                layoutView.findViewById<TextView>(R.id.store_name).text = shopName.toString().split(' ').joinToString(" ") { it.capitalize() }

                Toast.makeText(requireContext(),"Profile Details changed successfully", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
            else
                Toast.makeText(requireContext(),"Don't leave any of the fields empty", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.product_cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun shareApp(){
        ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain")
                .setChooserTitle("Share the apk link via...")
                .setText("You can download Riki Stores APK using this link. \n " +
                        "https://firebasestorage.googleapis.com/v0/b/barcode-scanner-b4a04.appspot.com/o/app-release.apk")
                .startChooser()
    }

    private fun logoutFromApp(dialog:Dialog){
        dialog.show()
        AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener{
                    dialog.dismiss()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finish()
                }
    }
}
