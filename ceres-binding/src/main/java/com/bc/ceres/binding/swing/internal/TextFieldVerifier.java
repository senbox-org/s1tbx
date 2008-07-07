package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.*;
import com.bc.ceres.core.Assert;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * An {@link javax.swing.InputVerifier} used by the {@link TextFieldAdapter}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class TextFieldVerifier extends InputVerifier {
    private TextFieldAdapter adapter;

    TextFieldVerifier(TextFieldAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Checks whether the JComponent's input is valid. This method should have no side effects. It returns a boolean
     * indicating the status of the argument's input.
     *
     * @param input the JComponent to verify
     * @return <code>true</code> when valid, <code>false</code> when invalid
     * @see javax.swing.JComponent#setInputVerifier
     * @see javax.swing.JComponent#getInputVerifier
     */
    @Override
    public boolean verify(JComponent input) {
        try {
            final String text = ((JTextField) input).getText();
            final String name = adapter.getBinding().getPropertyName();
            final ValueContainer valueContainer = adapter.getBinding().getContext().getValueContainer();
            final ValueModel model = valueContainer.getModel(name);
            final ValueDescriptor descriptor = model.getDescriptor();
            final Converter converter = descriptor.getConverter();
            Assert.notNull(converter);
            final Object value = converter.parse(text);
            final Validator validator = model.getValidator();
            if (validator != null) {
                validator.validateValue(valueContainer.getModel(name), value);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Calls <code>verify(input)</code> to ensure that the input is valid. This method can have side effects. In
     * particular, this method is called when the user attempts to advance focus out of the argument component into
     * another Swing component in this window. If this method returns <code>true</code>, then the focus is
     * transfered normally; if it returns <code>false</code>, then the focus remains in the argument component.
     *
     * @param input the JComponent to verify
     * @return <code>true</code> when valid, <code>false</code> when invalid
     * @see javax.swing.JComponent#setInputVerifier
     * @see javax.swing.JComponent#getInputVerifier
     */
    @Override
    public boolean shouldYieldFocus(JComponent input) {
        return adapter.adjustValue();
    }
}
