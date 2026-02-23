package com.loaderapp.di

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
import com.loaderapp.features.orders.data.local.db.AppDatabase as OrdersAppDatabase
import com.loaderapp.features.orders.data.local.db.Migration2To3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей базы данных
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Предоставить единственный экземпляр базы данных
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "loader_app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    

    @Provides
    @Singleton
    fun provideOrdersAppDatabase(
        @ApplicationContext context: Context
    ): OrdersAppDatabase {
        return Room.databaseBuilder(
            context,
            OrdersAppDatabase::class.java,
            "orders_feature_database"
        )
            .addMigrations(Migration2To3)
            .build()
    }

    @Provides
    @Singleton
    fun provideOrdersDao(database: OrdersAppDatabase): OrdersDao {
        return database.ordersDao()
    }

    @Provides
    @Singleton
    fun provideApplicationsDao(database: OrdersAppDatabase): ApplicationsDao {
        return database.applicationsDao()
    }

    @Provides
    @Singleton
    fun provideAssignmentsDao(database: OrdersAppDatabase): AssignmentsDao {
        return database.assignmentsDao()
    }

    /**
     * Предоставить OrderDao
     */
    @Provides
    @Singleton
    fun provideOrderDao(database: AppDatabase): OrderDao {
        return database.orderDao()
    }
    
    /**
     * Предоставить UserDao
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    /**
     * Предоставить OrderWorkerDao
     */
    @Provides
    @Singleton
    fun provideOrderWorkerDao(database: AppDatabase): OrderWorkerDao {
        return database.orderWorkerDao()
    }
    
    /**
     * Предоставить ChatDao
     */
    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }
}
