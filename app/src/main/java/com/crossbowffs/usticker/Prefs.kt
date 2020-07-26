package com.crossbowffs.usticker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import java.lang.IllegalArgumentException

/**
 * Manages app preferences, with a touch of business logic.
 */
object Prefs {
    private const val PREF_STICKER_DIR_URI = "pref_sticker_dir_uri"
    private const val PREF_STICKER_SORT_ORDER = "pref_sticker_sort_order"

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Returns the current configured sticker directory, or null if no
     * directory is set.
     */
    fun getStickerDir(context: Context): Uri? {
        val prefs = getSharedPrefs(context)
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
    fun setStickerDir(context: Context, stickerDir: Uri) {
        val resolver = context.contentResolver
        val origStickerDir = getStickerDir(context)

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

        val prefs = getSharedPrefs(context)
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

    /**
     * Returns the currently selected sticker sort order, or null if
     * no sorting order has been selected.
     */
    fun getStickerSortOrder(context: Context): StickerSortOrder? {
        val prefs = getSharedPrefs(context)
        val key = prefs.getString(PREF_STICKER_SORT_ORDER, null) ?: return null
        return try {
            StickerSortOrder.valueOf(key)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * If no sticker sort order has been selected, initializes it to
     * the default sorting order.
     *
     * TODO: We should change the default to alphabetical sometime in the future.
     */
    fun initStickerSortOrder(context: Context) {
        if (getStickerSortOrder(context) == null) {
            val prefs = getSharedPrefs(context)
            prefs.edit().putString(PREF_STICKER_SORT_ORDER, StickerSortOrder.NONE.name).apply()
        }
    }
}
