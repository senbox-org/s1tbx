package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.math.MathUtils;
import org.jfree.chart.axis.ValueAxis;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.GridBagConstraints;

/**
 * @author Norman Fomferra
 */
public class AxisRangeControl {

    public final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    public final static String PROPERTY_NAME_MIN = "min";
    public final static String PROPERTY_NAME_MAX = "max";

    private final BindingContext bindingContext;
    private final String axisName;
    private JPanel panel;
    private TitledSeparator titledSeparator;

    public AxisRangeControl(String axisName) {
        this.axisName = axisName;
        PropertySet propertyContainer = PropertyContainer.createObjectBacked(new Model());
        bindingContext = new BindingContext(propertyContainer);
    }

    public JPanel getPanel() {
        if (panel == null) {
            panel = createPanel();
            panel.setName(axisName);
        }
        return panel;
    }

    private JPanel createPanel() {
        final JCheckBox autoMinMaxBox = new JCheckBox("Auto min/max");
        final JLabel minLabel = new JLabel("Min:");
        final JLabel maxLabel = new JLabel("Max:");
        final JTextField minTextField = new JTextField();
        final JTextField maxTextField = new JTextField();

        final JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,insets.top=2,weightx=1");

        titledSeparator = new TitledSeparator(axisName, SwingConstants.CENTER);
        GridBagUtils.addToPanel(panel, titledSeparator, gbc, "gridy=0,insets.right=-2");
        GridBagUtils.addToPanel(panel, autoMinMaxBox, gbc, "gridy=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1");

        GridBagUtils.addToPanel(panel, minLabel, gbc, "insets.left=22,gridx=0,gridy=2,weightx=0");
        GridBagUtils.addToPanel(panel, minTextField, gbc, "insets=2,gridx=1,gridy=2,weightx=1");

        GridBagUtils.addToPanel(panel, maxLabel, gbc, "insets.left=22,gridx=0,gridy=3,weightx=0");
        GridBagUtils.addToPanel(panel, maxTextField, gbc, "insets=2,gridx=1,gridy=3,weightx=1");

        bindingContext.bind("autoMinMax", autoMinMaxBox);
        bindingContext.bind("min", minTextField);
        bindingContext.bind("max", maxTextField);

        bindingContext.getPropertySet().getDescriptor("min").setDescription("Minimum display value for " + axisName);
        bindingContext.getPropertySet().getDescriptor("max").setDescription("Maximum display value for " + axisName);

        bindingContext.getPropertySet().getDescriptor("min").setValidator(new Validator() {
            @Override
            public void validateValue(Property property, Object value) throws ValidationException {
                final Double max = bindingContext.getPropertySet().getValue("max");
                if ((Double) value >= max) {
                    throw new ValidationException("min value has to be less than " + max);
                }
            }
        });

        bindingContext.getPropertySet().getDescriptor("max").setValidator(new Validator() {
            @Override
            public void validateValue(Property property, Object value) throws ValidationException {
                final Double min = bindingContext.getPropertySet().getValue("min");
                if ((Double) value <= min) {
                    throw new ValidationException("max value has to be greater than " + min);
                }
            }
        });

        bindingContext.getBinding("min").addComponent(minLabel);
        bindingContext.getBinding("max").addComponent(maxLabel);

        bindingContext.bindEnabledState("min", true, "autoMinMax", false);
        bindingContext.bindEnabledState("max", true, "autoMinMax", false);

        return panel;
    }

    public void setTitleSuffix(String suffix) {
        final JLabel label = (JLabel) titledSeparator.getLabelComponent();
        if (suffix == null || suffix.trim().length() == 0) {
            label.setText(axisName);
        } else {
            label.setText(axisName + " (" + suffix.trim() + ")");
        }
        titledSeparator.repaint();
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public void setComponentsEnabled(boolean enabled) {
        if (!enabled) {
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                bindingContext.setComponentsEnabled(property.getName(), enabled);
            }
        } else {
            for (Property property : bindingContext.getPropertySet().getProperties()) {
                if (property.getName().equals("min") || property.getName().equals("max")) {
                    bindingContext.setComponentsEnabled(property.getName(), !isAutoMinMax());
                } else {
                    bindingContext.setComponentsEnabled(property.getName(), enabled);
                }
            }
        }
    }

    public boolean isAutoMinMax() {
        return (Boolean) bindingContext.getBinding("autoMinMax").getPropertyValue();
    }

    public void adjustComponents(ValueAxis axis, int numDecimalPlaces) {
        adjustComponents(axis.getLowerBound(), axis.getUpperBound(), numDecimalPlaces);
    }

    public void adjustComponents(double min, double max, int numDecimalPlaces) {
        final Double oldMax = getMax();

        double newMax = MathUtils.round(max, roundFactor(numDecimalPlaces));
        double newMin = MathUtils.round(min, roundFactor(numDecimalPlaces));
        if(newMin == newMax) {
            newMax += Math.pow(10, -numDecimalPlaces);
        }

        if (min >= oldMax) {
            setMax(newMax);
            setMin(newMin);
        } else {
            setMin(newMin);
            setMax(newMax);
        }

    }

    public void adjustAxis(ValueAxis axis, int numDecimalPlaces) {
        final double lowerRange = MathUtils.round((Double) getBindingContext().getBinding("min").getPropertyValue(), roundFactor(numDecimalPlaces));
        final double upperRange = MathUtils.round((Double) getBindingContext().getBinding("max").getPropertyValue(), roundFactor(numDecimalPlaces));
        axis.setRange(lowerRange, upperRange);
    }

    private double roundFactor(int n) {
        return Math.pow(10.0, n);
    }

    public Double getMin() {
        return (Double) getBindingContext().getPropertySet().getValue("min");
    }

    public Double getMax() {
        return (Double) getBindingContext().getPropertySet().getValue("max");
    }

    private void setMin(double min) {
        getBindingContext().getPropertySet().setValue("min", min);
    }

    private void setMax(double max) {
        getBindingContext().getPropertySet().setValue("max", max);
    }

    private static class Model {
        private boolean autoMinMax = true;
        private double min = 0.0;
        private double max = 100.0;
    }
}
