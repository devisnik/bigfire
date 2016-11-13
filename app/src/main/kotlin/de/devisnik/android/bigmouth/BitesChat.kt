package de.devisnik.android.bigmouth

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.devisnik.android.bigmouth.channels.Channel
import de.devisnik.android.bigmouth.data.SoundBite
import de.devisnik.android.bigmouth.speaker.Speaker
import de.devisnik.android.bigmouth.speaker.SpeakerPresenter
import kotlinx.android.synthetic.main.activity_bites_chat.*
import java.lang.Float.parseFloat
import java.util.*

class BitesChat : AppCompatActivity(), OnInitListener, Speaker {
    private var itsTextToSpeech: TextToSpeech? = null
    private var itsRestoreAudio = -1

    private lateinit var speakerPresenter: SpeakerPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        LOGGER.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bites_chat)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val database = FirebaseDatabase.getInstance()

        database.getReference("users")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val users = snapshot.children
                                .map { it.getValue(Channel::class.java) }
                                .toList()
                        initChannels(database, users)
                    }

                    override fun onCancelled(p0: DatabaseError) = Unit
                })


        val currentUser = FirebaseAuth.getInstance().currentUser!!.displayName!!
        speakerPresenter = SpeakerPresenter(currentUser)
    }

    private fun initChannels(database: FirebaseDatabase, channels: List<Channel>) {
        chat_input_channel_chooser.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, channels.map(Channel::name))

        chat_send.setOnClickListener {
            val user = channels[chat_input_channel_chooser.selectedItemPosition]

            val task = object : AsyncTask<String, Void, String>() {
                override fun doInBackground(vararg text: String): String {
                    val from = getPrefValue(R.string.pref_language).substring(0..1)
                    val to = user.language.substring(0..1)
                    return Translator().translate(text[0], from = from, to = to)
                }

                override fun onPostExecute(result: String) {
                    val bite = createBiteWithDefaults(result).copy(language = user.language)
                    val channelRef = database.getReference(user.name)
                    channelRef.setValue(bite)
                }
            }
            task.execute(chat_input.text.toString())
        }
    }

    override fun makeSound(sound: SoundBite) {
        speak(sound)
    }

    private fun display(value: SoundBite) {
        chat_message.text = value.message
    }

    override fun onStart() {
        super.onStart()
        startCheckTTS()
        speakerPresenter.bind(this)
    }

    override fun onStop() {
        itsTextToSpeech?.shutdown()
        itsTextToSpeech = null
        speakerPresenter.unbind()
        super.onStop()
    }

    private fun startCheckTTS() {
        startActivityForResult(Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), CHECK_TTS)
    }

    private fun createBiteWithDefaults(message: String): SoundBite = SoundBite(
            language = getPrefValue(R.string.pref_language),
            pitch = getPrefValue(R.string.pref_pitch),
            speed = getPrefValue(R.string.pref_speed),
            volume = getPrefValue(R.string.pref_volume),
            message = message,
            title = message
    )

    private fun getPrefValue(resourceId: Int): String {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(resourceId), null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            CHECK_TTS -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    itsTextToSpeech = TextToSpeech(this, this)
                } else {
                    InstallTTSDialogBuilder(this).create().show()
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

    private fun speak(bite: SoundBite, ref: DatabaseReference) {
        if (itsTextToSpeech == null) {
            LOGGER.e("TTS not properly set up!")
            return
        }
        itsTextToSpeech?.setOnUtteranceCompletedListener {
            adjustAudio(itsRestoreAudio)
            ref.removeValue()
        }

        if (getString(R.string.volume_value_no_adjust) == bite.volume) {
            itsRestoreAudio = currentAudio
        } else {
            itsRestoreAudio = adjustAudio(Math.round(maxAudio * parseFloat(bite.volume)))
        }
        val params = HashMap<String, String>()
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "text")
        itsTextToSpeech?.setPitch(parseFloat(bite.pitch))
        itsTextToSpeech?.language = parseLocale(bite.language)
        itsTextToSpeech?.setSpeechRate(parseFloat(bite.speed))
        itsTextToSpeech?.speak(bite.message, TextToSpeech.QUEUE_FLUSH, params)
    }

    private fun speak(bite: SoundBite) {
        val tts = itsTextToSpeech
        if (tts == null) {
            LOGGER.e("TTS not properly set up!")
            return
        }

        itsRestoreAudio = if (getString(R.string.volume_value_no_adjust) == bite.volume) {
            currentAudio
        } else {
            adjustAudio(Math.round(maxAudio * parseFloat(bite.volume)))
        }

        tts.run {
            setOnUtteranceCompletedListener { adjustAudio(itsRestoreAudio) }
            setPitch(parseFloat(bite.pitch))
            language = parseLocale(bite.language)
            setSpeechRate(parseFloat(bite.speed))
            speak(bite.message, TextToSpeech.QUEUE_FLUSH, hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to "text"))
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this).inflate(R.menu.bites_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            startActivity(Intent(this, BitePreferences::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    internal fun stopSpeaking() {
        if (itsTextToSpeech != null && itsTextToSpeech!!.isSpeaking) {
            itsTextToSpeech?.stop()
            adjustAudio(itsRestoreAudio)
        }
    }

    companion object {
        private val CHECK_TTS = 11
        private val LOGGER = Logger(BitesChat::class.java)
    }
}
