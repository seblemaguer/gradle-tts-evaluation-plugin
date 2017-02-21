package de.dfki.mary.ttsanalysis


import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import marytts.analysis.utils.LoadingHelpers;
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


        // Load configuration
        def slurper = new JsonSlurper()
        def config_file = project.rootProject.ext.config_file
        def config = slurper.parseText( config_file.text )

        // Adapt pathes
        DataFileFinder.project_path = new File(getClass().protectionDomain.codeSource.location.path).parent
        if (config.data.project_dir) {
            DataFileFinder.project_path = config.data.project_dir
}

        // See for number of processes for parallel mode
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

        (new File(project.rootProject.buildDir.toString() + "/acousticAnalysis")).mkdirs()

        project.ext {
            // User configuration
            user_configuration = config;

            acousticOutputDir = new File(project.rootProject.buildDir.toString() + "/acousticAnalysis")
            // FIXME: externalize that !
            list_file = new File(DataFileFinder.getFilePath("list_test"))
            referenceDir = ["mgc": "../extraction/build/mgc"]
            synthesizeDir = ["mgc": "../synthesis/build/output/imposed_dur"]
            mgcDim = 50;

            nb_proc = nb_proc_local;

            loading = new LoadingHelpers();
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


            project.task("generateAcousticReport") {
                dependsOn "computeMCDIST"
            }
        }
    }
}
