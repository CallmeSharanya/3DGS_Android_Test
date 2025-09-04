package com.example.a3dgs.net

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream

class Client(baseUrl: String) {
    private val service: ApiService

    init {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val http = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(http)
            .build()
        service = retrofit.create(ApiService::class.java)
    }

    suspend fun processImages(
        contentResolver: ContentResolver,
        uris: List<Uri>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parts = uris.mapIndexed { idx, uri ->
                val temp = streamToTempFile(contentResolver, uri, "upload_$idx.jpg")
                val body = temp.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", temp.name, body)
            }
            val resp = service.uploadImages(parts)
            if (resp.isSuccessful) {
                Result.success(resp.body()!!.jobId)
            } else {
                Result.failure(IllegalStateException("Upload failed: ${resp.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun downloadPly(jobId: String, destFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val resp = service.downloadResult(jobId)
            if (!resp.isSuccessful || resp.body() == null) {
                return@withContext Result.failure(IllegalStateException("Download failed: ${resp.code()}"))
            }
            resp.body()!!.byteStream().use { input ->
                FileOutputStream(destFile).use { out ->
                    input.copyTo(out)
                }
            }
            Result.success(destFile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun streamToTempFile(cr: ContentResolver, uri: Uri, name: String): File {
        val file = File.createTempFile(name.substringBefore('.'), ".${name.substringAfter('.')}" )
        cr.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { out -> input.copyTo(out) }
        }
        return file
    }
}


