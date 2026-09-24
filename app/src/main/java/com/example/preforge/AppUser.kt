package com.example.preforge

data class AppUser(
    val id: String,
    val displayName: String,
    val isGuest: Boolean = false
)
