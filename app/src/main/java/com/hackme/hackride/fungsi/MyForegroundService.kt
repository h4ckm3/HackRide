package com.hackme.hackride.fungsi

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*
import com.hackme.hackride.MainActivity
import com.hackme.hackride.R
import java.util.*

class MyForegroundService : Service() {

    private var latitudeMotor : Double = 0.0
    private var longitudeMotor : Double = 0.0
    private var mesinMotor: String = ""
    private var inMesin : Int = 0
    private var getaranMotor : Boolean = false
    private var latPerkir : Double = 0.0
    private var longParkir : Double =0.0


    private lateinit var database: DatabaseReference
    private var status:String =""
    private lateinit var motorLocation: Location
    private var latUser : Double = 0.0
    private var longUser : Double = 0.0
    private var idMotor: String = ""
    private var alertDialog: AlertDialog? = null
    private var timerServis: Timer? = null
    private var timerData: Timer? = null
    private var isServiceRunning = false
    private var isLoggedOut = false
    private var id_user : String =""

    companion object {
        const val ACTION_STOP_LOCATION_COMPARISON = "com.hackme.hackride.STOP_LOCATION_COMPARISON"
        private var instance: MyForegroundService? = null

        fun getInstance(): MyForegroundService? {
            return instance
        }
    }

    private val stopLocationComparisonReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_STOP_LOCATION_COMPARISON) {
                stopLocationComparison()
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().reference

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "foreground_service_channel"
            val channelName = "Foreground Service Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        // Daftarkan receiver untuk ACTION_STOP_LOCATION_COMPARISON
        val intentFilter = IntentFilter(ACTION_STOP_LOCATION_COMPARISON)
        registerReceiver(stopLocationComparisonReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("id_motor")) {
            idMotor = intent.getStringExtra("id_motor") ?: ""
            status = intent.getStringExtra("status") ?: ""
            id_user = intent.getStringExtra("id_user") ?: ""
            if (status == "Pemilik"){
                startForegroundService("HackRide is Running","Welcome Owner\n" +
                    "Safe state")
            }else{
                startForegroundService("HackRide is Running","Welcome Officer\n" +
                        "no case situation")
            }

            startLocationComparison()
            instance = this // Set instance MyForegroundService
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationComparison()
        dismissAlertDialog()
        if (isServiceRunning) {
            stopForegroundService()
            stopLocationComparison()
        }
        unregisterReceiver(stopLocationComparisonReceiver)
        stopService()
    }

    private fun getDataMotor(id_motor: String) {
        val motorRef = database.child("motor").child(id_motor)
        motorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                    val mesin = dataSnapshot.child("mesin").getValue(Int::class.java)
                    val getaran = dataSnapshot.child("getaran").getValue(Boolean::class.java)
                    val latitudeParkir = dataSnapshot.child("latitudeparkir").getValue(Double::class.java)
                    val longitudeParkir = dataSnapshot.child("longitudeparkir").getValue(Double::class.java)

                    if (latitude != null && longitude != null && latitudeParkir != null && longitudeParkir != null && mesin != null&& getaran != null) {
                        Log.d("Data Motor", "Latitude: $latitude, Longitude: $longitude, Mesin: $mesin, Latitude Parkir: $latitudeParkir, Longitude Parkir: $longitudeParkir")
                        latitudeMotor = latitude
                        longitudeMotor = longitude
                        latPerkir = latitudeParkir
                        longParkir = longitudeParkir
                        getaranMotor = getaran
                        inMesin = mesin
                        if (mesin == 0){
                            mesinMotor = "nonActive"
                        }else{
                            mesinMotor ="Active"
                        }
                        compareDistanceToParking(latitude, longitude, latitudeParkir, longitudeParkir, mesin)
                    }
                } else {
                    // Data motor dengan id_motor tersebut tidak ditemukan
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error jika gagal mengambil data dari Firebase
            }
        })
    }

    private fun startLocationComparison() {
        timerServis = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                if (isLoggedOut) {
                    // Jika telah logout, batalkan pemanggilan getDataMotor() dan getUserLocation()
                    timerServis?.cancel()
                    return
                }
                getUserLocation()
                getDataMotor(idMotor)

            }
        }
        timerServis?.schedule(timerTask, 0, 3000)
    }

    private fun compareDistanceToParking(motorLat: Double, motorLng: Double, parkirLat: Double, parkirLng: Double, mesin: Int) {
        val motorLocation = Location("Motor Location")
        motorLocation.latitude = motorLat
        motorLocation.longitude = motorLng

        val parkirLocation = Location("Parkir Location")
        parkirLocation.latitude = parkirLat
        parkirLocation.longitude = parkirLng

        val distance = calculateEuclideanDistance(motorLat, motorLng, parkirLat, parkirLng)
        val jarakUser = Math.round(calculateEuclideanDistance(motorLat,motorLng,latUser,longUser))
        Log.d("Jarak", "jarak $distance status: $status id motor: $idMotor")
        if (status == "Pemilik"){
            if (distance > 50 && mesin == 0) {
                showHeadsUpNotification("Beware of Detected Theft", "Distance from you $jarakUser m")
            }else{
                startForegroundService("HackRide is Running","Welcome Owner\n" +
                        "Safe state")
            }
        }
    }

    private fun showHeadsUpNotification(title: String, message: String) {
        val channelId = "heads_up_channel_id"
        val channelName = "Heads Up Channel"

        // Buat intent untuk dijalankan saat notifikasi ditekan
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Membuat notifikasi dengan tipe heads-up
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logoappbgputihblt)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        // Menampilkan notifikasi sebagai heads-up notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }


    private fun getFullScreenIntent(): PendingIntent? {
        // Create intent to be executed when notification is pressed
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        // Set intent type to PendingIntent
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun stopLocationComparison() {
        // Stop distance comparison (e.g., stop timer task)
        timerServis?.cancel()
        timerServis = null
    }

    private fun calculateEuclideanDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun startForegroundService(title: String,message: String) {
        val channelId = "foreground_service_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logoappbgputihblt)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Create the notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Foreground Service Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        startForeground(1, notificationBuilder)
    }

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                // Handle positive button click (e.g., navigate to MyActivity)
                navigateToMyActivity()
                dialog.dismiss()
            }

        alertDialog = builder.create()
