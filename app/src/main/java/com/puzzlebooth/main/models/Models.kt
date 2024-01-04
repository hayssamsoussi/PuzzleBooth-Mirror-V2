package com.puzzlebooth.main.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatus(
    val battery: String,
    val printCount: String,
    val mosaicOn: Boolean
)

@Serializable
data class MosaicInfo(
    val originals: Int,
    val images: Int,
    val drafts: Int,
    val toPrint: Int,
    val done: Int,
    val merge: Int,
    val mosaicPrint: Int,
    val boxes: List<MosaicBox>
)

@Serializable
data class MosaicBox(
    val boxNumber: Int,
    val imageExist: Boolean
)