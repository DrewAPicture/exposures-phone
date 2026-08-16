package com.exposures.datalayer

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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
    private val nodeClient = Wearable.getNodeClient(appContext)

    init {
        // The res/values/wear.xml manifest declaration alone can be slow to propagate through Play
        // services' capability index on some devices; registering explicitly at runtime too is the
        // documented, more reliable alternative and costs nothing if the manifest path already won.
        // Registration outlives the app process, so every launch after the first successful one
        // throws DUPLICATE_CAPABILITY (ApiException 4006) here — expected, safe to ignore.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                capabilityClient.addLocalCapability(DataLayerPaths.CAPABILITY_EXPOSURES_APP).await()
            } catch (e: Exception) {
                // Already registered from a previous launch, or a transient GMS failure either way
                // the manifest declaration remains the fallback — nothing more to do here.
            }
        }
    }

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
        val localNodeId = runCatching { nodeClient.localNode.await().id }.getOrNull()
        val connectedRemoteNodes = runCatching { nodeClient.connectedNodes.await() }
            .getOrElse { emptyList() }
            .filterNot { it.id == localNodeId }
        if (connectedRemoteNodes.isEmpty()) return null

        val capabilityReachableNodeIds = getCapabilityNodeIds(CapabilityClient.FILTER_REACHABLE, localNodeId)
        val capabilityAllNodeIds = getCapabilityNodeIds(CapabilityClient.FILTER_ALL, localNodeId)

        val nearbyConnected = connectedRemoteNodes.filter { it.isNearby }
        return pickNode(
            preferred = nearbyConnected,
            fallback = connectedRemoteNodes,
            reachableCapabilityNodeIds = capabilityReachableNodeIds,
            allCapabilityNodeIds = capabilityAllNodeIds,
        )?.id
    }

    private suspend fun getCapabilityNodeIds(filter: Int, localNodeId: String?): Set<String> =
        runCatching {
            capabilityClient.getCapability(DataLayerPaths.CAPABILITY_EXPOSURES_APP, filter)
                .await()
                .nodes
                .map { it.id }
                .filterNot { it == localNodeId }
                .toSet()
        }.getOrElse { emptySet() }

    private fun pickNode(
        preferred: List<Node>,
        fallback: List<Node>,
        reachableCapabilityNodeIds: Set<String>,
        allCapabilityNodeIds: Set<String>,
    ): Node? {
        val reachablePreferred = preferred.firstOrNull { it.id in reachableCapabilityNodeIds }
        if (reachablePreferred != null) return reachablePreferred

        val reachableFallback = fallback.firstOrNull { it.id in reachableCapabilityNodeIds }
        if (reachableFallback != null) return reachableFallback

        // Some device/GMS combinations intermittently return empty capability sets despite
        // connected peers. In that case, prefer transport reachability over strict capability.
        val allowAnyConnected = allCapabilityNodeIds.isEmpty()
        val allPreferred = preferred.firstOrNull { allowAnyConnected || it.id in allCapabilityNodeIds }
        if (allPreferred != null) return allPreferred
        return fallback.firstOrNull { allowAnyConnected || it.id in allCapabilityNodeIds }
    }

    companion object {
        private const val KEY_UPDATED_AT = "updatedAt"
    }
}
