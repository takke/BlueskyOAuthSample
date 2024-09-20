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
        mavenLocal().mavenContent {
            // kbsky(local)
            includeGroup("work.socialhub.kbsky")
        }

        // khttpclient
        maven {
            url = uri("https://repo.repsy.io/mvn/uakihir0/public")
            content {
                includeGroup("work.socialhub")
            }
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "BlueskyOAuthSample"
include(":app")
 