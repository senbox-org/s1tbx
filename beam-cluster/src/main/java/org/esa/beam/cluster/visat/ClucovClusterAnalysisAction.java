package org.esa.beam.cluster.visat;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class ClucovClusterAnalysisAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (dialog == null) {
            DefaultSingleTargetProductDialog dstpDialog = 
                new DefaultSingleTargetProductDialog("ClucovClusterAnalysis", getAppContext(), "Clucov Cluster Analysis", null);
            dstpDialog.setTargetProductNameSuffix("_clucov");
            dialog = dstpDialog;
        }
        dialog.show();
    }
}
