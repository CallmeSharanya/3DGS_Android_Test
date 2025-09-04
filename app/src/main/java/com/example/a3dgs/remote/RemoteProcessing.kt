package com.example.a3dgs.remote

import android.content.ContentResolver
import android.net.Uri
import com.example.a3dgs.net.Client
import java.io.File

class RemoteProcessing(
    private val baseUrl: String
) {
    private val client by lazy { Client(baseUrl) }

    suspend fun runGaussianSplatting(
        contentResolver: ContentResolver,
        imageUris: List<Uri>,
        outputPly: File
    ): Result<File> {
        val jobId = client.processImages(contentResolver, imageUris)
            .getOrElse { return Result.failure(it) }
        return client.downloadPly(jobId, outputPly)
    }
}


