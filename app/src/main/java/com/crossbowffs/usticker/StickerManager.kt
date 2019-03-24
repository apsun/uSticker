package com.crossbowffs.usticker

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import android.text.TextUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Sticker "manager" - responsible for finding and loading
 * sticker files from the filesystem.
 */
object StickerManager {
    const val PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
    private const val DEFAULT_STICKER_DIR = "Pictures/Stickers/"

    /**
     * Checks whether a file (directory) is a child of a
     * given parent directory.
     */
    private fun isChildPath(parent: File, child: File): Boolean {
        var tmp: File? = child
        while (tmp != null) {
            if (tmp == parent) {
                return true
            }
            tmp = tmp.parentFile
        }
        return false
    }

    /**
     * Returns the configured sticker directory path.
     */
    fun getConfigStickerDir(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("pref_sticker_dir", null) ?: DEFAULT_STICKER_DIR
    }

    /**
     * Initializes the configured sticker directory to its default value,
     * if a value has not already been set.
     */
    fun initStickerDir(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (TextUtils.isEmpty(prefs.getString("pref_sticker_dir", null))) {
            prefs.edit().putString("pref_sticker_dir", DEFAULT_STICKER_DIR).apply()
        }
    }

    /**
     * Validates the specified sticker directory path.
     */
    fun validateStickerDir(stickerDir: String): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val file = File(sdcard, stickerDir).canonicalFile
        if (!isChildPath(sdcard, file)) {
            throw IOException("Path is outside external storage: $stickerDir")
        }
        if (!file.exists()) {
            throw FileNotFoundException("Directory does not exist: $stickerDir")
        }
        if (!file.isDirectory) {
            throw IOException("Path exists but is a file: $stickerDir")
        }
        return file
    }

    /**
     * Returns the sticker base directory. May throw an exception if the
     * directory does not exist or the path in preferences is invalid.
     */
    fun getStickerDir(context: Context): File {
        val stickerDir = getConfigStickerDir(context)
        return validateStickerDir(stickerDir)
    }

    /**
     * Converts a sticker "relative path" into a File object.
     * Returns null if the path is invalid, or the sticker file
     * does not exist.
     */
    fun getStickerFile(context: Context, path: String): File? {
        val stickerDir = try {
            getStickerDir(context)
        } catch (e: Exception) {
            Klog.e("Sticker directory is invalid", e)
            return null
        }

        // Convert relative path into an absolute,
        // normalized path
        val file = try {
            File(stickerDir, path).canonicalFile
        } catch (e: IOException) {
            Klog.e("Could not normalize path: $path")
            return null
        }

        // Ensure that path refers to a file, and that
        // the file exists
        if (!file.isFile) {
            Klog.e("File not found or is a directory: $path")
            return null
        }

        // Check that the path didn't contain any kind
        // of sneaky directory traversal tricks by ensuring
        // the sticker file is a child of the base directory
        if (!isChildPath(stickerDir, file)) {
            Klog.e("Invalid/malicious path: $path")
            return null
        }

        return file
    }
}
