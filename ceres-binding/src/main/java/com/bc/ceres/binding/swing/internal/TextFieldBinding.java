package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A binding for a {@link javax.swing.JTextField} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class TextFieldBinding extends Binding implements ActionListener {

    private final JTextField textField;

    public TextFieldBinding(BindingContext context, JTextField textField, String propertyName) {
        super(context, propertyName);
        this.textField = textField;
        textField.addActionListener(this);
        textField.setInputVerifier(createInputVerifier());
    }

    public void actionPerformed(ActionEvent e) {
        adjustValue();
    }

    @Override
    protected void doAdjustComponents() {
        String text = getValueContainer().getAsText(getName());
        textField.setText(text);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return textField;
    }

    boolean adjustValue() {
        try {
            getValueContainer().setFromText(getName(), textField.getText());
            return true;
        } catch (ValidationException e) {
            handleError(e);
            return false;
        } catch (ConversionException e) {
            handleError(e);
            return false;
        }
    }

    public InputVerifier createInputVerifier() {
        return new TextFieldVerifier(this);
    }
}
