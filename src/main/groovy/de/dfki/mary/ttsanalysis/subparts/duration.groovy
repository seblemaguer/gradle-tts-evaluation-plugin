package de.dfki.mary.ttsanalysis.subparts

import org.gradle.api.Project

import de.dfki.mary.ttsanalysis.AnalysisInterface

import marytts.analysis.distances.acoustic.*;
import marytts.analysis.alignment.IDAlignment;


class DurationAnalysis implements AnalysisInterface
{
    public void addTasks(Project project)
    {
        project.task('computeRMSEDur')
        {
            // Hooking
            dependsOn "configurationAcoustic"

            // Input files
            project.configurationAcoustic.list_basenames.each { line ->
                inputs.files "${project.configurationAcoustic.reference_dir['dur']}/${line}.lab"
                inputs.files "${project.configurationAcoustic.synthesize_dir['dur']}/${line}.lab"
            }

            // Output file
            ext.output_f = new File("${project.configurationAcoustic.output_dir}/rms_dur.csv")
            outputs.files output_f

            doLast {
                output_f.text = "#id\trms (ms)\n"

                project.configurationAcoustic.list_basenames.each { line ->

                    // Loading reference labels
                    def ref_dur_list = []
                    (new File("${project.configurationAcoustic.reference_dir['dur']}/${line}.lab")).eachLine { label ->
                        def elts = label.split()
                        ref_dur_list << (elts[1].toInteger() - elts[0].toInteger())/ 10000
                    }
                    double[][] ref_dur = new double[ref_dur_list.size()][1];
                    ref_dur_list.eachWithIndex {val, idx ->
                        ref_dur[idx][0] = val
                    }

                    // Loading synthesized labels
                    def synth_dur_list = []
                    (new File("${project.configurationAcoustic.synthesize_dir['dur']}/${line}.lab")).eachLine { label ->
                        def elts = label.split()
                        synth_dur_list << (elts[1].toInteger() - elts[0].toInteger())/ 10000
                    }

                    double[][] synth_dur = new double[synth_dur_list.size()][1];
                    synth_dur_list.eachWithIndex {val, idx ->
                        synth_dur[idx][0] = val
                    }

                    if (synth_dur.length != ref_dur.length) {
                        throw new Exception("what ? ${synth_dur.length} != ${ref_dur.length}");
                    }


                    // Compute distance and dump
                    def alignment = new IDAlignment(synth_dur.length);
                    def v = new RMS(ref_dur, synth_dur, 1);
                    Double d = v.distancePerUtterance(alignment);
                    output_f << "$line\t$d\n";
                }
            }
        }
    }
}
