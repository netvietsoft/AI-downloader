package com.nextaitechnology.antidetect.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper

/**
 * Activity Chọn ngôn ngữ (LanguageActivity)
 * Standalone activity for language selection.
 */
class LanguageActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsActivity.start(this)
        finish()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LanguageActivity::class.java)
            context.startActivity(intent)
        }
    }
}
