package com.theseuntaylor.bookawards.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

sealed interface RemoteResult {
    data object NotModified : RemoteResult
    data class Updated(val text: String, val etag: String?) : RemoteResult
    data object Failed : RemoteResult
}

class RemoteNominations(private val client: HttpClient, private val url: String) {

    suspend fun fetchLatest(etag: String?): RemoteResult = try {
        val response = client.get(url) {
            timeout { requestTimeoutMillis = 10_000 }
            etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        }
        when (response.status) {
            HttpStatusCode.NotModified -> RemoteResult.NotModified
            HttpStatusCode.OK -> RemoteResult.Updated(response.bodyAsText(), response.headers[HttpHeaders.ETag])
            else -> RemoteResult.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RemoteResult.Failed
    }
}
