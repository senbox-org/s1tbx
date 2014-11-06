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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * A binding for a {@link javax.swing.text.JTextComponent} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public class TextComponentAdapter extends ComponentAdapter implements ActionListener, FocusListener {

    private final JTextComponent textComponent;

    public TextComponentAdapter(JTextComponent textComponent) {
        super();
        this.textComponent = textComponent;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textComponent};
    }

    @Override
    public void bindComponents() {
        if (textComponent instanceof JTextField) {
            ((JTextField) textComponent).addActionListener(this);
        }
        textComponent.addFocusListener(this);
        textComponent.setInputVerifier(createInputVerifier());
    }

    @Override
    public void unbindComponents() {
        if (textComponent instanceof JTextField) {
            ((JTextField) textComponent).removeActionListener(this);
        }
        textComponent.removeFocusListener(this);
        textComponent.setInputVerifier(null);
    }

    @Override
    public void adjustComponents() {
        final PropertySet propertyContainer = getBinding().getContext().getPropertySet();
        final Property property = propertyContainer.getProperty(getBinding().getPropertyName());
        final String textValue = property != null ? property.getValueAsText() : "";
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                textComponent.setText(textValue);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }

    }

    void adjustValue() {
        try {
            final PropertySet propertyContainer = getBinding().getContext().getPropertySet();
            final Property property = propertyContainer.getProperty(getBinding().getPropertyName());
            property.setValueFromText(textComponent.getText());
            getBinding().clearProblem();
        } catch (BindingException e) {
            getBinding().reportProblem(e);
        }
    }

    public InputVerifier createInputVerifier() {
        return new TextVerifier();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        adjustValue();
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (getBinding().getProblem() != null) {
            textComponent.selectAll();
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
    }

    class TextVerifier extends InputVerifier {
        /*
         * Only called by base class InputVerifier.shouldYieldFocus()?
         */
        @Override
        public boolean verify(JComponent input) {
            return getBinding().getProblem() == null;
        }

        /*
         * Called by JComponent.focusController.
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            adjustValue();
            return getBinding().getProblem() == null;
        }
    }
}
