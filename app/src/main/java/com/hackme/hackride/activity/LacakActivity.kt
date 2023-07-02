package com.hackme.hackride.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.core.view.isNotEmpty
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.hackme.hackride.R
import com.hackme.hackride.fungsi.AparatService
import com.hackme.hackride.fungsi.MarkerMotor
import com.hackme.hackride.fungsi.MarkerUser
import com.hackme.hackride.fungsi.MyForegroundService
import com.hackme.hackride.fungsi.calculateEuclideanDistance
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.sql.Time
import java.util.Timer
import java.util.TimerTask

class LacakActivity : AppCompatActivity() {
    //data kasus
    private var Id_User : String = ""
    private var Id_Motor : String =""
    private var Id_Pemilik :String =""
    private var StatusUser: String=""
    private var latMotor: Double =0.0
    private var longMotor : Double = 0.0
    private var MesinMotor : String =""
    private var GetaranMotor : Boolean = false
    private var latParkir : Double =0.0
    private var longParkir : Double = 0.0
    private var jarakUser : Long = 0
    private var latUser : Double = 0.0
    private var longUser : Double = 0.0
    private var noHp :String = ""
    private var ikutUser : Boolean = false

    //databases
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    //masp
    private lateinit var maps : MapView
    private var motorMarker : Marker? = null
    private var marker: Marker? = null
    private var markerAparat:Marker? = null

    //legenda
    private lateinit var tvIdMotor: TextView

    //timer data semua
    private var timerData : Timer? = null

