package com.kmp.webtonative.ui.screens.home.carousel

import androidx.compose.ui.graphics.Color

// Carousel slide data
data class CarouselSlide(
    val tag: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>
)

