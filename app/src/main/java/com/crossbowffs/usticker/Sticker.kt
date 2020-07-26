package com.crossbowffs.usticker

import android.net.Uri

/**
 * Represents a sticker file with an associated URI and filesystem name.
 */
class Sticker(
    private val documentId: String,
    val name: String)
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

    /**
     * Returns a unique URI representing this sticker in Firebase.
     * It has no real meaning (we could replace it with a random UUID),
     * the only requirement is that it is unique per sticker.
     */
    fun getFirebaseUri(stickerPackUri: Uri): Uri {
        return stickerPackUri.buildUpon().appendPath(name).build()
    }
}