/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.param;

import org.esa.snap.core.util.Debug;

import javax.swing.InputVerifier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The <code>AbstractParamEditor</code> acts as a base class for implementations of <code>ParamEditor</code> interface
 * by providing some default method implementations and several utility methods for common editors.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see ParamEditor
 * @see ParamValidator
 * @see ParamExceptionHandler
 */
public abstract class AbstractParamEditor implements ParamEditor, ParamExceptionHandler {

    private final Parameter _parameter;
    private final DefaultInputVerifier _defaultInputVerifier;
    private JLabel _labelComponent;
    private JLabel _physUnitLabelComponent;
    private boolean _enabled;
    private boolean _ensureTrailingColon;
    private boolean _ensureLeadingSpace;

    /**
     * Creates the object with a given parameter.
     *
     * @param parameter          the <code>Parameter</code> to be edited.
     * @param useDefaultVerifier <code>true</code> if a default verifier should be used
     */
    protected AbstractParamEditor(Parameter parameter, boolean useDefaultVerifier) {
        _parameter = parameter;
        _defaultInputVerifier = useDefaultVerifier ? new DefaultInputVerifier() : null;
        _labelComponent = null;
        _enabled = true;
        initUI();
        updateUI();
    }

    /**
     * Gets the parameter to which this editor belongs to.
     */
    public Parameter getParameter() {
        return _parameter;
    }

    /**
     * Gets the label component.
     */
    public JLabel getLabelComponent() {
        return _labelComponent;
    }

    /**
     * Sets the label component.
     */
    public void setLabelComponent(JLabel labelComponent) {
        _labelComponent = labelComponent;
        nameLabelComponent(_labelComponent);
    }

    /**
     * Gets the physical unit label component.
     */
    public JLabel getPhysUnitLabelComponent() {
        return _physUnitLabelComponent;
    }

    /**
     * Sets the physical unit component.
     */
    public void setPhysUnitLabelComponent(JLabel labelComponent) {
        _physUnitLabelComponent = labelComponent;
        nameUnitComponent(_physUnitLabelComponent);
    }

    protected void setDefaultLabelComponent(boolean ensureTrailingColon) {
        String labelText = getParameter().getProperties().getLabel();
        if (labelText == null) {
            return;
        }
        _ensureTrailingColon = ensureTrailingColon;
        labelText = ensureTrailingColon(labelText);
        JLabel label = new JLabel(labelText);
        String toolTipText = getParameter().getProperties().getDescription();
        if (toolTipText != null) {
            label.setToolTipText(toolTipText);
        }
        setLabelComponent(label);
    }

    private String ensureTrailingColon(String labelText) {
        if (_ensureTrailingColon) {
            if (labelText.endsWith(": ")) {
                // text is ok
            } else if (labelText.trim().endsWith(":")) {
                labelText = labelText.trim().concat(" ");
            } else {
                labelText = labelText.trim().concat(": ");
            }
        }
        return labelText;
    }

    protected void setDefaultPhysUnitLabelComponent(boolean ensureLeadingSpace) {
        String labelText = getParameter().getProperties().getPhysicalUnit();
        if (labelText == null) {
            return;
        }
        _ensureLeadingSpace = ensureLeadingSpace;
        labelText = ensureLeadingSpace(labelText);
        JLabel label = new JLabel(labelText);
        setPhysUnitLabelComponent(label);
    }

    private String ensureLeadingSpace(String labelText) {
        if (_ensureLeadingSpace && !labelText.startsWith(" ")) {
            return " " + labelText;
        }
        return labelText;
    }

    /**
     * Checks whether or not the editor is enabled,
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * Enables/disables this editor.
     */
    public void setEnabled(boolean enabled) {
        if (_enabled == enabled) {
            return;
        }
        _enabled = enabled;
        updateUI();
    }

    /**
     * Initialized the UI of this editor. Called only once within this editor's constructor.
     * <p> The default implementation creates a label component for the parameter's "label" and "physicalUnit"
     * properties.
     */
    protected void initUI() {
        setDefaultLabelComponent(true);
        setDefaultPhysUnitLabelComponent(true);
    }

    /**
     * Tells the UI to update it's state. The default implementations en-/disables the label components (if any) of this
     * editor.
     * <p>Note: If you override this method, you should call this base class version first.
     */
    public void updateUI() {
        if (_labelComponent != null) {
            final String labelText = ensureTrailingColon(getParameter().getProperties().getLabel());
            _labelComponent.setText(labelText);
            if (_labelComponent.isEnabled() != _enabled) {
                _labelComponent.setEnabled(_enabled);
            }
        }
        if (_physUnitLabelComponent != null) {
            final String labelText = ensureLeadingSpace(getParameter().getProperties().getPhysicalUnit());
            _physUnitLabelComponent.setText(labelText);
            if (_physUnitLabelComponent.isEnabled() != _enabled) {
                _physUnitLabelComponent.setEnabled(_enabled);
            }
        }
    }

