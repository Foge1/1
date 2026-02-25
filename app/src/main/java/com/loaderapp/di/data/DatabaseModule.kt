package com.loaderapp.di.data

import android.content.Context
import androidx.room.Room
import com.loaderapp.data.AppDatabase
import com.loaderapp.data.dao.ChatDao
import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "loader_app_database"
    ).build()

    @Provides
    @Singleton
    fun provideOrderDao(database: AppDatabase): OrderDao = database.orderDao()

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideOrderWorkerDao(database: AppDatabase): OrderWorkerDao = database.orderWorkerDao()

    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()
}
