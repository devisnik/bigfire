package de.devisnik.android.bigmouth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import de.devisnik.android.bigmouth.channels.Channel

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile.
 */
class SignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleApiClient = GoogleApiClient.Builder(this).enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build()

        mAuth = FirebaseAuth.getInstance()

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Channel is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                // Channel is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
        }

        val signInButton = findViewById(R.id.sign_in_button) as SignInButton
        signInButton.setSize(SignInButton.SIZE_STANDARD)
        signInButton.setScopes(gso.scopeArray)

        signInButton.setOnClickListener { signIn() }
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        mAuth.removeAuthStateListener(mAuthListener)
        super.onStop()
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                // Google Sign In was successful, authenticate with Firebase
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, OnCompleteListener<com.google.firebase.auth.AuthResult> { task ->
            Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

            // If sign in fails, display a message to the user. If sign in succeeds
            // the auth state listener will be notified and logic to handle the
            // signed in user can be handled in the listener.
            if (!task.isSuccessful) {
                Log.w(TAG, "signInWithCredential", task.exception)
                Toast.makeText(this@SignInActivity, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                return@OnCompleteListener
            }

            val users = FirebaseDatabase.getInstance().getReference("users")
            val channel = Channel(name = acct.displayName ?: "", language = getPrefValue(R.string.pref_language, default = "en-GB"))
            users.child(acct.id).setValue(channel)

            setResult(Activity.RESULT_OK)

            val intent = Intent(this@SignInActivity, BitesChat::class.java)
            startActivity(intent)
        })
    }

    private fun getPrefValue(resourceId: Int, default: String): String {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(resourceId), default)
    }

    companion object {

        fun createIntent(context: Context): Intent {
            val intent = Intent(context, SignInActivity::class.java)
            return intent
        }

        private val RC_SIGN_IN = 9001
        private val TAG = SignInActivity::class.java.javaClass.simpleName
    }
}
