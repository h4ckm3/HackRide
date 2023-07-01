package com.hackme.hackride.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.hackme.hackride.R
import com.hackme.hackride.fungsi.AparatService
import com.hackme.hackride.fungsi.MarkerMotor
import com.hackme.hackride.fungsi.MyForegroundService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LacakActivity : AppCompatActivity() {
    //data kasus
    private var Id_User : String = ""
    private var Id_Motor : String =""
    private var StatusUser: String=""

    //databases
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    //masp
    lateinit var maps : MapView
    private var marker: Marker? = null

    //legenda
    private lateinit var tvIdMotor: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lacak)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("OpenStreetMap", MODE_PRIVATE)
        )

        //inisialisasi maps
        maps = findViewById(R.id.mapView)
        maps.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        //inisialisasi lengenda
        tvIdMotor = findViewById(R.id.tv_MarkerMotor)
    }


    // memulai menu lacak
    private fun mulaiMenuLacak(){
        AmbilDataKasus()

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