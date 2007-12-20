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

        ValueModel nameModel = vc.getModel("name");
        assertNotNull(nameModel);
        nameModel.setValue("Bert");
        assertEquals("Bert", vc.getModel("name").getValue());

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        nameModel.addPropertyChangeListener(pcl);

        nameModel.setValue("Ernie");
        assertEquals("Ernie", vc.getModel("name").getValue());
        assertEquals("name", pcl.evt.getPropertyName());
        assertEquals("Bert", pcl.evt.getOldValue());
        assertEquals("Ernie", pcl.evt.getNewValue());

        ValueModel ageModel = vc.getModel("age");
        ageModel.setValue(16);
        assertEquals(16, vc.getModel("age").getValue());

        ValueModel weightModel = vc.getModel("weight");
        weightModel.setValue(72.9);
        assertEquals(72.9, vc.getModel("weight").getValue());
    }

    public void testMapBackedValueContainer() throws ValidationException {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        ValueContainer vc = valueContainerFactory.createMapBackedValueContainer(Pojo.class, map);

        ValueModel nameModel = vc.getModel("name");
        assertNotNull(nameModel);
        nameModel.setValue("Bert");
        assertEquals("Bert", vc.getModel("name").getValue());
        assertEquals("Bert", map.get("name"));

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        nameModel.addPropertyChangeListener(pcl);

        nameModel.setValue("Ernie");
        assertEquals("Ernie", vc.getModel("name").getValue());
        assertEquals("Ernie", map.get("name"));
        assertEquals("name", pcl.evt.getPropertyName());
        assertEquals("Bert", pcl.evt.getOldValue());
        assertEquals("Ernie", pcl.evt.getNewValue());

        ValueModel ageModel = vc.getModel("age");
        ageModel.setValue(16);
        assertEquals(16, vc.getModel("age").getValue());
        assertEquals(16, map.get("age"));

        ValueModel weightModel = vc.getModel("weight");
        weightModel.setValue(72.9);
        assertEquals(72.9, vc.getModel("weight").getValue());
        assertEquals(72.9, map.get("weight"));
    }

    public void testObjectBackedValueContainer() throws ValidationException {
        Pojo pojo = new Pojo();
        ValueContainer vc = valueContainerFactory.createObjectBackedValueContainer(pojo);

        ValueModel nameModel = vc.getModel("name");
        assertNotNull(nameModel);
        nameModel.setValue("Bert");
        assertEquals("Bert", pojo.name);

        pojo.name = "Bibo";
        assertEquals("Bibo", nameModel.getValue());

        MyPropertyChangeListener pcl = new MyPropertyChangeListener();
        nameModel.addPropertyChangeListener(pcl);

        nameModel.setValue("Ernie");
        assertEquals("Ernie", pojo.name);
        assertEquals("name", pcl.evt.getPropertyName());
        assertEquals("Bibo", pcl.evt.getOldValue());
        assertEquals("Ernie", pcl.evt.getNewValue());

        ValueModel ageModel = vc.getModel("age");
        ageModel.setValue(16);
        assertEquals(16, vc.getModel("age").getValue());
        assertEquals(16, pojo.age);

        ValueModel weightModel = vc.getModel("weight");
        weightModel.setValue(72.9);
        assertEquals(72.9, vc.getModel("weight").getValue());
        assertEquals(72.9, pojo.weight);
    }

    public void testInitialValues() {
    }

    public void testDefaultValues() {
        ValueContainerFactory vcFactory = new ValueContainerFactory(new ValueDescriptorFactory() {
            public ValueDescriptor createValueDescriptor(Field field) {
                ValueDescriptor descriptor = new ValueDescriptor(field.getName(), field.getType());
                if (!field.getName().endsWith("NoDefault")) {
                    if (field.getType().equals(String.class)) {
                        descriptor.setConverter(new StringConverter());
                        descriptor.setDefaultValue("It's a string.");
                    } else if (field.getType().equals(int.class)) {
                        descriptor.setConverter(new IntegerConverter());
                        descriptor.setDefaultValue(42);
                    } else if (field.getType().equals(double.class)) {
                        descriptor.setConverter(new DoubleConverter());
                        descriptor.setDefaultValue(3.67);
                    } else {
                        fail("Test is not prepared for " + field.getType() + " types.");
                    }
                }
                return descriptor;
            }
        });

        ValueContainer container = vcFactory.createValueBackedValueContainer(Pojo.class);
        assertEquals("It's a string.", container.getValue("name"));
        assertEquals(42, container.getValue("age"));
        assertEquals(3.67, container.getValue("weight"));
        assertEquals(null, container.getValue("nameNoDefault"));
        assertEquals(0, container.getValue("ageNoDefault"));
        assertEquals(0.0, container.getValue("weightNoDefault"));

        HashMap<String, Object> map = new HashMap<String, Object>(5);
        container = vcFactory.createMapBackedValueContainer(Pojo.class, map);
        assertEquals("It's a string.", container.getValue("name"));
        assertEquals(42, container.getValue("age"));
        assertEquals(3.67, container.getValue("weight"));
        assertEquals(null, container.getValue("nameNoDefault"));
        assertEquals(0, container.getValue("ageNoDefault"));
        assertEquals(0.0, container.getValue("weightNoDefault"));

        container = vcFactory.createObjectBackedValueContainer(new Pojo());
        assertEquals("It's a string.", container.getValue("name"));
        assertEquals(42, container.getValue("age"));
        assertEquals(3.67, container.getValue("weight"));
        assertEquals(null, container.getValue("nameNoDefault"));
        assertEquals(0, container.getValue("ageNoDefault"));
        assertEquals(0.0, container.getValue("weightNoDefault"));
    }

    static class Pojo {

        String name;
        int age;
        double weight;

        String nameNoDefault;
        int ageNoDefault;
        double weightNoDefault;
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        PropertyChangeEvent evt;

        public void propertyChange(PropertyChangeEvent evt) {
            this.evt = evt;
        }
    }
}
