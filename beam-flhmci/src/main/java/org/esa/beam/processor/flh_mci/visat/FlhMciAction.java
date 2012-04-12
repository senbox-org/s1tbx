package org.esa.beam.processor.flh_mci.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.processor.flh_mci.FlhMciOp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class FlhMciAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final String operatorName = FlhMciOp.Spi.class.getName();
        final AppContext appContext = getAppContext();
        final String title = "FLH/MCI Processor";
        final String helpID = event.getCommand().getHelpId();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(operatorName, appContext,
                                                                                             title, helpID);
        final BindingContext bindingContext = dialog.getBindingContext();
        final PropertySet propertySet = bindingContext.getPropertySet();
        configurePropertySet(propertySet);

        bindingContext.bindEnabledState("slopeBandName", true, "slope", true);
        bindingContext.addPropertyChangeListener("preset", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Presets preset = (Presets) evt.getNewValue();
                if (preset != Presets.NONE) {
                    setValueIfValid(propertySet, "lowerBaselineBandName", preset.getLowerBaselineBandName());
                    setValueIfValid(propertySet, "upperBaselineBandName", preset.getUpperBaselineBandName());
                    setValueIfValid(propertySet, "signalBandName", preset.getSignalBandName());
                    propertySet.setValue("lineHeightBandName", preset.getLineHeightBandName());
                    propertySet.setValue("slopeBandName", preset.getSlopeBandName());
                    propertySet.setValue("maskExpression", preset.getMaskExpression());
                }
            }

            private void setValueIfValid(PropertySet propertySet, String propertyName, String bandName) {
                if (propertySet.getDescriptor(propertyName).getValueSet().contains(bandName)) {
                    propertySet.setValue(propertyName, bandName);
                }
            }
        });

        dialog.setTargetProductNameSuffix("_flhmci");
        dialog.getJDialog().pack();
        dialog.show();
    }

    private void configurePropertySet(PropertySet propertySet) {
        final PropertySet presetPropertySet = PropertyContainer.createObjectBacked(new PresetContainer());

        // awkward - purpose is to insert 'preset' property at the first position of the binding context's property set
        final Property[] properties = propertySet.getProperties();
        propertySet.removeProperties(properties);
        propertySet.addProperty(presetPropertySet.getProperty("preset"));
        propertySet.addProperties(properties);
    }

    private static class PresetContainer {

        private Presets preset = Presets.NONE;
    }
}
