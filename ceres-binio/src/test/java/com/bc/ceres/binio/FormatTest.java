package com.bc.ceres.binio;

import com.bc.ceres.binio.util.TypeBuilder;
import junit.framework.TestCase;

public class FormatTest extends TestCase {

    public void testBasisFormat() {
        Format basisFormat = new Format();
        Format format = new Format();
        assertNull(format.getBasisFormat());
        format.setBasisFormat(basisFormat);
        assertSame(basisFormat, format.getBasisFormat());

        basisFormat.addTypeDef("string80", TypeBuilder.SEQ(SimpleType.BYTE, 80));
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") == basisFormat.getTypeDef("string80"));
        
        format.addTypeDef("string80", TypeBuilder.SEQ(SimpleType.USHORT, 80));
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") != basisFormat.getTypeDef("string80"));

        format.removeTypeDef("string80");
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") == basisFormat.getTypeDef("string80"));

        basisFormat.removeTypeDef("string80");
        assertFalse(format.isTypeDef("string80"));
    }

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
