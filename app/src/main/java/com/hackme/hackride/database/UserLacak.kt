package com.hackme.hackride.database

data class UserLacak(
    val userId :String,
    val hp : String,
    val nama:String,
    val latitude : Double,
    val longitude :Double,
    val ikut : Boolean,
    val pesan :String,
    val type: String
)
