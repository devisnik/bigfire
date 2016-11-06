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
                .addValueEventListener(listenToSounds())
    }

    private fun listenToSounds(): ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val soundBite = snapshot.getValue(SoundBite::class.java)
            soundBite?.let { soundSubject.onNext(soundBite) }
        }

        override fun onCancelled(error: DatabaseError) = Unit
    }

    fun soundStream(): Observable<SoundBite> = soundSubject.asObservable()

}


