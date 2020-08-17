/*
 *    Copyright (C) 2018 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package de.qaware.gradle.plugin.xsd2java.xjc

import de.qaware.gradle.plugin.xsd2java.xjc.tasks.XSD2JavaTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

/**
 * Configures the XSD2Java gradle plugin.
 */
class Xsd2JavaPlugin implements Plugin<Project> {

    def final static XML_BIND_VERSION = '2.3.3'

    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin('java')) {
            project.apply plugin: 'java'
        }
        def xsd2JavaExtension = new Xsd2JavaPluginExtension(project)
        project.extensions.add(Xsd2JavaPluginExtension.NAME, xsd2JavaExtension)

        addConfigurations(project)
        def xjc = addEnvironment(project, 'xsd2java')

        addsDependencies(project, xjc)

        project.afterEvaluate {
            xsd2JavaExtension.schemas.asMap.forEach({ name, config ->
                configureXsd2javaTask(project, xsd2JavaExtension, name, config)
            })
        }
    }

    /**
     * Configures the xsd2java tasks for every source directory.
     *
     * @param project The project to configure.
     * @param name The name suffix for the source directory task.
     * @param config The configuration for the source directory.
     */
    private void configureXsd2javaTask(Project project, Xsd2JavaPluginExtension xsd2JavaExtension, String name, config) {
        Task xsd2Java = project.task("xsd2java${name.capitalize()}", type: XSD2JavaTask) {
            schemaDirPath = config.schemaDirPath.toFile()
            packageName = config.packageName
            outputDir = xsd2JavaExtension.outputDir
            arguments = xsd2JavaExtension.arguments
            extension = xsd2JavaExtension.extension
            removeOldOutput = xsd2JavaExtension.removeOldOutput
            fork = xsd2JavaExtension.fork
        }
        project.tasks.compileXsd2javaJava.dependsOn xsd2Java
    }

    /**
     * Adds the required dependencies to the specific configurations.
     *
     * @param project The project to add the dependencies to.
     * @param xjc The source set used to add the dependencies to.
     */
    private static void addsDependencies(Project project, SourceSet xjc) {
        project.dependencies.add(xjc.compileConfigurationName, "org.glassfish.jaxb:jaxb-xjc:$XML_BIND_VERSION")
        project.dependencies.add(xjc.compileConfigurationName, "org.glassfish.jaxb:jaxb-runtime:$XML_BIND_VERSION")
        project.dependencies.add(xjc.compileConfigurationName, "com.sun.activation:jakarta.activation:1.2.2")

        project.dependencies.add('xsd2javaExtension', "com.github.jaxb-xew-plugin:jaxb-xew-plugin:1.9")
        project.dependencies.add('xsd2javaExtension', "net.java.dev.jaxb2-commons:jaxb-fluent-api:2.1.8")
    }

    /**
     * Adds the configurations required by the plugin.
     *
     * @param project the project to add the configurations to.
     */
    private static void addConfigurations(Project project) {
        project.configurations.maybeCreate('xsd2java')
        project.configurations.maybeCreate('xsd2javaExtension')
    }

    /**
     * Initializes the given environment.
     *
     * @param project The project to initialize the environment for.
     * @param environment The environments name.
     * @return The source set for the created environment.
     */
    private static SourceSet addEnvironment(Project project, String environment) {
        def sourceSet = project.sourceSets.findByName(environment)

        if (sourceSet == null) {
            sourceSet = project.sourceSets.create(environment, {
                java.srcDirs += project.file("$project.buildDir/generated-sources/${environment}")
            })
        }

        project.dependencies.add('compile', sourceSet.output)
        sourceSet
    }
}
