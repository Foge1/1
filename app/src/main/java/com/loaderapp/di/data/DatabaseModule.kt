package com.loaderapp.di.data

import android.content.Context
import androidx.room.Room
import com.loaderapp.data.AppDatabase
import com.loaderapp.data.dao.ChatDao
import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.dao.UserDao
import com.loaderapp.features.orders.data.local.dao.ApplicationsDao
import com.loaderapp.features.orders.data.local.dao.AssignmentsDao
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.db.OrdersDatabase
import com.loaderapp.features.orders.data.local.db.OrdersMigrations
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
    fun provideOrdersDatabase(
        @ApplicationContext context: Context
    ): OrdersDatabase = Room.databaseBuilder(
        context,
        OrdersDatabase::class.java,
        "orders_feature_database"
    )
        .addMigrations(*OrdersMigrations.ALL)
        .build()

    @Provides
    @Singleton
    fun provideOrdersDao(database: OrdersDatabase): OrdersDao = database.ordersDao()

    @Provides
    @Singleton
    fun provideApplicationsDao(database: OrdersDatabase): ApplicationsDao = database.applicationsDao()

    @Provides
    @Singleton
    fun provideAssignmentsDao(database: OrdersDatabase): AssignmentsDao = database.assignmentsDao()

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
