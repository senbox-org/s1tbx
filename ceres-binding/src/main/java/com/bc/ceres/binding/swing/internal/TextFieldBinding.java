package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.internal.TextFieldVerifier;

import javax.swing.JTextField;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
        adjustValueContainer();
    }

    @Override
    protected void adjustComponentImpl() {
        String text = getValueContainer().getAsText(getPropertyName());
        textField.setText(text);
    }

    boolean adjustValueContainer() {
        try {
            getValueContainer().setFromText(getPropertyName(), textField.getText());
            return true;
        } catch (Exception e) {
            handleError(e);
            return false;
        }
    }

    public InputVerifier createInputVerifier() {
        return new TextFieldVerifier(this);
    }

    @Override
    protected JComponent getPrimaryComponent() {
        return textField;
    }
}
