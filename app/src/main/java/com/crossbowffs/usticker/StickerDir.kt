package com.crossbowffs.usticker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager

/**
 * Manages the sticker directory preference. Also responsible for taking
 * persistent permissions when necessary.
 */
object StickerDir {
    private const val PREF_STICKER_DIR_URI = "pref_sticker_dir_uri"

    /**
     * Returns the current configured sticker directory, or null if no
     * directory is set.
     */
    fun get(context: Context): Uri? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val stickerDir = prefs.getString(PREF_STICKER_DIR_URI, null) ?: return null
        return Uri.parse(stickerDir)
    }

    /**
     * Sets the configured sticker directory, and persists read permissions
     * on the directory for future use. If a previously configured directory
     * exists, this also releases permissions on it.
     *
     * @throws SecurityException if takePersistableUriPermission() fails
     */
    fun set(context: Context, stickerDir: Uri) {
        val resolver = context.contentResolver
        val origStickerDir = get(context)

        // Release original URI, but only if it changed. Apparently releasing
        // a URI and then taking it again will fail.
        if (origStickerDir != null && origStickerDir != stickerDir) {
            try {
                resolver.releasePersistableUriPermission(origStickerDir, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // Ignore, maybe user revoked our permission externally
                Klog.e("releasePersistableUriPermission() failed", e)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            resolver.takePersistableUriPermission(stickerDir, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            Klog.e("takePersistableUriPermission() failed", e)
            prefs.edit().remove(PREF_STICKER_DIR_URI).apply()
            throw e
        }

        prefs.edit().putString(PREF_STICKER_DIR_URI, stickerDir.toString()).apply()
        Klog.i("Set sticker dir -> $stickerDir")
    }
}
