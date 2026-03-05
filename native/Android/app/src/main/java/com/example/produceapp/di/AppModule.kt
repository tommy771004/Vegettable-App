package com.example.produceapp.di

import android.content.Context
import com.example.produceapp.BuildConfig
import com.example.produceapp.data.ProduceDao
import com.example.produceapp.data.ProduceDatabase
import com.example.produceapp.network.ProduceService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideProduceService(retrofit: Retrofit): ProduceService {
        return retrofit.create(ProduceService::class.java)
    }

    @Provides
    @Singleton
    fun provideProduceDatabase(@ApplicationContext context: Context): ProduceDatabase {
        return ProduceDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideProduceDao(database: ProduceDatabase): ProduceDao {
        return database.produceDao()
    }
}
