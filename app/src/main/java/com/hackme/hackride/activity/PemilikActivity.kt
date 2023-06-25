package com.hackme.hackride.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.R
import com.hackme.hackride.fungsi.MyForegroundService
import com.hackme.hackride.fungsi.calculateEuclideanDistance
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.Timer
import java.util.TimerTask

class PemilikActivity : AppCompatActivity(), LocationListener {
    private lateinit var btnLogout: CardView
    private lateinit var btnCVHome: CardView
    private lateinit var btnCVAbout: CardView
    private lateinit var btnCVHelp: CardView
    private lateinit var ketCvhome: CardView
    private lateinit var ketCvabout: CardView
    private lateinit var ketCvhelp: CardView
    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var auth: FirebaseAuth
    private var marker: Marker? = null
    private var motorMarker: Marker? = null
    private var polyline: Polyline? = null
    private lateinit var database: DatabaseReference
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var motorLatitude: Double = 0.0
    private var motorLongitude: Double = 0.0
    private var ID_Motor:String =""
    private var nama_pemilik:String =""
    private var jarakuser: String =""
    private var Id_user:String=""
    private var status:String=""
    private var noHp: String=""
    private var timerMarker: Timer? = null
    private var Mesin : String =""
    private var Gtaran : Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pemilik)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("OpenStreetMap", MODE_PRIVATE)
        )
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        // Inisialisasi komponen lainnya
        btnLogout = findViewById(R.id.cv_btnlogout)
        btnCVHome = findViewById(R.id.cv_btnhome)
        btnCVAbout = findViewById(R.id.cv_btnabout)
        btnCVHelp = findViewById(R.id.cv_btnhelp)
        ketCvhome = findViewById(R.id.cv_kethome)
        ketCvabout = findViewById(R.id.cv_ketabout)
        ketCvhelp = findViewById(R.id.cv_kethelp)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference


        // ambi data dari login user
        datadarilogin()
        // memulai maps

        btnLogout.setOnClickListener {
            logoutUser()
            motorMarker?.onDestroy()
            marker?.onDestroy()
            mapView.onPause()
            cancelClock()
            // Kirim broadcast untuk ACTION_STOP_LOCATION_COMPARISON
            val intent = Intent(MyForegroundService.ACTION_STOP_LOCATION_COMPARISON)
            sendBroadcast(intent)
            val foregroundServiceInstance = MyForegroundService.getInstance()
            foregroundServiceInstance?.setLoggedOut()
            val serviceIntent = Intent(this, MyForegroundService::class.java)
            stopService(serviceIntent)
            Intent(this, PemilikActivity::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(Intent(this, LoginActivity::class.java))
        }
        btnHome()
        lokasiuser()
        motor()
        startClock()
        setupMapView()
        if (ID_Motor != "" && status !=""){
            startBackgroundService(ID_Motor,status)
        }
        if (savedInstanceState != null) {
            motorLatitude = savedInstanceState.getDouble("motorLatitude");
            motorLongitude = savedInstanceState.getDouble("motorLongitude");
            userLatitude = savedInstanceState.getDouble("userLatitude")
            userLongitude = savedInstanceState.getDouble("userLongitude")
        }
    }


    override fun onProviderDisabled(provider: String) {
       checkLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // All location permissions are granted
            setupMapWithLocation()
        }
    }

    private fun setupMapWithLocation() {
        // Get the user's current location
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocationPermission && hasCoarseLocationPermission) {
            // Request location updates from NETWORK_PROVIDER for faster location retrieval
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,
                1f,
                this
            )

            // Request location updates from GPS_PROVIDER for more accurate location
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                1f,
                this
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        val userLocation = GeoPoint(location.latitude, location.longitude)

        userLatitude = location.latitude
        userLongitude = location.longitude
        markerUser(userLatitude,userLongitude)
        addMotorMarker(motorLatitude,motorLongitude)
        drawPolyline(motorLatitude,motorLongitude,userLatitude,userLongitude)
        // Set the map focus to the user's current location
    }


    override fun onResume() {
        super.onResume()
        // Resume the map view when the activity is resumed
        mapView.onResume()
        motorMarker?.onResume()
        startClock()
    }

    override fun onPause() {
        super.onPause()
        // Pause the map view when the activity is paused
        mapView.onPause()
        motorMarker?.onPause()
        marker?.onPause()
        cancelClock()
    }
    override fun onStop() {
        super.onStop()
        locationManager.removeUpdates(this)
        cancelClock()
    }

