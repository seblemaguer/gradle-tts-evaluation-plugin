package de.dfki.mary.ttsanalysis.subparts

import org.gradle.api.Project

import de.dfki.mary.ttsanalysis.AnalysisInterface

import marytts.analysis.distances.acoustic.*;
import marytts.analysis.alignment.IDAlignment;

class SpectrumAnalysis implements AnalysisInterface
{
    public void addTasks(Project project)
    {
        project.task("computeMCDIST")
        {
            // Hooking
            dependsOn "configurationAcoustic"

            // Input files
            project.configurationAcoustic.list_basenames.each { line ->
                inputs.files "${project.configurationAcoustic.reference_dir['mgc']}/${line}.mgc"
                inputs.files "${project.configurationAcoustic.synthesize_dir['mgc']}/${line}.mgc"
            }

            // Output files
            ext.output_f = new File("${project.configurationAcoustic.output_dir}/mcdist.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\tmcdist\n"

                project.configurationAcoustic.list_basenames.each { line ->
                    // Load files
                    double[][] src =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.reference_dir['mgc']}/${line}.mgc",
                                                        project.configurationAcoustic.mgc_dim)
                    double[][] tgt =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.synthesize_dir['mgc']}/${line}.mgc",
                                                        project.configurationAcoustic.mgc_dim);


                    def nb_frames = Math.min(src.length, tgt.length)

                    // Compute and dump the distance
                    def alignment = new IDAlignment(nb_frames);
                    def v = new CepstralDistorsion(src, tgt, project.configurationAcoustic.mgc_dim);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }
    }
}