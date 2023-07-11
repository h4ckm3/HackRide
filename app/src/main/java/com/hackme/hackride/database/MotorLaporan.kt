package com.hackme.hackride.database

data class MotorLaporan(
    val id_motor : String,
    val latitude: Double,
    val longitude: Double,
    val mesin: Int,
    val getaran: Boolean,
    val latitudeDipakai: Double,
    val longitudeDipakai: Double,
)

