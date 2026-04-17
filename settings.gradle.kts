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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Alter Core reposu eklendi!
        maven {
            name = "Alter"
            url = uri("https://facemoji.jfrog.io/artifactory/default-maven-local/")
        }
    }
}

rootProject.name = "streetme"
include(":app")