package com.crossbowffs.usticker

import android.content.ClipDescription
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo

class StickerInputMethodService: InputMethodService() {
    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.view_keyboard, null)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        val supportedMimeTypes = info?.contentMimeTypes ?: emptyArray()
        // TODO
    }

    private fun sendSticker(sticker: Sticker) {
        val inputContentInfo = InputContentInfo(
            sticker.getFileUri(),
            ClipDescription(sticker.name, arrayOf(sticker.mimeType)),
            null
        )
        val flags = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        currentInputConnection.commitContent(inputContentInfo, flags, null)
    }
}