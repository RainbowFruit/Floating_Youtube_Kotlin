package com.example.youtubewidgetkotlin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isOverlayPermissionGranted()){
            initializeWidgetView()
        }
    }

    private fun initializeWidgetView() {
        startService(Intent(this, FloatingWidgetService::class.java))
        finish()
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            openOverlayPermissionWindow()
            false
        } else {
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun openOverlayPermissionWindow() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 0) {
            requestCodeZeroHandle()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requestCodeZeroHandle() {
        //Check if permission is granted because resultCode is always zero
        if(isOverlayPermissionGranted()) {
            initializeWidgetView()
        } else {
            Toast.makeText(this,
                "Draw over other app permission not available. Closing the application",
                Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
