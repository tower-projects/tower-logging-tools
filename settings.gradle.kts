rootProject.name = "tower-logging-tools"

include("jdeparser")

include("jboss-logging-annotations","tower-logging-processor")

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/central")
    }

    pluginManagement {
        repositories {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            gradlePluginPortal()
        }
    }

}