package com.example.a3dgs.net

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class ProcessResponse(
    val jobId: String,
    val status: String
)

interface ApiService {
    @Multipart
    @POST("process")
    suspend fun uploadImages(
        @Part images: List<MultipartBody.Part>
    ): Response<ProcessResponse>

    @GET("result/{jobId}")
    suspend fun downloadResult(
        @Path("jobId") jobId: String
    ): Response<ResponseBody>
}


