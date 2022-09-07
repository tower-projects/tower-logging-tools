plugins {
    `java-library`
    id("java-conventions")
    id("maven-deploy")
}

dependencies{
    implementation(project(":jdeparser"))
    implementation(project(":jboss-logging-annotations"))
    compileOnly("org.jboss.logging:jboss-logging:3.4.2.Final")

    testImplementation("org.jboss.logging:jboss-logging:3.4.2.Final")
    testImplementation("org.jboss.logmanager:jboss-logmanager:2.1.18.Final")
    testImplementation("org.jboss.forge.roaster:roaster-api:2.22.3.Final")
    testImplementation("org.jboss.forge.roaster:roaster-jdt:2.22.3.Final")
}