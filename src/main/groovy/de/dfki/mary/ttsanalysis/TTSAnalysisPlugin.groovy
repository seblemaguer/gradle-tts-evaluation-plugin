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

        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin

        project.sourceCompatibility = JavaVersion.VERSION_1_7


        // Create output
        (new File(project.rootProject.buildDir.toString() + "/acousticAnalysis")).mkdirs()


        project.repositories {
            jcenter()
            maven {
                url 'http://oss.jfrog.org/artifactory/repo'
            }
        }


        project.afterEvaluate {

            /*************************************************************
             ** Configuration of the plugin
             *************************************************************/
            project.task("configurationAcoustic") {
                dependsOn "configuration"

                // Input
                ext.list_basenames = project.configuration.list_basenames ? project.configuration.list_basenames : []
                ext.reference_dir = project.configuration.reference_dir ? project.configuration.reference_dir:[]
                ext.synthesize_dir = project.configuration.synthesize_dir ? project.configuration.synthesize_dir:[]


                // Some parameters
                ext.mgc_dim = project.configuration.mgc_dim ? project.configuration.mgc_dim : 50
                ext.nb_proc = project.configuration.nb_proc ? project.configuration.nb_proc : 1

                // Outputdir
                ext.output_dir = new File(project.rootProject.buildDir.toString() + "/acousticAnalysis");

                // Loading helping
                ext.loading = new LoadingHelpers();
            }



            (new DurationAnalysis()).addTasks(project)
            (new SpectrumAnalysis()).addTasks(project)
            (new ProsodyAnalysis()).addTasks(project)


            project.task("generateAcousticReport") {
                dependsOn "computeMCDIST", "computeVUVRate", "computeRMSEF0Cent", "computeRMSEDur"


                def input_rms_f0_cent = project.computeRMSEF0Cent.output_f
                def input_vuvrate = project.computeVUVRate.output_f
                def input_mcdist = project.computeMCDIST.output_f
                def input_rms_dur = project.computeRMSEDur.output_f

                def output_f = new File("${project.configurationAcoustic.output_dir}/global_report.csv")
                outputs.files output_f

                doLast {
                    def values = []
                    def dist = null
                    def s = null
                    output_f.text = "#id\tmean\tstd\tconfint\n"

                    // RMS DUR part
                    values = []
                    input_rms_dur.eachLine { line ->
                        if (line.startsWith("#"))
                            return; // Continue...

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                    }
                    dist = new Double[values.size()];
                    values.toArray(dist);
                    s = new Statistics(dist);
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