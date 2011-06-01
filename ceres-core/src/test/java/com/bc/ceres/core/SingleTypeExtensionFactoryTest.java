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

import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;

import static org.junit.Assert.*;

public class SingleTypeExtensionFactoryTest {

    private static final Object OBJECT_INSTANCE = new Object();

    @Test
    public void test1ArgConstructor() {
        try {
            new SingleTypeExtensionFactory<Object, Object>(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        SingleTypeExtensionFactory<Object, JButton> f1 = new SingleTypeExtensionFactory<Object, JButton>(JButton.class);
        assertSame(JButton.class, f1.getExtensionType());
        assertSame(JButton.class, f1.getExtensionSubType());
    }

    @Test
    public void test2ArgConstructor() {

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

        SingleTypeExtensionFactory<Object, JComponent> f2 = new SingleTypeExtensionFactory<Object, JComponent>(JComponent.class, JButton.class);
        assertSame(JComponent.class, f2.getExtensionType());
        assertSame(JButton.class, f2.getExtensionSubType());
    }

    @Test
    public void testFactory() {
        Object extension;

        SingleTypeExtensionFactory<Object, JButton> f1 = new SingleTypeExtensionFactory<Object, JButton>(JButton.class);
        extension = f1.getExtension(OBJECT_INSTANCE, JButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f1.getExtension(OBJECT_INSTANCE, AbstractButton.class);
        assertNull(extension);
    }

    @Test
    public void testFactoryWithSubClass() {
        Object extension;

        SingleTypeExtensionFactory<Object, JComponent> f2 = new SingleTypeExtensionFactory<Object, JComponent>(JComponent.class, JButton.class);
        extension = f2.getExtension(OBJECT_INSTANCE, AbstractButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(OBJECT_INSTANCE, JButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(OBJECT_INSTANCE, AbstractButton.class);
        assertTrue(extension instanceof AbstractButton);
        extension = f2.getExtension(OBJECT_INSTANCE, JComponent.class);
        assertTrue(extension instanceof JComponent);
        extension = f2.getExtension(OBJECT_INSTANCE, Component.class);
        assertNull(extension);
    }

    @Test
    public void testFactoryWith1ArgConstructor() {
        Object extension;

        SingleTypeExtensionFactory<Object, ExtensionClass> f2 = new SingleTypeExtensionFactory<Object, ExtensionClass>(ExtensionClass.class);
        extension = f2.getExtension(OBJECT_INSTANCE, ExtensionClass.class);
        assertTrue(extension instanceof ExtensionClass);
        assertSame(OBJECT_INSTANCE, ((ExtensionClass) extension).arg);
    }

    @Test
    public void testFactoryWithSubClassWith1ArgConstructor() {
        Object extension;

        SingleTypeExtensionFactory<Object, ExtensionClass> f2 = new SingleTypeExtensionFactory<Object, ExtensionClass>(ExtensionClass.class, ExtensionClassSubClass.class);
        extension = f2.getExtension(OBJECT_INSTANCE, ExtensionClass.class);
        assertTrue(extension instanceof ExtensionClassSubClass);
        assertSame(OBJECT_INSTANCE, ((ExtensionClassSubClass) extension).arg);
    }

    public static class ExtensionClass {
        Object arg;

        public ExtensionClass(Object arg) {
            this.arg = arg;
        }
    }
    public static class ExtensionClassSubClass extends ExtensionClass {

        public ExtensionClassSubClass(Object arg) {
            super(arg);
        }
    }
}
