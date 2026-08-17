package com.exposures.datalayer

/**
 * The Data Layer API contract shared by convention between exposures-watch and exposures-phone
 * (the two apps don't share code, so this file's counterpart in exposures-watch must be kept in
 * sync by hand). Each DataItem path has exactly one writer, matching the plan's data authority
 * model: phone owns equipment/rolls/photo-status, watch owns exposures.
 *
 * DataItems hold a *complete* list/map, not a per-roll slice — at this app's scale (a handful of
 * rolls, tens of exposures) a single combined item per entity type is well under the ~100KB
 * DataItem limit and avoids managing a dynamic set of per-roll paths.
 */
object DataLayerPaths {
    const val CAMERA_BODIES = "/equipment/camera-bodies" // phone writes, watch reads
    const val LENSES = "/equipment/lenses" // phone writes, watch reads
    const val LIGHT_METERS = "/equipment/light-meters" // phone writes, watch reads
    const val FILM_BACKS = "/equipment/film-backs" // phone writes, watch reads
    const val ROLLS = "/rolls" // phone writes, watch reads
    const val EXPOSURES = "/exposures" // watch writes, phone reads
    const val PHOTO_STATUSES = "/photo-status" // phone writes, watch reads

    const val CAPTURE_PHOTO_COMMAND = "/command/capture-photo" // watch -> phone (MessageClient)
    const val CAPTURE_RESULT_COMMAND = "/command/capture-result" // phone -> watch (MessageClient)
    const val COMPLETE_ROLL_COMMAND = "/command/complete-roll" // watch -> phone (MessageClient)
    const val REQUEST_ROLLS_SYNC_COMMAND = "/command/request-rolls-sync" // watch -> phone (MessageClient)
    const val CONNECTIVITY_PING_COMMAND = "/command/connectivity-ping" // watch -> phone (MessageClient)
    const val CONNECTIVITY_PING_ACK_COMMAND = "/command/connectivity-ping-ack" // phone -> watch (MessageClient)

    /** Capability both apps advertise so each side can find the other's connected node. */
    const val CAPABILITY_EXPOSURES_APP = "exposures_app"

    /** DataMap key each DataItem's JSON payload is stored under — see [DataLayerClient.putPayload]. */
    const val KEY_PAYLOAD = "payload"
}
