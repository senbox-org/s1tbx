package org.jlinda.nest.dat;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.nest.dat.dialogs.NestSingleTargetProductDialog;
import org.jlinda.nest.gpf.SubtRefDemOp;

public class CrossResamplingAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        NestSingleTargetProductDialog dialog = new NestSingleTargetProductDialog("CrossResampling", getAppContext(), "CrossResampling", getHelpId());
        dialog.setTargetProductNameSuffix(SubtRefDemOp.PRODUCT_TAG);
        dialog.show();

    }
}
