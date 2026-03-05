package com.example.produceapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://ais-dev-gyv3my74fwisdg5piudwph-424197195798.asia-east1.run.app/api/produce/"

    val instance: ProduceService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ProduceService::class.java)
    }
}
