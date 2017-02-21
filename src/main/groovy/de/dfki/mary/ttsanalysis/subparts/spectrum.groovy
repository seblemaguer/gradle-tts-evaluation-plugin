package de.dfki.mary.ttsanalysis.subparts

import org.gradle.api.Project

import de.dfki.mary.ttsanalysis.AnalysisInterface


class ProsodyAnalysis implements AnalysisInterface
{
    public void addTasks(Project project)
    {
        project.task("computeMCDIST")
        {
            // FIXME: input file ?
            def output_f = new File("${project.acousticOutputDir}/mcdist.csv")
            outputs.files output_f
            doLast {
                def loading = new LoadingHelpers();
                output_f.text = "#id\tmcdist\n"

                project.list_file.eachLine { line ->
                    // Load files
                    double[][] src =
                        project.loading.loadFloatBinary("${project.referenceDir['mgc']}/${line}.mgc",
                                                        project.mgcDim)
                    double[][] tgt =
                        project.loading.loadFloatBinary("${project.synthesizeDir['mgc']}/${line}.mgc",
                                                        project.mgcDim);


                    def nb_frames = Math.min(src.length, tgt.length)

                    // Compute and dump the distance
                    def alignment = new IDAlignment(nb_frames);
                    def v = new CepstralDistorsion(src, tgt, project.mgcDim);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }
    }
}