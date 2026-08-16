package com.exposures.datalayer

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over the Play Services Wearable APIs. Deliberately mechanical — the interesting
 * logic (DTO mapping, JSON encoding) lives in [DataLayerJson]/DtoMappers, which are unit-testable;
 * this class touches real GMS APIs that can't be meaningfully exercised outside a real device or
 * emulator pair, so it needs manual verification per the plan's testing section.
 */
class DataLayerClient(context: Context) : DataLayerGateway {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val capabilityClient = Wearable.getCapabilityClient(appContext)

    override suspend fun putPayload(path: String, json: String) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(DataLayerPaths.KEY_PAYLOAD, json)
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    /** Emits the current payload at [path] immediately, then again on every subsequent change. */
    override fun observePayload(path: String): Flow<String> = callbackFlow {
        val buffer = dataClient.dataItems.await()
        try {
            buffer.firstOrNull { it.uri.path == path }
                ?.let { DataMapItem.fromDataItem(it).dataMap.getString(DataLayerPaths.KEY_PAYLOAD) }
                ?.let { trySend(it) }
        } finally {
            buffer.release()
        }

        val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
            try {
                for (event in events) {
                    if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == path) {
                        DataMapItem.fromDataItem(event.dataItem).dataMap.getString(DataLayerPaths.KEY_PAYLOAD)?.let { trySend(it) }
                    }
                }
            } finally {
                events.release()
            }
        }
        dataClient.addListener(listener)
        awaitClose { dataClient.removeListener(listener) }
    }

    /** Sends [payload] to the paired node advertising [DataLayerPaths.CAPABILITY_EXPOSURES_APP]. False if unreachable. */
    override suspend fun sendMessage(path: String, payload: String): Boolean {
        val nodeId = findReachableNodeId() ?: return false
        return try {
            messageClient.sendMessage(nodeId, path, payload.toByteArray()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun findReachableNodeId(): String? {
        val capabilityInfo = capabilityClient
            .getCapability(DataLayerPaths.CAPABILITY_EXPOSURES_APP, CapabilityClient.FILTER_REACHABLE)
            .await()
        return capabilityInfo.nodes.firstOrNull { it.isNearby }?.id ?: capabilityInfo.nodes.firstOrNull()?.id
    }

    companion object {
        private const val KEY_UPDATED_AT = "updatedAt"
    }
}
