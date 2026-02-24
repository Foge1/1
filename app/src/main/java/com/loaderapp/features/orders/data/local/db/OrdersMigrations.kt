package com.loaderapp.features.orders.data.local.db

import androidx.room.migration.Migration

object OrdersMigrations {
    val ALL: Array<Migration> = arrayOf(
        Migration2To3
    )
}
