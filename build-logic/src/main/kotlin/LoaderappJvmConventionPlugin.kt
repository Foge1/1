import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class LoaderappJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") {
            extensions.configure(ApplicationExtension::class.java) {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }

        pluginManager.withPlugin("com.android.library") {
            extensions.configure(LibraryExtension::class.java) {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            extensions.configure(KotlinAndroidProjectExtension::class.java) {
                jvmToolchain(17)
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            extensions.configure(KotlinJvmProjectExtension::class.java) {
                jvmToolchain(17)
            }
        }

        pluginManager.withPlugin("java") {
            extensions.configure(JavaPluginExtension::class.java) {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
        }

        tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions.jvmTarget = "17"
        }
    }
}
