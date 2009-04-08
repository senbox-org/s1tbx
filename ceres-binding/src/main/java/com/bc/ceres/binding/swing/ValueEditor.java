/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.TableLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A factory for editors that can edit values of a certain type
 * described by a {@link ValueDescriptor}.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class ValueEditor {
    
    /**
     * Checks if this value editor can be used for the values
     * described by the given value model.
     * 
     * @param valueDescriptor The value descriptor
     * @return true, is this editor can be used for the given value descriptor
     */
    public abstract boolean isValidFor(ValueDescriptor valueDescriptor);
    
    /**
     * Creates the editor component for the {@link ValueDescriptor} and bind it
     * to a {@link ValueContainer} using the {@link BindingContext}.
     * 
     * @param valueDescriptor The value descriptor
     * @param bindingContext The binding context
     * @return the editor component
     */
    public abstract JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext);
    
    /**
     * <p>Creates the editor component for the {@link ValueDescriptor} and bind it
     * to a {@link ValueContainer} using the {@link BindingContext}.
     * This editor component is then added to the {@link JPanel}.
     * </p><p>
     * The panel must use a {@link TableLayout} whith at least 2 columns.
     * </p>
     * 
     * @param panel The panel to which the editor is added 
     * @param tableLayout The table layout of the panel 
     * @param rowIndex The index of the row at which the first component must be added 
     * @param valueDescriptor The value descriptor
     * @param bindingContext The binding context
     * 
     * @return the number of rows that have been added to the panel
     */
    public int addEditorComponent(JPanel panel, TableLayout tableLayout, int rowIndex, ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        Assert.state(panel.getLayout() instanceof TableLayout);
        Assert.state(tableLayout.getColumnCount() >= 2);
        
        JComponent editorComponent = createEditorComponent(valueDescriptor, bindingContext);
        decorateEditor(editorComponent, valueDescriptor);
        
        JLabel label = new JLabel(getDisplayName(valueDescriptor) + ":");
        
        tableLayout.setCellWeightX(rowIndex, 0, 0.0);
        panel.add(label);
        tableLayout.setCellWeightX(rowIndex, 1, 1.0);
        panel.add(editorComponent);
        if (tableLayout.getColumnCount() > 2) {
            tableLayout.setCellColspan(rowIndex, 2, tableLayout.getColumnCount() - 2);
            panel.add(new JPanel());
        }
        return 1;
    }
    
    protected void decorateEditor(JComponent editorComponent, ValueDescriptor valueDescriptor) {
        editorComponent.setName(valueDescriptor.getName());
        editorComponent.setToolTipText(valueDescriptor.getDescription());
    }
    
    protected static String getDisplayName(ValueDescriptor valueDescriptor) {
        String label = valueDescriptor.getDisplayName();
        if (label != null) {
            return label;
        }
        String name = valueDescriptor.getName().replace("_", " ");
        return createDisplayName(name);
    }

    public static String createDisplayName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(ch));
            } else if (i > 0 && i < name.length() - 1
                    && Character.isUpperCase(ch) &&
                    Character.isLowerCase(name.charAt(i + 1))) {
                sb.append(' ');
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}