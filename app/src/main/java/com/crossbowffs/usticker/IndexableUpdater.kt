package com.crossbowffs.usticker

import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable

/**
 * Batches calls to Firebase to avoid the maximum parcel size limit (1MB).
 * The hard limit is 1000, though it seems it actually gets reached at around
 * 500 stickers in practice.
 */
class IndexableUpdater(
    private val fbIndex: FirebaseAppIndex,
    private val indexableList: List<Indexable>,
    private val cb: (Exception?) -> Unit)
{
    private var index = 0
    fun run() {
        val step = Math.min(indexableList.size - index, 250)
        if (step > 0) {
            fbIndex.update(*indexableList.subList(index, index + step).toTypedArray())
                .addOnSuccessListener { run() }
                .addOnFailureListener { cb(it) }
            index += step
        } else {
            cb(null)
        }
    }
}
