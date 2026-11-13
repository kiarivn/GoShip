package com.example.data.remote

import com.example.data.local.entity.OrderEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ĐỊA CHỈ BACKEND – chỉ cần sửa ở đây.
 *  - Máy ảo (emulator):  "http://10.0.2.2:3000"
 *  - Điện thoại thật:    "http://<IPv4 của máy tính>:3000"  (ví dụ "http://192.168.1.5:3000")
 *  - Backend đã đưa lên host: "https://ten-app.onrender.com"
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:3000"
}

/** Gọi backend (Node + MongoDB). Các hàm này chạy blocking, hãy gọi trong Dispatchers.IO. */
class OrderApi(private val baseUrl: String = ApiConfig.BASE_URL) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun getOrders(): List<OrderEntity> {
        val request = Request.Builder().url("$baseUrl/orders").get().build()
        client.newCall(request).execute().use { res ->
            check(res.isSuccessful) { "GET /orders lỗi ${res.code}" }
            val arr = JSONArray(res.body?.string() ?: "[]")
            return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    }

    fun upsertOrders(orders: List<OrderEntity>) {
        if (orders.isEmpty()) return
        val body = JSONArray().apply { orders.forEach { put(toJson(it)) } }
            .toString().toRequestBody(jsonType)
        val request = Request.Builder().url("$baseUrl/orders/bulk").post(body).build()
        client.newCall(request).execute().use { check(it.isSuccessful) { "POST /orders/bulk lỗi ${it.code}" } }
    }

    fun updateStatus(id: String, statusName: String) {
        val body = JSONObject().put("statusName", statusName).toString().toRequestBody(jsonType)
        val request = Request.Builder().url("$baseUrl/orders/$id/status").patch(body).build()
        client.newCall(request).execute().use { check(it.isSuccessful) { "PATCH lỗi ${it.code}" } }
    }

    fun deleteOrder(id: String) {
        val request = Request.Builder().url("$baseUrl/orders/$id").delete().build()
        client.newCall(request).execute().use { check(it.isSuccessful) { "DELETE lỗi ${it.code}" } }
    }

    private fun toJson(o: OrderEntity) = JSONObject().apply {
        put("id", o.id)
        put("trackingNumber", o.trackingNumber)
        put("senderName", o.senderName)
        put("senderAddress", o.senderAddress)
        put("recipientName", o.recipientName)
        put("recipientAddress", o.recipientAddress)
        put("statusName", o.statusName)
        put("serviceType", o.serviceType)
        put("feeVnd", o.feeVnd)
        put("codVnd", o.codVnd)
        put("createdAt", o.createdAt)
        put("estimatedDelivery", o.estimatedDelivery)
        put("weightKg", o.weightKg.toDouble())
        put("timestampMillis", o.timestampMillis)
    }

    private fun fromJson(j: JSONObject) = OrderEntity(
        id = j.getString("id"),
        trackingNumber = j.optString("trackingNumber"),
        senderName = j.optString("senderName"),
        senderAddress = j.optString("senderAddress"),
        recipientName = j.optString("recipientName"),
        recipientAddress = j.optString("recipientAddress"),
        statusName = j.optString("statusName", "PENDING"),
        serviceType = j.optString("serviceType"),
        feeVnd = j.optLong("feeVnd"),
        codVnd = j.optLong("codVnd"),
        createdAt = j.optString("createdAt"),
        estimatedDelivery = j.optString("estimatedDelivery"),
        weightKg = j.optDouble("weightKg", 0.0).toFloat(),
        timestampMillis = j.optLong("timestampMillis", System.currentTimeMillis())
    )
}
