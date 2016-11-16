package de.devisnik.android.bigmouth.channels

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.provider.Settings
import android.support.v7.app.NotificationCompat
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import de.devisnik.android.bigmouth.R
import de.devisnik.android.bigmouth.data.SoundBite
import de.devisnik.android.bigmouth.speaker.SpeakerUseCase


class ChannelService : Service() {

    private val channelBinder = MyBinder()
    private var notificationManager : NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder {
        return channelBinder
    }

    inner class MyBinder : Binder() {
        internal val service: ChannelService
            get() = this@ChannelService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        showNotification()

        val channelName = intent?.getStringExtra(EXTRA_CHANNEL_NAME)
        if (channelName != null) {
            val useCase = SpeakerUseCase(channelName)
            useCase
                    .soundStream()
                    .subscribe {
                        makeSound(it)
                    }
        }
        return Service.START_REDELIVER_INTENT
    }


    private fun showNotification() {
        notificationManager?.notify(NOTIFICATION, notification())
    }

    private fun notification(): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, EditChannelActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        return NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.bigmouth2)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setTicker("Edit Channel")
                .setContentText("Click to open EditChannel Screen")
                .build()
    }


    override fun onDestroy() {
        super.onDestroy()
        deleteChannel()
        notificationManager?.cancelAll()
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
        private val EXTRA_CHANNEL_NAME = "channel_name"
        private val NOTIFICATION = 1

        fun createIntent(context: Context, channelName: String): Intent {
            val service = intent(context)
            service.putExtra(EXTRA_CHANNEL_NAME, channelName)
            return service
        }

        fun stopIntent(context: Context): Intent {
            return intent(context)
        }

        private fun intent(context: Context): Intent {
            val service = Intent(context, ChannelService::class.java)
            return service
        }
    }
}

