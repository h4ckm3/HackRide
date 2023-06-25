package com.hackme.hackride.fungsi

import kotlin.math.sqrt

fun calculateEuclideanDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val dx = lat1 - lat2
    val dy = lon1 - lon2
    val distanceInDegrees = sqrt(dx * dx + dy * dy)
    val distanceInMeters = distanceInDegrees * 111.139 * 1000
    return distanceInMeters
}