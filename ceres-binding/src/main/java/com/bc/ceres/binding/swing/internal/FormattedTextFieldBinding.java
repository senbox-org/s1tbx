package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JFormattedTextField;
import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A binding for a {@link javax.swing.JFormattedTextField} component.
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
        setPropertyValue(textField.getValue());
    }

    @Override
    protected void adjustComponentImpl() {
        try {
            Object value = getValueContainer().getValue(getPropertyName());
            textField.setValue(value);
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    protected JComponent getPrimaryComponent() {
        return textField;
    }
}
