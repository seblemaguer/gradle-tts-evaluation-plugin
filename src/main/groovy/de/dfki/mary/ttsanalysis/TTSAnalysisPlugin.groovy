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
                ext.list_basenames = project.configuration.hasProperty("list_basenames") ? project.configuration.list_basenames : []
                ext.reference_dir = project.configuration.hasProperty("reference_dir") ? project.configuration.reference_dir : []
                ext.synthesize_dir = project.configuration.hasProperty("synthesize_dir") ? project.configuration.synthesize_dir : []


                // Some parameters
                ext.mgc_dim = project.configuration.hasProperty("mgc_dim") ? project.configuration.mgc_dim : 50
                ext.nb_proc = project.configuration.hasProperty("nb_proc") ? project.configuration.nb_proc : 1

                // Outputdir
                ext.output_dir = new File(project.rootProject.buildDir.toString() + "/output/acousticAnalysis");
                ext.output_dir.mkdirs()

                // Loading helping
                ext.loading = new LoadingHelpers();
            }



            (new DurationAnalysis()).addTasks(project)
            (new SpectrumAnalysis()).addTasks(project)
            (new ProsodyAnalysis()).addTasks(project)


            project.task("generateAcousticReport") {

                def input = []
                if (project.configurationAcoustic.reference_dir.containsKey("lf0"))
                {
                    dependsOn "computeVUVRate", "computeRMSEF0Cent", "computeRMSEF0Hz"
                    input << project.computeRMSEF0Cent.output_f
                    input << project.computeRMSEF0Hz.output_f
                    input << project.computeVUVRate.output_f
                }
                if (project.configurationAcoustic.reference_dir.containsKey("mgc"))
                {
                    dependsOn "computeMCDIST"
                    input << project.computeMCDIST.output_f
                }
                if (project.configurationAcoustic.reference_dir.containsKey("dur"))
                {
                    dependsOn "computeRMSEDur"
                    input << project.computeRMSEDur.output_f
                }


                ext.output_f = new File("${project.configurationAcoustic.output_dir}/global_report.csv")
                outputs.files ext.output_f

                doLast {
                    def values = []
                    def dist = null
                    def s = null
                    ext.output_f.text = "#id\tmean\tstd\tconfint\n"

                    // RMS DUR part
                    input.each { cur_input ->
                        values = []
                        def name = ""
                        cur_input.eachLine { line ->
                            if (line.startsWith("#")) {
                                name = line.replaceAll(/^#[ ]*id\t/, "")
                                return; // Continue...
                            }

                            def elts = line.split()
                            values << Double.parseDouble(elts[1])
                        }

                        dist = new Double[values.size()];
                        values.toArray(dist);
                        s = new Statistics(dist);
                        ext.output_f << name << "\t"
                        ext.output_f << s.mean() << "\t" << s.stddev() << "\t" << s.confint(0.05) << "\t"
                        ext.output_f << values.size() << "\n"
                    }
                }
            }
        }
    }
}