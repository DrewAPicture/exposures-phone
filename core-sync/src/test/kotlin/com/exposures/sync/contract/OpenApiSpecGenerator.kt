package com.exposures.sync.contract

import com.exposures.sync.SyncApi
import com.exposures.sync.dto.ExposureSyncDto
import com.exposures.sync.dto.ReferencePhotoAckDto
import com.exposures.sync.dto.ShutterSpeedSyncDto
import com.exposures.sync.dto.SyncAckDto
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.serializer
import retrofit2.http.POST

/**
 * Renders the machine-readable OpenAPI 3.1 spec for [SyncApi] from Retrofit annotations and
 * kotlinx.serialization descriptors. Checked in at `docs/openapi/sync-api.json` and compared in
 * [OpenApiSpecDriftTest].
 */
object OpenApiSpecGenerator {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private data class Operation(
        val methodName: String,
        val path: String,
        val summary: String,
        val requestSchema: String? = null,
        val responseSchema: String,
        val pathParams: List<String> = emptyList(),
        val multipartPart: String? = null,
    )

    private val operations = listOf(
        Operation(
            methodName = "uploadExposure",
            path = "/exposures",
            summary = "Upload a captured exposure",
            requestSchema = "ExposureSyncDto",
            responseSchema = "SyncAckDto",
        ),
        Operation(
            methodName = "uploadReferencePhoto",
            path = "/exposures/{exposureId}/reference-photo",
            summary = "Upload the reference photo for an exposure",
            responseSchema = "ReferencePhotoAckDto",
            pathParams = listOf("exposureId"),
            multipartPart = "photo",
        ),
    )

    private val schemaTypes = listOf(
        serializer<ShutterSpeedSyncDto>(),
        serializer<ExposureSyncDto>(),
        serializer<SyncAckDto>(),
        serializer<ReferencePhotoAckDto>(),
    )

    fun render(): String {
        val annotated = SyncApi::class.java.declaredMethods.mapNotNull { method ->
            val post = method.getAnnotation(POST::class.java) ?: return@mapNotNull null
            method.name to "/" + post.value.trimStart('/')
        }.toMap()
        check(annotated.keys == operations.map { it.methodName }.toSet()) {
            "OpenAPI operations must cover every SyncApi @POST method. " +
                "Missing: ${annotated.keys - operations.map { it.methodName }.toSet()}. " +
                "Extra: ${operations.map { it.methodName }.toSet() - annotated.keys}."
        }
        operations.forEach { operation ->
            val actualPath = annotated.getValue(operation.methodName)
            check(actualPath == operation.path) {
                "${operation.methodName} path drifted: SyncApi has $actualPath, spec metadata has ${operation.path}."
            }
        }

        val spec = buildJsonObject {
            put("openapi", "3.1.0")
            putJsonObject("info") {
                put("title", "Exposures phone HTTP sync API")
                put(
                    "description",
                    "Client-side contract against the not-yet-built backend, derived from SyncApi " +
                        "and core-sync DTOs. Independent of the Wear Data Layer contract. " +
                        "Regenerate with ./gradlew :core-sync:test -PupdateOpenApiSpec",
                )
                put("version", "0.1.0")
            }
            putJsonObject("paths") {
                operations.forEach { operation ->
                    putJsonObject(operation.path) {
                        putJsonObject("post") {
                            put("operationId", operation.methodName)
                            put("summary", operation.summary)
                            if (operation.pathParams.isNotEmpty()) {
                                putJsonArray("parameters") {
                                    operation.pathParams.forEach { name ->
                                        add(
                                            buildJsonObject {
                                                put("name", name)
                                                put("in", "path")
                                                put("required", true)
                                                putJsonObject("schema") { put("type", "string") }
                                            },
                                        )
                                    }
                                }
                            }
                            putJsonObject("requestBody") {
                                put("required", true)
                                putJsonObject("content") {
                                    if (operation.multipartPart != null) {
                                        putJsonObject("multipart/form-data") {
                                            putJsonObject("schema") {
                                                put("type", "object")
                                                putJsonArray("required") {
                                                    add(JsonPrimitive(operation.multipartPart))
                                                }
                                                putJsonObject("properties") {
                                                    putJsonObject(operation.multipartPart) {
                                                        put("type", "string")
                                                        put("format", "binary")
                                                    }
                                                }
                                            }
                                        }
                                    } else if (operation.requestSchema != null) {
                                        putJsonObject("application/json") {
                                            putJsonObject("schema") {
                                                put("\$ref", "#/components/schemas/${operation.requestSchema}")
                                            }
                                        }
                                    }
                                }
                            }
                            putJsonObject("responses") {
                                putJsonObject("200") {
                                    put("description", "Success")
                                    putJsonObject("content") {
                                        putJsonObject("application/json") {
                                            putJsonObject("schema") {
                                                put("\$ref", "#/components/schemas/${operation.responseSchema}")
                                            }
                                        }
                                    }
                                }
                            }
                            putJsonArray("security") {
                                add(buildJsonObject {})
                                add(
                                    buildJsonObject {
                                        putJsonArray("bearerAuth") {}
                                    },
                                )
                            }
                        }
                    }
                }
            }
            putJsonObject("components") {
                putJsonObject("securitySchemes") {
                    putJsonObject("bearerAuth") {
                        put("type", "http")
                        put("scheme", "bearer")
                        put(
                            "description",
                            "Optional. AuthProvider.authHeader() value; omitted when the no-op provider is used.",
                        )
                    }
                }
                putJsonObject("schemas") {
                    schemaTypes.forEach { serializer ->
                        val descriptor = serializer.descriptor
                        put(simpleName(descriptor), schemaObject(descriptor))
                    }
                }
            }
        }
        return json.encodeToString(JsonElement.serializer(), spec) + "\n"
    }

    private fun schemaObject(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
        put("type", "object")
        val required = (0 until descriptor.elementsCount)
            .filterNot { descriptor.isElementOptional(it) }
            .map { descriptor.getElementName(it) }
        if (required.isNotEmpty()) {
            putJsonArray("required") {
                required.forEach { add(JsonPrimitive(it)) }
            }
        }
        putJsonObject("properties") {
            for (i in 0 until descriptor.elementsCount) {
                put(descriptor.getElementName(i), propertySchema(descriptor.getElementDescriptor(i)))
            }
        }
    }

    private fun propertySchema(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
        when (descriptor.kind) {
            StructureKind.LIST -> {
                put("type", "array")
                put("items", propertySchema(descriptor.getElementDescriptor(0)))
            }
            StructureKind.CLASS, StructureKind.OBJECT -> {
                val name = simpleName(descriptor)
                if (schemaTypes.any { simpleName(it.descriptor) == name }) {
                    put("\$ref", "#/components/schemas/$name")
                } else {
                    put("type", "object")
                }
            }
            else -> {
                val typeName = jsonTypeName(descriptor)
                if (descriptor.isNullable) {
                    put("type", buildJsonArray {
                        add(JsonPrimitive(typeName))
                        add(JsonPrimitive("null"))
                    })
                } else {
                    put("type", typeName)
                }
            }
        }
    }

    private fun jsonTypeName(descriptor: SerialDescriptor): String = when (descriptor.kind) {
        PrimitiveKind.STRING, PrimitiveKind.CHAR -> "string"
        PrimitiveKind.BOOLEAN -> "boolean"
        PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE -> "integer"
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
        else -> descriptor.kind.toString()
    }

    private fun simpleName(descriptor: SerialDescriptor): String =
        descriptor.serialName.substringAfterLast('.')
}
