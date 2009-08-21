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

import javax.swing.JComponent;
import javax.swing.JLabel;

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
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        return false;
    }
    
    /**
     * Creates the editor component for the {@link ValueDescriptor} and bind it
     * to a {@link ValueContainer} using the {@link BindingContext}.
     * 
     * @param valueDescriptor The value descriptor
     * @param bindingContext The binding context
     * @return the editor component
     */
    public JComponent[] createComponents(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JComponent editorComponent = createEditorComponent(valueDescriptor, bindingContext);
        
        JLabel label = new JLabel(valueDescriptor.getDisplayName() + ":");
        Binding binding = bindingContext.getBinding(valueDescriptor.getName());
        binding.addComponent(label);
        
        return new JComponent[] {editorComponent, label};
    }
    
    /**
     * Creates the editor component together with an optional label for the {@link ValueDescriptor} and bind it
     * to a {@link ValueContainer} using the {@link BindingContext}.
     * 
     * If for this editor component a label is applicable, it is return as the second element in the array.
     * 
     * 
     * @param valueDescriptor The value descriptor
     * @param bindingContext The binding context
     * @return an array containing the editor component as first element and (if applicable) a label as second element
     */
    public abstract JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext);

}