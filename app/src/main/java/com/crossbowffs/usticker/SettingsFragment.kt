package com.crossbowffs.usticker

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.text.Html
import android.util.Log
import android.widget.Toast
import java.io.FileNotFoundException

class SettingsFragment : PreferenceFragment() {
    companion object {
        private const val TWITTER_URL = "https://twitter.com/crossbowffs"
        private const val GITHUB_URL = "https://github.com/apsun/uSticker"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StickerManager.initStickerDir(activity)
        addPreferencesFromResource(R.xml.settings)

        findPreference("pref_import_stickers").setOnPreferenceClickListener {
            importStickers()
            true
        }

        findPreference("pref_sticker_dir").setOnPreferenceChangeListener { _, newValue ->
            val stickerDir = newValue as String
            try {
                StickerManager.validateStickerDir(stickerDir)
                true
            } catch (e: Exception) {
                showInvalidStickerDirDialog(newValue, e)
                false
            }
        }

        findPreference("pref_about_help").setOnPreferenceClickListener {
            showHelpDialog()
            true
        }

        findPreference("pref_about_developer").setOnPreferenceClickListener {
            startBrowserActivity(TWITTER_URL)
            true
        }

        findPreference("pref_about_github").setOnPreferenceClickListener {
            startBrowserActivity(GITHUB_URL)
            true
        }

        findPreference("pref_about_version").apply {
            setSummary("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            setOnPreferenceClickListener {
                showChangelogDialog()
                true
            }
        }
    }

    private fun startBrowserActivity(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun getHtmlString(resId: Int): CharSequence {
        return Html.fromHtml(getString(resId))
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.help)
            .setMessage(getHtmlString(R.string.help_text))
            .setPositiveButton(R.string.got_it, null)
            .show()
    }

    private fun showChangelogDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.changelog)
            .setMessage(getHtmlString(R.string.changelog_text))
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(null, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showInvalidStickerDirDialog(dir: String, e: Exception) {
        val message = if (e is FileNotFoundException) {
            getString(R.string.sticker_dir_not_found, dir)
        } else {
            e.message
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.invalid_sticker_dir)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showStacktraceDialog(e: Throwable) {
        val stacktrace = Log.getStackTraceString(e)
        AlertDialog.Builder(activity)
            .setTitle(R.string.import_failed_title)
            .setMessage(stacktrace)
            .setNeutralButton(R.string.copy) { _, _ ->
                copyToClipboard(stacktrace)
                Toast.makeText(activity, R.string.stacktrace_copied, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun onImportSuccess(dialog: Dialog, numStickers: Int) {
        dialog.dismiss()
        Klog.i("Successfully imported $numStickers stickers")
        val message = getString(R.string.import_success_toast, numStickers)
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun onImportFailed(dialog: Dialog, err: Exception) {
        dialog.dismiss()
        Klog.e("Failed to import stickers", err)
        showStacktraceDialog(err)
    }

    private fun importStickers() {
        val stickerDir = try {
            StickerManager.getStickerDir(activity)
        } catch (e: Exception) {
            showInvalidStickerDirDialog(StickerManager.getConfigStickerDir(activity), e)
            return
        }

        val dialog = ProgressDialog(activity).apply {
            setIndeterminate(true)
            setCancelable(false)
            setMessage(getString(R.string.importing_stickers))
            show()
        }

        StickerScanner().executeWithCallback(stickerDir) { scanResult ->
            when (scanResult) {
                is Result.Err -> onImportFailed(dialog, scanResult.err)
                is Result.Ok -> FirebaseIndexUpdater().executeWithCallback(scanResult.value) { updateResult ->
                    when (updateResult) {
                        is Result.Err -> onImportFailed(dialog, updateResult.err)
                        is Result.Ok -> onImportSuccess(dialog, updateResult.value)
                    }
                }
            }
        }
    }
}
