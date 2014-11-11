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

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A binding for a set of {@link javax.swing.AbstractButton} components sharing a multiple-exclusion scope.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ButtonGroupAdapter extends ComponentAdapter implements ActionListener {
    private final ButtonGroup buttonGroup;
    private AbstractButton[] buttons;
    private final Map<AbstractButton, Object> buttonToValueMap;
    private final Map<Object, AbstractButton> valueToButtonMap;

    public ButtonGroupAdapter(ButtonGroup buttonGroup, Map<AbstractButton, Object> buttonToValueMap) {
        this.buttonGroup = buttonGroup;
        this.buttonToValueMap = buttonToValueMap;
        this.valueToButtonMap = new HashMap<Object, AbstractButton>(buttonToValueMap.size());
    }

    public ButtonGroup getButtonGroup() {
        return buttonGroup;
    }

    @Override
    public JComponent[] getComponents() {
        return buttons.clone();
    }

    @Override
    public void bindComponents() {
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        int count = buttonGroup.getButtonCount();
        buttons = new AbstractButton[count];
        for (int i = 0; i < count; i++) {
            AbstractButton button = buttonEnum.nextElement();
            button.addActionListener(this);
            buttons[i] = button;
            valueToButtonMap.put(buttonToValueMap.get(button), button);
        }
    }

    @Override
    public void unbindComponents() {
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        int count = buttonGroup.getButtonCount();
        for (int i = 0; i < count; i++) {
            AbstractButton button = buttonEnum.nextElement();
            button.removeActionListener(this);
            valueToButtonMap.remove(buttonToValueMap.get(button));
        }
        valueToButtonMap.clear();
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        if (value != null) {
            AbstractButton button = valueToButtonMap.get(value);
            if (button != null) {
                button.setSelected(true);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractButton button = (AbstractButton) e.getSource();
        getBinding().setPropertyValue(buttonToValueMap.get(button));
    }

    public static Map<AbstractButton, Object> createButtonToValueMap(ButtonGroup buttonGroup, PropertySet propertySet, String propertyName) {
        PropertyDescriptor descriptor = propertySet.getDescriptor(propertyName);
        if (descriptor == null) {
            throw new IllegalStateException(String.format("descriptor == null (property '%s')", propertyName));
        }
        ValueSet valueSet = descriptor.getValueSet();
        if (valueSet == null) {
            throw new IllegalStateException(String.format("valueSet == null (property '%s')", propertyName));
        }
        Object[] items = valueSet.getItems();
        if (buttonGroup.getButtonCount() != items.length) {
            throw new IllegalStateException(String.format("buttonGroup.getButtonCount() != items.length (property '%s')", propertyName));
        }
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        HashMap<AbstractButton, Object> buttonToValueMap = new HashMap<AbstractButton, Object>(items.length);
        for (Object item : items) {
            buttonToValueMap.put(buttonEnum.nextElement(), item);
        }
        return buttonToValueMap;
    }

}
