package com.loaderapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val orderAddressTextStyle = TextStyle(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold
)

val orderRateTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.SemiBold
)

val orderDateTextStyle = TextStyle(
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium
)

val orderMetaTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal
)

@Composable
fun addressTextStyle(): TextStyle = MaterialTheme.typography.bodyLarge.merge(orderAddressTextStyle)

@Composable
fun rateTextStyle(): TextStyle = MaterialTheme.typography.bodyLarge.merge(orderRateTextStyle)

@Composable
fun dateTextStyle(): TextStyle = MaterialTheme.typography.bodyMedium.merge(orderDateTextStyle)

@Composable
fun metaTextStyle(): TextStyle = MaterialTheme.typography.bodySmall.merge(orderMetaTextStyle)
