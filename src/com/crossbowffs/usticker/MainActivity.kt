package com.crossbowffs.usticker

import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.help_text).movementMethod = LinkMovementMethod.getInstance()
        findViewById<Button>(R.id.refresh_button).setOnClickListener {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {

        if (requestCode != 0) {
            return
        }

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "External storage permissions required", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = ProgressDialog(this)
        dialog.isIndeterminate = true
        dialog.setCancelable(false)
        dialog.setMessage(getString(R.string.refreshing_in_progress))
        dialog.show()
        StickerManager.refreshStickers { e ->
            dialog.dismiss()
            if (e == null) {
                Toast.makeText(this, "Stickers successfully refreshed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to refresh stickers", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
