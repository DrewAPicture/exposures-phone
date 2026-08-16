package com.exposures.datalayer

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the real Data Layer calls ([DataLayerClient]) so orchestration logic that uses
 * it (sync pushers, command handlers) can be unit tested against a fake instead of real GMS APIs.
 */
interface DataLayerGateway {
    suspend fun putPayload(path: String, json: String)
    fun observePayload(path: String): Flow<String>
    suspend fun sendMessage(path: String, payload: String): Boolean

    /** The paired node's id, if currently reachable — null otherwise (e.g. out of Bluetooth range). */
    suspend fun findReachableNodeId(): String?
}
