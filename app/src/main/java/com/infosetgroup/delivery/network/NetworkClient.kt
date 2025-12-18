package com.infosetgroup.delivery.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class NetworkResult {
    data class Success(val code: Int, val body: String?) : NetworkResult()
    data class Failure(val throwable: Throwable?) : NetworkResult()
}

object NetworkClient {
    private const val BASE = "https://deliveries.devi7.in"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun postDelivery(json: String): NetworkResult {
        val url = "$BASE/api/rest/v1/deliveries"
        return try {
            val req = Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (t: Throwable) {
            NetworkResult.Failure(t)
        }
    }

    suspend fun postDeliveriesBulk(jsonArray: String): NetworkResult {
        val url = "$BASE/api/rest/v1/deliveries/bulk"
        return try {
            val req = Request.Builder()
                .url(url)
                .post(jsonArray.toRequestBody(JSON))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            NetworkResult.Success(resp.code, body)
        } catch (t: Throwable) {
            NetworkResult.Failure(t)
        }
    }
}

