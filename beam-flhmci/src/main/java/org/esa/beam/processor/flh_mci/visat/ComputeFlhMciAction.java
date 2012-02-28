package org.esa.beam.processor.flh_mci.visat;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.processor.flh_mci.ComputeFlhMciOp;
import org.esa.beam.processor.flh_mci.Presets;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ComputeFlhMciAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final String operatorName = ComputeFlhMciOp.Spi.class.getName();
        final AppContext appContext = getAppContext();
        final String title = "FLH/MCI Computation";
        final String helpID = event.getCommand().getHelpId();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(operatorName, appContext,
                                                                                             title, helpID);
        final BindingContext bindingContext = dialog.getBindingContext();
        bindingContext.bindEnabledState("slopeBandName", true, "slope", true);
        bindingContext.addPropertyChangeListener("preset", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Presets preset = (Presets) evt.getNewValue();
                if (preset != Presets.NONE) {
                    final PropertySet propertySet = bindingContext.getPropertySet();
                    propertySet.setValue("lowerBaselineBandName", preset.getLowerBaselineBandName());
                    propertySet.setValue("upperBaselineBandName", preset.getUpperBaselineBandName());
                    propertySet.setValue("signalBandName", preset.getSignalBandName());
                    propertySet.setValue("lineHeightBandName", preset.getLineHeightBandName());
                    propertySet.setValue("slopeBandName", preset.getSlopeBandName());
                    propertySet.setValue("maskExpression", preset.getMaskExpression());
                }
            }
        });

        dialog.setTargetProductNameSuffix("_flhmci");
        dialog.getJDialog().pack();
        dialog.show();
    }
}
