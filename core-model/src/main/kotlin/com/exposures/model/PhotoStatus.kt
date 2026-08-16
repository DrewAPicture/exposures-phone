package com.exposures.model

/** Status of the phone-captured reference photo for an exposure. Phone-owned; the watch only ever reads it. */
enum class PhotoStatus {
    NONE,
    REQUESTED,
    CAPTURED,
    UPLOADED,
    FAILED,
}
