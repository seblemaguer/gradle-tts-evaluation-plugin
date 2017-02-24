package de.dfki.mary.ttsanalysis.subparts

import org.gradle.api.Project

import de.dfki.mary.ttsanalysis.AnalysisInterface


import marytts.analysis.distances.acoustic.*;
import marytts.analysis.alignment.IDAlignment;

class ProsodyAnalysis implements AnalysisInterface
{
    public void addTasks(Project project)
    {
        project.task("computeRMSEF0Hz")
        {
            // Hooking
            dependsOn "configurationAcoustic"

            // Input files
            project.configurationAcoustic.list_basenames.each { line ->
                inputs.files "${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0"
                inputs.files "${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0"
            }

            // Output files
            ext.output_f = new File("${project.configurationAcoustic.output_dir}/rms_f0_hz.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\trms (cent)\n"

                project.configurationAcoustic.list_basenames.each { line ->
                    // Loading files
                    double[][] src =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0", 1);
                    double[][] tgt =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0", 1);

                    //  Transform LF0 => F0
                    def nb_frames = Math.min(src.length, tgt.length)
                    for (int i=0; i<nb_frames; i++)
                    {
                        if (src[i][0] != -1.0e+10)
                        {
                            src[i][0] = Math.exp(src[i][0])
                        }
                        else
                        {
                            src[i][0] = 0;
                        }

                        if (tgt[i][0] != -1.0e+10)
                        {
                            tgt[i][0] = Math.exp(tgt[i][0])
                        }
                        else
                        {
                            tgt[i][0] = 0;
                        }
                    }

                    // Compute and dump the distance
                    def alignment = new IDAlignment(nb_frames);
                    def v = new CentRMS(src, tgt, 0.0);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }

        project.task("computeRMSEF0Cent")
        {
            // Hooking
            dependsOn "configurationAcoustic"

            // Input files
            project.configurationAcoustic.list_basenames.each { line ->
                inputs.files "${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0"
                inputs.files "${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0"
            }

            // Output files
            ext.output_f = new File("${project.configurationAcoustic.output_dir}/rms_f0_cent.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\trms (cent)\n"

                project.configurationAcoustic.list_basenames.each { line ->
                    // Loading files
                    double[][] src =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0", 1);
                    double[][] tgt =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0", 1);

                    //  Transform LF0 => F0
                    def nb_frames = Math.min(src.length, tgt.length)
                    for (int i=0; i<nb_frames; i++)
                    {
                        if (src[i][0] != -1.0e+10)
                        {
                            src[i][0] = Math.exp(src[i][0])
                        }
                        else
                        {
                            src[i][0] = 0;
                        }

                        if (tgt[i][0] != -1.0e+10)
                        {
                            tgt[i][0] = Math.exp(tgt[i][0])
                        }
                        else
                        {
                            tgt[i][0] = 0;
                        }
                    }

                    // Compute and dump the distance
                    def alignment = new IDAlignment(nb_frames);
                    def v = new CentRMS(src, tgt, 0.0);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }

        project.task("computeVUVRate")
        {
            // Hooking
            dependsOn "configurationAcoustic"

            // Input files
            project.configurationAcoustic.list_basenames.each { line ->
                inputs.files "${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0"
                inputs.files "${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0"
            }

            // Output files
            ext.output_f = new File("${project.configurationAcoustic.output_dir}/voicing_error.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\tvuv (%)\n"

                project.configurationAcoustic.list_basenames.each { line ->
                    // Loading files
                    double[][] src =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.reference_dir['lf0']}/${line}.lf0", 1);
                    double[][] tgt =
                        project.configurationAcoustic.loading.loadFloatBinary("${project.configurationAcoustic.synthesize_dir['lf0']}/${line}.lf0", 1);

                    //  Transform LF0 => F0
                    def nb_frames = Math.min(src.length, tgt.length)
                    for (int i=0; i<nb_frames; i++)
                    {
                        if (src[i][0] != -1.0e+10)
                        {
                            src[i][0] = Math.exp(src[i][0])
                        }
                        else
                        {
                            src[i][0] = 0;
                        }

                        if (tgt[i][0] != -1.0e+10)
                        {
                            tgt[i][0] = Math.exp(tgt[i][0])
                        }
                        else
                        {
                            tgt[i][0] = 0;
                        }
                    }

                    // Compute distance and dump
                    def alignment = new IDAlignment(nb_frames);
                    def v = new VoicingError(src, tgt, 0.0);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }
    }
}