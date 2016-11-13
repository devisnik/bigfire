package de.devisnik.android.bigmouth.channels

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import de.devisnik.android.bigmouth.data.SoundBite
import de.devisnik.android.bigmouth.speaker.SpeakerUseCase

class ChannelService : Service() {

    private val channelBinder = MyBinder()


    override fun onBind(intent: Intent?): IBinder {
        return channelBinder
    }

    inner class MyBinder : Binder() {
        internal val service: ChannelService
            get() = this@ChannelService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelName = intent?.getStringExtra(EXTRA_CHANNEL_NAME)
        if (channelName != null) {
            val useCase = SpeakerUseCase(channelName)
            useCase
                    .soundStream()
                    .subscribe {
                        makeSound(it)
                    }
        }


        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteChannel()
    }

    private fun deleteChannel() {
        val androidId = getAndroidId()
        val users = FirebaseDatabase.getInstance().getReference("users")
        users.child(androidId).removeValue()
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
    }

    private fun makeSound(sound: SoundBite?) {
        if (sound != null) {
            Toast.makeText(this, sound.message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        val EXTRA_CHANNEL_NAME = "channel_name"

        fun startIntent(context: Context, channelName: String): Intent {
            val service = intent(context)
            service.putExtra(EXTRA_CHANNEL_NAME, channelName)
            return service
        }

        fun stopIntent(context: Context) : Intent {
            return intent(context)
        }

        private fun intent(context: Context): Intent {
            val service = Intent(context, ChannelService::class.java)
            return service
        }
    }
}

