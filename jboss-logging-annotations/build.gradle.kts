plugins {
    `java-library`
    id("java-conventions")
    id("maven-deploy")
}

dependencies {
    compileOnly("org.jboss.logging:jboss-logging:3.4.2.Final")
}