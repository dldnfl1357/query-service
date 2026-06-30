package codegen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class GenerateScreenTask extends DefaultTask {

    @Input
    String specPath

    @Option(option = "spec", description = "Path to screen YAML spec, relative to project root")
    void setSpecPath(String value) { this.specPath = value }

    @TaskAction
    void run() {
        if (!specPath) throw new IllegalArgumentException("Usage: ./gradlew generateScreen --spec=screens-specs/<name>.yaml")
        def specFile = project.file(specPath)
        if (!specFile.exists()) throw new IllegalArgumentException("spec not found: ${specFile}")

        def spec = ScreenSpec.load(specFile)
        new ScreenGenerator(spec, project.projectDir).generate()
        println "✓ screen '${spec.screen}' generated. Review the diff before committing."
    }
}
