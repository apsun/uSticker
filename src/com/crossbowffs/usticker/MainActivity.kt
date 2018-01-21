package com.crossbowffs.usticker

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, SettingsFragment())
            .commit()
        requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {

        if (requestCode != 0) {
            return
        }

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.storage_permissions_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
