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
import marytts.analysis.Statistics;

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
            referenceDir = ["mgc": "../extraction/build/mgc", "lf0": "../extraction/build/lf0", "dur":DataFileFinder.getFilePath(config.data.full_lab_dir)]
            synthesizeDir = ["mgc": "../synthesis/build/output/imposed_dur", "lf0": "../synthesis/build/output/imposed_dur", "dur": "../synthesis/build/output/normal"]
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
                dependsOn "computeMCDIST", "computeVUVRate", "computeRMSEF0Cent", "computeRMSEDur"


                def input_rms_f0_cent = new File("${project.acousticOutputDir}/rms_f0_cent.csv")
                def input_vuvrate = new File("${project.acousticOutputDir}/voicing_error.csv")
                def input_mcdist = new File("${project.acousticOutputDir}/mcdist.csv")
                def input_rms_dur = new File("${project.acousticOutputDir}/rms_dur.csv")


                def output_f = new File("${project.acousticOutputDir}/global_report.csv")
                outputs.files output_f

                doLast {

                    output_f.text = "#id\tmean\tstd\tconfint\n"

                    // RMS DUR part
                    def values = []
                    input_rms_dur.eachLine { line ->
                        if (line.startsWith("#"))
                            return; // Continue...

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                    }
                    def dist = new Double[values.size()];
                    values.toArray(dist);
                    def s = new Statistics(dist);
                    output_f << "rms dur\t" << s.mean() << "\t" << s.stddev() << "\t" << s.confint(0.05) << "\n"


                    // RMS F0 part
                    values = []
                    input_rms_f0_cent.eachLine { line ->
                        if (line.startsWith("#"))
                            return; // Continue...

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                    }
                    dist = new Double[values.size()];
                    values.toArray(dist);
                    s = new Statistics(dist);
                    output_f << "rms f0\t" << s.mean() << "\t" << s.stddev() << "\t" << s.confint(0.05) << "\n"


                    // Voice/Unvoice error rate part
                    values = []
                    input_vuvrate.eachLine { line ->
                        if (line.startsWith("#"))
                            return; // Continue...

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                    }
                    dist = new Double[values.size()];
                    values.toArray(dist);
                    s = new Statistics(dist);
                    output_f << "vuvrate\t" << s.mean() << "\t" << s.stddev() << "\t" << s.confint(0.05) << "\n"

                    // Mel CepstralDistorsion part
                    values = []
                    input_mcdist.eachLine { line ->
                        if (line.startsWith("#"))
                            return; // Continue...

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                    }
                    dist = new Double[values.size()];
                    values.toArray(dist);
                    s = new Statistics(dist);
                    output_f << "mcdist\t" << s.mean() << "\t" << s.stddev() << "\t" << s.confint(0.05) << "\n"
                }
            }
        }
    }
}