package com.crossbowffs.usticker

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceFragment
import android.provider.DocumentsContract
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

class SettingsFragment : PreferenceFragment() {
    companion object {
        private const val REQUEST_SELECT_STICKER_DIR = 2333
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        if (requestCode != REQUEST_SELECT_STICKER_DIR) {
            return
        }

        val flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (data.flags and flags != flags) {
            Klog.e("FLAG_GRANT_PERSISTABLE_URI_PERMISSION or FLAG_GRANT_READ_URI_PERMISSION not set")
            Toast.makeText(activity, R.string.failed_to_obtain_read_permissions, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Prefs.setStickerDir(activity, data.data!!)
        } catch (e: SecurityException) {
            showStacktraceDialog(e)
            return
        }

        importStickers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)

        findPreference("pref_import_stickers").setOnPreferenceClickListener {
            importStickers()
            true
        }

        findPreference("pref_change_sticker_dir").setOnPreferenceClickListener {
            selectStickerDir()
            true
        }

        findPreference("pref_about_help").setOnPreferenceClickListener {
            showHelpDialog()
            true
        }

        findPreference("pref_about_github").setOnPreferenceClickListener {
            startGitHubActivity()
            true
        }

        findPreference("pref_about_version").apply {
            setSummary(getAppVersion())
            setOnPreferenceClickListener {
                showChangelogDialog()
                true
            }
        }

        if (!isKeyboardEnabled()) {
            showEnableKeyboardDialog()
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val component = ComponentName(context, StickerInputMethodService::class.java)
        return imm.enabledInputMethodList.any {
            it.component == component
        }
    }

    private fun getAppVersion(): String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun getIssueTemplate(stacktrace: String?): String {
        val appVersion = getAppVersion()
        val osVersion = "${Build.VERSION.RELEASE} (API${Build.VERSION.SDK_INT})"
        val fmt = if (stacktrace == null) {
            R.string.issue_template
        } else {
            R.string.issue_template_stacktrace
        }
        return getString(fmt, appVersion, osVersion, stacktrace)
    }

    private fun startInputMethodSettingsActivity() {
        val enableIntent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(enableIntent)
    }

    private fun startBrowserActivity(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun startGitHubActivity() {
        startBrowserActivity("https://github.com/apsun/uSticker")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(null, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun reportBug(stacktrace: String?) {
        val template = getIssueTemplate(stacktrace)
        copyToClipboard(template)
        Toast.makeText(activity, R.string.issue_template_copied, Toast.LENGTH_SHORT).show()
    }

    private fun getHtmlString(resId: Int): CharSequence {
        return Html.fromHtml(getString(resId))
    }

    private fun showEnableKeyboardDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.enable_keyboard_title)
            .setMessage(R.string.enable_keyboard_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startInputMethodSettingsActivity()
            }
            .setNeutralButton(R.string.ignore, null)
            .setCancelable(false)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.help)
            .setMessage(getHtmlString(R.string.help_text))
            .setPositiveButton(R.string.got_it, null)
            .setNeutralButton(R.string.report) { _, _ ->
                reportBug(null)
            }
            .show()
    }

    private fun showChangelogDialog() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.changelog)
            .setMessage(getHtmlString(R.string.changelog_text))
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun showStacktraceDialog(e: Throwable) {
        val stacktrace = Log.getStackTraceString(e)
        AlertDialog.Builder(activity)
            .setTitle(R.string.import_failed_title)
            .setMessage(getString(R.string.import_failed_message, e))
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.report) { _, _ ->
                reportBug(stacktrace)
            }
            .show()
    }

    private fun dismissDialog(dialog: Dialog) {
        // This is an ugly hack, but Android is stupid and I can't figure
        // out how to properly solve this so let's just do this to at least
        // stop getting exceptions
        try {
            dialog.dismiss()
        } catch (e: IllegalArgumentException) {
            // Don't care
        }
    }

    private fun onImportSuccess(dialog: Dialog, numStickers: Int) {
        dismissDialog(dialog)
        Klog.i("Successfully imported $numStickers stickers")
        val message = getString(R.string.import_success_toast, numStickers)
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun onImportFailed(dialog: Dialog, err: Exception) {
        Klog.e("Failed to import stickers", err)
        dismissDialog(dialog)

        if (err is SecurityException ||
            err is IllegalStateException ||
            err is IllegalArgumentException)
        {
            Toast.makeText(activity, R.string.sticker_dir_invalid, Toast.LENGTH_SHORT).show()
            selectStickerDir()
            return
        }

        showStacktraceDialog(err)
    }

    private fun selectStickerDir() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stickerDir = Prefs.getStickerDir(activity)
            if (stickerDir != null) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, stickerDir)
            }
        }

        try {
            startActivityForResult(intent, REQUEST_SELECT_STICKER_DIR)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.cannot_find_file_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importStickers() {
        val stickerDir = Prefs.getStickerDir(activity)
        if (stickerDir == null) {
            Klog.i("Sticker directory not configured")
            selectStickerDir()
            return
        }

        val dialog = ProgressDialog(activity).apply {
            setIndeterminate(true)
            setCancelable(false)
            setMessage(getString(R.string.importing_stickers))
            show()
        }

        StickerScanner(activity.contentResolver).executeWithCallback(stickerDir) { scanResult ->
            when (scanResult) {
                is Result.Err -> onImportFailed(dialog, scanResult.err)
                is Result.Ok -> StickerImporter().executeWithCallback(scanResult.value) { updateResult ->
                    when (updateResult) {
                        is Result.Err -> onImportFailed(dialog, updateResult.err)
                        is Result.Ok -> onImportSuccess(dialog, updateResult.value)
                    }
                }
            }
        }
    }
}
