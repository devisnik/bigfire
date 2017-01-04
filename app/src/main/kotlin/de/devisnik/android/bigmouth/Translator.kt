package de.devisnik.android.bigmouth

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Translator {

    companion object {
        private val API_KEY = BuildConfig.YANDEX_API_KEY
        private val URL = "https://translate.yandex.net/api/v1.5/tr.json/translate"
    }

    private val client: OkHttpClient = OkHttpClient()

    fun translate(text: String, from: String, to: String): String {
        val request = Request.Builder()
                .url("$URL?lang=$from-$to&key=$API_KEY&text=$text")
                .get()
                .build()
        val response = client.newCall(request).execute()
        val string = response.body().string()
        val jsonObject = JSONObject(string)
        return jsonObject.getJSONArray("text")[0].toString()
    }
}

fun main(args: Array<String>) {
    val translated = Translator().translate("hello", from = "en", to = "de")
    println(translated)
}
