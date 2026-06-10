package com.lumiroom.feature.auth.domain.model

data class LumiroomUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean,
)
