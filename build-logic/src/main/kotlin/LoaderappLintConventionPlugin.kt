import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class LoaderappLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        extensions.configure(DetektExtension::class.java) {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("$rootDir/config/lint/detekt.yml"))
            parallel = true
            ignoreFailures = false
            autoCorrect = false
        }

        tasks.withType(Detekt::class.java).configureEach {
            jvmTarget = "17"
            reports {
                html.required.set(true)
                sarif.required.set(false)
                md.required.set(false)
                xml.required.set(false)
                txt.required.set(false)
            }
        }

        extensions.configure(KtlintExtension::class.java) {
            version.set("1.3.1")
            android.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            enableExperimentalRules.set(false)
            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }
    }
}
