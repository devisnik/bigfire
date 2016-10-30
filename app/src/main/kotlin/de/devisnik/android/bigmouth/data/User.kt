package de.devisnik.android.bigmouth.data

class User {
    constructor()

    var name: String? = null
    var language: String? = null

    override fun toString(): String {
        return name!!
    }
}


