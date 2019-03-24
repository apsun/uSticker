package com.crossbowffs.usticker

import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import java.io.File

/**
 * Updates the Firebase index with a new sticker map.
 */
class FirebaseIndexUpdater {
    private val fbIndex = FirebaseAppIndex.getInstance()

    /**
     * Generates a list of keywords for the sticker based on its
     * file name. Currently, this splits on all non-alphanumeric
     * characters and alpha-numeric boundaries.
     */
    private fun getKeywords(fileName: String): Array<String> {
        // First, split on any non-alphanumeric characters
        val nonSplitAlphaNum = fileName
            .split(Regex("[\\W_]"))
            .filter { it.isNotBlank() }

        // Then, split patterns like abc123def to [abc, 123, def]
        val splitAlphaNum = nonSplitAlphaNum.map {
            Regex("[0-9]+|[a-zA-Z]+")
                .findAll(it)
                .map { it.value }
                .toList()
        }.flatten()

        return splitAlphaNum.toTypedArray()
    }

    /**
     * Converts a map of sticker files into a flattened list of
     * Indexable objects to pass to the Firebase indexer.
     */
    private fun stickerMapToIndexables(stickerMap: Map<String, List<File>>): List<Indexable> {
        return stickerMap.map { (packPath, stickerFiles) ->
            val packName = stickerFiles[0].parentFile.name
            val packUrl = "usticker://sticker/$packPath"

            // Create all stickers in the pack
            val stickers = stickerFiles.map { stickerFile ->
                Indexables.stickerBuilder()
                    .setName(stickerFile.name)
                    .setImage("content://${StickerManager.PROVIDER_AUTHORITY}/$packPath/${stickerFile.name}")
                    .setKeywords(*(getKeywords(packName) + getKeywords(stickerFile.nameWithoutExtension)))
                    .setUrl("usticker://sticker/$packPath/${stickerFile.name}")
            }

            // Then create the sticker pack
            val pack = Indexables.stickerPackBuilder()
                .setName(packName)
                .setImage("content://${StickerManager.PROVIDER_AUTHORITY}/$packPath/${stickerFiles[0].name}")
                .setUrl(packUrl)
                .setHasSticker(*stickers.toTypedArray())
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()

            // Finally add the sticker pack attribute to the stickers
            stickers.map { it
                .setIsPartOf(Indexables.stickerPackBuilder()
                    .setName(packName)
                    .setUrl(packUrl))
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()
            } + listOf(pack)
        }.flatten()
    }

    /**
     * Performs batched updates of the Firebase sticker index.
     * Calls the provided callback on completion.
     */
    private fun addIndexables(indexableList: List<Indexable>, offset: Int, callback: (Result<Int>) -> Unit) {
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
     * Replaces all indexed stickers with the given sticker map.
     */
    fun executeWithCallback(stickerMap: Map<String, List<File>>, callback: (Result<Int>) -> Unit) {
        val indexables = stickerMapToIndexables(stickerMap)
        fbIndex.removeAll()
            .addOnSuccessListener { addIndexables(indexables, 0, callback) }
            .addOnFailureListener { callback(Result.Err(it)) }
    }
}
