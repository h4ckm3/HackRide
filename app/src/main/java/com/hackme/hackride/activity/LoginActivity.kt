package com.hackme.hackride.activity

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
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.firebase.FirebaseError.ERROR_INVALID_EMAIL
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.hackme.hackride.database.AparatData

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
            closeKeyboard()
            checkLocation()
        }
    }

    private fun signInUsers() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.requestFocus()
            etEmail.error = "Email is required"
            return
        }
        if (password.isEmpty()){
            etPassword.requestFocus()
            etPassword.error = "Password is required"
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
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidCredentialsException){
                        val errorCode = (exception as FirebaseAuthInvalidCredentialsException).errorCode
                        if (errorCode == "ERROR_INVALID_EMAIL") {
                            etEmail.error = "Invalid email address"
                            etEmail.error = "Email is Wrong"
                            etEmail.requestFocus()
                        } else {
                            etPassword.error = "Invalid password"
                            etPassword.error = "Email is Wrong"
                            etPassword.requestFocus()
                        }
                    }
                    else {
                        Toast.makeText(this, "Wrong Email and Password", Toast.LENGTH_SHORT).show()
                        etEmail.error = "Email is Wrong"
                        etPassword.error = "Email is Wrong"
                    }
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
                    if (nama != null && hp != null) {
                        val Aparat = AparatData(userId, type, nama, hp)
                        saveAparatDataToDevice(Aparat)
                    }
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

    //simpan data aparat
    private fun saveAparatDataToDevice(aparatData: AparatData) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("AparatData", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("userId", aparatData.userId)
        editor.putString("type", aparatData.type)
        editor.putString("nama", aparatData.nama)
        editor.putString("hp", aparatData.hp)
        editor.apply()

    }

    private fun checkLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
        } else {
            signInUsers()
        }
    }

    override fun onPause() {
        super.onPause()
        Intent(this, LoginActivity::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity() // Menutup semua aktivitas yang terkait dengan aktivitas saat ini
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }


}

