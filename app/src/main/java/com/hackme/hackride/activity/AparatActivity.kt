package com.hackme.hackride.activity

import android.Manifest
import android.annotation.SuppressLint
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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.hackme.hackride.R
import com.hackme.hackride.R.layout.activity_aparat
import com.hackme.hackride.database.DataKasus
import com.hackme.hackride.database.Motor
import com.hackme.hackride.database.MotorLaporan
import com.hackme.hackride.fungsi.AparatService
import com.hackme.hackride.fungsi.MarkerUser
import com.hackme.hackride.fungsi.calculateEuclideanDistance
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.Timer
import java.util.TimerTask

@Suppress("DEPRECATION")
class AparatActivity : AppCompatActivity(), LocationListener {
    //data motor
    private var jumlahMotor : Int =0
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
    private lateinit var btnAktifkanLokasi : Button
    private lateinit var btnExitnonLokasi :Button
    private lateinit var notifKasus : CardView
    private lateinit var teksNotifKasus : TextView
    private lateinit var btnLacakKasus : Button

    //database
    private lateinit var auth: FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var databaseRef : DatabaseReference
    private val motorList: MutableList<Motor> = mutableListOf()
    private val motorListLaporan: MutableList<MotorLaporan> = mutableListOf()

    //masp
    lateinit var maps : MapView
    private var marker: Marker? = null
    private var compassOverlay: CompassOverlay? = null
    private var compassEnabled = false
    //lokasi
    private lateinit var locationManager: LocationManager
    // waktu
    private var timerData : Timer? = null
    @SuppressLint("MissingInflatedId")
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
        btnAktifkanLokasi = findViewById(R.id.btn_aktifkanlokasi)
        btnExitnonLokasi = findViewById(R.id.btn_tolakaktifkanlokasi)
        teksNotifKasus = findViewById(R.id.tv_notifikasikasus)
        btnLacakKasus = findViewById(R.id.btn_lacakkemalingan)
        //inisialisasi database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseRef = database.reference
        //ujicoba
        btnLacakKasus.setOnClickListener {
            val lacak = Intent(this, LacakActivity::class.java)
            startActivity(lacak)
            finishAffinity()
        }


        //inisialisasi screen mulai
        tombolHome()
        setinganMulai()

