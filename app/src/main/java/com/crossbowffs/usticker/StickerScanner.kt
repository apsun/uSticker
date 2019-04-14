package com.crossbowffs.usticker

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Scans stickers starting from a given parent directory.
 */
class StickerScanner(private val resolver: ContentResolver) : FailableAsyncTask<Uri, List<StickerPack>>() {
    companion object {
        private val STICKER_MIME_TYPES = arrayOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp"
        )
    }

    /**
     * Standard filesystem traversal algorithm, calls cb for each file
     * (not directory!) it finds. Does not yield/recurse into
     * files/directories with '.' as the first character in the name.
     */
    private fun traverseDirectory(
        rootDir: Uri,
        packPath: MutableList<String>,
        dirDocumentId: String,
        cb: (Array<String>, Sticker) -> Unit)
    {
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(rootDir, dirDocumentId),
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE),
            null,
            null,
            null
        )?.use {
            while (it.moveToNext()) {
                val documentId = it.getString(0)
                val name = it.getString(1)
                val mimeType = it.getString(2)
                if (name.startsWith('.')) {
                    continue
                } else if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    packPath.add(name)
                    traverseDirectory(rootDir, packPath, documentId, cb)
                    packPath.removeAt(packPath.lastIndex)
                } else if (mimeType in STICKER_MIME_TYPES) {
                    cb(packPath.toTypedArray(), Sticker(documentId, name))
                }
            }
        }
    }

    /**
     * Returns a collection of sticker packs. Each sticker pack is guaranteed
     * to have at least one file. The ordering is not guaranteed, however.
     */
    override fun run(arg: Uri): List<StickerPack> {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(arg)
        val stickerMap = mutableMapOf<String, MutableList<Sticker>>()
        traverseDirectory(arg, mutableListOf(), rootDocumentId) { packPath, sticker ->
            stickerMap.getOrPut(packPath.joinToString("/"), ::mutableListOf).add(sticker)
        }
        return stickerMap.map { entry ->
            val packPath = if (entry.key == "") { emptyList() } else { entry.key.split('/') }
            StickerPack(packPath, entry.value)
        }
    }
}
