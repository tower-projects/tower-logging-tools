plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.property("MAVEN_USERNAME").toString())
            password.set(project.property("MAVEN_PASSWORD").toString())
        }
    }
}

ext{
    set("junit","5.8.2")
    set("assertj","3.22.0")
    set("junit-platform-launcher","1.9.0")

}

allprojects {

    group = "io.iamcyw.tower.logging"

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.MINUTES)
    }
}