// atau

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        mapView.onPause()
        motorMarker?.onDestroy()
        marker?.onDestroy()
        cancelClock()
    }


    private fun logoutUser() {
        mapView.overlays.clear()
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
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
            setupMapWithLocation()
        }
    }

    private fun getDataMotor(id_motor: String) {
        val motorRef = database.child("motor").child(id_motor)
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
                        Gtaran = getaran
                        if (mesin != 0){
                            Mesin = "Acktive"
                        }else{
                            Mesin = "Nonactive"
                        }
                        motorLatitude = latitude
                        motorLongitude = longitude
                        markerUser(userLatitude,userLongitude)
                        addMotorMarker(motorLatitude,motorLongitude)
                        drawPolyline(motorLatitude,motorLongitude,userLatitude,userLongitude)
                        val initialLocation = GeoPoint(motorLatitude, motorLongitude)
                        mapView.controller.setCenter(initialLocation)

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

    private fun motor(){
        // Ambil id_motor dari penyimpanan perangkat
        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val idMotor = sharedPreferences.getString("id_motor", "")

        if (idMotor.isNullOrEmpty()) {
            // id_motor tidak tersedia di penyimpanan perangkat
            Log.e("PemilikActivity", "ID Motor tidak tersedia")
        } else {
            // Panggil fungsi untuk mengambil data motor dari Firebase
            getDataMotor(idMotor)
        }
    }

    private fun lokasiuser(){
        // Request location permissions
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            fineLocationPermission
        }
        val permissions = arrayOf(
            fineLocationPermission,
            coarseLocationPermission,
            backgroundLocationPermission
        )
        val grantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (grantedPermissions.size == permissions.size) {
            // All location permissions are granted
            checkLocation()
        } else {
            // Request location permissions from the user
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun addMotorMarker(latitude: Double, longitude: Double) {
        val motorLocation = GeoPoint(latitude, longitude)
        // Hapus marker sebelumnya jika ada
        motorMarker?.let {
            mapView.overlays.remove(it)
        }
        mapView.addOnFirstLayoutListener { v, left, top, right, bottom ->
        motorMarker = Marker(mapView)}
        motorMarker?.position = motorLocation
        motorMarker?.icon = ContextCompat.getDrawable(this, R.drawable.ic_markermotor) // Ganti dengan ikon marker yang sesuai
        motorMarker?.title = "ID : $ID_Motor\nEngine : $Mesin\nVibration : $Gtaran\nDistance from you : $jarakuser"

        motorMarker?.let {
            mapView.overlays.add(it)
        }

        mapView.invalidate()
    }


    private fun markerUser(latitude: Double, longitude: Double){
        val userLocation = GeoPoint(latitude,longitude)
        // Remove existing marker from overlays if it exists
        marker?.let {
            mapView.overlays.remove(it)
        }

        // Create a new marker
        mapView.addOnFirstLayoutListener { v, left, top, right, bottom ->
        marker = Marker(mapView)}
        marker?.position = userLocation
        marker?.icon = ContextCompat.getDrawable(this, R.drawable.ic_markerpemilik)
        marker?.title = "Status: $status\nName: $nama_pemilik\nHp : $noHp\n Distance from Bike : $jarakuser "

        // Add the marker overlay to the map
        marker?.let {
            mapView.overlays.add(it)
        }

        // Invalidate the map view to refresh the display
        mapView.invalidate()
    }


    private fun setupMapView() {
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)

        enableRotation()
        enableCompass()
        enableZoomControls()
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)
        val initialLocation = GeoPoint(0.0, 0.0)
        mapView.controller.setCenter(initialLocation)
        mapView.controller.setZoom(19.0)
    }

    private fun enableRotation() {
        mapView.setMapOrientation(0f) // Mengatur sudut rotasi ke 0 derajat untuk memulai
    }

    private fun enableCompass() {
        val compassOverlay = CompassOverlay(this, mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)
    }

    private fun enableZoomControls() {
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
    }

    //mengambil data user dari login aktivity
    private fun datadarilogin(){
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "")
        val type = sharedPreferences.getString("type", "")
        val nama = sharedPreferences.getString("nama", "")
        val hp = sharedPreferences.getString("hp", "")
        val id_motor = sharedPreferences.getString("id_motor","")
        if (id_motor != null && nama != null && userId != null && type != null && hp != null ){
            ID_Motor = id_motor
            nama_pemilik= nama
            status = type
            noHp = hp
            Id_user = userId
        }
    }

    //timer pengambilan data
    private fun startClock() {
        val mapView = findViewById<MapView>(R.id.mapView) // Inisialisasi mapView
        timerMarker = Timer()
        timerMarker?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Kode untuk pembaruan data setiap detik
                runOnUiThread {
                    // Panggil fungsi updateData() atau kode pembaruan data lainnya di sini
                    val jarak = calculateEuclideanDistance(userLatitude, userLongitude, motorLatitude, motorLongitude)
                    val bulatjarak = Math.round(jarak)
                    jarakuser = bulatjarak.toString()
                    markerUser(userLatitude,userLongitude)
                    addMotorMarker(motorLatitude,motorLongitude)
                }
            }
        }, 0, 1000) // Menjalankan tugas setiap 1000 milidetik (1 detik)
    }

    private fun cancelClock() {
        timerMarker?.cancel()
    }

    private fun drawPolyline(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double) {
        val startGeoPoint = GeoPoint(startLatitude, startLongitude)
        val endGeoPoint = GeoPoint(endLatitude, endLongitude)

        // Hapus polyline sebelumnya jika ada
        if (polyline != null) {
            mapView.overlays.remove(polyline)
            polyline = null
        }

        polyline = Polyline().apply {
            addPoint(startGeoPoint)
            addPoint(endGeoPoint)
            color = Color.RED
            width = 8f
        }

        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    //tampilan home,abaout,help
    private fun btnHome(){
        btnLogout = findViewById(R.id.cv_btnlogout)
        btnCVHome = findViewById(R.id.cv_btnhome)
        btnCVAbout = findViewById(R.id.cv_btnabout)
        btnCVHelp = findViewById(R.id.cv_btnhelp)
        ketCvhome = findViewById(R.id.cv_kethome)
        ketCvabout = findViewById(R.id.cv_ketabout)
        ketCvhelp = findViewById(R.id.cv_kethelp)
        val warnaKlik = "#00BCD4" // Contoh warna merah
        val klik = Color.parseColor(warnaKlik)
        val warnaTidakKlik = "#FFFFFFFF"
        val tidakklik = Color.parseColor(warnaTidakKlik)
        btnLogout.setCardBackgroundColor(tidakklik)
        btnCVHome.setCardBackgroundColor(klik)
        btnCVHelp.setCardBackgroundColor(tidakklik)
        btnCVAbout.setCardBackgroundColor(tidakklik)

        ketCvhome.visibility = View.VISIBLE
        ketCvabout.visibility = View.GONE
        ketCvhelp.visibility = View.GONE


    }


    //baground taks
    private fun startBackgroundService(idMotor: String, status: String) {
        val intent = Intent(this, MyForegroundService::class.java)
        intent.putExtra("id_motor", idMotor) // Contoh pengiriman data "id_motor"
        intent.putExtra("status", status) // Contoh pengiriman data "status"
        ContextCompat.startForegroundService(this, intent)

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("motorLatitude", motorLatitude)
        outState.putDouble("motorLongitude", motorLongitude)
        outState.putDouble("userLatitude", userLatitude)
        outState.putDouble("userLongitude", userLongitude)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        motorLatitude = savedInstanceState.getDouble("motorLatitude")
        motorLongitude = savedInstanceState.getDouble("motorLongitude")
        userLatitude = savedInstanceState.getDouble("userLatitude")
        userLongitude = savedInstanceState.getDouble("userLongitude")
    }

}


