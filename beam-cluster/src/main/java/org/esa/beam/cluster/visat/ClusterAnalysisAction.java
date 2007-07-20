package org.esa.beam.cluster.visat;

import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;

import javax.swing.JOptionPane;

public class ClusterAnalysisAction extends ExecCommand {

    @Override
    public void updateState(CommandEvent commandEvent) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        setEnabled(selectedProduct != null);
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        if (selectedProduct != null) {
            VisatApp.getApp().showMessageDialog("Cluster Analysis", "Sorry GUI mode not implemented yet.", JOptionPane.INFORMATION_MESSAGE, null);
        }
    }
}
