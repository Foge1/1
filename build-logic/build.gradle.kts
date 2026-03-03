plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

val detektVersion = "1.23.8"
val ktlintGradleVersion = "12.1.2"

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:$ktlintGradleVersion")
}

gradlePlugin {
    plugins {
        register("loaderappLint") {
            id = "loaderapp.lint"
            implementationClass = "LoaderappLintConventionPlugin"
        }
    }
}
