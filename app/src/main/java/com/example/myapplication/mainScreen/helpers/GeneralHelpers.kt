package com.example.myapplication.mainScreen.helpers

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.mainScreen.helpers.Pie.Style

interface ActivityData {
    val label: String?
    val data: Int
    val color: Color
    val lat: Double?
    val lng: Double?
    val endLat: Double?
    val endLng: Double?
    val durationText: String?
    val icon: ImageVector?
    val type: PieType
    val selectedColor: Color
}


