plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // api, not implementation: MultipartBody.Part appears in SyncApi's own public signature, so
    // consumers need okhttp types on their compile classpath too.
    api(libs.okhttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.withType<Test>().configureEach {
    systemProperty("sync.openapi.spec", rootProject.file("docs/openapi/sync-api.json").absolutePath)
    if (project.hasProperty("updateOpenApiSpec")) {
        systemProperty("updateOpenApiSpec", "true")
    }
}
