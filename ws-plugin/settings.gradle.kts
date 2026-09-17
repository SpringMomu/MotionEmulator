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
        maven("https://api.xposed.info/")
        maven("https://jitpack.io")
    }
}

rootProject.name = "MotionEmulatorWebsocketPlugin"
include(":app", ":stub", ":plugin", ":xposed")
for (module in listOf("stub", "plugin", "xposed")) {
    project(":$module").projectDir = file("sdk/$module")
}
