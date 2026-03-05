package com.example.produceapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProduceEntity::class], version = 1, exportSchema = false)
abstract class ProduceDatabase : RoomDatabase() {
    abstract fun produceDao(): ProduceDao

    companion object {
        @Volatile
        private var INSTANCE: ProduceDatabase? = null

        fun getDatabase(context: Context): ProduceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProduceDatabase::class.java,
                    "produce_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
