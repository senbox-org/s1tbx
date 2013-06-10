package org.jlinda.nest.dat;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.nest.dat.dialogs.NestSingleTargetProductDialog;
import org.jlinda.nest.gpf.SubtRefDemOp;

public class SubtRefDemAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        NestSingleTargetProductDialog dialog = new NestSingleTargetProductDialog("SubtRefDem", getAppContext(), "SubtRefDem", getHelpId());
        dialog.setTargetProductNameSuffix(SubtRefDemOp.PRODUCT_TAG);
        dialog.show();

//        final GraphBuilderDialog dialog = new GraphBuilderDialog(VisatApp.getApp(), "TOPO phase computation and subtraction", "SubtRefDemOp", false);
//        dialog.show();
//
//        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
//        final File graphFile = new File(graphPath, "SubtRefDemGraph.xml");
//
//        dialog.LoadGraph(graphFile);
    }
}
