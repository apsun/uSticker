package com.crossbowffs.usticker

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import java.io.FileNotFoundException

/**
 * Provides sticker files to the Firebase indexing service.
 * Only supports reading files via openFile().
 */
class StickerProvider : ContentProvider() {
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val path = uri.lastPathSegment ?: throw FileNotFoundException("Invalid URI: $uri")
        Klog.i("Requesting sticker: $path")
        val stickerDir = Prefs.getStickerDir(context!!) ?: throw FileNotFoundException("Sticker directory not set")
        val fileUri = DocumentsContract.buildDocumentUriUsingTree(stickerDir, path)
        try {
            return context!!.contentResolver.openFileDescriptor(fileUri, mode)
        } catch (e: Exception) {
            Klog.e("openFileDescriptor() failed", e)
            throw e
        }
    }

    override fun onCreate() = true
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?) = null
    override fun getType(uri: Uri) = null
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?) = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?) = 0
}