        //penggunanan tombol menu bar
        btnLout.setOnClickListener {
            logoutAparat()
            tombolLogout()
            locationManager.removeUpdates(this)
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

        //tombol maps
        btnFokusAparat.setOnClickListener {
            fokusAparat()
        }

        //tombol notifikasi lokasi
        btnAktifkanLokasi.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            notifSensorLokasi.visibility = View.GONE
            finishAffinity()
        }
        btnExitnonLokasi.setOnClickListener {
            val serviceIntent = Intent(this, AparatService::class.java)
            stopService(serviceIntent)
            notifSensorLokasi.visibility = View.GONE
            finishAffinity()
        }

    }

    //fungsi memulai halaman
    private fun setinganMulai(){
        notifSensorLokasi.visibility = hilang
        notifKasus.visibility = hilang
        datadarilogin()
        lokasiuser()
        setupMapView()
        startClockData()
        // Memulai AparatService dengan mengirimkan ID pengguna
        val startServiceIntent = Intent(this, AparatService::class.java)
        startServiceIntent.putExtra("USER_ID", idAparat)
        startService(startServiceIntent)
        marker = Marker(maps)
    }

    //fungsi
    private fun logoutAparat(){
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        // Menghentikan AparatService
        val stopServiceIntent = Intent(this, AparatService::class.java)
        stopService(stopServiceIntent)
        Intent(this, AparatService::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        startActivity(intent)
        hapusDataAparat()
        finishAffinity()
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

        latAparat = location.latitude
        longAparat = location.longitude
        markerUser(latAparat,longAparat)
        // Set the map focus to the user's current location
    }


    override fun onResume() {
        super.onResume()
        setupMapWithLocation()
        // Resume the map view when the activity is resumed
        maps.onResume()
        enableCompass()
    }

    override fun onPause() {
        maps.onPause()
        disableCompass()
        super.onPause()
        cancelClockData()

    }
    override fun onStop() {
        super.onStop()
        disableCompass()
        maps.onPause()
        cancelClockData()
    }

// atau

    override fun onDestroy() {
        super.onDestroy()
        disableCompass()
//        locationManager.removeUpdates(this)
        maps.onPause()
        marker?.onDestroy()
        Intent(this, PemilikActivity::class.java).flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        cancelClockData()

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
        maps.controller.setZoom(15.0)

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

    private fun enableCompass() {
        if (!compassEnabled) {
            compassOverlay?.disableCompass() // Pastikan untuk menonaktifkan kompas jika sudah aktif sebelumnya
            compassOverlay = CompassOverlay(this, maps)
            compassOverlay?.enableCompass()
            maps.overlays.add(compassOverlay)
            compassEnabled = true
        }
    }

    private fun disableCompass() {
        if (compassEnabled) {
            compassOverlay?.disableCompass()
            maps.overlays.remove(compassOverlay)
            compassEnabled = false
        }
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
//        maps.addOnFirstLayoutListener { v, left, top, right, bottom ->
//            marker = Marker(maps)}
        val customMarker = MarkerUser(this)
        marker?.position = userLocation
        marker?.icon = customMarker.createMarker(namaAparat,R.drawable.ic_markeraparat4)
        marker?.title = "Status: $statusAparat\nName: $namaAparat\nHp : $hpAparat\n "

        // Add the marker overlay to the map
        marker?.let {
            maps.overlays.add(it)
        }

        // Invalidate the map view to refresh the display
        maps.invalidate()
    }
    private fun fokusAparat(){
        markerUser(latAparat,longAparat)
        val initialLocation = GeoPoint(latAparat, longAparat)
        maps.controller.setCenter(initialLocation)
        maps.controller.setZoom(17.0)
        val rotationGestureOverlay = RotationGestureOverlay(maps)
        rotationGestureOverlay.isEnabled = false
        if (!maps.boundingBox.contains(latAparat, longAparat)) {
            Handler().postDelayed({ fokusAparat() }, 1000)
        }else{
            rotationGestureOverlay.isEnabled = true
            marker?.showInfoWindow()
        }
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


    @SuppressLint("SetTextI18n")
    private fun getDataMotor() {
        databaseRef.child("motor").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                val count = dataSnapshot?.childrenCount?.toInt() ?: 0
                val childNames = mutableListOf<Motor>()
                dataSnapshot?.children?.forEach { childSnapshot ->
                    val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = childSnapshot.child("longitude").getValue(Double::class.java)
                    val mesin = childSnapshot.child("mesin").getValue(Int::class.java)
                    val getaran = childSnapshot.child("getaran").getValue(Boolean::class.java)
                    val latitudedipakai = childSnapshot.child("latitudedipakai").getValue(Double::class.java)
                    val longitudedipakai = childSnapshot.child("longitudedipakai").getValue(Double::class.java)
                    val idPemilik = childSnapshot.child("id_pemilik").getValue(String::class.java)
                    val laporan = childSnapshot.child("laporan").getValue(Boolean::class.java)
                    val id_motor = childSnapshot.key
                    var idMotorTerdekat:String
                    if (motorList.isEmpty()&&motorListLaporan.isEmpty()){
                        notifKasus.visibility = hilang
                        hapusDataKasus()
                    }
                    Log.d("lis motor","$motorList")
                    Log.d("lis motor","$motorListLaporan")

                    if (id_motor!= null&&latitude != null && longitude != null && latitudedipakai != null && longitudedipakai != null && mesin != null && getaran != null&& idPemilik != null) {
                        val jarakAman = calculateEuclideanDistance(latitude, longitude, latitudedipakai, longitudedipakai)
                        val jarakAparat = calculateEuclideanDistance(latAparat, longAparat, latitude, longitude)
                        val bulatJarakaparat = Math.round(jarakAparat)
                        if (mesin == 0 && jarakAman > 50 && jarakAparat < 10000) {
                            val motor = Motor(
                                id_motor =id_motor,
                                latitude = latitude,
                                longitude = longitude,
                                mesin = mesin,
                                getaran = getaran,
                                latitudeDipakai = latitudedipakai,
                                longitudeDipakai = longitudedipakai
                            )
                            if (!motorList.any { it.id_motor == id_motor }) {
                                motorList.add(motor)
                            }
                            idMotorTerdekat = getNearestMotorId(latitude,longitude).toString()
                            if (id_motor == idMotorTerdekat){
                                notifKasus.visibility = nampak
                                teksNotifKasus.text = "The motor with id $id_motor has been stolen\ndistance from you $bulatJarakaparat m"
                                val kasusData = DataKasus(idAparat, statusAparat, id_motor, idPemilik)
                                saveDataKasus(kasusData)
                            }
                        } else {
                            // Hapus data motor dari motorList
                            val motorToRemove = motorList.firstOrNull { it.id_motor == id_motor }
                            if (motorToRemove != null) {
                                motorList.remove(motorToRemove)
                            }
                        }
                    }
                    if (id_motor!= null &&laporan == true && latitude != null && longitude != null && idPemilik != null&& latitudedipakai != null && longitudedipakai != null && mesin != null && getaran != null) {
                        val jarakAparat = calculateEuclideanDistance(latAparat, longAparat, latitude, longitude)
                        val bulat = Math.round(jarakAparat)
                        if (jarakAparat < 10000) {
                            val motor = MotorLaporan(
                                id_motor =id_motor,
                                latitude = latitude,
                                longitude = longitude,
                                mesin = mesin,
                                getaran = getaran,
                                latitudeDipakai = latitudedipakai,
                                longitudeDipakai = longitudedipakai
                            )
                            if (!motorListLaporan.any { it.id_motor == id_motor }) {
                                motorListLaporan.add(motor)
                            }
                            idMotorTerdekat = getNearestMotorIdLaporan(latitude,longitude).toString()
                            if (id_motor == idMotorTerdekat){
                                notifKasus.visibility = nampak
                                teksNotifKasus.text = "The motor with id $id_motor has been stolen\ndistance from you $bulat m"
                                val kasusData = DataKasus(idAparat, statusAparat, id_motor, idPemilik)
                                saveDataKasus(kasusData)
                            }
                        }
                    }else {
                        // Hapus data motor dari motorList
                        val motorToRemove = motorListLaporan.firstOrNull { it.id_motor == id_motor }
                        if (motorToRemove != null) {
                            motorListLaporan.remove(motorToRemove)
                        }
                    }
                    // Lakukan sesuatu dengan data yang telah diambil
                    // Contoh: tampilkan data di logcat
                    Log.d("Data Motor Aparat aktifiti", "Latitude: $latitude, Longitude: $longitude, Mesin: $mesin, Getaran: $getaran, Latitude Parkir: $latitudedipakai, Longitude Parkir: $longitudedipakai")
                }
            } else {
                // Terjadi kesalahan saat mengambil data dari Firebase
                val exception = task.exception
                Log.e("Data Motor", "Error: ${exception?.message}")
            }
        }
    }


    //fungsi timer
    private fun startClockData() {
        timerData = Timer()
        timerData?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Kode untuk pembaruan data setiap detik
                runOnUiThread {
                    markerUser(latAparat,longAparat)
                }
                // Panggil fungsi updateData() atau kode pembaruan data lainnya di sini
                getDataMotor()
            }
        }, 0, 1000) // Menjalankan tugas setiap 1000 milidetik (1 detik)
    }
    private fun cancelClockData() {
        timerData?.cancel()
        timerData = null
    }

    //fungsi ketika terjadi kasus
    private fun saveDataKasus(dataKasus: DataKasus) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("DataKasus", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("id_user", dataKasus.id_user)
        editor.putString("status", dataKasus.status)
        editor.putString("id_motor", dataKasus.id_motor)
        editor.putString("id_pemilik", dataKasus.id_pemilik)
        editor.apply()

    }
    private fun hapusDataKasus(){
        // Mendapatkan instance dari SharedPreferences
        val sharedPreferences = getSharedPreferences("DataKasus", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("id_user")
        editor.remove("status")
        editor.remove("id_motor")
        editor.remove("id_pemilik")
        editor.apply()
    }

    // pembaning lokasi apara
    fun getNearestMotorId(lat:Double,long:Double): String? {
        var nearestMotorId: String? = null
        var closestDistance: Double = Double.MAX_VALUE

        for (motor in motorList) {
            val distance = calculateEuclideanDistance(latAparat,longAparat,lat,long)
            if (distance < closestDistance) {
                nearestMotorId = motor.id_motor
                closestDistance = distance
            }
        }

        return nearestMotorId
    }
    fun getNearestMotorIdLaporan(lat:Double,long:Double): String? {
        var nearestMotorId: String? = null
        var closestDistance: Double = Double.MAX_VALUE

        for (motor in motorListLaporan) {
            val distance = calculateEuclideanDistance(latAparat,longAparat,lat,long)
            if (distance < closestDistance) {
                nearestMotorId = motor.id_motor
                closestDistance = distance
            }
        }

        return nearestMotorId
    }
}