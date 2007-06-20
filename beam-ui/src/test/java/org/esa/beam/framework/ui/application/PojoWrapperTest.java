package org.esa.beam.framework.ui.application;

import junit.framework.TestCase;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.esa.beam.framework.ui.application.support.PojoWrapper;

public class PojoWrapperTest extends TestCase {
    public void testA() {
        A a = new A();
        PojoWrapper pwA = new PojoWrapper(a);

        pwA.setValue("name", "Mimi");
        pwA.setValue("age", 5);

        assertEquals("Mimi", a.name);
        assertEquals(5, a.age);
    }

    public void testB() {
        B b = new B();
        PojoWrapper pwB = new PojoWrapper(b);

        pwB.setValue("name", "Momo");
        pwB.setValue("age", 32);
        pwB.setValue("bloodGroup", 'A');

        assertEquals("Momo", b.name);
        assertEquals(32, b.age);
        assertEquals('A', b.bloodGroup);
    }

    public void testPropertyChangesFired() {

        B b = new B();
        PojoWrapper pwB = new PojoWrapper(b);
        PropertyChangeHandler pch = new PropertyChangeHandler();
        pwB.addPropertyChangeListener(pch);

        pwB.setValue("name", "Momo");
        pwB.setValue("age", 32);
        pwB.setValue("bloodGroup", 'A');

        assertEquals("name,null,Momo;age,0,32;bloodGroup,0,A;", pch.toString());
        pch.reset();

        pwB.setValue("name", "Mimi");
        pwB.setValue("age", 64);
        pwB.setValue("bloodGroup", 'B');

        assertEquals("name,Momo,Mimi;age,32,64;bloodGroup,A,B;", pch.toString());
    }


    static class A {
        String name;
        int age;
    }

    static class B extends A {
        private char bloodGroup = '0';
    }

    private static class PropertyChangeHandler implements PropertyChangeListener {
        StringBuffer sb = new StringBuffer();

        public void propertyChange(PropertyChangeEvent evt) {
            sb.append(evt.getPropertyName());
            sb.append(',');
            sb.append(evt.getOldValue());
            sb.append(',');
            sb.append(evt.getNewValue());
            sb.append(';');
        }

        public void reset() {
            sb.setLength(0);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
