package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.swing.ComponentAdapter;

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
public class TextFieldAdapter extends ComponentAdapter implements ActionListener {

    private final JTextField textField;

    public TextFieldAdapter(JTextField textField) {
        super();
        this.textField = textField;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textField};
    }

    @Override
    public void bindComponents() {
        textField.addActionListener(this);
        textField.setInputVerifier(createInputVerifier());
    }

    @Override
    public void unbindComponents() {
        textField.removeActionListener(this);
        textField.setInputVerifier(null);
    }

    @Override
    public void adjustComponents() {
        String text = getBinding().getContext().getValueContainer().getAsText(getBinding().getPropertyName());
        textField.setText(text);
    }

    boolean adjustValue() {
        try {
            getBinding().getContext().getValueContainer().setFromText(getBinding().getPropertyName(), textField.getText());
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

    public void actionPerformed(ActionEvent e) {
        adjustValue();
    }
}
