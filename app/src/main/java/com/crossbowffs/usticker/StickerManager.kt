package com.crossbowffs.usticker

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import android.text.TextUtils
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Sticker "manager" - responsible for finding and loading
 * sticker files from the filesystem.
 */
object StickerManager {
    private const val PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
    private const val DEFAULT_STICKER_DIR = "Pictures/Stickers/"
    private val STICKER_EXTENSIONS = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

    /**
     * Checks whether a file (directory) is a child of a
     * given parent directory.
     */
    private fun isChildPath(parent: File, child: File): Boolean {
        var tmp: File? = child
        while (tmp != null) {
            if (tmp == parent) {
                return true
            }
            tmp = tmp.parentFile
        }
        return false
    }

    /**
     * Returns the configured sticker directory path.
     */
    private fun getConfigStickerDir(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("pref_sticker_dir", null) ?: DEFAULT_STICKER_DIR
    }

    /**
     * Initializes the configured sticker directory to its default value,
     * if a value has not already been set.
     */
    fun initStickerDir(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (TextUtils.isEmpty(prefs.getString("pref_sticker_dir", null))) {
            prefs.edit().putString("pref_sticker_dir", DEFAULT_STICKER_DIR).apply()
        }
    }

    /**
     * Validates the specified sticker directory path.
     */
    fun validateStickerDir(stickerDir: String): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val file = File(sdcard, stickerDir).canonicalFile
        if (!isChildPath(sdcard, file)) {
            throw IOException("Invalid path: $stickerDir")
        }
        if (!file.exists()) {
            throw FileNotFoundException("Directory not found: $stickerDir")
        }
        if (!file.isDirectory) {
            throw IOException("Directory is a file: $stickerDir")
        }
        return file
    }

    /**
     * Returns the sticker base directory. May throw an exception if the
     * directory does not exist or the path in preferences is invalid.
     */
    private fun getStickerDir(context: Context): File {
        val stickerDir = getConfigStickerDir(context)
        return validateStickerDir(stickerDir)
    }

    /**
     * Converts a sticker "relative path" into a File object.
     * Returns null if the path is invalid, or the sticker file
     * does not exist.
     */
    fun getStickerFile(context: Context, path: String): File? {
        val stickerDir = try {
            getStickerDir(context)
        } catch (e: Exception) {
            Klog.e("Sticker directory is invalid", e)
            return null
        }

        // Convert relative path into an absolute,
        // normalized path
        val file = try {
            File(stickerDir, path).canonicalFile
        } catch (e: IOException) {
            Klog.e("Could not normalize path: $path")
            return null
        }

        // Ensure that path refers to a file, and that
        // the file exists
        if (!file.isFile) {
            Klog.e("File not found or is a directory: $path")
            return null
        }

        // Check that the path didn't contain any kind
        // of sneaky directory traversal tricks by ensuring
        // the sticker file is a child of the base directory
        if (!isChildPath(stickerDir, file)) {
            Klog.e("Invalid/malicious path: $path")
            return null
        }

        return file
    }

    /**
     * Standard filesystem traversal algorithm, calls cb for each file
     * (not directory!) it finds. Does not yield/recurse into
     * files/directories with '.' as the first character in the name.
     * Only finds JPG/PNG/GIF/BMP files.
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
    private fun scanStickers(context: Context): Map<String, List<File>> {
        val stickerMap = mutableMapOf<String, MutableList<File>>()
        val stickerDir = getStickerDir(context)
        traverseDirectory(".", stickerDir) { packPath, stickerFile ->
            Klog.i("Discovered sticker: $packPath/${stickerFile.name}")
            stickerMap.getOrPut(packPath, ::mutableListOf).add(stickerFile)
        }
        return stickerMap
    }

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
     * Refreshes the Firebase sticker index. Calls cb when complete;
     * the argument will be null if the indexing was successful.
     */
    fun refreshStickerIndex(context: Context, cb: (Exception?) -> Unit) {
        Klog.i("Scanning stickers...")
        val stickerMap = try {
            scanStickers(context)
        } catch (e: Exception) {
            cb(e)
            return
        }

        val stickerPackList = stickerMap.map { (packPath, stickerFiles) ->
            val packName = stickerFiles[0].parentFile.name
            val packUrl = "usticker://sticker/$packPath"

            // Create all stickers in the pack
            val stickers = stickerFiles.map { stickerFile ->
                Indexables.stickerBuilder()
                    .setName(stickerFile.name)
                    .setImage("content://$PROVIDER_AUTHORITY/$packPath/${stickerFile.name}")
                    .setKeywords(*(arrayOf(packName) + getKeywords(stickerFile.nameWithoutExtension)))
                    .setUrl("usticker://sticker/$packPath/${stickerFile.name}")
            }

            // Then create the sticker pack
            val pack = Indexables.stickerPackBuilder()
                .setName(packName)
                .setImage("content://$PROVIDER_AUTHORITY/$packPath/${stickerFiles[0].name}")
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

        Klog.i("Updating Firebase index...")
        val fbIndex = FirebaseAppIndex.getInstance()
        fbIndex.removeAll()
        IndexableUpdate(fbIndex, stickerPackList, cb).run()
    }
}

/**
 * Dumb workaround for the MAX_INDEXABLES_TO_BE_UPDATED_IN_ONE_CALL limit.
 */
class IndexableUpdate(
    private val fbIndex: FirebaseAppIndex,
    private val indexableList: List<Indexable>,
    private val cb: (Exception?) -> Unit)
{
    private var index = 0
    fun run() {
        val step = Math.min(indexableList.size - index, Indexable.MAX_INDEXABLES_TO_BE_UPDATED_IN_ONE_CALL)
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
