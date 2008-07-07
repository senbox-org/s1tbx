package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A binding for a {@link JSpinner} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class SpinnerAdapter extends ComponentAdapter implements ChangeListener {

    final JSpinner spinner;

    public SpinnerAdapter(JSpinner spinner) {
        this.spinner = spinner;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{spinner};
    }

    @Override
    public void bindComponents() {
        updateSpinnerModel();
        spinner.addChangeListener(this);
    }

    @Override
    public void unbindComponents() {
        spinner.removeChangeListener(this);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        spinner.setValue(value);
    }

    public void stateChanged(ChangeEvent evt) {
        getBinding().setPropertyValue(spinner.getValue());
    }

    private void updateSpinnerModel() {
        ValueDescriptor valueDescriptor = getBinding().getContext().getValueContainer().getDescriptor(getBinding().getPropertyName());
        if (valueDescriptor.getValueRange() != null) {
            Class<?> type = valueDescriptor.getType();

            if (Number.class.isAssignableFrom(type)) {
                Number defaultValue = (Number) valueDescriptor.getDefaultValue(); // todo - why not the current value? (mp,nf - 18.02.2008)
                double min = valueDescriptor.getValueRange().getMin();
                double max = valueDescriptor.getValueRange().getMax();
                // todo - get step size from interval

                if (type == Byte.class) {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, (byte) min, (byte) max, 1));
                } else if (type == Short.class) {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, (short) min, (short) max, 1));
                } else if (type == Integer.class) {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, (int) min, (int) max, 1));
                } else if (type == Long.class) {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, (long) min, (long) max, 1));
                } else if (type == Float.class) {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, (float) min, (float) max, 1));
                } else {
                    spinner.setModel(new SpinnerNumberModel(defaultValue, min, max, 1));
                }
            }
        } else if (valueDescriptor.getValueSet() != null) {
            spinner.setModel(new SpinnerListModel(valueDescriptor.getValueSet().getItems()));
        }
    }
}