//        alertDialog?.show()
    }

    private fun navigateToMyActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun dismissAlertDialog() {
        alertDialog?.dismiss()
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopLocationComparison()
        stopSelf()
        isServiceRunning = false
    }

    fun setLoggedOut() {
        isLoggedOut = true
        stopLocationComparison()
        stopForegroundService()
    }
    private fun stopService() {
        stopLocationComparison()
        dismissAlertDialog()
        stopForegroundService()
        stopForeground(true)
        stopSelf()
        isServiceRunning = false
    }

    //mendapatkan lokasi user
    private fun getUserLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Tambahkan logika untuk meminta izin lokasi jika belum diberikan
            // ActivityCompat.requestPermissions(...)
            return null
        }

        val locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        var userLocation: Location? = null

        if (locationGPS != null && locationGPS.time > System.currentTimeMillis() - 2 * 60 * 1000) {
            userLocation = locationGPS
        } else if (locationNetwork != null && locationNetwork.time > System.currentTimeMillis() - 2 * 60 * 1000) {
            userLocation = locationNetwork
        }

        // Simpan latitude dan longitude ke dalam variabel latUser dan longUser
        userLocation?.let {
            latUser = it.latitude
            longUser = it.longitude

            // Kirim data ke Realtime Database Firebase
            val userLocationRef = database.child("users").child(id_user)
            userLocationRef.child("latitude").setValue(latUser)
            userLocationRef.child("longitude").setValue(longUser)
            Log.d("data terkirim","latitude user $latUser longitude user : $longUser")
        }

        return userLocation
    }

}
