package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

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
public class FormattedTextFieldBinding extends Binding implements PropertyChangeListener {

    private final JFormattedTextField textField;

    public FormattedTextFieldBinding(BindingContext context, JFormattedTextField textField, String propertyName) {
        super(context, propertyName);
        this.textField = textField;
        textField.addPropertyChangeListener("value", this);
    }

    public void propertyChange(PropertyChangeEvent e) {
        setValue(textField.getValue());
    }

    @Override
    protected void doAdjustComponents() {
        Object value = getValue();
        textField.setValue(value);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return textField;
    }
}
