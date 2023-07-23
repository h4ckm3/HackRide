package com.hackme.hackride.fungsi
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object NetworkLoggingUtil {
    fun enableNetworkLogging(context: Context) {
        val deviceAdmin = ComponentName(context, DeviceAdminReceiver::class.java)
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (devicePolicyManager.isAdminActive(deviceAdmin)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                devicePolicyManager.setNetworkLoggingEnabled(deviceAdmin, true)
            } else {
                // Enable network logging for earlier versions
                val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable network logging")
                context.startActivity(intent)
            }
        }
    }
}
