package com.crossbowffs.usticker

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, SettingsFragment())
            .commit()
    }
}
