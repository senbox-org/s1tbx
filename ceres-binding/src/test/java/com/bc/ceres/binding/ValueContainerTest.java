package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.StringConverter;
import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;


public class ValueContainerTest extends TestCase {

    public void testValueBackedValueContainer() throws ValidationException {
        ValueContainer vc = ValueContainer.createValueBacked(Pojo.class);

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        vc.addPropertyChangeListener(pcl);

        testValueContainerModels(vc);

        assertEquals("(name:null-->Ernie)(age:0-->16)(weight:0.0-->72.9)(code:" + '\0' + "-->#)", pcl.trace);
    }

    public void testMapBackedValueContainer() throws ValidationException {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        ValueContainer vc = ValueContainer.createMapBacked(map, Pojo.class);
        assertEquals(0, map.size());

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
        ValueContainer vc = ValueContainer.createObjectBacked(pojo);
        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        vc.addPropertyChangeListener(pcl);

        testValueContainerModels(vc);

        assertEquals("(name:Hermann-->Ernie)(age:59-->16)(weight:82.5-->72.9)(code:X-->#)", pcl.trace);
        assertEquals('#', pojo.code);
        assertEquals("Ernie", pojo.name);
        assertEquals(16, pojo.age);
        assertEquals(72.9, pojo.weight);
    }

    public void testAddValueContainer() {
        final Pojo pojo = new Pojo();
        ValueContainer pojoContainer = ValueContainer.createObjectBacked(pojo);

        final B b = new B();
        ValueContainer bContainer = ValueContainer.createObjectBacked(b);

        pojoContainer.addValueContainer(bContainer);

        final ValueModel[] models = pojoContainer.getModels();
        assertEquals(9, models.length);

        final ValueModel nameModel = pojoContainer.getModel("name");
        assertNotNull(nameModel);
        assertEquals("Hermann", nameModel.getValueAsText());

        final ValueModel xModel = pojoContainer.getModel("x");
        assertNotNull(xModel);
        assertEquals("42", xModel.getValueAsText());
    }

    public void testAddValueCOntainer_duplicateModelNames() {
        final Pojo pojo = new Pojo();
        ValueContainer vc_1 = ValueContainer.createObjectBacked(pojo);
        ValueContainer vc_2 = ValueContainer.createObjectBacked(pojo);

        try {
            vc_1.addValueContainer(vc_2);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {            
        }
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
        ClassFieldDescriptorFactory valueDescriptorFactory = new MyValueDescriptorFactory();

        ValueContainer container;

        container = ValueContainer.createObjectBacked(new Pojo(), valueDescriptorFactory);
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        container = ValueContainer.createValueBacked(Pojo.class, valueDescriptorFactory);
        testDefaultValuesUsed(container);
        container.setDefaultValues();
        testDefaultValuesUsed(container);

        HashMap<String, Object> map = new HashMap<String, Object>(5);
        container = ValueContainer.createMapBacked(map, Pojo.class, valueDescriptorFactory);
        assertEquals(0, map.size());
        testInitialValuesUsed(container);
        container.setDefaultValues();
        assertEquals(4, map.size()); // 4 default values set
        testDefaultValuesUsed(container);

        map = new HashMap<String, Object>(5);
        map.put("code", 'X');
        map.put("name", "Hermann");
        map.put("age", 59);
        map.put("weight", 82.5);
        container = ValueContainer.createMapBacked(map, Pojo.class, valueDescriptorFactory);
        assertEquals(4, map.size());
        testCurrentValuesUsed(container);
        container.setDefaultValues();
        assertEquals(4, map.size()); // no change in size, 4 default values set
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

    private static class MyValueDescriptorFactory implements ClassFieldDescriptorFactory {
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
    }

}
