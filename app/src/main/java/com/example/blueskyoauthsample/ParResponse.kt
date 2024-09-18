package com.example.blueskyoauthsample

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParResponse(
    @SerialName("request_uri")
    val requestUri: String,
    @SerialName("expires_in")
    val expiresIn: Int,
)
