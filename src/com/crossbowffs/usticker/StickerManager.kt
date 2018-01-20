package com.crossbowffs.usticker

import android.os.Environment
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.appindexing.builders.StickerBuilder
import java.io.File
import java.io.IOException

private const val PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
private const val STICKER_PATH = "Pictures/Stickers/"

object StickerManager {
    private val baseDir = File(Environment.getExternalStorageDirectory(), STICKER_PATH).canonicalFile

    /**
     * Converts a sticker "relative path" into a File object.
     * Returns null if the path is invalid, or the sticker file
     * does not exist.
     */
    fun getStickerFile(path: String): File? {
        // Convert relative path into an absolute,
        // normalized path
        val file: File
        try {
            file = File(baseDir, path).canonicalFile
        } catch (e: IOException) {
            return null
        }

        // Ensure that path refers to a file, and that
        // the file exists
        if (!file.isFile) {
            return null
        }

        // Check that the path didn't contain any kind
        // of sneaky directory traversal tricks by ensuring
        // the sticker file is a child of the base directory
        var tmp: File? = file
        while (tmp != null) {
            if (tmp == baseDir) {
                return file
            }
            tmp = tmp.parentFile
        }

        // No evil for you!
        return null
    }

    /**
     * Standard filesystem traversal algorithm, calls cb for each file
     * (not directory!) it finds. Does not yield/recurse into
     * files/directories with '.' as the first character in the name.
     */
    private fun traverseDirectory(path: String, dir: File, cb: (String, File) -> Unit) {
        dir.listFiles()
            .filter { it.name[0] != '.' }
            .forEach {
                if (it.isDirectory) {
                    traverseDirectory(path + "/" + it.name, it, cb)
                } else {
                    cb(path, it)
                }
            }
    }

    private fun scanStickers(): Map<String, List<File>> {
        val stickerMap = mutableMapOf<String, MutableList<File>>()
        traverseDirectory("", baseDir, { path, file ->
            stickerMap.getOrPut(path, ::mutableListOf).add(file)
        })
        return stickerMap
    }

    fun refreshStickers(cb: (Exception?) -> Unit) {
        val fbIndex = FirebaseAppIndex.getInstance()
        val stickerMap = scanStickers()
        val stickerPackList = mutableListOf<Indexable>()

        stickerMap.forEach { (path, files) ->
            // Create the pack first
            val stickerPack = Indexables.stickerPackBuilder()
                .setName(files[0].parentFile.name)
                .setImage("content://$PROVIDER_AUTHORITY/$path/${files[0].name}")
                .setUrl("usticker://sticker/$path")

            // Create all stickers in the pack
            val stickerList = mutableListOf<StickerBuilder>()
            files.forEach { file ->
                val sticker = Indexables.stickerBuilder()
                    .setName(file.name)
                    .setImage("content://$PROVIDER_AUTHORITY/$path/${file.name}")
                    .setUrl("usticker://sticker/$path/${file.name}")
                    .setIsPartOf(stickerPack)
                stickerList.add(sticker)
            }

            stickerPack.setHasSticker(*stickerList.toTypedArray())
            stickerPackList.add(stickerPack.build())
        }

        fbIndex.removeAll()
        fbIndex.update(*stickerPackList.toTypedArray())
            .addOnSuccessListener { cb(null) }
            .addOnFailureListener { cb(it) }
    }
}
