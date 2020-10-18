package com.example.zoosumx2

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.Window
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ConfirmRecycleDialog(context: Context) {

    var fbAuth: FirebaseAuth? = null
    var fbFirestore: FirebaseFirestore? = null

    private val dlg = Dialog(context)
    private lateinit var btnOk : Button

    fun start(context: Context) {

        fbAuth = FirebaseAuth.getInstance()
        fbFirestore = FirebaseFirestore.getInstance()

        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.setContentView(R.layout.confirm_recycle_dialog)
        dlg.setCancelable(false)

        btnOk = dlg.findViewById(R.id.confirm_ok)
        dlg.show()

        btnOk.setOnClickListener{
            dlg.dismiss()
            val intent = Intent((context as PhotoActivity),GetRewardActivity::class.java)
            intent.putExtra("reward",2)

            fbFirestore?.collection("users")?.document(fbAuth?.uid.toString())?.update("rewardPoint",
                FieldValue.increment(2))
            (context as PhotoActivity).startActivity(intent)
        }
    }


}