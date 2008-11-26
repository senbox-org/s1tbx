package com.bc.ceres.binio;

import com.bc.ceres.binio.util.TypeBuilder;
import junit.framework.TestCase;

public class FormatTest extends TestCase {
    public void testTypeDef() {
        Format format = new Format(TypeBuilder.COMP("Point",
                                                    TypeBuilder.MEMBER("x", SimpleType.FLOAT),
                                                    TypeBuilder.MEMBER("y", SimpleType.FLOAT)));

        assertEquals(false, format.isTypeDef("bool"));
        try {
            format.getTypeDef("bool");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        format.addTypeDef("bool", SimpleType.BYTE);
        assertSame(SimpleType.BYTE, format.getTypeDef("bool"));
        assertEquals(true, format.isTypeDef("bool"));

        assertEquals(false, format.isTypeDef("ui32"));
        format.addTypeDef("ui32", SimpleType.UINT);
        assertSame(SimpleType.UINT, format.getTypeDef("ui32"));


        assertSame(SimpleType.BYTE, format.getTypeDef("bool"));
        format.addTypeDef("bool", SimpleType.BYTE);
        try {
            format.addTypeDef("bool", SimpleType.SHORT);
            fail();
        } catch (IllegalArgumentException e) {
// ok
        }
        Type type = format.removeTypeDef("bool");
        assertSame(SimpleType.BYTE, type);
        assertEquals(false, format.isTypeDef("bool"));
        format.addTypeDef("bool", SimpleType.BYTE);
        assertEquals(true, format.isTypeDef("bool"));
    }
}
