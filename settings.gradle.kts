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

// Phase 3 of exp-shared-library-feasibility-plan.md: consume core-model/core-datalayer from
// exposures-common via composite build. :core-model and :core-datalayer below stay included as
// the documented rollback path (see that plan's Rollback strategy) — they're no longer referenced
// by :app/:core-database, but remain buildable/testable standalone if a repoint is ever needed.
includeBuild("../common") {
    dependencySubstitution {
        substitute(module("com.exposures.common:core-model")).using(project(":core-model"))
        substitute(module("com.exposures.common:core-datalayer")).using(project(":core-datalayer"))
    }
}

include(":core-model")
include(":core-datalayer")
include(":core-database")
include(":core-sync")
include(":app")
