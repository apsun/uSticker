package com.crossbowffs.usticker

import android.net.Uri

/**
 * Represents a collection of stickers with an associated filesystem path.
 */
class StickerPack(
    val path: String,
    val stickers: List<Sticker>)
{
    /**
     * Returns a unique URI representing this sticker pack in Firebase.
     * It has no real meaning (we could replace it with a random UUID),
     * the only requirement is that it is unique per sticker pack.
     */
    fun getFirebaseUri(): Uri {
        return Uri.Builder()
            .scheme("usticker")
            .authority("sticker")
            .path(path)
            .build()
    }
}