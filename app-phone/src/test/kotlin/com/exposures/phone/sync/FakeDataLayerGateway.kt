package com.exposures.phone.sync

import com.exposures.datalayer.DataLayerGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeDataLayerGateway : DataLayerGateway {
    val putPayloads = mutableListOf<Pair<String, String>>()
    val sentMessages = mutableListOf<Pair<String, String>>()
    var sendMessageResult = true

    private val payloadFlows = mutableMapOf<String, MutableSharedFlow<String>>()

    override suspend fun putPayload(path: String, json: String) {
        putPayloads.add(path to json)
    }

    override fun observePayload(path: String): Flow<String> = flowFor(path)

    override suspend fun sendMessage(path: String, payload: String): Boolean {
        sentMessages.add(path to payload)
        return sendMessageResult
    }

    suspend fun emit(path: String, json: String) {
        flowFor(path).emit(json)
    }

    fun lastPayload(path: String): String? = putPayloads.lastOrNull { it.first == path }?.second

    private fun flowFor(path: String) = payloadFlows.getOrPut(path) { MutableSharedFlow(replay = 1) }
}
