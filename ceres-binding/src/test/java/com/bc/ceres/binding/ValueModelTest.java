package com.bc.ceres.binding;

import junit.framework.TestCase;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.lang.reflect.Field;


public class ValueModelTest extends TestCase {
    private ValueContainerFactory valueContainerFactory;

    @Override
    protected void setUp() throws Exception {

        valueContainerFactory = new ValueContainerFactory();
    }

    public  void testValueBackedValueContainer() throws ValidationException {
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
        assertEquals(72.9,vc.getModel("weight").getValue());
    }

    public  void testMapBackedValueContainer() throws ValidationException {
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
        assertEquals(72.9,vc.getModel("weight").getValue());
        assertEquals(72.9,map.get("weight"));
    }

    public  void testObjectBackedValueContainer() throws ValidationException {
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
        assertEquals(72.9,vc.getModel("weight").getValue());
        assertEquals(72.9, pojo.weight);
    }

    static class Pojo {
        String name;
        int age;
        double weight;
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {
        PropertyChangeEvent evt;

        public void propertyChange(PropertyChangeEvent evt) {
            this.evt = evt;
        }
    }
}
