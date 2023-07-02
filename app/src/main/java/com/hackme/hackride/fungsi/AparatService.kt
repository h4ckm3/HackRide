package com.hackme.hackride.fungsi

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackme.hackride.MainActivity
import com.hackme.hackride.R
import com.hackme.hackride.activity.PemilikActivity
import java.util.Timer
import java.util.TimerTask

class AparatService : Service() {
    private lateinit var databaseRef: DatabaseReference
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var latAparat: Double = 0.0
    private var longAparat: Double = 0.0
    private var userId: String = ""
    private var timerData : Timer? = null
    private var isLoggedOut = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        databaseRef = FirebaseDatabase.getInstance().reference
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("USER_ID")){
            userId = intent.getStringExtra("USER_ID") ?: ""
        }

        startForeground(1, createNotification()) // Memulai foreground service dengan notifikasi

        // Menghitung jumlah child pada node "motor"
        databaseRef.child("motor").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount
                Log.d("ChildCount", "Jumlah child pada node motor: $count")

                // Mengambil data motor untuk setiap child pada node "motor"
                for (childSnapshot in dataSnapshot.children) {
                    val motorId = childSnapshot.key
                    if (motorId != null) {
                        getDataMotor(motorId)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ChildCount", "Error: ${databaseError.message}")
            }
        })

        // Memeriksa izin lokasi dari pengguna
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation()
        } else {
            Log.e("LocationPermission", "Izin lokasi tidak diberikan.")
        }
        startAmbildata()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Konfigurasi notifikasi
        val channelId = "ForegroundServiceChannel"
        val channelName = "Foreground Service Channel"

        // Membuat kanal notifikasi (hanya diperlukan pada Android Oreo dan versi di atasnya)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // Membuat intent untuk membuka activity saat notifikasi ditekan (opsional)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Membuat notifikasi menggunakan NotificationCompat.Builder
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("HackRide is Running")
            .setContentText("Welcome Officer")
            .setSmallIcon(R.drawable.logoappbgputihblt)
            .setContentIntent(pendingIntent) // Tambahkan pending intent (opsional)
            .build()

        // Memulai service sebagai foreground service dengan notifikasi
        startForeground(1, notification)

        return notification
    }

    private fun getDataMotor(id_motor: String) {
        val motorRef = databaseRef.child("motor").child(id_motor)
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
                        val jarakAman = calculateEuclideanDistance(latitude, longitude, latitudeParkir, longitudeParkir)
                        val jarakAparat = calculateEuclideanDistance(latAparat, longAparat, latitude, longitude)
                        val bulat = Math.round(jarakAparat)
                        Log.d("notif seharusnya muncul","$mesin  $jarakAman  $jarakAparat")
                        if (mesin == 0 && jarakAman > 50 && jarakAparat < 10000) {
                            // Lakukan sesuatu jika kondisi memenuhi
                            Log.d("notif seharusnya muncul","$mesin  $jarakAman  $jarakAparat")
                            cekAktiviti(id_motor,bulat)
                        } else {
                            // Lakukan sesuatu jika kondisi tidak memenuhi
                            cancelNotification()
                        }
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
            latAparat = it.latitude
            longAparat = it.longitude

            // Kirim data ke Realtime Database Firebase
            val userLocationRef = databaseRef.child("users").child(userId)
            userLocationRef.child("latitude").setValue(latAparat)
            userLocationRef.child("longitude").setValue(longAparat)
            Log.d("data terkirim","latitude user $latAparat longitude user : $longAparat")
        }

        return userLocation
    }

    //timer
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
                databaseRef.child("motor").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val count = dataSnapshot.childrenCount
                        Log.d("ChildCount", "Jumlah child pada node motor: $count")

                        // Mengambil data motor untuk setiap child pada node "motor"
                        for (childSnapshot in dataSnapshot.children) {
                            val motorId = childSnapshot.key
                            if (motorId != null) {
                                getDataMotor(motorId)
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("ChildCount", "Error: ${databaseError.message}")
                    }
                })

            }
        }
        timerData?.schedule(timerTask, 0, 1000)
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
        isLoggedOut = true
    }
    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()

    }
    //notifikasi
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

    //fungsi cek aktifitas sekarang
    private fun cekAktiviti(idMotor: String, JarakAparat: Long){
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
                showHeadsUpNotification("A theft case has occurred","Id $idMotor distance from you $JarakAparat m")
            }
        }

    }
}
