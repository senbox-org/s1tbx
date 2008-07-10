package org.esa.beam.cluster.visat;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class EMClusterAnalysisAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (dialog == null) {
            DefaultSingleTargetProductDialog dstpDialog =
                    new DefaultSingleTargetProductDialog("EMClusterAnalysis", getAppContext(), "EM Cluster Analysis",
                                                         "clusterAnalysisEM");
            dstpDialog.setTargetProductNameSuffix("_em");
            dialog = dstpDialog;
        }
        dialog.show();
    }
}
