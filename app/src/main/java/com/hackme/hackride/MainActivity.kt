package com.hackme.hackride

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.hackme.hackride.activity.AparatActivity
import com.hackme.hackride.activity.LoginActivity
import com.hackme.hackride.activity.PemilikActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var btnMulai: Button
    private var progressBar: ProgressBar? = null
    private var handler: Handler? = null
    private var progressStatus = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landingpage)

        progressBar = findViewById(R.id.progressBar)
        btnMulai = findViewById(R.id.btn_masuk)

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
            // Pengguna telah login
            currentUser.uid

            // Ambil peran pengguna dari penyimpanan lokal atau database
            val userType = getUserTypeFromLocal() // Ubah ini dengan metode yang sesuai

            // Arahkan ke menu yang sesuai berdasarkan peran pengguna
            if (userType == "Pemilik") {
                val intent = Intent(this, PemilikActivity::class.java)
                startActivity(intent)
                finish()
            } else if (userType == "Aparat") {
                val intent = Intent(this, AparatActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
    private fun getUserTypeFromLocal(): String {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPreferences.getString("type", "") ?: ""
    }
}
