package com.crossbowffs.usticker

import java.io.File

/**
 * Scans stickers starting from a given parent directory.
 */
class StickerScanner : FailableAsyncTask<File, Map<String, List<File>>>() {
    companion object {
        private val STICKER_EXTENSIONS = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    /**
     * Standard filesystem traversal algorithm, calls cb for each file
     * (not directory!) it finds. Does not yield/recurse into
     * files/directories with '.' as the first character in the name.
     */
    private fun traverseDirectory(packPath: String, dir: File, cb: (String, File) -> Unit) {
        dir.listFiles()
            ?.filter { it.name[0] != '.' }
            ?.forEach {
                if (it.isDirectory) {
                    traverseDirectory(packPath + "/" + it.name, it, cb)
                } else if (STICKER_EXTENSIONS.contains(it.extension.toLowerCase())) {
                    cb(packPath, it)
                }
            }
    }

    /**
     * Returns a collection of sticker packs as a map {dir -> list<file>}.
     * Each directory is guaranteed to have at least one file. The ordering
     * is not guaranteed, however.
     */
    override fun run(arg: File): Map<String, List<File>> {
        val stickerMap = mutableMapOf<String, MutableList<File>>()
        traverseDirectory(".", arg) { packPath, stickerFile ->
            Klog.i("Discovered sticker: $packPath/${stickerFile.name}")
            stickerMap.getOrPut(packPath, ::mutableListOf).add(stickerFile)
        }
        return stickerMap
    }
}
