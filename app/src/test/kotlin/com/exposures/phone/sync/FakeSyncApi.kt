package com.exposures.phone.sync

import com.exposures.sync.SyncApi
import com.exposures.sync.dto.ExposureSyncDto
import com.exposures.sync.dto.ReferencePhotoAckDto
import com.exposures.sync.dto.SyncAckDto
import java.io.IOException
import okhttp3.MultipartBody

class FakeSyncApi : SyncApi {
    val uploadedExposures = mutableListOf<ExposureSyncDto>()
    val uploadedPhotoExposureIds = mutableListOf<String>()
    var failUploads = false

    override suspend fun uploadExposure(exposure: ExposureSyncDto): SyncAckDto {
        if (failUploads) throw IOException("offline")
        uploadedExposures.add(exposure)
        return SyncAckDto(remoteId = "server-${exposure.id}")
    }

    override suspend fun uploadReferencePhoto(exposureId: String, photo: MultipartBody.Part): ReferencePhotoAckDto {
        if (failUploads) throw IOException("offline")
        uploadedPhotoExposureIds.add(exposureId)
        return ReferencePhotoAckDto(remoteUrl = "https://cdn.example/$exposureId.jpg")
    }
}
