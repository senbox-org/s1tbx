package org.esa.beam.unmixing.visat;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.unmixing.ui.SpectralUnmixingDialog;
import org.esa.beam.visat.actions.AbstractVisatAction;


public class SpectralUnmixingAction extends AbstractVisatAction {
    private SpectralUnmixingDialog dialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (dialog == null) {
            dialog = new SpectralUnmixingDialog(getAppContext());
        }
        dialog.show();
    }
}