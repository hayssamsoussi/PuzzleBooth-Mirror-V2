package com.puzzlebooth.main.models

data class RemotePhotoRequest(val entries: List<RemotePhoto>)
data class RemotePhoto(
    val name: String,
    val sender: Int,
    val personName: String,
    val email: String,
    val phone: String
)