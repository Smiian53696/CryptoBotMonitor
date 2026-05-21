package com.example.cryptobotmonitor.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Obiekt odpowiedzialny za konfigurację Retrofit
object RetrofitInstance {

    // Logger pokazujący zapytania HTTP w Logcat
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Klient HTTP
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Instancja Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Tworzenie API
    val api: CoinGeckoApi = retrofit.create(CoinGeckoApi::class.java)
}