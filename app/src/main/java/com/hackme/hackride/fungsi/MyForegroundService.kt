package com.hackme.hackride.fungsi

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.MainActivity
import com.hackme.hackride.R
import java.util.Timer
import java.util.TimerTask

class MyForegroundService : Service() {

    private var latitudeMotor : Double = 0.0
    private var longitudeMotor : Double = 0.0
    private var mesinMotor: String = ""
    private var inMesin : Int = 0
    private var getaranMotor : Boolean = false
    private var latPerkir : Double = 0.0
    private var longParkir : Double =0.0
    private var JarakUser : Int = 0
    private var Abaikan : Boolean = false

    private lateinit var database: DatabaseReference
    private var status:String =""
    private lateinit var motorLocation: Location
    private var latUser : Double = 0.0
    private var longUser : Double = 0.0
    private var idMotor: String = ""
    private var alertDialog: AlertDialog? = null
    private var timerServis: Timer? = null
    private var timerData: Timer? = null
    private var timerAbaikan: Timer? = null
    private var isServiceRunning = false
    private var isLoggedOut = false
    private var id_user : String =""

    companion object {
        const val ACTION_STOP_LOCATION_COMPARISON = "com.hackme.hackride.STOP_LOCATION_COMPARISON"
        const val ACTION_START_ABAIKAN = "com.hackme.hackride.ACTION_START_ABAIKAN"
        const val ACTION_STOP_ABAIKAN = "com.hackme.hackride.ACTION_STOP_ABAIKAN"
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
                startForegroundService("HackRide is Running","Welcome Owner")
            }else{
                startForegroundService("HackRide is Running","Welcome Officer")
            }

            startLocationComparison()
            startAmbildata()
            instance = this // Set instance MyForegroundService
        }
        when (intent?.action) {
            ACTION_START_ABAIKAN -> startAbaikan()
            ACTION_STOP_ABAIKAN -> stopAbaikan()
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
//                getUserLocation()
                 jarakUserMotorHidup(inMesin)
                compareDistanceToParking(latitudeMotor, longitudeMotor, latPerkir, longParkir, inMesin)

            }
        }
        timerServis?.schedule(timerTask, 0, 1000)
    }
    private fun startAmbildata() {
        timerData = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                if (isLoggedOut) {
                    // Jika telah logout, batalkan pemanggilan getDataMotor() dan getUserLocation()
                    timerData?.cancel()
                    return
                }
                getUserLocation()
                getDataMotor(idMotor)

            }
        }
        timerData?.schedule(timerTask, 0, 1000)
    }

    private fun startAbaikan() {
        Abaikan = true
    }
    private fun stopAbaikan() {
        Abaikan = false
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
        JarakUser = jarakUser
        Log.d("Jarak", "jarak $distance status: $status id motor: $idMotor")
        if (status == "Pemilik"){
            if (distance > 50 && mesin == 0) {
                cekAktiviti()
            }else{
                cancelNotification()
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
        notificationManager.notify(2, notificationBuilder.build())
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)
    }

    private fun showHeadsUpNotification1(title: String, message: String) {
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
        notificationManager.notify(3, notificationBuilder.build())
    }

    private fun cancelNotification1() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(3)
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
        // Membuat intent untuk membuka activity saat notifikasi ditekan (opsional)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logoappbgputihblt)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
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

    //cek aktifiti fungsi
    private fun cekAktiviti(){
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningActivities = activityManager.getRunningTasks(1)

        if (runningActivities.isNotEmpty()) {
            val topActivity = runningActivities[0].topActivity
            val packageName = topActivity?.packageName
            val className = topActivity?.className
            Log.d("aktifitas","$packageName   $className")

            // Lakukan pengecekan packageName dan className untuk menentukan aktivitas yang sedang aktif
            if (packageName == "com.hackme.hackride" && className == "com.hackme.hackride.activity.LacakActivity") {
                // Lakukan tindakan sesuai dengan aktivitas LacakActivity yang sedang aktif
                cancelNotification()
            }
            else{
                showHeadsUpNotification("Beware of Detected Theft", "Distance from you $JarakUser m")
            }
        }

    }
    private fun cekAktiviti1(){
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningActivities = activityManager.getRunningTasks(1)

        if (runningActivities.isNotEmpty()) {
            val topActivity = runningActivities[0].topActivity
            val packageName = topActivity?.packageName
            val className = topActivity?.className
            Log.d("aktifitas","$packageName   $className")

            // Lakukan pengecekan packageName dan className untuk menentukan aktivitas yang sedang aktif
            if (packageName == "com.hackme.hackride" && className == "com.hackme.hackride.activity.LacakActivity") {
                // Lakukan tindakan sesuai dengan aktivitas LacakActivity yang sedang aktif
                cancelNotification1()
            }
            else{
                showHeadsUpNotification1("you are too far from the active motor", "Distance from you $JarakUser m")
            }
        }

    }
    private fun jarakUserMotorHidup(mesin: Int){
        val jarakUserMotorHidup = com.hackme.hackride.fungsi.calculateEuclideanDistance(
            latitudeMotor,
            longitudeMotor,
            latUser,
            longUser
        )
        if (mesin != 0 && jarakUserMotorHidup > 200 ){
            Log.d("abaikan","$Abaikan")
            if (Abaikan == false){
                cekAktiviti1()
            }else{
                cancelNotification1()
            }
        }else{
            cancelNotification1()
        }
    }

}
