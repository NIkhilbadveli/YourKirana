package com.titos.barcodescanner.profileFeature


import agency.tango.android.avatarview.loader.PicassoLoader
import agency.tango.android.avatarview.views.AvatarView
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ShareCompat
import androidx.core.content.edit
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.titos.barcodescanner.R
import com.titos.barcodescanner.base.BaseFragment
import com.titos.barcodescanner.loginFeature.LoginActivity
import com.titos.barcodescanner.utils.ProgressDialog
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder


class ProfileFragment : BaseFragment(R.layout.fragment_profile) {

    override fun initView() {

        val profileAvatar = layoutView.findViewById<AvatarView>(R.id.profile_avatar)
        val user = FirebaseAuth.getInstance().currentUser!!

        val picassoLoader = PicassoLoader()

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        val shopName = sharedPref?.getString("shopName", "shop")!!
        val userName = sharedPref.getString("userName", "UserName")!!

        if (userName!="null")
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
        groupAdapter.add(ProfileItem(R.drawable.ic_rupee_indian, "Calculate Margin"))
        groupAdapter.add(ProfileItem(R.drawable.ic_call_black_24dp, "Contact Us"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_share_48px, "Share App"))
        groupAdapter.add(ProfileItem(R.drawable.icons8_logout_rounded_left_48px, "Logout"))
        groupAdapter.add(ProfileItem(R.drawable.ic_baseline_product_add_24, "Add New Product"))

        val dialog = ProgressDialog(requireContext(), "Logging out...")
        groupAdapter.setOnItemClickListener { _, itemView ->
            when (recyclerView.getChildAdapterPosition(itemView)) {
                0 -> editProfile(user)
                1 -> calcMargin()
                2 -> {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:8309572197")
                    startActivity(intent)
                }
                3 -> shareApp()
                4 -> logoutFromApp(dialog)
                5 -> {
                    findNavController().navigate(R.id.action_profileFragment_to_addNewProductFragment, Bundle().apply {
                        putString("barcode", firebaseHelper.getNewBarcode())
                    })
                }
            }
        }

        val customerRequestsButton = layoutView.findViewById<Button>(R.id.customer_requests)
        customerRequestsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_customerRequestsFragment)
        }

        val shortTask = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse("https://www.yourkirana.com?shopName=$shopName"))
                .setDomainUriPrefix("https://yourkirana.page.link")
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder("com.titos.barcodescanner").build())
                .buildShortDynamicLink()

        shortTask.addOnSuccessListener { sd ->
                val msg = "$userName invited you to join his store '$shopName' on YourKirana app.\n \n" +
                        "Steps to follow after installing: \n " +
                        "1) Login with Google or FB or Mobile\n " +
                        "2) Tap on 'Join Later' when asked for a shop name\n " +
                        "3) Select the below link for joining" +
                        "\n \n Click here to join ----> " + sd.shortLink

                layoutView.findViewById<ImageView>(R.id.btn_add_user).setOnClickListener {
                    ShareCompat.IntentBuilder.from(requireActivity())
                            .setType("text/plain")
                            .setChooserTitle("Send the store link via...")
                            .setText(msg)
                            .startChooser()
                }
        }
    }

    private fun calcMargin(){
        val viewGroup = layoutView.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_margin, viewGroup, false)

        val etDisc = dialogView.findViewById<EditText>(R.id.etDisc)
        val etMrp = dialogView.findViewById<EditText>(R.id.etMrp)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.btnCalc).setOnClickListener {
            if (etDisc.text.isNotEmpty() && etMrp.text.isNotEmpty()){
                val margin = (etDisc.text.toString().toDouble()*100/etMrp.text.toString().toDouble()).round(2)

                dialogView.findViewById<TextView>(R.id.tvTitle).text = "Margin - $margin %"
            }
            else
                showToast("Don't leave empty")
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            alertDialog.cancel()
        }

    }

    private fun editProfile(user: FirebaseUser){

        val viewGroup = layoutView.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, viewGroup, false)

        val etName = dialogView.findViewById<EditText>(R.id.et_dp_name)
        val etStore = dialogView.findViewById<EditText>(R.id.et_store_name)

        firebaseHelper.getUserDetails(user.uid).observe(this) {

            if (it.userName!="null"&&it.shopName!="null") {
                etName.setText(it.userName)
                etStore.setText(it.shopName)
            }
            else if (it.userName=="null"&&it.shopName!="null")
                etStore.setText(it.shopName)
        }

        val sharedPref = activity?.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)!!
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.product_add_button).setOnClickListener {
            val userName = etName.text
            val shopName = etStore.text
            if (userName.isNotEmpty()&&shopName.isNotEmpty()){
                firebaseHelper.updateUserName(user.uid, userName.toString(), shopName.toString())
                sharedPref.edit {
                    putString("shopName", shopName.toString())
                    putString("userName", userName.toString())
                    commit()
                }

                layoutView.findViewById<TextView>(R.id.profile_name).text = userName.toString().split(' ').joinToString(" ") { it.capitalize() }
                layoutView.findViewById<TextView>(R.id.store_name).text = shopName.toString().split(' ').joinToString(" ") { it.capitalize() }

                Toast.makeText(requireContext(), "Profile Details changed successfully", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
            else
                Toast.makeText(requireContext(), "Don't leave any of the fields empty", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.product_cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun shareApp(){
        ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain")
                .setChooserTitle("Share the apk link via...")
                .setText("Download the latest version of YourKirana app using this link. \n " +
                        "https://play.google.com/store/apps/details?id=com.titos.barcodescanner&hl=en")
                .startChooser()
    }

    private fun logoutFromApp(dialog: ProgressDialog){
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