    //marker motor

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lacak)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("OpenStreetMap", MODE_PRIVATE)
        )

        //inisialisasi maps
        maps = findViewById(R.id.mapViewLacak)
        maps.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        //inisialisasi database
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference

        //inisialisasi lengenda
        tvIdMotor = findViewById(R.id.tv_MarkerMotor)
        mulaiMenuLacak()
    }


    // memulai menu lacak
    private fun mulaiMenuLacak(){
        AmbilDataKasus()
        AmbildataMotor()
        setupMapView()
        startClockData()
        motorMarker = Marker(maps)
        markerAparat = Marker(maps)
        marker = Marker(maps)

    }

    // fungsi utama halaman
    override fun onResume() {
        super.onResume()
        maps.onResume()
        Marker(maps).onResume()
    }
    override fun onPause() {
        super.onPause()
        maps.onPause()
        cancelClockData()
        Marker(maps).onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        maps.onPause()
        Marker(maps).onDestroy()
        cancelClockData()
    }

    override fun onStop() {
        super.onStop()
        maps.onPause()
        cancelClockData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (StatusUser == "Pemilik"){
            val inten = Intent(this, PemilikActivity::class.java)
            startActivity(inten)
        }
        if (StatusUser == "Aparat"){
            val inten = Intent(this, AparatActivity::class.java)
            startActivity(inten)
        }
        maps.onPause()
        cancelClockData()
        Marker(maps).onDestroy()
        finishAffinity()
    }

    //data kasus
    private fun AmbilDataKasus(){
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("DataKasus", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id_user", "")
        val type = sharedPreferences.getString("status", "")
        val motorID = sharedPreferences.getString("id_motor", "")
        val idPemilik = sharedPreferences.getString("id_pemilik", "")
        if (motorID != null && userId != null && type != null && idPemilik != null){
           Id_User = userId
            Id_Motor = motorID
            StatusUser = type
            Id_Pemilik = idPemilik
        }
    }
    //data motor
    private fun AmbildataMotor(){
        val motorRef = databaseReference.child("motor").child(Id_Motor)
        motorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                    val mesin = dataSnapshot.child("mesin").getValue(Int::class.java)
                    val getaran = dataSnapshot.child("getaran").getValue(Boolean::class.java)
                    val latitudeParkir = dataSnapshot.child("latitudeparkir").getValue(Double::class.java)
                    val longitudeParkir = dataSnapshot.child("longitudeparkir").getValue(Double::class.java)

                    if (latitude != null && longitude != null && latitudeParkir != null && longitudeParkir != null && mesin != null && getaran != null) {
                        GetaranMotor = getaran
                        if (mesin != 0){
                            MesinMotor = "Acktive"
                        }else{
                            MesinMotor = "Nonactive"
                        }
                        latMotor = latitude
                        longMotor = longitude
                        latParkir = latitudeParkir
                        longParkir = longitudeParkir
                        Log.d("data motor","$latMotor")
                        addMotorMarker(latitude,longitude)
                    }
                    // Lakukan sesuatu dengan data yang telah diambil
                    // Contoh: tampilkan data di logcat
                    Log.d("Data Motor", "Latitude: $latitude, Longitude: $longitude, Mesin: $mesin, Getaran: $getaran, Latitude Parkir: $latitudeParkir, Longitude Parkir: $longitudeParkir")
                } else {
                    // Data motor dengan id_motor tersebut tidak ditemukan
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Terjadi kesalahan saat mengambil data dari Firebase
                Log.e("Data Motor", "Error: ${databaseError.message}")
            }
        })
    }

    //maps mulai
    private fun setupMapView() {
        maps.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        maps.setMultiTouchControls(true)

        enableCompass()
        enableZoomControls()
        val rotationGestureOverlay = RotationGestureOverlay(maps)
        rotationGestureOverlay.isEnabled = false // Nonaktifkan fitur rotasi
        maps.overlays.add(rotationGestureOverlay)

        val initialLocation = GeoPoint(latMotor, longMotor)
        maps.controller.setCenter(initialLocation)
        maps.controller.setZoom(15.0)

        if (latMotor != 0.0 && longMotor != 0.0) {
            if (!maps.boundingBox.contains(latMotor, longMotor)) {
                Handler().postDelayed({ setupMapView() }, 1000)
            }else{
                rotationGestureOverlay.isEnabled = true
            }
        } else {
            Handler().postDelayed({ AmbildataMotor() }, 1000)
            Handler().postDelayed({ setupMapView() }, 1000)
        }
    }
    private fun enableRotation() {
        maps.setMapOrientation(0f) // Mengatur sudut rotasi ke 0 derajat untuk memulai
    }

    private fun enableCompass() {
        val compassOverlay = CompassOverlay(this, maps)
        compassOverlay.enableCompass()
        maps.overlays.add(compassOverlay)
    }

    private fun enableZoomControls() {
        maps.setBuiltInZoomControls(true)
        maps.setMultiTouchControls(true)
    }

    //marker
    private fun addMotorMarker(latitude: Double, longitude: Double) {
        val motorLocation = GeoPoint(latitude, longitude)
        val jarakuser = Math.round(calculateEuclideanDistance(latUser,longUser,latMotor,longMotor))
        // Hapus marker sebelumnya jika ada
        motorMarker?.let {
            maps.overlays.remove(it)
        }
        maps.addOnFirstLayoutListener { v, left, top, right, bottom ->
            Log.d("masuk buat maps","masuk")
            }
        val customMarker = MarkerMotor(this)
        motorMarker?.position = motorLocation
        motorMarker?.icon = customMarker.createMarker(Id_Motor)
       motorMarker?.title = "ID : $Id_Motor\nEngine : $MesinMotor\nVibration : $GetaranMotor\nDistance from you : $jarakuser"

        motorMarker?.let {
            maps.overlays.add(it)
        }

        maps.invalidate()
    }

    // fungsi data user
    fun countChildren(childPath: String, callback: (List<String>) -> Unit) {
        databaseReference.child(childPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount.toInt()
                val childNames = mutableListOf<String>()

                dataSnapshot.children.forEach { childSnapshot ->
                    val childName = childSnapshot.key
                    childName?.let { childNames.add(it) }
                }

                callback(childNames)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Error handling, if needed
            }
        })
    }
    private fun dataUser(UserId : String){
        val userRef = databaseReference.child("users").child(UserId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                    val hp = dataSnapshot.child("hp").getValue(String::class.java)
                    val type = dataSnapshot.child("type").getValue(String::class.java)
                    val nama = dataSnapshot.child("nama").getValue(String::class.java)
                    val pesan = dataSnapshot.child("pesan").getValue(String::class.java)
                    val ikut = dataSnapshot.child("ikut").getValue(Boolean::class.java)

                    if (latitude != null && longitude != null && hp != null && type != null && nama != null && ikut != null ) {
                        noHp = hp
                        ikutUser = ikut
                        if (UserId == Id_User){
                            jarakUser = Math.round(calculateEuclideanDistance(latitude,latitude,latMotor,longMotor))
                        }
                        if (type == "Aparat"){
                            markerAparat(latitude,longitude,nama,hp,type)
                        }
                        if (UserId == Id_Pemilik){
                            markerPemilik(latitude,longitude,nama,hp,type)
                        }
                        if (pesan != null){

                        }else{

                        }

                    }
                    // Lakukan sesuatu dengan data yang telah diambil
                    // Contoh: tampilkan data di logcat
                    Log.d("Data user", "Latitude: $latitude, Longitude: $longitude")
                } else {
                    // Data motor dengan id_motor tersebut tidak ditemukan
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Terjadi kesalahan saat mengambil data dari Firebase
                Log.e("Data Motor", "Error: ${databaseError.message}")
            }
        })
    }


    //fungsi marker
    private fun markerAparat(latitude: Double, longitude: Double, namaAparat: String, hpAparat: String, statusAparat: String) {
            val userLocation = GeoPoint(latitude, longitude)
            val jarakAparat = Math.round(calculateEuclideanDistance(latMotor, longMotor, latitude, longitude))

            // Remove existing marker from overlays if it exists
            marker?.let {
                maps.overlays.remove(it)
            }

            // Create a new marker

            val customMarker = MarkerUser(this)
            marker?.position = userLocation
            marker?.icon = customMarker.createMarker(namaAparat, R.drawable.ic_markeraparat4)
            marker?.title = "Status: $statusAparat\nName: $namaAparat\nHp: $hpAparat\nDistance: $jarakAparat m"

            // Add the marker overlay to the map
            marker?.let {
                maps.overlays.add(it)
            }

            // Invalidate the map view to refresh the display
            maps.invalidate()
    }

    private fun markerPemilik(latitude: Double, longitude: Double, namaPemilik :String, hpPemilik : String, statusPemilik :String){
        val userLocation = GeoPoint(latitude,longitude)
        // Remove existing marker from overlays if it exists
        val jarakPemilik = Math.round(calculateEuclideanDistance(latMotor,longMotor,latitude,longitude))
        markerAparat?.let {
            maps.overlays.remove(it)
        }
        // Create a new marker
        val customMarker = MarkerUser(this)
        markerAparat?.position = userLocation
        markerAparat?.icon = customMarker.createMarker(namaPemilik,R.drawable.ic_markerpemilik)
        markerAparat?.title = "Status: $statusPemilik\nName: $namaPemilik\nHp : $hpPemilik\n Distance : $jarakPemilik m "

        // Add the marker overlay to the map
        markerAparat?.let {
            maps.overlays.add(it)
        }

        // Invalidate the map view to refresh the display
        maps.invalidate()
    }

    //fungsi timer
    private fun startClockData() {
        val mapView = findViewById<MapView>(R.id.mapView) // Inisialisasi mapView
        timerData = Timer()
        timerData?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Kode untuk pembaruan data setiap detik
                runOnUiThread {
                    Log.d("jalan waktu "," sekarang sedang ambil data")
                    // Panggil fungsi updateData() atau kode pembaruan data lainnya di sini
                    AmbildataMotor()
                    val childPath = "users"
                    countChildren(childPath) { childNames ->
                        childNames.forEach { childName ->
                            dataUser(childName)
                        }
                    }
                }
            }
        }, 0, 1000) // Menjalankan tugas setiap 1000 milidetik (1 detik)
    }
    private fun cancelClockData() {
        timerData?.cancel()
        timerData = null
    }

}