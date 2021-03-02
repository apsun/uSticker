package com.crossbowffs.usticker

import android.net.Uri

/**
 * Represents a sticker file with an associated URI and filesystem name.
 */
class Sticker(
    private val documentId: String,
    val name: String,
    val mimeType: String)
{
    /**
     * Returns the URI that is used to load sticker files from our
     * provider. This is an externally visible content:// URI.
     */
    fun getFileUri(): Uri {
        return Uri.Builder()
            .scheme("content")
            .authority(BuildConfig.APPLICATION_ID + ".provider")
            .appendPath(documentId)
            .build()
    }
}