package de.devisnik.android.bigmouth.channels

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.devisnik.android.bigmouth.R
import de.devisnik.android.bigmouth.SignInActivity
import kotlinx.android.synthetic.main.activity_edit_channel.*

class EditChannelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_channel)

        setupViews()

        edit_channel_start.setOnClickListener {
            val channelName = edit_channel_name.text.toString()
            if (channelName.isNotEmpty()) {
                createChannel(channelName)
                startChannel(channelName)
                setupViews()
            }
        }

        edit_channel_stop.setOnClickListener {
            stopCurrentChannel()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            setupViews()
        }
    }

    private fun setupViews() {
        val isChannelServiceRunning = isServiceRunning(ChannelService::class.java.name!!)
        edit_channel_name.isEnabled = !isChannelServiceRunning

        if (isChannelServiceRunning) {
            edit_channel_start.visibility = View.GONE
            edit_channel_stop.visibility = View.VISIBLE
        } else {
            edit_channel_start.visibility = View.VISIBLE
            edit_channel_stop.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().addAuthStateListener({ firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                val signInIntent = SignInActivity.createIntent(this)
                startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
            }
        })
    }

    private fun stopCurrentChannel() {
        stopService(ChannelService.stopIntent(this))
    }

    private fun startChannel(channelName: String) {
        val serviceIntent = ChannelService.createIntent(this, channelName)
        startService(serviceIntent)
    }

    private fun createChannel(channelName: String) {
        val id = getAndroidId()

        val users = FirebaseDatabase.getInstance().getReference("users")
        val channel = Channel(name = channelName, language = "en-GB")
        users.child(id).setValue(channel)
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
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

    companion object {
        private val REQUEST_CODE_SIGN_IN = 100
    }
}
