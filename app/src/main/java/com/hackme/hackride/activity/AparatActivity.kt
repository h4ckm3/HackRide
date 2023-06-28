package com.hackme.hackride.activity

import android.Manifest
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
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.hackme.hackride.R
import com.hackme.hackride.R.layout.activity_aparat
import com.hackme.hackride.fungsi.MarkerUser
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

class AparatActivity : AppCompatActivity(), LocationListener {
    //lokasi aparat
    private var latAparat: Double =0.0
    private var longAparat : Double = 0.0
    // data user
    private var namaAparat : String =""
    private var statusAparat: String =""
    private var hpAparat : String =""
    private  var idAparat : String=""
    //tombol menu bar
    private lateinit var btnLout : CardView
    private lateinit var btnHome : CardView
    private lateinit var btnAbot : CardView
    private lateinit var btnHelp : CardView
    //keterangan menubar
    private lateinit var ketHome : CardView
    private lateinit var ketAbout : CardView
    private lateinit var ketHelp : CardView

    //konten
    private lateinit var kontenHome : LinearLayout
    //tombol konten home
    private lateinit var btnFokusAparat: CardView
    private lateinit var kontenAbout : LinearLayout
    private lateinit var kontenHelp : LinearLayout

    //notifikasi
    private lateinit var notifSensorLokasi : CardView
    private lateinit var notifKasus : CardView

    //database
    private lateinit var auth: FirebaseAuth
    private lateinit var database : DatabaseReference

