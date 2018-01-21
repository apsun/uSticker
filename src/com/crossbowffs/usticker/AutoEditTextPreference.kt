package com.crossbowffs.usticker

import android.content.Context
import android.preference.EditTextPreference
import android.util.AttributeSet

/**
 * EditTextPreference that displays the preference value
 * in the summary field.
 */
class AutoEditTextPreference(context: Context, attrs: AttributeSet)
    : EditTextPreference(context, attrs) {

    override fun setText(text: String?) {
        super.setText(text)
        summary = text
    }

    // ... Wait what, that's it? Yes, that's it! Wasn't that easy?
}
