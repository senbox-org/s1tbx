package com.bc.ceres.jai.opimage;

import junit.framework.TestCase;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import com.bc.ceres.jai.operator.ExpressionDescriptorTest;
import com.bc.ceres.jai.opimage.ExpressionCode;
import com.bc.ceres.jai.opimage.ExpressionCodeGenerator;
import com.bc.ceres.compiler.Code;

public class ExpressionCodeGeneratorTest extends TestCase {

    public void testGeneratedCode() {
        final HashMap<String, RenderedImage> sourceMap = ExpressionDescriptorTest.createSourceMap();

        ExpressionCode code = ExpressionCodeGenerator.generate("com.bc.ceres.jai.opimage",
                                                               "ExpressionOpImage_1",
                                                               sourceMap,
                                                               DataBuffer.TYPE_DOUBLE,
                                                               "S1 * S2 / S3 % S4 + S5 - S6");
        assertNotNull(code);
        assertEquals("com.bc.ceres.jai.opimage.ExpressionOpImage_1", code.getClassName());
        assertNotNull(code.getSources());
        assertEquals(6, code.getSources().size());
        assertNotNull(code.getCharContent(true));
        assertTrue(code.getCharContent(true).length() > 0);

        // Write jave code so that we can test the generated class.
        // Note that actual compilation is done from source code in RAM (Code).
        write(code);
    }

    static void write(Code code) {
        try {
            File file = new File("./src/test/java/" + code.getClassName().replace('.', '/') + ".java");
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            String source = code.getCharContent(true).toString();
            //System.out.println("source = " + source);
            writer.write(source);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}