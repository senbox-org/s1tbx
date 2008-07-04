package org.esa.beam.cluster.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class ClusterAnalysisAction extends AbstractVisatAction {

    @Override
    public void updateState(CommandEvent commandEvent) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        setEnabled(selectedProduct != null);
    }


    @Override
    public void actionPerformed(final CommandEvent event) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        if (selectedProduct != null) {
            DefaultSingleTargetProductDialog productDialog =
                    new DefaultSingleTargetProductDialog("ClucovClusterAnalysis", getAppContext(), "Clucov Cluster Analysis", null);
            productDialog.setTargetProductNameSuffix("_clucov");
            productDialog.show();
        }
    }
}
