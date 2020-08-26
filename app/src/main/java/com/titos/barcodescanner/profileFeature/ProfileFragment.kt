package com.titos.barcodescanner.profileFeature


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.storage.FirebaseStorage
import com.titos.barcodescanner.ProgressDialog

import com.titos.barcodescanner.R
import com.titos.barcodescanner.loginFeature.LoginActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val profileAvatar = view.findViewById<AvatarView>(R.id.profile_avatar)
        val user = FirebaseAuth.getInstance().currentUser!!

        val picassoLoader = PicassoLoader()

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName","shop")!!
        val userName = sharedPref.getString("userName","UserName")!!

        view.findViewById<TextView>(R.id.profile_name).text = userName.split(' ').joinToString(" ") { it.capitalize() }
        view.findViewById<TextView>(R.id.store_name).text = shopName.split(' ').joinToString(" ") { it.capitalize() }
        picassoLoader.loadImage(profileAvatar, user.photoUrl.toString(), userName)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_profile)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        groupAdapter.add(ProfileItem(R.drawable.icons8_administrator_male_48px_2, "Edit Profile"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_share_48px, "Share App"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_office_phone_48px_1, "Contact Us"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_logout_rounded_left_48px, "Logout"))

        val dialog = ProgressDialog.progressDialog(requireContext())
        groupAdapter.setOnItemClickListener { item, view ->
            when (recyclerView.getChildAdapterPosition(view)) {
                0 -> editProfile(user)
                1 -> shareApp()
                2 -> shareContactDetails()
                3 -> logoutFromApp(dialog)
            }
        }

        val customerRequestsButton = view.findViewById<Button>(R.id.customer_requests)
        customerRequestsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_customerRequestsFragment)
        }

        val shortTask = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse("https://www.rikistores.com?shopName=$shopName"))
                .setDomainUriPrefix("https://titos.page.link")
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder("com.titos.rikistores").build())
                .buildShortDynamicLink()

        shortTask.addOnSuccessListener {
            val msg = "$userName invited you to join his store $shopName on Rikistores app. " +
                    "\n \n Click here to join ----> " + it.shortLink
            view.findViewById<ImageView>(R.id.btn_add_user).setOnClickListener {
                ShareCompat.IntentBuilder.from(requireActivity())
                        .setType("text/plain")
                        .setChooserTitle("Send the referral code via...")
                        .setText(msg)
                        .startChooser()
            }
        }.addOnFailureListener{ }

        return  view
    }

    private fun editProfile(user:FirebaseUser){
        val userRef = FirebaseDatabase.getInstance().reference.child("userData").child(user.uid)
        val viewGroup = view?.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_product_dialog, viewGroup, false)

        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Edit Profile Details"
        dialogView.findViewById<EditText>(R.id.name_text_input).hint = "Display Name"
        dialogView.findViewById<TextView>(R.id.price_text_input).hint = "Shop Name"

        dialogView.findViewById<TextView>(R.id.product_price).inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
        dialogView.findViewById<TextView>(R.id.product_name).inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE

        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.child("userName").value.toString().isNotEmpty())
                    dialogView.findViewById<TextView>(R.id.product_name).text = p0.child("userName").value.toString()
                dialogView.findViewById<TextView>(R.id.product_price).text = p0.child("shopName").value.toString()
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.product_add_button).setOnClickListener {
            val userName = dialogView.findViewById<TextView>(R.id.product_name).text
            val shopName = dialogView.findViewById<TextView>(R.id.product_price).text
            if (userName.isNotEmpty()&&shopName.isNotEmpty()){
                userRef.child("userName").setValue(userName.toString())
                userRef.child("shopName").setValue(shopName.toString())
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

    private fun shareContactDetails(){
        val viewGroup = view?.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.contact_details_dialog, viewGroup, false)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun shareApp(){
        ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain")
                .setChooserTitle("Share the app via...")
                .setText("https://play.google.com/store/apps/details?id=com.titos.barcodescanner&hl=en")
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
