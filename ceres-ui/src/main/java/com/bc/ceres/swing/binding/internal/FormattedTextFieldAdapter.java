package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for a {@link javax.swing.JFormattedTextField} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class FormattedTextFieldAdapter extends ComponentAdapter implements PropertyChangeListener {

    private final JFormattedTextField textField;

    public FormattedTextFieldAdapter(JFormattedTextField textField) {
        this.textField = textField;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textField};
    }

    @Override
    public void bindComponents() {
        textField.addPropertyChangeListener("value", this);
    }

    @Override
    public void unbindComponents() {
        textField.removePropertyChangeListener("value", this);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        textField.setValue(value);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        getBinding().setPropertyValue(textField.getValue());
    }
}
