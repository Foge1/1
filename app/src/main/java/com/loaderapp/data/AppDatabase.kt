package com.loaderapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loaderapp.data.dao.ChatDao
import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.ChatMessage
import com.loaderapp.data.model.User

@Database(
    entities = [User::class, ChatMessage::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                val dbInstance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "loader_app_database",
                        ).addMigrations(
                            migrations = AppMigrations.ALL,
                        ).build()
                instance = dbInstance
                dbInstance
            }
    }
}
