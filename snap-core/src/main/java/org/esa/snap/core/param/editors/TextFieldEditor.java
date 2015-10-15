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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.AbstractParamEditor;
import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * An editor which uses a {@link javax.swing.JTextField} or {@link javax.swing.JTextArea}.
 */
public class TextFieldEditor extends AbstractParamEditor {

    private JTextComponent _textComponent;
    private JComponent _component;
    private FocusListener _focusListener;


    public TextFieldEditor(Parameter parameter) {
        super(parameter, true);
    }

    public JTextComponent getTextComponent() {
        return _textComponent;
    }

    private void setTextComponent(JTextComponent textComponent) {
        if (_textComponent != null) {
            _textComponent.removeFocusListener(_focusListener);
            _focusListener = null;
        }

        _textComponent = textComponent;

        if (_textComponent != null) {
            _focusListener = createFocusListener();
            _textComponent.addFocusListener(_focusListener);
        }

        if (_textComponent instanceof JTextArea) {
            _textComponent.setBorder(null);
            final JScrollPane pane = new JScrollPane(_textComponent);
            nameComponent(_component, "ScrollPane");
            pane.setMinimumSize(_textComponent.getPreferredSize());
            _component = pane;
        } else {
            _component = _textComponent;
        }
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getTextComponent();
    }

    /**
     * Gets a {@link javax.swing.JTextField} componet or a {@link javax.swing.JTextArea} wrapped with e.g. a {@link
     * JScrollPane}.
     *
     * @return a {@link javax.swing.JTextField} componet or a {@link javax.swing.JTextArea} wrapped with e.g. a {@link
     *         JScrollPane}.
     * @see #getEditorComponent()
     */
    @Override
    public JComponent getComponent() {
        return _component;
    }

    @Override
    protected void initUI() {
        super.initUI(); // creates the default label components for us

        int numCols = getParamProps().getNumCols();
        int numRows = getParamProps().getNumRows();


        if (numRows <= 1) {
            JTextField textComponent = new JTextField();
            nameEditorComponent(textComponent);
            // Configure text field
            //
            if (numCols <= 0) {
                if (getParameter().isTypeOf(Character.class)) {
                    textComponent.setColumns(2);
                } else if (getParameter().isTypeOf(Byte.class)) {
                    textComponent.setColumns(4);
                } else if (getParameter().isTypeOf(Short.class)) {
                    textComponent.setColumns(6);
                } else if (getParameter().isTypeOf(Integer.class)) {
                    textComponent.setColumns(8);
                } else if (getParameter().isTypeOf(Long.class)) {
                    textComponent.setColumns(8);
                } else if (getParameter().isTypeOf(Float.class)) {
                    textComponent.setColumns(8);
                } else if (getParameter().isTypeOf(Double.class)) {
                    textComponent.setColumns(8);
                } else {
                    textComponent.setColumns(24);
                }
            } else {
                textComponent.setColumns(numCols);
            }
            if (getParameter().isTypeOf(Number.class)) {
                textComponent.setHorizontalAlignment(JTextField.RIGHT);
            }
            textComponent.addActionListener(getDefaultActionListener());
            setTextComponent(textComponent);
        } else {
            JTextArea textComponent = new JTextArea();
            nameEditorComponent(textComponent);
            textComponent.setRows(numRows);
            if (numCols > 0) {
                textComponent.setColumns(numCols);
            }
            if (getParamProps().getPropertyValue(ParamProperties.WORD_WRAP_KEY, false)) {
                textComponent.setLineWrap(true);
                textComponent.setWrapStyleWord(true);
            }

            textComponent.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
            setTextComponent(textComponent);
        }

        if (getParamProps().getDescription() != null) {
            _textComponent.setToolTipText(getParamProps().getDescription());
        }
        final boolean enabled = !getParamProps().isReadOnly();
        _textComponent.setEnabled(enabled);
        _component.setEnabled(enabled);
        _textComponent.setInputVerifier(getDefaultInputVerifier());
    }

    private ParamProperties getParamProps() {
        return getParameter().getProperties();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        String text = getParameter().getValueAsText();
        if (!getTextComponent().getText().equals(text)) {
            getTextComponent().setText(text);
        }
        if (getTextComponent().isEnabled() != isEnabled()) {
            if (getParameter().getProperties().isReadOnly()) {
                getTextComponent().setEnabled(false);
                getComponent().setEnabled(false);
            } else {
                getTextComponent().setEnabled(isEnabled());
                getComponent().setEnabled(isEnabled());
            }
        }
    }

    private FocusAdapter createFocusListener() {
        final ParamProperties paramProperties = getParameter().getProperties();
        final boolean selectAll = paramProperties.getPropertyValue(ParamProperties.SELECT_ALL_ON_FOCUS_KEY, true);

        if (selectAll) {
            return new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    final JTextComponent tc = ((JTextComponent) e.getComponent());
                    tc.setCaretPosition(tc.getText().length());
                    tc.moveCaretPosition(0);
                }
            };
        } else {
            return new FocusAdapter() {
            };
        }
    }
}