    /**
     * Tells the UI component to reconfigure itself, since the parameter's properties have changed.
     * <p>The default implementation simply calls the <code>updateUI</code> method.
     */
    public void reconfigureUI() {
        updateUI();
    }

    /**
     * Implements the default behaviour for _parameter errors: A message box is shown for the given message string.
     */
    public boolean handleParamException(ParamException e) {
        /*
         * @todo 3 nf/nf - use a more generalized message dialog
         */
        final JComponent editorComponent = getEditorComponent();
        JOptionPane.showMessageDialog(editorComponent,
                                      e.getMessage(),
                                      "Input Error", /*I18N*/
                                      JOptionPane.ERROR_MESSAGE);
        editorComponent.requestFocus();
        if (editorComponent instanceof JTextField) {
            ((JTextField) editorComponent).selectAll();
        } else if (editorComponent instanceof JComboBox) {
            ((JComboBox) editorComponent).getEditor().selectAll();
        } else if (editorComponent instanceof JPanel) {
            final Component[] components = editorComponent.getComponents();
            for (Component component : components) {
                if (component instanceof JTextField) {
                    final JTextField jTextField = ((JTextField) component);
                    jTextField.requestFocus();
                    jTextField.selectAll();
                    break;
                }
            }
        }
        return true;
    }

    protected InputVerifier getDefaultInputVerifier() {
        return _defaultInputVerifier;
    }

    protected ActionListener getDefaultActionListener() {
        return _defaultInputVerifier;
    }

    protected boolean setParameterValue(JTextComponent textComponent) {
        Debug.assertNotNull(textComponent);
        return getParameter().setValueAsText(textComponent.getText(), getExceptionHandler());
    }

    /**
     * Checks whether or not the given text component provides a valid value for this parameter. This method has no
     * side-effects.
     *
     * @param textComponent the text component which provides the parameter value.
     * @return <code>true</code> if so
     */
    protected boolean checkParameterValue(JTextComponent textComponent) {
        Debug.assertNotNull(textComponent);
        try {
            Object value = getParameter().parseValue(textComponent.getText());
            getParameter().validateValue(value);
        } catch (ParamParseException e) {
            return false;
        } catch (ParamValidateException e) {
            return false;
        }
        return true;
    }

    class DefaultInputVerifier extends InputVerifier implements ActionListener {

        private boolean _settingValue;

        DefaultInputVerifier() {
        }

        /**
         * Checks whether the JComponent's input is valid. This method should have no side effects. It returns a boolean
         * indicating the status of the argument's input.
         *
         * @param input the JComponent to verify
         * @return <code>true</code> when valid, <code>false</code> when invalid
         * @see JComponent#setInputVerifier
         * @see JComponent#getInputVerifier
         */
        @Override
        public boolean verify(JComponent input) {
            Debug.trace("AbstractParamEditor: parameter '"
                    + getParameter().getName()
                    + "': DefaultInputVerifier.verify called");

            if (input instanceof JTextComponent) {
                return checkParameterValue((JTextComponent) input);
            }
            return false;
        }

        /**
         * Calls <code>verify(input)</code> to ensure that the input is valid. This method can have side effects. In
         * particular, this method is called when the user attempts to advance focus out of the argument component into
         * another Swing component in this window. If this method returns <code>true</code>, then the focus is
         * transfered normally; if it returns <code>false</code>, then the focus remains in the argument component.
         *
         * @param input the JComponent to verify
         * @return <code>true</code> when valid, <code>false</code> when invalid
         * @see JComponent#setInputVerifier
         * @see JComponent#getInputVerifier
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            Debug.trace("AbstractParamEditor: parameter '"
                    + getParameter().getName()
                    + "': DefaultInputVerifier.shouldYieldFocus called");
            if (!_settingValue && input instanceof JTextComponent) {
                _settingValue = true;
                boolean success = setParameterValue((JTextComponent) input);
                _settingValue = false;
                return success;
            }
            return true;
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent event) {
            Debug.trace("AbstractParamEditor: parameter '"
                    + getParameter().getName()
                    + "': DefaultInputVerifier.actionPerformed called");
            if (event.getSource() instanceof JTextComponent) {
                setParameterValue((JTextComponent) event.getSource());
            }
        }
    }

    /**
     * Returns an optional error handler for this editor. The default implementation simply returns <code>this</code>
     * instance.
     */
    public ParamExceptionHandler getExceptionHandler() {
        return this;
    }

    /**
     * Simply returns {@link #getEditorComponent}.
     *
     * @see #getEditorComponent()
     */
    public JComponent getComponent() {
        return getEditorComponent();
    }


    protected void nameLabelComponent(JComponent component) {
        nameComponent(component, "Label");
    }

    protected void nameEditorComponent(JComponent component) {
        nameComponent(component, "Editor");
    }

    protected void nameUnitComponent(JComponent component) {
        nameComponent(component, "Unit");
    }

    protected void nameComponent(JComponent component, String suffix) {
        if (component != null && component.getName() == null) {
            component.setName(getParameter().getName() + "." + suffix);
        }
    }
}


