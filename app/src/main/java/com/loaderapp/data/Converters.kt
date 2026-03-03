package com.loaderapp.data

import androidx.room.TypeConverter
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus =
        runCatching { OrderStatus.valueOf(value) }
            .getOrElse { OrderStatus.AVAILABLE }

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole =
        runCatching { UserRole.valueOf(value) }
            .getOrElse { UserRole.LOADER }
}
