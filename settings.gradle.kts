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
    }
}

rootProject.name = "exposures-phone"

// Phase 4 of exp-shared-library-feasibility-plan.md: core-model/core-datalayer are consumed
// exclusively from exposures-common via composite build — the local duplicate modules this
// substituted for (kept through Phase 3 as a rollback path) are deleted. To roll back, check out
// the pre-Phase-4 commit rather than repointing a dependency.
includeBuild("../common") {
    dependencySubstitution {
        substitute(module("com.exposures.common:core-model")).using(project(":core-model"))
        substitute(module("com.exposures.common:core-datalayer")).using(project(":core-datalayer"))
    }
}

include(":core-database")
include(":core-sync")
include(":app")
