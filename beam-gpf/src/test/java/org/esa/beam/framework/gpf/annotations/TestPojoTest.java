package org.esa.beam.framework.gpf.annotations;

import junit.framework.TestCase;

import java.lang.reflect.Field;

public class TestPojoTest extends TestCase {
    private Class<? extends TestPojo> testPojoClass;

    @Override
    public void setUp() {
        TestPojo testPojo = new TestPojo();
        testPojoClass = testPojo.getClass();
    }

    public void testTargetProductAnnotation() throws NoSuchFieldException {
        Field vapourField = testPojoClass.getDeclaredField("vapour");
        TargetProduct tpa = vapourField.getAnnotation(TargetProduct.class);
        assertNotNull(tpa);
    }

    public void testSourceProductAnnotation() throws NoSuchFieldException {
        Field brrField = testPojoClass.getDeclaredField("brr");
        SourceProduct spa = brrField.getAnnotation(SourceProduct.class);
        assertNotNull(spa);
        assertEquals(true, spa.optional());
        assertEquals("MERIS_BRR", spa.type());
        assertEquals(2, spa.bands().length);
        assertEquals("radiance_2", spa.bands()[0]);
        assertEquals("radiance_5", spa.bands()[1]);
    }

    public void testParameterAnnotation() throws NoSuchFieldException {
        Field percentage = testPojoClass.getDeclaredField("percentage");
        Parameter pa = percentage.getAnnotation(Parameter.class);
        assertNotNull(pa);
        assertEquals("(0, 100]", pa.interval());
    }

    public void testTargetPropertyAnnotation() throws NoSuchFieldException {
        Field propertyField = testPojoClass.getDeclaredField("property");
        TargetProperty tpa = propertyField.getAnnotation(TargetProperty.class);
        assertNotNull(tpa);
        assertEquals("a test property", tpa.description());
        assertEquals("bert", tpa.alias());
    }
}
