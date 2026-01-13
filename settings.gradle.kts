plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "rf-catalog-svc"
include(":rf-catalog-app")
include(":rf-feature-svc")
include("rf-intersync-svc")
include("rf-common-lib")
include("rf-righttype-svc")
include("rf-oip-svc")
include("rf-parties-svc")
include("rf-contract-client")
include("rf-pge-svc")