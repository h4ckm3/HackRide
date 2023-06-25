package com.hackme.hackride.activity

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.R
import com.hackme.hackride.database.User
import android.location.LocationManager
import androidx.core.app.NotificationCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var Fauth: FirebaseAuth
    private lateinit var HRDBS: DatabaseReference
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Fauth = FirebaseAuth.getInstance()
        btnLogin = findViewById(R.id.btn_login)
        HRDBS = FirebaseDatabase.getInstance().reference

        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)

        btnLogin.setOnClickListener {
            checkLocation()
        }
    }

    private fun signInUsers() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            // Validasi jika kolom email atau kata sandi kosong
            return
        }

        Fauth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = Fauth.currentUser
                    if (currentUser != null) {
                        val userId = currentUser.uid
                        getUserRole(userId)
                    }
                } else {
                    // Gagal login
                    // Tambahkan penanganan kesalahan sesuai kebutuhan Anda
                }
            }
    }

    private fun getUserRole(userId: String) {
        val userRef = HRDBS.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val type = dataSnapshot.child("type").getValue(String::class.java)
                val nama = dataSnapshot.child("nama").getValue(String::class.java)
                val hp = dataSnapshot.child("hp").getValue(String::class.java)
                val id_motor = dataSnapshot.child("id_motor").getValue(String::class.java)

                if (type == "Pemilik") {
                    if (nama != null && hp != null && id_motor != null) {
                        val user = User(userId, type, nama, hp, id_motor)
                        saveUserDataToDevice(user)
                    }
                    val intent = Intent(this@LoginActivity, PemilikActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (type == "Aparat") {
                    val intent = Intent(this@LoginActivity, AparatActivity::class.java)
                    startActivity(intent)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Gagal mendapatkan data user dari Realtime Database
                // Tambahkan penanganan kesalahan sesuai kebutuhan Anda
            }
        })
    }

    private fun saveUserDataToDevice(user: User) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("userId", user.userId)
        editor.putString("type", user.type)
        editor.putString("nama", user.nama)
        editor.putString("hp", user.hp)
        editor.putString("id_motor", user.id_motor)
        editor.apply()

    }

    private fun checkLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Activate Location Sensor")
            alertDialog.setMessage("Please activate the location sensor to proceed")
            alertDialog.setPositiveButton("OK") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            alertDialog.setCancelable(false)
            alertDialog.show()
        } else {
            signInUsers()
        }
    }
}

