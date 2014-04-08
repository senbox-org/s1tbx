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

package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.StringConverter;
import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;


public class PropertyContainerTest extends TestCase {

    public void testValueBackedValueContainer() throws ValidationException {
        PropertyContainer pc = PropertyContainer.createValueBacked(Pojo.class);

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        pc.addPropertyChangeListener(pcl);

        testValueContainerModels(pc);

        assertEquals("(name:null-->Ernie)(age:0-->16)(weight:0.0-->72.9)(code:" + '\0' + "-->#)", pcl.trace);
    }

    public void testMapBackedValueContainer() throws ValidationException {
        final HashMap<String, Object> map = new HashMap<>();
        PropertyContainer pc = PropertyContainer.createMapBacked(map, Pojo.class);
        assertEquals(0, map.size());

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        pc.addPropertyChangeListener(pcl);

        testValueContainerModels(pc);

        assertEquals("(name:null-->Ernie)(age:0-->16)(weight:0.0-->72.9)(code:" + '\0' + "-->#)", pcl.trace);
        assertEquals('#', map.get("code"));
        assertEquals("Ernie", map.get("name"));
        assertEquals(16, (int)map.get("age"));
        assertEquals(72.9, (double)map.get("weight"), 1.0e-6);
    }

    public void testObjectBackedValueContainer() throws ValidationException {
        Pojo pojo = new Pojo();
        PropertyContainer pc = PropertyContainer.createObjectBacked(pojo);
        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        pc.addPropertyChangeListener(pcl);

        testValueContainerModels(pc);

        assertEquals("(name:Hermann-->Ernie)(age:59-->16)(weight:82.5-->72.9)(code:X-->#)", pcl.trace);
        assertEquals('#', pojo.code);
        assertEquals("Ernie", pojo.name);
        assertEquals(16, pojo.age);
        assertEquals(72.9, pojo.weight);
    }

    private void testValueContainerModels(PropertyContainer vc) throws ValidationException {
        final Property name = vc.getProperty("name");
        assertNotNull(name);
        name.setValue("Ernie");
        assertEquals("Ernie", name.getValue());
        try {
            name.setValue(3);
            fail("ValidationException expected");
        } catch (ValidationException ignored) {
        }

        final Property age = vc.getProperty("age");
        assertNotNull(age);
        age.setValue(16);
        assertEquals(16, (int)age.getValue());
        try {
            age.setValue("");
            fail("ValidationException expected");
        } catch (ValidationException ignored) {
        }

        final Property weight = vc.getProperty("weight");
        assertNotNull(weight);
        weight.setValue(72.9);
        assertEquals(72.9, (double)weight.getValue(), 1.0e-6);
        try {
            weight.setValue("");
            fail("ValidationException expected");
        } catch (ValidationException ignored) {
        }

        final Property code = vc.getProperty("code");
        assertNotNull(code);
        code.setValue('#');
        assertEquals('#', (char)code.getValue());
        try {
            code.setValue(2.5);
            fail("ValidationException expected");
        } catch (ValidationException ignored) {
        }

        final Property unknownModel = vc.getProperty("unknown");
        assertNull(unknownModel);
    }

    public void testDefaultValues() throws ValidationException {
        PropertyDescriptorFactory valueDescriptorFactory = new MyValueDescriptorFactory();

        PropertyContainer container;

        container = PropertyContainer.createObjectBacked(new Pojo(), valueDescriptorFactory);
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        container = PropertyContainer.createValueBacked(Pojo.class, valueDescriptorFactory);
        testDefaultValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        HashMap<String, Object> map = new HashMap<>(5);
        container = PropertyContainer.createMapBacked(map, Pojo.class, valueDescriptorFactory);
        assertEquals(0, map.size());
        testInitialValuesUsed(container);
        container.setDefaultValues();
        assertEquals(4, map.size()); // 4 default values set
        testDefaultValuesUsed(container);

        map = new HashMap<>(5);
        map.put("code", 'X');
        map.put("name", "Hermann");
        map.put("age", 59);
        map.put("weight", 82.5);
        container = PropertyContainer.createMapBacked(map, Pojo.class, valueDescriptorFactory);
        assertEquals(4, map.size());
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        assertEquals(4, map.size()); // no change in size, 4 default values set
        testDefaultValuesUsed(container);
    }

    private void testInitialValuesUsed(PropertyContainer container) {
        assertEquals('\0', (char)container.getValue("code"));
        assertEquals(null, container.getValue("name"));
        assertEquals(0, (int)container.getValue("age"));
        assertEquals(0.0, (double)container.getValue("weight"), 1.0e-6);
        testUnhandledValues(container);
    }

    private void testCurrentValuesUsed(PropertyContainer container) {
        assertEquals('X', (char)container.getValue("code"));
        assertEquals("Hermann", container.getValue("name"));
        assertEquals(59, (int)container.getValue("age"));
        assertEquals(82.5, (double)container.getValue("weight"), 1.0e-6);
        testUnhandledValues(container);
    }

    private void testDefaultValuesUsed(PropertyContainer container) {
        assertEquals('Y', (char)container.getValue("code"));
        assertEquals("Kurt", container.getValue("name"));
        assertEquals(42, (int)container.getValue("age"));
        assertEquals(90.0, (double)container.getValue("weight"), 1.0e-6);
        testUnhandledValues(container);
    }

    private void testUnhandledValues(PropertyContainer container) {
        assertEquals('\0', (char)container.getValue("codeNoDefault"));
        assertEquals(null, container.getValue("nameNoDefault"));
        assertEquals(0, (int)container.getValue("ageNoDefault"));
        assertEquals(0.0, (double)container.getValue("weightNoDefault"), 1.0e-6);
    }

    abstract static class PojoBase {
        char code = 'X';
        char codeNoDefault;
    }

    static class Pojo extends PojoBase {
        String name = "Hermann";
        int age = 59;
        double weight = 82.5;

        String nameNoDefault;
        int ageNoDefault;
        double weightNoDefault;
    }

    abstract static class A {
        A() {
            try {
                getClass().getDeclaredField("x").setInt(this, 42);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class B extends A {
        int x;

        B() {
        }

        B(int x) {
            this.x = x;
        }
    }

    public void testDerivedClassReflectionWorksInBaseClassInitializer() {
        assertEquals(42, new B().x);
        assertEquals(99, new B(99).x);
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        String trace = "";

        public void propertyChange(PropertyChangeEvent evt) {
            trace += "(" + evt.getPropertyName() + ":" + evt.getOldValue() + "-->" + evt.getNewValue() + ")";
        }
    }

    private static class MyValueDescriptorFactory implements PropertyDescriptorFactory {
        public PropertyDescriptor createValueDescriptor(Field field) {
            PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), field.getType());
            if (!field.getName().endsWith("NoDefault")) {
                if (field.getType().equals(String.class)) {
                    descriptor.setConverter(new StringConverter());
                    descriptor.setDefaultValue("Kurt");
                } else if (field.getType().equals(char.class)) {
                    descriptor.setConverter(new IntegerConverter());
                    descriptor.setDefaultValue('Y');
                } else if (field.getType().equals(int.class)) {
                    descriptor.setConverter(new IntegerConverter());
                    descriptor.setDefaultValue(42);
                } else if (field.getType().equals(double.class)) {
                    descriptor.setConverter(new DoubleConverter());
                    descriptor.setDefaultValue(90.0);
                } else {
                    fail("Test is not prepared for " + field.getType() + " types.");
                }
            }
            return descriptor;
        }
    }

}
