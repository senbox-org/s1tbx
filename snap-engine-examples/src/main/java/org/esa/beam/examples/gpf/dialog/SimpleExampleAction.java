package org.esa.beam.examples.gpf.dialog;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class SimpleExampleAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final OperatorMetadata opMetadata = SimpleExampleOp.class.getAnnotation(OperatorMetadata.class);
        final SimpleExampleDialog operatorDialog = new SimpleExampleDialog(opMetadata.alias(), getAppContext(),
                                                                           "Simple Example of a Simple Processor",
                                                                           event.getCommand().getHelpId());
        operatorDialog.getJDialog().pack();
        operatorDialog.show();


    }

}
