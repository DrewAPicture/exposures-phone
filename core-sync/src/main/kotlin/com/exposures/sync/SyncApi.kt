package com.exposures.sync

import com.exposures.sync.dto.ExposureSyncDto
import com.exposures.sync.dto.ReferencePhotoAckDto
import com.exposures.sync.dto.SyncAckDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** REST surface against the not-yet-built backend — see [SyncApiFactory] and [AuthProvider]. */
interface SyncApi {
    @POST("exposures")
    suspend fun uploadExposure(@Body exposure: ExposureSyncDto): SyncAckDto

    @Multipart
    @POST("exposures/{exposureId}/reference-photo")
    suspend fun uploadReferencePhoto(
        @Path("exposureId") exposureId: String,
        @Part photo: MultipartBody.Part,
    ): ReferencePhotoAckDto
}
