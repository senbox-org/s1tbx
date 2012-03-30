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

package com.bc.ceres.swing.binding;

import com.bc.ceres.binding.PropertyContainer;
import junit.framework.TestCase;

import javax.swing.*;

public class BindingContextEnablementTest extends TestCase {

    public void testEnablement() throws Exception {
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(new X()));
        final JTextField aField = new JTextField();
        final JTextField bField = new JTextField();
        bindingContext.bind("a", aField);
        bindingContext.bind("b", bField);

        assertEquals(true, aField.isEnabled());
        assertEquals(true, bField.isEnabled());

        bindingContext.bindEnabledState("b", true, "a", "Hanni");
        bindingContext.adjustComponents();

        assertEquals(true, aField.isEnabled());
        assertEquals(false, bField.isEnabled());

        bindingContext.getPropertySet().setValue("a", "Nanni");

        assertEquals(true, aField.isEnabled());
        assertEquals(false, bField.isEnabled());


        bindingContext.getPropertySet().setValue("a", "Hanni");

        assertEquals(true, aField.isEnabled());
        assertEquals(true, bField.isEnabled());

        bindingContext.unbind(bindingContext.getBinding("b"));

        bindingContext.getPropertySet().setValue("a", "Pfanni");

        assertEquals(true, aField.isEnabled());
        assertEquals(true, bField.isEnabled());
    }

    public static class X {
        String a;
        String b;
    }
}
