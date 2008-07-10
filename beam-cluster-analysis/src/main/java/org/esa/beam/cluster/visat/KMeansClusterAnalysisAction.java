package org.esa.beam.cluster.visat;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class KMeansClusterAnalysisAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (dialog == null) {
            DefaultSingleTargetProductDialog dstpDialog =
                    new DefaultSingleTargetProductDialog("KMeansClusterAnalysis", getAppContext(),
                                                         "K-Means Cluster Analysis", "clusterAnalysisKMeans");
            dstpDialog.setTargetProductNameSuffix("_kmeans");
            dialog = dstpDialog;
        }
        dialog.show();
    }
}
