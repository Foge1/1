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
    implementation("com.android.tools.build:gradle:8.1.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:$ktlintGradleVersion")
}

gradlePlugin {
    plugins {
        register("loaderappLint") {
            id = "loaderapp.lint"
            implementationClass = "LoaderappLintConventionPlugin"
        }

        register("loaderappJvm") {
            id = "loaderapp.jvm"
            implementationClass = "LoaderappJvmConventionPlugin"
        }
    }
}