    //masp
    lateinit var maps : MapView
    private var marker: Marker? = null
    //lokasi
    private lateinit var locationManager: LocationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_aparat)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("OpenStreetMap", MODE_PRIVATE)
        )
        maps = findViewById(R.id.mapView)
        maps.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        //inisialisasi tombol menu bar
        btnLout = findViewById(R.id.cv_btnlogout)
        btnHome = findViewById(R.id.cv_btnhome)
        btnAbot = findViewById(R.id.cv_btnabout)
        btnHelp = findViewById(R.id.cv_btnhelp)
        //inisialisasi keterangan tombol menu bar
        ketHome = findViewById(R.id.cv_kethome)
        ketAbout = findViewById(R.id.cv_ketabout)
        ketHelp = findViewById(R.id.cv_kethelp)
        //insialisasi konten
        kontenHome = findViewById(R.id.linlay_home)
        kontenAbout = findViewById(R.id.linlay_about)
        kontenHelp = findViewById(R.id.linlay_help)
        //inisialisasi tombol di konten home
        btnFokusAparat = findViewById(R.id.cv_btnfokusaparat)
        //nisialisasi notifikasi
        notifKasus = findViewById(R.id.cv_notifikasikasus)
        notifSensorLokasi = findViewById(R.id.cv_notifikasisensorlokasi)
        //inisialisasi database
        auth = FirebaseAuth.getInstance()


        //inisialisasi screen mulai
        tombolHome()
        setinganMulai()

        //penggunanan tombol menu bar
        btnLout.setOnClickListener {
            logoutAparat()
            tombolLogout()
        }
        btnHome.setOnClickListener {
            tombolHome()
        }
        btnAbot.setOnClickListener {
            tombolAbout()
        }
        btnHelp.setOnClickListener {
            tombolHelp()
        }

    }

    //fungsi memulai halaman
    private fun setinganMulai(){
        notifSensorLokasi.visibility = hilang
        notifKasus.visibility = hilang
        datadarilogin()
        lokasiuser()
        setupMapView()
    }

    //fungsi
    private fun logoutAparat(){
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        Intent(this, PemilikActivity::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        finishAffinity()
        startActivity(intent)
    }
    //lokasi user

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
    override fun onProviderDisabled(provider: String) {
        checkLocation()
    }
    override fun onProviderEnabled(provider: String) {
        // Implementasi logika saat provider diaktifkan
        setupMapView()
        setupMapWithLocation()
        notifSensorLokasi.visibility = View.GONE
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

        latAparat = location.latitude
        longAparat = location.longitude
        // Set the map focus to the user's current location
    }


    override fun onResume() {
        super.onResume()
        setupMapWithLocation()
        // Resume the map view when the activity is resumed
        maps.onResume()
    }

    override fun onPause() {
        super.onPause()

    }
    override fun onStop() {
        super.onStop()
    }

// atau

    override fun onDestroy() {
        super.onDestroy()
//        locationManager.removeUpdates(this)
        maps.onPause()
        marker?.onDestroy()
        Intent(this, PemilikActivity::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    private fun checkLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            notifSensorLokasi.visibility = View.VISIBLE
        } else {
            setupMapWithLocation()
            notifSensorLokasi.visibility = View.GONE
        }
    }
    private fun setupMapView() {
        maps.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        maps.setMultiTouchControls(true)

        enableCompass()
        enableZoomControls()
        val rotationGestureOverlay = RotationGestureOverlay(maps)
        rotationGestureOverlay.isEnabled = false // Nonaktifkan fitur rotasi
        maps.overlays.add(rotationGestureOverlay)

        val initialLocation = GeoPoint(latAparat, longAparat)
        maps.controller.setCenter(initialLocation)
        maps.controller.setZoom(19.0)

        if (latAparat != 0.0 && longAparat != 0.0) {
            if (!maps.boundingBox.contains(latAparat, longAparat)) {
                Handler().postDelayed({ setupMapView() }, 1000)
            }else{
                rotationGestureOverlay.isEnabled = true
            }
        } else {
            Handler().postDelayed({ lokasiuser() }, 1000)
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
    private fun markerUser(latitude: Double, longitude: Double){
        val userLocation = GeoPoint(latitude,longitude)
        // Remove existing marker from overlays if it exists
        marker?.let {
            maps.overlays.remove(it)
        }

        // Create a new marker
        maps.addOnFirstLayoutListener { v, left, top, right, bottom ->
            marker = Marker(maps)}
        val customMarker = MarkerUser(this)
        marker?.position = userLocation
        marker?.icon = customMarker.createMarker(namaAparat,R.drawable.ic_markeraparat1)
        marker?.title = "Status: $statusAparat\nName: $namaAparat\nHp : $hpAparat\n Distance from Bike : "

        // Add the marker overlay to the map
        marker?.let {
            maps.overlays.add(it)
        }

        // Invalidate the map view to refresh the display
        maps.invalidate()
    }

    //fungsi tombol menubar
    val warnaKlik = "#00BCD4" // Contoh warna merah
    val klik = Color.parseColor(warnaKlik)
    val warnaTidakKlik = "#FFFFFFFF"
    val tidakklik = Color.parseColor(warnaTidakKlik)
    val nampak = View.VISIBLE
    val hilang = View.GONE
    private fun tombolHome(){
        btnHome.setCardBackgroundColor(klik)
        btnAbot.setCardBackgroundColor(tidakklik)
        btnHelp.setCardBackgroundColor(tidakklik)
        btnLout.setCardBackgroundColor(tidakklik)

        ketHome.visibility = nampak
        ketAbout.visibility= hilang
        ketHelp.visibility = hilang

        kontenHome.visibility = nampak
        kontenAbout.visibility = hilang
        kontenHelp.visibility = hilang
    }
    private fun tombolAbout(){
        btnHome.setCardBackgroundColor(tidakklik)
        btnAbot.setCardBackgroundColor(klik)
        btnHelp.setCardBackgroundColor(tidakklik)
        btnLout.setCardBackgroundColor(tidakklik)

        ketHome.visibility = hilang
        ketAbout.visibility= nampak
        ketHelp.visibility = hilang

        kontenHome.visibility = hilang
        kontenAbout.visibility = nampak
        kontenHelp.visibility = hilang
    }
    private fun tombolHelp(){
        btnHome.setCardBackgroundColor(tidakklik)
        btnAbot.setCardBackgroundColor(tidakklik)
        btnHelp.setCardBackgroundColor(klik)
        btnLout.setCardBackgroundColor(tidakklik)

        ketHome.visibility = hilang
        ketAbout.visibility= hilang
        ketHelp.visibility = nampak

        kontenHome.visibility = hilang
        kontenAbout.visibility = hilang
        kontenHelp.visibility = nampak
    }
    private fun tombolLogout(){
        btnHome.setCardBackgroundColor(tidakklik)
        btnAbot.setCardBackgroundColor(tidakklik)
        btnHelp.setCardBackgroundColor(tidakklik)
        btnLout.setCardBackgroundColor(klik)

        ketHome.visibility = hilang
        ketAbout.visibility= hilang
        ketHelp.visibility = hilang

        kontenHome.visibility = hilang
        kontenAbout.visibility = hilang
        kontenHelp.visibility = hilang
    }

    //database fungsi
    private fun hapusDataAparat(){
        // Mendapatkan instance dari SharedPreferences
        val sharedPreferences = getSharedPreferences("AparatData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("userId")
        editor.remove("type")
        editor.remove("nama")
        editor.remove("hp")
        editor.apply()
    }

    //fungsi databse
    private fun datadarilogin(){
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("AparatData", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "")
        val type = sharedPreferences.getString("type", "")
        val nama = sharedPreferences.getString("nama", "")
        val hp = sharedPreferences.getString("hp", "")
        if (nama != null && userId != null && type != null && hp != null ){
            namaAparat= nama
            statusAparat = type
            hpAparat = hp
            idAparat = userId
        }
    }
}