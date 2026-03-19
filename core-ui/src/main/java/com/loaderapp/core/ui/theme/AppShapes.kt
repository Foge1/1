package com.loaderapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

val ShapeCard = RoundedCornerShape(16.dp)
val ShapeButton = RoundedCornerShape(12.dp)
val ShapeChip = RoundedCornerShape(8.dp)
val ShapeStatusPill = RoundedCornerShape(percent = 50)
val ShapeSearch = RoundedCornerShape(12.dp)
val ShapeAvatar = RoundedCornerShape(16.dp)
val ShapeDialog = RoundedCornerShape(28.dp)
val ShapeBottomBar = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
