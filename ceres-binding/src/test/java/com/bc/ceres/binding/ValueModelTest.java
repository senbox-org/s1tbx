package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.StringConverter;
import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;


public class ValueModelTest extends TestCase {

    private ValueContainerFactory valueContainerFactory;

    @Override
    protected void setUp() throws Exception {

        valueContainerFactory = new ValueContainerFactory();
    }

    public void testValueBackedValueContainer() throws ValidationException {
        ValueContainer vc = valueContainerFactory.createValueBackedValueContainer(Pojo.class);
        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        vc.addPropertyChangeListener(pcl);

        testValueContainerModels(vc);
        
        assertEquals("(name:null-->Ernie)(age:0-->16)(weight:0.0-->72.9)(code:" + '\0' + "-->#)", pcl.trace);
    }

    public void testMapBackedValueContainer() throws ValidationException {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        ValueContainer vc = valueContainerFactory.createMapBackedValueContainer(Pojo.class, map);
        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        vc.addPropertyChangeListener(pcl);

        testValueContainerModels(vc);

        assertEquals("(name:null-->Ernie)(age:0-->16)(weight:0.0-->72.9)(code:" + '\0' + "-->#)", pcl.trace);
        assertEquals('#', map.get("code"));
        assertEquals("Ernie", map.get("name"));
        assertEquals(16, map.get("age"));
        assertEquals(72.9, map.get("weight"));
    }

    public void testObjectBackedValueContainer() throws ValidationException {
        Pojo pojo = new Pojo();
        ValueContainer vc = valueContainerFactory.createObjectBackedValueContainer(pojo);
        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        vc.addPropertyChangeListener(pcl);

        testValueContainerModels(vc);

        assertEquals("(name:Hermann-->Ernie)(age:59-->16)(weight:82.5-->72.9)(code:X-->#)", pcl.trace);
        assertEquals('#', pojo.code);
        assertEquals("Ernie", pojo.name);
        assertEquals(16, pojo.age);
        assertEquals(72.9, pojo.weight);
    }

    private void testValueContainerModels(ValueContainer vc) throws ValidationException {

        final ValueModel nameModel = vc.getModel("name");
        assertNotNull(nameModel);
        nameModel.setValue("Ernie");
        assertEquals("Ernie", nameModel.getValue());
        try {
            nameModel.setValue(3);
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

        final ValueModel ageModel = vc.getModel("age");
        assertNotNull(ageModel);
        ageModel.setValue(16);
        assertEquals(16, ageModel.getValue());
        try {
            ageModel.setValue("");
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

        final ValueModel weightModel = vc.getModel("weight");
        assertNotNull(weightModel);
        weightModel.setValue(72.9);
        assertEquals(72.9, weightModel.getValue());
        try {
            weightModel.setValue("");
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

        final ValueModel codeModel = vc.getModel("code");
        assertNotNull(codeModel);
        codeModel.setValue('#');
        assertEquals('#', codeModel.getValue());
        try {
            codeModel.setValue(2.5);
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

        final ValueModel unknownModel = vc.getModel("unknown");
        assertNull(unknownModel);
    }

    public void testDefaultValues() throws ValidationException {
        ValueContainerFactory vcFactory = new ValueContainerFactory(new ValueDescriptorFactory() {
            public ValueDescriptor createValueDescriptor(Field field) {
                ValueDescriptor descriptor = new ValueDescriptor(field.getName(), field.getType());
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
        });

        ValueContainer container;

        container = vcFactory.createObjectBackedValueContainer(new Pojo());
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        container = vcFactory.createValueBackedValueContainer(Pojo.class);
        testDefaultValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        HashMap<String, Object> map = new HashMap<String, Object>(5);
        container = vcFactory.createMapBackedValueContainer(Pojo.class, map);
        testInitialValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        map = new HashMap<String, Object>(5);
        map.put("code", 'X');
        map.put("name", "Hermann");
        map.put("age", 59);
        map.put("weight", 82.5);
        container = vcFactory.createMapBackedValueContainer(Pojo.class, map);
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);
    }

    private void testInitialValuesUsed(ValueContainer container) {
        assertEquals('\0', container.getValue("code"));
        assertEquals(null, container.getValue("name"));
        assertEquals(0, container.getValue("age"));
        assertEquals(0.0, container.getValue("weight"));
        testUnhandledValues(container);
    }

    private void testCurrentValuesUsed(ValueContainer container) {
        assertEquals('X', container.getValue("code"));
        assertEquals("Hermann", container.getValue("name"));
        assertEquals(59, container.getValue("age"));
        assertEquals(82.5, container.getValue("weight"));
        testUnhandledValues(container);
    }

    private void testDefaultValuesUsed(ValueContainer container) {
        assertEquals('Y', container.getValue("code"));
        assertEquals("Kurt", container.getValue("name"));
        assertEquals(42, container.getValue("age"));
        assertEquals(90.0, container.getValue("weight"));
        testUnhandledValues(container);
    }

    private void testUnhandledValues(ValueContainer container) {
        assertEquals('\0', container.getValue("codeNoDefault"));
        assertEquals(null, container.getValue("nameNoDefault"));
        assertEquals(0, container.getValue("ageNoDefault"));
        assertEquals(0.0, container.getValue("weightNoDefault"));
    }

    static class PojoBase {
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

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        String trace = "";

        public void propertyChange(PropertyChangeEvent evt) {
            trace += "(" + evt.getPropertyName() + ":" + evt.getOldValue() + "-->" + evt.getNewValue() + ")";
        }
    }
}
