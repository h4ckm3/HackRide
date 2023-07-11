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
import android.location.Location
import android.location.LocationManager
import android.os.Build
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
import com.hackme.hackride.database.Motor
import com.hackme.hackride.database.MotorLaporan
import com.hackme.hackride.database.UserLacak
import java.util.Timer
import java.util.TimerTask

class AparatService : Service() {
    private lateinit var databaseRef: DatabaseReference
    private lateinit var locationManager: LocationManager
    private var latAparat: Double = 0.0
    private var longAparat: Double = 0.0
    private var userId: String = ""
    private var timerData : Timer? = null
    private var isLoggedOut = false
    private val motorList: MutableList<Motor> = mutableListOf()
    private val motorListLaporan: MutableList<MotorLaporan> = mutableListOf()

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
                getDataMotor()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ChildCount", "Error: ${databaseError.message}")
            }
        })

        // Memeriksa izin lokasi dari pengguna
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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

    private fun getDataMotor() {
        val motorRef = databaseRef.child("motor").get().addOnCompleteListener { task ->
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
                        val laporan = childSnapshot.child("laporan").getValue(Boolean::class.java)
                        val id_motor = childSnapshot.key
                        var idMotorTerdekat:String = ""
                    var idMotorLaporanTerdekat:String = ""
                    Log.d("Notifikasi Motor", "Motor $motorList memenuhi kriteria notifikasi")
                    Log.d("Notifikasi Motor", "Motor $motorListLaporan memenuhi kriteria notifikasi")
                    if (motorList == null){
                        cancelNotification()
                    }
                    if (motorListLaporan == null){
                        cancelNotification1()
                    }
                        if (id_motor != null&&latitude != null && longitude != null && latitudedipakai != null && longitudedipakai != null && mesin != null && getaran != null) {

                            val jarakAman = calculateEuclideanDistance(latitude, longitude, latitudedipakai, longitudedipakai)
                            val jarakAparat = calculateEuclideanDistance(latAparat, longAparat, latitude, longitude)
                            val bulat = Math.round(jarakAparat)
                            Log.d("Data Motor", "$idMotorTerdekat - Jarak Aman: $jarakAman, Jarak Aparat: $jarakAparat")

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
                                // Kirim data ke Realtime Database Firebase
                                Log.d("Notifikasi Motor", "Motor $id_motor memenuhi kriteria notifikasi")
                                if (id_motor == idMotorTerdekat){
                                    cekAktiviti(id_motor,bulat)
                                }


                            } else {
                                // Hapus data motor dari motorList
                                val motorToRemove = motorList.firstOrNull { it.id_motor == id_motor }
                                if (motorToRemove != null) {
                                    motorList.remove(motorToRemove)
                                }
                            }
                        }

                        if (id_motor!=null&&laporan == true && latitude != null && longitude != null&& latitudedipakai != null && longitudedipakai != null && mesin != null && getaran != null) {
                            val jarakAparat = calculateEuclideanDistance(latAparat, longAparat, latitude, longitude)
                            val bulat = Math.round(jarakAparat)
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
                            idMotorLaporanTerdekat = getNearestMotorIdLaporan(latitude,longitude).toString()
                            if (id_motor == idMotorLaporanTerdekat&&jarakAparat < 10000) {
                                Log.d("Notifikasi Motor", "Motor $motorList memenuhi kriteria notifikasi")
                                Log.d("Notifikasi Motor", "Motor $motorListLaporan memenuhi kriteria notifikasi")
                                cekAktiviti1(id_motor,bulat)
                            } else {
//                                cancelNotification1()
                            }
                        } else {
                            // Hapus data motor dari motorList
                            val motorToRemove = motorListLaporan.firstOrNull { it.id_motor == id_motor }
                            if (motorToRemove != null) {
                                motorListLaporan.remove(motorToRemove)
                            }
//                            cancelNotification1()
                        }
                    }
                } else {
                // Terjadi kesalahan saat mengambil data dari Firebase
                val exception = task.exception
                Log.e("Data Motor", "Error: ${exception?.message}")
            }
        }
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
                    timerData == null
                    return
                }
                getUserLocation()
                databaseRef.child("motor").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val count = dataSnapshot.childrenCount
                        Log.d("ChildCount", "Jumlah child pada node motor: $count")

                        // Mengambil data motor untuk setiap child pada node "motor"
                        getDataMotor()
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
                Log.d("Notifikasi","$idMotor   $JarakAparat")
            }
        }

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

    //fungsi cek aktifitas sekarang
    private fun cekAktiviti1(idMotor: String, JarakAparat: Long){
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
                showHeadsUpNotification1("A theft case has occurred","Id $idMotor distance from you $JarakAparat m")
            }
        }

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
