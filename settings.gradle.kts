pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Phase 5 of exp-shared-library-feasibility-plan.md: core-model/core-datalayer are
        // resolved as real published artifacts (com.exposures.common, pinned in
        // gradle/libs.versions.toml) rather than composite-build substitution (Phase 3/4).
        // Credentials: GITHUB_ACTOR/GITHUB_TOKEN env vars (CI, see .github/workflows/ci.yml) or
        // gpr.user/gpr.token Gradle project properties (local — e.g. ~/.gradle/gradle.properties,
        // never committed). GitHub Packages requires authentication to read Maven artifacts even
        // on a public repo.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/DrewAPicture/exposures-common")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}

rootProject.name = "exposures-phone"

include(":core-database")
include(":core-sync")
include(":app")
