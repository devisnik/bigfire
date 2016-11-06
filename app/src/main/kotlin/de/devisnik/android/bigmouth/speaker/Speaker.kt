package de.devisnik.android.bigmouth.speaker

import de.devisnik.android.bigmouth.data.SoundBite

interface Speaker {
    fun makeSound(sound: SoundBite)
}
