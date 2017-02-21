package de.dfki.mary.ttsanalysis

import org.gradle.api.Plugin
import org.gradle.api.Project

import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import de.dfki.mary.ttsanalysis.subparts.*

class TTSAnalysisPlugin implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.task('hello') {
            doLast {
                println "Hello from the GreetingPlugin"
            }
        }

        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin

        project.sourceCompatibility = JavaVersion.VERSION_1_7

        def nb_proc_local = 1
        if (project.gradle.startParameter.getMaxWorkerCount() != 0) {
            nb_proc_local = Runtime.getRuntime().availableProcessors(); // By default the number of core
            if (config.settings.nb_proc) {
                if (config.settings.nb_proc > nb_proc_local) {
                    throw Exception("You will overload your machine, preventing stop !")
                }

                nb_proc_local = config.settings.nb_proc
            }
        }

        project.ext {
        }

        project.status = project.version.endsWith('SNAPSHOT') ? 'integration' : 'release'

        project.repositories {
            jcenter()
            maven {
                url 'http://oss.jfrog.org/artifactory/repo'
            }
        }

        project.configurations.create 'legacy'

        project.sourceSets {
            main {
                java {
                    // srcDir project.generatedSrcDir
                }
            }
            test {
                java {
                    // srcDir project.generatedTestSrcDir
                }
            }
        }

        project.afterEvaluate {
            (new DurationAnalysis()).addTasks(project)
            (new SpectrumAnalysis()).addTasks(project)
            (new ProsodyAnalysis()).addTasks(project)
        }
    }
}
