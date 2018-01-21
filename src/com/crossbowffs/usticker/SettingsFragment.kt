package com.crossbowffs.usticker

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.widget.Toast

class SettingsFragment : PreferenceFragment() {
    companion object {
        private const val TWITTER_URL = "https://twitter.com/crossbowffs"
        private const val GITHUB_URL = "https://github.com/apsun/uSticker"
        private const val EASTER_EGG_URL = "https://i.imgur.com/hh7TZfm.jpg"
    }

    private var easterEggCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StickerManager.initStickerDir(context)
        addPreferencesFromResource(R.xml.settings)

        findPreference("pref_refresh_index").setOnPreferenceClickListener {
            refreshStickerIndex()
            true
        }

        findPreference("pref_sticker_dir").setOnPreferenceChangeListener { _, newValue ->
            val stickerDir = newValue as String
            try {
                StickerManager.validateStickerDir(stickerDir)
                true
            } catch (e: Exception) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.invalid_sticker_dir)
                    .setMessage(e.message)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                false
            }
        }

        findPreference("pref_about_help").setOnPreferenceClickListener {
            showHelpDialog()
            true
        }

        findPreference("pref_about_credits").setOnPreferenceClickListener {
            startBrowserActivity(TWITTER_URL)
            true
        }

        findPreference("pref_about_github").setOnPreferenceClickListener {
            startBrowserActivity(GITHUB_URL)
            true
        }

        findPreference("pref_about_version").apply {
            summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            setOnPreferenceClickListener {
                if (++easterEggCounter == 7) {
                    easterEggCounter = 0
                    startBrowserActivity(EASTER_EGG_URL)
                }
                true
            }
        }
    }

    private fun startBrowserActivity(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(context)
            .setTitle(R.string.help)
            .setMessage(R.string.help_text)
            .setPositiveButton(R.string.got_it, null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(null, text)
        clipboard.primaryClip = clip
    }

    private fun showStacktraceDialog(e: Throwable) {
        val stacktrace = Log.getStackTraceString(e)
        AlertDialog.Builder(context)
            .setTitle(R.string.refresh_failed)
            .setMessage(stacktrace)
            .setNeutralButton(R.string.copy) { _, _ ->
                copyToClipboard(stacktrace)
                Toast.makeText(context, R.string.stacktrace_copied, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun refreshStickerIndex() {
        val dialog = ProgressDialog(context).apply {
            isIndeterminate = true
            setCancelable(false)
            setMessage(getString(R.string.refreshing_sticker_index))
            show()
        }

        StickerManager.refreshStickerIndex(context) { e ->
            dialog.dismiss()
            if (e == null) {
                Klog.i("Sticker index successfully refreshed")
                Toast.makeText(context, R.string.refresh_success_toast, Toast.LENGTH_SHORT).show()
            } else {
                Klog.e("Failed to refresh sticker index", e)
                Toast.makeText(context, R.string.refresh_failure_toast, Toast.LENGTH_SHORT).show()
                showStacktraceDialog(e)
            }
        }
    }
}
