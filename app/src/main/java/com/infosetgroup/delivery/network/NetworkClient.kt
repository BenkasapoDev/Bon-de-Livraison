package com.infosetgroup.delivery.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

sealed class NetworkResult {
    data class Success(val code: Int, val body: String?) : NetworkResult()
    data class Failure(val throwable: Throwable?) : NetworkResult()
}

object NetworkClient {
    private const val BASE = "https://deliveries.devi7.in"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun postDelivery(json: String): NetworkResult = withContext(Dispatchers.IO) {
        val url = "$BASE/api/rest/v1/deliveries"
        try {
            val req = Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (_: Throwable) {
            NetworkResult.Failure(Exception("Network request failed"))
        }
    }

    suspend fun postDeliveriesBulk(jsonArray: String): NetworkResult = withContext(Dispatchers.IO) {
        val url = "$BASE/api/rest/v1/deliveries/bulk"
        try {
            val req = Request.Builder()
                .url(url)
                .post(jsonArray.toRequestBody(JSON))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (_: Throwable) {
            NetworkResult.Failure(Exception("Network request failed"))
        }
    }

    // fetch paged history (GET)
    suspend fun getHistory(page: Int = 1, limit: Int = 20, keyword: String? = null): NetworkResult = withContext(Dispatchers.IO) {
        val urlBuilder = StringBuilder("$BASE/api/rest/v1/deliveries/history?page=$page&limit=$limit")
        if (!keyword.isNullOrBlank()) {
            val k = java.net.URLEncoder.encode(keyword, "UTF-8")
            urlBuilder.append("&keyword=").append(k)
        }
        val url = urlBuilder.toString()
        try {
            val req = Request.Builder()
                .url(url)
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (_: Throwable) {
            NetworkResult.Failure(Exception("Network request failed"))
        }
    }

    // fetch history detail by code
    suspend fun getHistoryDetail(code: String): NetworkResult = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(code, "UTF-8")
        val url = "$BASE/api/rest/v1/deliveries/history/$encoded"
        try {
            val req = Request.Builder()
                .url(url)
                .get()
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (_: Throwable) {
            NetworkResult.Failure(Exception("Network request failed"))
        }
    }
}
