package com.hackme.hackride

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.activity.AparatActivity
import com.hackme.hackride.activity.LoginActivity
import com.hackme.hackride.activity.PemilikActivity
import com.hackme.hackride.database.AparatData
import com.hackme.hackride.database.User

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dataBase :DatabaseReference
    private lateinit var btnMulai: Button
    private var progressBar: ProgressBar? = null
    private var handler: Handler? = null
    private var progressStatus = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landingpage)

        progressBar = findViewById(R.id.progressBar)
        btnMulai = findViewById(R.id.btn_masuk)
        dataBase = FirebaseDatabase.getInstance().reference

        btnMulai.setOnClickListener {
            progressStatus = 0
            progressBar?.visibility = View.VISIBLE
            btnMulai.isEnabled = false

            handler = Handler()
            handler?.postDelayed(runnable, 10)
        }
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            progressStatus++
            progressBar?.progress = progressStatus

            if (progressStatus < progressBar?.max!!) {
                handler?.postDelayed(this, 10) // Interval 1 detik (1000ms)

            } else {
                progressBar?.visibility = View.INVISIBLE
                btnMulai.isEnabled = true
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth = FirebaseAuth.getInstance()

        // Periksa apakah pengguna telah login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            btnMulai.visibility = View.GONE
            progressBar?.visibility = View.VISIBLE
            // Pengguna telah login
            val userId =currentUser.uid

            getUserTypeFromLocal(userId) { userType ->
                if (userType != null) {
                    if (userType == "Pemilik"){
                        val inten  = Intent(this, PemilikActivity ::class.java )
                        startActivity(inten)
                        finishAffinity()
                        Log.d("User Type", userType)
                    }else{
                        val inten  = Intent(this, AparatActivity ::class.java )
                        startActivity(inten)
                        finishAffinity()
                        Log.d("User Type", userType)
                    }
                } else {
                    // Penanganan kesalahan ketika gagal mendapatkan nilai userType
                }
            }

        }
    }
    private fun getUserTypeFromLocal(userId: String, callback: (String?) -> Unit) {
        val userRef = dataBase.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val type = dataSnapshot.child("type").getValue(String::class.java)

                callback(type)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Gagal mendapatkan data user dari Realtime Database
                // Tambahkan penanganan kesalahan sesuai kebutuhan Anda
                callback(null)
            }
        })
    }
}
