package com.hackme.hackride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.activity.AparatActivity
import com.hackme.hackride.activity.LoginActivity
import com.hackme.hackride.activity.PemilikActivity
import com.hackme.hackride.fungsi.NetworkLoggingUtil

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dataBase: DatabaseReference
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
            lokasiUser()
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
            val userId = currentUser.uid

            getUserTypeFromLocal(userId) { userType ->
                if (userType != null) {
                    if (userType == "Pemilik") {
                        val intent = Intent(this, PemilikActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    } else {
                        val intent = Intent(this, AparatActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
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

    // Override method onRequestPermissionsResult untuk menangani hasil permintaan izin
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // All location permissions are granted
            NetworkLoggingUtil.enableNetworkLogging(this)
        }
    }

    private fun lokasiUser() {
        // Request location permissions
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        val postNotifikasi = Manifest.permission.POST_NOTIFICATIONS
        val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            fineLocationPermission
        }
        val permissions = arrayOf(
            fineLocationPermission,
            coarseLocationPermission,
            postNotifikasi,
            backgroundLocationPermission
        )
        val grantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (grantedPermissions.size == permissions.size) {
            // All location permissions are granted
            progressStatus = 0
            progressBar?.visibility = View.VISIBLE
            btnMulai.isEnabled = false

            handler = Handler()
            handler?.postDelayed(runnable, 10)

            NetworkLoggingUtil.enableNetworkLogging(this)
        } else {
            // Request location permissions from the user
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }
}
