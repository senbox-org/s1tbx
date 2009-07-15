package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.BindingProblem;

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
        final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
        final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
        if (model != null) {
            textField.setText(model.getValueAsText());
        } else {
            textField.setText("");
        }
    }

    boolean adjustValue() {
        final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
        final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
        try {
            model.setValueFromText(textField.getText());
            // Now model is in sync with UI
            getBinding().setProblem(null);
            return true;
        } catch (ValidationException e) {
            getBinding().setProblem(new BindingProblem(getBinding(), e));
            handleError(e);
            return false;
        } catch (ConversionException e) {
            getBinding().setProblem(new BindingProblem(getBinding(), e));
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
