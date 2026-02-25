package com.loaderapp.features.orders.di

import android.content.Context
import androidx.room.Room
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
internal object OrdersDataModule {

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
}
