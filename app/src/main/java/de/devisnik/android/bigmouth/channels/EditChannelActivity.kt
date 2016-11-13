package de.devisnik.android.bigmouth.channels

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.database.FirebaseDatabase
import de.devisnik.android.bigmouth.R
import kotlinx.android.synthetic.main.activity_edit_channel.*
import java.util.*

class EditChannelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_channel)

        val isChannelServiceRunning = isServiceRunning(ChannelService::class.java.simpleName!!)
        if (isChannelServiceRunning) {
            edit_channel_start.visibility = View.GONE
            edit_channel_stop.visibility = View.VISIBLE
        } else {
            edit_channel_start.visibility = View.VISIBLE
            edit_channel_stop.visibility = View.GONE
        }

        edit_channel_start.setOnClickListener {
            val channelName = edit_channel_name.text.toString()
            if (channelName.isNotEmpty()) {
                registerChannel(channelName)
                startChannel(channelName)
            }
        }

        edit_channel_stop.setOnClickListener {
            stopCurrentChannel()
        }
    }

    private fun stopCurrentChannel() {
        stopService(ChannelService.stopIntent(this))
    }

    private fun startChannel(channelName: String) {
        val serviceIntent = ChannelService.startIntent(this, channelName)
        startService(serviceIntent)
    }

    private fun registerChannel(channelName: String) {
        val users = FirebaseDatabase.getInstance().getReference("users")
        val channel = Channel(name = channelName, language = "en-GB")
        users.child(UUID.randomUUID().toString()).setValue(channel)
    }

    fun isServiceRunning(serviceClassName: String): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)

        for (runningServiceInfo in services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true
            }
        }
        return false
    }
}
