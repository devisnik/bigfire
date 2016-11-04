package de.devisnik.android.bigmouth.speaker

import rx.Subscription

class SpeakerPresenter(channelName: String) {

    private val useCase: SpeakerUseCase = SpeakerUseCase(channelName)
    private var soundSubscription: Subscription? = null

    fun bind(speaker: Speaker) {
        soundSubscription = useCase
                .soundStream()
                .subscribe {
                    speaker.makeSound(it)
                }
    }


    fun unbind() {
        soundSubscription?.unsubscribe()
    }


}
