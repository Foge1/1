package com.loaderapp.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDataImportBoundaryTest {

    @Test
    fun `Given feature source files When scanning imports Then cross-feature data imports are absent`() {
        val repoRoot = File(System.getProperty("user.dir"))
        val featuresRoot = File(repoRoot, "app/src/main/java/com/loaderapp/features")
        assertTrue("Features folder not found: ${featuresRoot.path}", featuresRoot.exists())

        val forbiddenImports = mutableListOf<String>()

        featuresRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { sourceFile ->
                val sourceFeature = sourceFile.inferFeatureName(featuresRoot) ?: return@forEach

                sourceFile.useLines { lines ->
                    lines
                    .map { it.trim() }
                    .filter { it.startsWith("import com.loaderapp.features.") }
                    .forEach { importLine ->
                        val importedFeature = importLine.substringAfter("import com.loaderapp.features.")
                            .substringBefore('.')
                        val importsDataLayer = importLine.contains(".data.") || importLine.endsWith(".data")

                        if (importsDataLayer && importedFeature != sourceFeature) {
                            forbiddenImports += "${sourceFile.relativeTo(repoRoot).path}: $importLine"
                        }
                    }
                }
            }

        assertTrue(
            "Found forbidden cross-feature data imports:\n${forbiddenImports.joinToString("\n")}",
            forbiddenImports.isEmpty()
        )
    }

    private fun File.inferFeatureName(featuresRoot: File): String? {
        val relativePath = relativeTo(featuresRoot).invariantSeparatorsPath
        return relativePath.substringBefore('/').takeIf { it.isNotBlank() }
    }
}
