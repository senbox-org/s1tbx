package org.jlinda.nest.dat;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.snap.dat.dialogs.SingleOperatorDialog;
import org.jlinda.nest.gpf.SubtRefDemOp;

public class CrossResamplingAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        SingleOperatorDialog dialog = new SingleOperatorDialog("CrossResampling", getAppContext(), "CrossResampling", getHelpId());
        dialog.setTargetProductNameSuffix(SubtRefDemOp.PRODUCT_TAG);
        dialog.show();

    }
}
