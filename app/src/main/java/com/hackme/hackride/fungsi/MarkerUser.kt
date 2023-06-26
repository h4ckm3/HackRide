package com.hackme.hackride.fungsi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.hackme.hackride.R

class MarkerUser(private val context: Context) {
    fun createMarker(name: String): Drawable {
        val markerLayout = LayoutInflater.from(context).inflate(R.layout.maker_pemilik, null)

        val markerTextView = markerLayout.findViewById<TextView>(R.id.tv_namauser)
        markerTextView.text = name

        markerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
        markerLayout.isDrawingCacheEnabled = true
        markerLayout.buildDrawingCache()

        val markerBitmap = Bitmap.createBitmap(markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888)
        val markerCanvas = Canvas(markerBitmap)
        markerLayout.draw(markerCanvas)

        return BitmapDrawable(context.resources, markerBitmap)
    }
}
