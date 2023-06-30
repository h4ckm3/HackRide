package com.hackme.hackride.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.hackme.hackride.R
import com.hackme.hackride.fungsi.MarkerMotor

class LacakActivity : AppCompatActivity() {
    //data kasus
    private var Id_User : String = ""
    private var Id_Motor : String =""
    private var StatusUser: String=""

    //legenda
    private lateinit var tvIdMotor: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lacak)


        //inisialisasi lengenda
        tvIdMotor = findViewById(R.id.tv_MarkerMotor)
        AmbilDataKasus()
        tvIdMotor.text = " $Id_Motor  $Id_User  $StatusUser"
    }

    //data kasus
    private fun AmbilDataKasus(){
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("DataKasus", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id_user", "")
        val type = sharedPreferences.getString("status", "")
        val motorID = sharedPreferences.getString("id_motor", "")
        if (motorID != null && userId != null && type != null){
           Id_User = userId
            Id_Motor = motorID
            StatusUser = type
        }
    }
}