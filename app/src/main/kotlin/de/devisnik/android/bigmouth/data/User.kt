package de.devisnik.android.bigmouth.data

data class User(var name: String = "", var language: String = "") {

    override fun toString(): String = name
}


