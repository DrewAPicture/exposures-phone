package com.exposures.sync

/**
 * Pluggable auth boundary for [SyncApi] requests — kept separate from [SyncApiFactory] so the
 * actual mechanism (bearer token, API key, ...) can be filled in once the backend decides one,
 * without touching the request/response contract.
 */
interface AuthProvider {
    /** The `Authorization` header value to attach, or null to send the request unauthenticated. */
    fun authHeader(): String?
}

/** No backend auth exists yet — every request goes out unauthenticated. */
object NoOpAuthProvider : AuthProvider {
    override fun authHeader(): String? = null
}
