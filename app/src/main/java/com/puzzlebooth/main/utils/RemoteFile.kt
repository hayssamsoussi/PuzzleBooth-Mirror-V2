package com.puzzlebooth.main.utils

data class RemoteFilesResponse(val files: List<RemoteFile>)
data class RemoteFile(val file: String)