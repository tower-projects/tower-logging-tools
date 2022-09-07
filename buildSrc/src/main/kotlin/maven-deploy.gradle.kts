plugins {
    `maven-publish`
    signing
}

plugins.withType(JavaPlugin::class) {
    extensions.getByType(JavaPluginExtension::class).withJavadocJar()
    extensions.getByType(JavaPluginExtension::class).withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            plugins.withType(JavaPlugin::class).all {
                if ((tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).isEnabled) {
                    components.matching { element -> element.name == "java" }.all {
                        from(this)
                    }
                }
            }

            plugins.withType(JavaPlatformPlugin::class).all {
                components.matching { element -> element.name == "javaPlatform" }.all {
                    from(this)
                }
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/tower-projects/tower-logging-tools")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("iamcyw")
                        name.set("loomis w")
                    }
                }
                scm {
                    connection.set("scm:https://github.com/tower-projects/tower-logging-tools.git")
                    developerConnection.set("scm:git@github.com:tower-projects/tower-logging-tools.git")
                    url.set("https://github.com/tower-projects/tower-logging-tools")
                }
            }
        }
    }
}