package de.devisnik.android.bigmouth

import okhttp3.OkHttpClient
import okhttp3.Request

class Translator {

    private val API_KEY = BuildConfig.YANDEX_API_KEY

    private val client: OkHttpClient
    private val url: String

    init {
        client = OkHttpClient ()
        url = "https://translate.yandex.net/api/v1.5/tr.json/translate"
    }


    fun translate(text: String, from: String, to: String): String {
        val request = Request.Builder()
                .url("$url?lang=$from-$to&key=$API_KEY&text=$text")
                .get()
                .build()
        val response = client.newCall(request).execute()
        return response.body().string()
    }
}

fun main(args: Array<String>) {
    val translated = Translator().translate("hello", from = "en", to = "de")

    println(translated)
}
