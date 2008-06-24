/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueSet;

/**
 * Updates the ValueSet of the ValueContainer, when the selection changes.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ValueSetUpdater implements SelectionChangeListener {
    
    private final ValueDescriptor valueDescriptor;

    public ValueSetUpdater(ValueDescriptor valueDescriptor) {
        this.valueDescriptor = valueDescriptor;
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        final Product selectedProduct = (Product) event.getSelection().getFirstElement();
        ValueSet valueSet;
        if (selectedProduct != null) {
            valueSet = new ValueSet(selectedProduct.getBandNames());
        } else {
            valueSet = new ValueSet(new String[0]);
        }
        valueDescriptor.setValueSet(valueSet);
    }
}
