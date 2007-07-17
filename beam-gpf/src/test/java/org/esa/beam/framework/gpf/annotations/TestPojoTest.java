package org.esa.beam.framework.gpf.annotations;

import junit.framework.TestCase;

import java.lang.reflect.Field;

public class TestPojoTest extends TestCase {

    public void testAnnotations() throws NoSuchFieldException {
        TestPojo testPojo = new TestPojo();
        Class<? extends TestPojo> testPojoClass = testPojo.getClass();

        Field vapourField = testPojoClass.getDeclaredField("vapour");
        TargetProduct tpa = vapourField.getAnnotation(TargetProduct.class);
        assertNotNull(tpa);

        Field brrField = testPojoClass.getDeclaredField("brr");
        SourceProduct spa = brrField.getAnnotation(SourceProduct.class);
        assertNotNull(spa);
        assertEquals(true, spa.optional());
        assertEquals("MERIS_BRR", spa.type());
        assertEquals(2, spa.bands().length);
        assertEquals("radiance_2", spa.bands()[0]);
        assertEquals("radiance_5", spa.bands()[1]);

        Field percentage = testPojoClass.getDeclaredField("percentage");
        Parameter pa = percentage.getAnnotation(Parameter.class);
        assertNotNull(pa);
        assertEquals("(0, 100]", pa.interval());
    }
}
