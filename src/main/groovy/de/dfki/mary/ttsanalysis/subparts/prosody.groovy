package de.dfki.mary.ttsanalysis.subparts

import org.gradle.api.Project

import de.dfki.mary.ttsanalysis.AnalysisInterface


import marytts.analysis.distances.acoustic.*;
import marytts.analysis.alignment.IDAlignment;

class ProsodyAnalysis implements AnalysisInterface
{
    public void addTasks(Project project)
    {
        project.task("computeRMSEF0Cent")
        {
            // FIXME: input file ?
            def output_f = new File("${project.acousticOutputDir}/rms_f0_cent.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\trms (cent)\n"

                project.list_file.eachLine { line ->
                    // Loading files
                    double[][] src =
                        project.loading.loadFloatBinary("${project.referenceDir['lf0']}/${line}.lf0", 1);
                    double[][] tgt =
                        project.loading.loadFloatBinary("${project.synthesizeDir['lf0']}/${line}.lf0", 1);

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
            // FIXME: input file ?
            def output_f = new File("${project.acousticOutputDir}/voicing_error.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\tvuv (%)\n"

                project.list_file.eachLine { line ->
                    // Loading files
                    double[][] src =
                        project.loading.loadFloatBinary("${project.referenceDir['lf0']}/${line}.lf0", 1);
                    double[][] tgt =
                        project.loading.loadFloatBinary("${project.synthesizeDir['lf0']}/${line}.lf0", 1);

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