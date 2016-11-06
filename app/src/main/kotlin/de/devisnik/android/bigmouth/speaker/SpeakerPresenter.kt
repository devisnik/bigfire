package de.devisnik.android.bigmouth.speaker

import rx.Subscription
import rx.subscriptions.Subscriptions

class SpeakerPresenter(channelName: String) {

    private val useCase: SpeakerUseCase = SpeakerUseCase(channelName)
    private var soundSubscription: Subscription = Subscriptions.empty()

    fun bind(speaker: Speaker) {
        soundSubscription = useCase
                .soundStream()
                .subscribe {
                    speaker.makeSound(it)
                }
    }


    fun unbind() {
        soundSubscription.unsubscribe()
    }


}
