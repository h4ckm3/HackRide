package com.hackme.hackride.fungsi

import java.util.*

fun startClock(updateData: () -> Unit) {
    val timer = Timer()

    timer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            updateData()
        }
    }, 0, 1000)
}
