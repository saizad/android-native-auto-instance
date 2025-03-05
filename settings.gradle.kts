pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        includeBuild("auto-instance-plugin")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io" )
    }
}

rootProject.name = "AutoInstancePlugin"
include(":app")
//include(":auto-instance-plugin")
//include(":autoinstance")
//project(":autoinstance").projectDir = file("../AutoInstance/autoinstance")
include(":reflect-instance")
include(":auto-instance-annotations")
include(":auto-instance-processor")
