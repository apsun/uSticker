package com.crossbowffs.usticker

import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables

/**
 * Updates the Firebase index with a new list of sticker packs.
 */
class FirebaseIndexUpdater {
    companion object {
        private val NON_ALPHA_NUM_CHAR_REGEX = Regex("[^\\p{N}\\p{L}]")
        private val ALPHA_OR_NUM_REGEX = Regex("\\p{N}+|\\p{L}+")
    }

    private val fbIndex = FirebaseAppIndex.getInstance()

    /**
     * Generates a list of keywords given a filename.
     * Currently, this splits on all non-alphanumeric
     * characters and alpha-numeric boundaries.
     */
    private fun getKeywords(fileName: String): List<String> {
        // First, split on any non-alphanumeric characters
        val nonSplitAlphaNum = fileName
            .split(NON_ALPHA_NUM_CHAR_REGEX)
            .filter(String::isNotBlank)

        // Then, split patterns like abc123def to [abc, 123, def]
        return nonSplitAlphaNum.flatMap {
            ALPHA_OR_NUM_REGEX
                .findAll(it)
                .map(MatchResult::value)
                .toList()
        }
    }

    /**
     * Trims off the extension from a filename (e.g. foo.bar -> foo).
     */
    private fun getNameWithoutExtension(fileName: String): String {
        return fileName.substringBeforeLast('.')
    }

    /**
     * Converts a list of sticker packs into a list of
     * Indexable objects to pass to the Firebase indexer.
     */
    private fun stickerPacksToIndexables(stickerPacks: List<StickerPack>): List<Indexable> {
        return stickerPacks.flatMap { stickerPack ->
            val packName = stickerPack.path.lastOrNull() ?: "Stickers"
            val packUri = stickerPack.getFirebaseUri()
            val packKeywords = stickerPack.path.flatMap(this::getKeywords)
            Klog.i("Importing $packName with ${stickerPack.stickers.size} stickers")

            // Create all stickers in the pack
            val stickers = stickerPack.stickers.map { sticker ->
                val keywords = packKeywords + getKeywords(getNameWithoutExtension(sticker.name))
                Indexables.stickerBuilder()
                    .setName(sticker.name)
                    .setImage(sticker.getFileUri().toString())
                    .setKeywords(*keywords.toTypedArray())
                    .setUrl(sticker.getFirebaseUri(packUri).toString())
            }

            // Then create the sticker pack
            val pack = Indexables.stickerPackBuilder()
                .setName(packName)
                .setImage(stickerPack.stickers[0].getFileUri().toString())
                .setUrl(packUri.toString())
                .setHasSticker(*stickers.toTypedArray())
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()

            // Finally add the sticker pack attribute to the stickers
            stickers.map { it
                .setIsPartOf(Indexables.stickerPackBuilder()
                    .setName(packName)
                    .setUrl(packUri.toString()))
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()
            } + listOf(pack)
        }
    }

    /**
     * Performs batched updates of the Firebase sticker index.
     * Calls the provided callback on completion with the number
     * of sticker files imported.
     */
    private fun addIndexables(
        indexableList: List<Indexable>,
        offset: Int,
        callback: (Result<Int>) -> Unit)
    {
        val step = Math.min(indexableList.size - offset, 250)
        if (step > 0) {
            fbIndex.update(*indexableList.subList(offset, offset + step).toTypedArray())
                .addOnSuccessListener { addIndexables(indexableList, offset + step, callback) }
                .addOnFailureListener { callback(Result.Err(it)) }
        } else {
            callback(Result.Ok(indexableList.size))
        }
    }

    /**
     * Replaces all indexed stickers with the given sticker pack list.
     */
    fun executeWithCallback(stickerPacks: List<StickerPack>, callback: (Result<Int>) -> Unit) {
        val indexables = stickerPacksToIndexables(stickerPacks)
        fbIndex.removeAll()
            .addOnSuccessListener { addIndexables(indexables, 0, callback) }
            .addOnFailureListener { callback(Result.Err(it)) }
    }
}
