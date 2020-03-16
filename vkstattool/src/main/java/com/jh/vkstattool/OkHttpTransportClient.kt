package com.jh.vkstattool

import com.vk.api.sdk.client.ClientResponse
import com.vk.api.sdk.client.TransportClient
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class OkHttpTransportClient : TransportClient {
    private val client = OkHttpClient()
    private val requestTimeoutMillis = 300
    private var lastRespondTime = -1L

    private fun convertHeaders(headers: Headers): Map<String, String> {
        return HashMap<String, String>().apply {
            headers.names().forEach { headerName ->
                this[headerName] = headers[headerName] ?: ""
            }
        }
    }

    override fun get(url: String?): ClientResponse {
        if (url == null) throw IOException("Empty url!")

        if (System.currentTimeMillis() - lastRespondTime < requestTimeoutMillis)
            Thread.sleep(requestTimeoutMillis - System.currentTimeMillis() + lastRespondTime)

        client.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) throw IOException("Error during get occurred $it")
            lastRespondTime = System.currentTimeMillis()
            return ClientResponse(it.code, it.body?.string(), convertHeaders(it.headers))
        }
    }

    override fun get(url: String?, contentType: String?): ClientResponse {
        if (url == null) throw IOException("Empty url!")
        if (contentType == null) return get(url)

        if (System.currentTimeMillis() - lastRespondTime < requestTimeoutMillis)
            Thread.sleep(requestTimeoutMillis - System.currentTimeMillis() + lastRespondTime)

        client.newCall(Request.Builder().url(url).addHeader("content-type", contentType).build())
            .execute().use {
                if (!it.isSuccessful) throw IOException("Error during get occurred $it")
                lastRespondTime = System.currentTimeMillis()
                return ClientResponse(it.code, it.body?.string(), convertHeaders(it.headers))
            }
    }

    override fun post(url: String?, body: String?): ClientResponse {
        return post(url, body, contentType = null)
    }

    override fun post(url: String?, fileName: String?, file: File?): ClientResponse {
        if (url == null) throw IOException("Empty url!")
        if (file == null) throw IOException("Empty file!")

        if (System.currentTimeMillis() - lastRespondTime < requestTimeoutMillis)
            Thread.sleep(requestTimeoutMillis - System.currentTimeMillis() + lastRespondTime)

        client.newCall(Request.Builder().url(url).post(file.asRequestBody()).build())
            .execute().let {
                if (!it.isSuccessful) throw IOException("Error during post occurred $it")
                lastRespondTime = System.currentTimeMillis()
                return ClientResponse(it.code, it.body?.string(), convertHeaders(it.headers))
            }
    }

    override fun post(url: String?, body: String?, contentType: String?): ClientResponse {
        if (url == null) throw IOException("Empty url!")

        if (System.currentTimeMillis() - lastRespondTime < requestTimeoutMillis)
            Thread.sleep(requestTimeoutMillis - System.currentTimeMillis() + lastRespondTime)

        client.newCall(
            Request.Builder().url(url)
                .post((body ?: "").toRequestBody(contentType?.toMediaType())).build()
        ).execute().use {
            if (!it.isSuccessful) throw IOException("Error during post occurred $it")
            lastRespondTime = System.currentTimeMillis()
            return ClientResponse(it.code, it.body?.string(), convertHeaders(it.headers))
        }
    }

    override fun post(url: String?): ClientResponse {
        return post(url, body = null, contentType = null)
    }

    override fun delete(url: String?): ClientResponse {
        return delete(url, null, null)
    }

    override fun delete(url: String?, body: String?): ClientResponse {
        return delete(url, body, contentType = null)
    }

    override fun delete(url: String?, body: String?, contentType: String?): ClientResponse {
        if (url == null) throw IOException("Empty url!")

        if (System.currentTimeMillis() - lastRespondTime < requestTimeoutMillis)
            Thread.sleep(requestTimeoutMillis - System.currentTimeMillis() - lastRespondTime)

        client.newCall(
            Request.Builder().url(url)
                .delete((body ?: "").toRequestBody(contentType?.toMediaType())).build()
        ).execute().use {
            if (!it.isSuccessful) throw IOException("Error during post occurred $it")
            lastRespondTime = System.currentTimeMillis()
            return ClientResponse(it.code, it.body?.string(), convertHeaders(it.headers))
        }
    }
}