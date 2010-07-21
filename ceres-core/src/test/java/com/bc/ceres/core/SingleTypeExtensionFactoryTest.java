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

package com.bc.ceres.core;

import junit.framework.TestCase;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;

public class SingleTypeExtensionFactoryTest extends TestCase {
    public void testConstruction() {

        try {
            new SingleTypeExtensionFactory<Object, Object>(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            new SingleTypeExtensionFactory<Object, Object>(null, null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            new SingleTypeExtensionFactory<Object, Object>(Object.class, null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            new SingleTypeExtensionFactory<Object, Object>(null, Object.class);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            Class<?> aClass = AbstractButton.class;
            new SingleTypeExtensionFactory<Object, JButton>(JButton.class, (Class<? extends JButton>) aClass);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        SingleTypeExtensionFactory<Object, JButton> f1 = new SingleTypeExtensionFactory<Object, JButton>(JButton.class);
        assertSame(JButton.class, f1.getExtensionType());
        assertSame(JButton.class, f1.getExtensionSubType());

        SingleTypeExtensionFactory<Object, JComponent> f2 = new SingleTypeExtensionFactory<Object, JComponent>(JComponent.class, JButton.class);
        assertSame(JComponent.class, f2.getExtensionType());
        assertSame(JButton.class, f2.getExtensionSubType());
    }

    public void testFactory() {
        Object extension;

        SingleTypeExtensionFactory<Object, JButton> f1 = new SingleTypeExtensionFactory<Object, JButton>(JButton.class);
        extension = f1.getExtension(Object.class, JButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f1.getExtension(Object.class, AbstractButton.class);
        assertNull(extension);

        SingleTypeExtensionFactory<Object, JComponent> f2 = new SingleTypeExtensionFactory<Object, JComponent>(JComponent.class, JButton.class);
        extension = f2.getExtension(Object.class, AbstractButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(Object.class, JButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(Object.class, AbstractButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(Object.class, JComponent.class);
        assertTrue(extension instanceof JComponent);
        extension = f2.getExtension(Object.class, Component.class);
        assertNull(extension);
    }
}
