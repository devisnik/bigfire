package de.devisnik.android.bigmouth.speaker

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.devisnik.android.bigmouth.data.SoundBite
import rx.Observable
import rx.subjects.PublishSubject

class SpeakerUseCase(channelName: String) {

    private val soundSubject: PublishSubject<SoundBite> = PublishSubject.create()

    init {
        FirebaseDatabase.getInstance()
                .getReference(channelName)
                .addValueEventListener(listenToChannel())
    }

    private fun listenToChannel(): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot?) {
                val soundBite = snapshot?.getValue(SoundBite::class.java)
                if (soundBite != null) {
                    soundSubject.onNext(soundBite)
                }
            }

            override fun onCancelled(p0: DatabaseError?) {
                // no-op
            }
        }
    }


    fun soundStream(): Observable<SoundBite> {
        return soundSubject
                .asObservable()
    }
}


