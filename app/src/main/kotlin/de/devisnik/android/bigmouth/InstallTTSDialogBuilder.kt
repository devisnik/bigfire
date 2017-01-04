package de.devisnik.android.bigmouth

import android.app.AlertDialog
import android.content.Intent
import android.speech.tts.TextToSpeech

class InstallTTSDialogBuilder(private val bitesChat: BitesChat) : AlertDialog.Builder(bitesChat) {

    init {
        setTitle(R.string.dialog_install_tts_title)
        setMessage(R.string.dialog_install_tts_message)
        setPositiveButton(R.string.dialog_install_tts_button) { dialog, which ->
            bitesChat.startActivity(Intent().apply {
                action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
