import gradle.kotlin.dsl.accessors._4ad077ad74816558e52d7069eb18a2f7.ext

plugins.withType(JavaBasePlugin::class) {
    project.setProperty("sourceCompatibility", JavaVersion.VERSION_1_8)
}

plugins.withType(JavaPlugin::class) {
    dependencies {
        add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher:${rootProject.ext["junit-platform-launcher"]}")
        add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-engine:${rootProject.ext["junit"]}")
        add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-api:${rootProject.ext["junit"]}")
        add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.assertj:assertj-core:${rootProject.ext["assertj"]}")
    }
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    maxHeapSize = "1024M"
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    sourceCompatibility = JavaVersion.VERSION_1_8.majorVersion
    targetCompatibility = JavaVersion.VERSION_1_8.majorVersion
}

tasks.withType(Javadoc::class) {
    options {
        encoding("UTF-8").source(JavaVersion.VERSION_1_8.majorVersion)
        (this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}