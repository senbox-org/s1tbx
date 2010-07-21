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

package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;

import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.gpf.operators.standard.MosaicOp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ConditionsTableAdapterTest {

    private BindingContext bindingContext;

    @Before
    public void before() {
        final PropertyContainer pc = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic");
        bindingContext = new BindingContext(pc);
    }

    @Test
    public void variablesProperty() {
        final JTable table = new JTable();
        bindingContext.bind("conditions", new ConditionsTableAdapter(table));
        assertTrue(table.getModel() instanceof DefaultTableModel);

        final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.addRow((Object[]) null);
        tableModel.addRow((Object[]) null);

        assertEquals(2, table.getRowCount());

        table.setValueAt("a", 0, 0);
        assertEquals("a", table.getValueAt(0, 0));
        table.setValueAt("A", 0, 1);
        assertEquals("A", table.getValueAt(0, 1));
        table.setValueAt(true, 0, 2);
        assertEquals(true, table.getValueAt(0, 2));

        table.setValueAt("b", 1, 0);
        assertEquals("b", table.getValueAt(1, 0));
        table.setValueAt("B", 1, 1);
        assertEquals("B", table.getValueAt(1, 1));
        table.setValueAt(false, 1, 2);
        assertEquals(false, table.getValueAt(1, 2));

        bindingContext.getPropertySet().setValue("conditions", new MosaicOp.Condition[]{
                new MosaicOp.Condition("d", "D", true)
        });

        assertEquals(1, table.getRowCount());
        assertEquals("d", table.getValueAt(0, 0));
        assertEquals("D", table.getValueAt(0, 1));
        assertEquals(true, table.getValueAt(0, 2));
    }
}
