package de.devisnik.android.bigmouth


import android.app.AlertDialog.Builder
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.devisnik.android.bigmouth.data.SoundBite
import kotlinx.android.synthetic.main.activity_bites_chat.*
import java.util.*

class BitesChat : AppCompatActivity(), OnInitListener, ValueEventListener {

    private var itsTextToSpeech: TextToSpeech? = null
    private var itsRestoreAudio = -1

    private inner class InstallTTSDialogBuilder : Builder(this@BitesChat) {

        init {
            setTitle(R.string.dialog_install_tts_title)
            setMessage(R.string.dialog_install_tts_message)
            setPositiveButton(R.string.dialog_install_tts_button) { dialog, which ->
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    startActivity(installIntent)
                } catch (e: ActivityNotFoundException) {
                    LOGGER.e("while starting TTS installer.", e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LOGGER.d("onCreate")
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        setContentView(R.layout.activity_bites_chat)

        val database = FirebaseDatabase.getInstance()

        chat_input_channel_chooser.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CHANNELS)
        chat_output_channel_chooser.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CHANNELS)
        registerSpeaker(CHANNELS[chat_output_channel_chooser.selectedItemPosition], database)

        chat_output_channel_chooser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                CHANNELS.withIndex()
                        .filter { it.index != index }
                        .forEach { unregister(it.value, database) }

                registerSpeaker(CHANNELS[index], database)
            }
        }

        chat_send.setOnClickListener {
            val message = chat_input.text.toString()
            val channel = CHANNELS[chat_input_channel_chooser.selectedItemPosition]
            val ref = database.getReference(channel)
            ref.setValue(message)
        }

    }

    override fun onCancelled(p0: DatabaseError?) {

    }

    override fun onDataChange(p0: DataSnapshot?) {
        val value = p0!!.getValue(String::class.java)

        val message = if (value == null) {
            ""
        } else {
            value.toString()
        }
        val bite = createBiteWithDefaults(message)
        speak(bite)
    }

    private fun registerSpeaker(channel: String, database: FirebaseDatabase) {
        val myRef = database.getReference(channel)
        myRef.addValueEventListener(this)
    }

    private fun unregister(channel : String, database: FirebaseDatabase) {
        val myRef = database.getReference(channel)
        myRef.removeEventListener(this)
    }

    override fun onStart() {
        LOGGER.d("onStart")
        super.onStart()
        startCheckTTS()
    }

    override fun onRestart() {
        LOGGER.d("onRestart")
        super.onRestart()
    }

    override fun onStop() {
        LOGGER.d("onStop")
        if (itsTextToSpeech != null) {
            itsTextToSpeech!!.shutdown()
        }
        itsTextToSpeech = null
        super.onStop()
    }

    private fun startCheckTTS() {
        val ttsIntent = Intent()
        ttsIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(ttsIntent, CHECK_TTS)
    }

    private fun createBiteWithDefaults(message: String): SoundBite {
        return SoundBite(id = 0,
                language = getPrefValue(R.string.pref_language),
                pitch = getPrefValue(R.string.pref_pitch),
                speed = getPrefValue(R.string.pref_speed),
                volume = getPrefValue(R.string.pref_volume),
                message = message, title = message)
    }

    private fun getPrefValue(resourceId: Int): String {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(resourceId),
                null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        LOGGER.d("onActivityResult")
        when (requestCode) {
            CHECK_TTS -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    itsTextToSpeech = TextToSpeech(this, this)
                } else {
                    showDialog(DIALOG_INSTALL_TTS)
                }
                return
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onPause() {
        stopSpeaking()
        super.onPause()
    }

    override fun onInit(status: Int) {
        LOGGER.d("onInit")
    }

    private fun parseLocale(localeString: String): Locale {
        val splitData = localeString.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return Locale(splitData[0], splitData[1])
    }

    private fun speak(bite: SoundBite) {
        if (itsTextToSpeech == null) {
            LOGGER.e("TTS not properly set up!")
            return
        }
        itsTextToSpeech!!.setOnUtteranceCompletedListener { adjustAudio(itsRestoreAudio) }
        if (getString(R.string.volume_value_no_adjust) == bite.volume) {
            itsRestoreAudio = currentAudio
        } else {
            itsRestoreAudio = adjustAudio(Math.round(maxAudio * java.lang.Float.parseFloat(bite.volume)))
        }
        val params = HashMap<String, String>()
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "text")
        itsTextToSpeech!!.setPitch(java.lang.Float.parseFloat(bite.pitch))
        itsTextToSpeech!!.language = parseLocale(bite.language)
        itsTextToSpeech!!.setSpeechRate(java.lang.Float.parseFloat(bite.speed))
        itsTextToSpeech!!.speak(bite.message, TextToSpeech.QUEUE_FLUSH, params)
    }

    private val maxAudio: Int
        get() {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

    private val currentAudio: Int
        get() {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

    private fun adjustAudio(value: Int): Int {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val oldVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
        return oldVolume
    }

    override fun onCreateDialog(id: Int): Dialog {
        if (id == DIALOG_INSTALL_TTS) {
            return InstallTTSDialogBuilder().create()
        }
        return super.onCreateDialog(id)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        MenuInflater(this).inflate(R.menu.bites_context, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    internal fun stopSpeaking() {
        if (itsTextToSpeech == null || !itsTextToSpeech!!.isSpeaking) {
            return
        }
        itsTextToSpeech!!.stop()
        adjustAudio(itsRestoreAudio)
    }

    companion object {

        private val DIALOG_INSTALL_TTS = 555
        private val CHECK_TTS = 11

        private val LOGGER = Logger(BitesChat::class.java)

        private val CHANNELS = arrayOf("channel_1", "channel_2", "channel_3")
    }
}
