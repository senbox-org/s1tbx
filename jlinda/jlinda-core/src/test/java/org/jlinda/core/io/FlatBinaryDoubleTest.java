package org.jlinda.core.io;

import org.jlinda.core.Window;
import org.junit.*;

import java.io.File;
import java.nio.ByteOrder;

public class FlatBinaryDoubleTest {

    private static FlatBinaryDouble flatBinaryDoubleRead;
    private static FlatBinaryDouble flatBinaryDoubleWrite;
    private static Window testDataWindow;

    private static double[][] testData;

    private static FlatBinaryDouble flatBinaryDoubleLittleRead;
    private static FlatBinaryDouble flatBinaryDoubleLittleWrite;

    @BeforeClass
    public static void setupTestData() throws Exception {

        testDataWindow = new Window(0, 123, 0, 321);
        int lines = (int) testDataWindow.lines();
        int pixels = (int) testDataWindow.pixels();
        testData = new double[lines][pixels];

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < pixels; j++) {
                testData[i][j] = Math.random() * 100;
            }
        }

        flatBinaryDoubleRead = new FlatBinaryDouble();
        flatBinaryDoubleWrite = new FlatBinaryDouble();

        flatBinaryDoubleRead.setFile(new File("test/test.in"));
        flatBinaryDoubleWrite.setFile(new File("test/test.out"));

        flatBinaryDoubleLittleRead = new FlatBinaryDouble();
        flatBinaryDoubleLittleRead.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        flatBinaryDoubleLittleWrite = new FlatBinaryDouble();
        flatBinaryDoubleLittleWrite.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        flatBinaryDoubleLittleRead.setFile(new File("test/test.in.swapped"));
        flatBinaryDoubleLittleWrite.setFile(new File("test/test.out.swapped"));

    }

    @AfterClass
    public static void cleanTestData() {

//        if (!testFile.exists())
//            throw new IllegalArgumentException("Delete: no such file or directory: " + testFile.getName());
//
//        boolean success = testFile.delete();
//
//        if (!success)
//            throw new IllegalArgumentException("Delete: deletion of file" + testFile.getName() + " failed");
//
//        System.gc();

    }

    @Test
    public void testCreateAndCheck() throws Exception {
        flatBinaryDoubleWrite.create();
        Assert.assertEquals("File creating: ", true, flatBinaryDoubleWrite.checkExists());
    }

    @Test
    public void testWritingReadingData() throws Exception {

        flatBinaryDoubleWrite.setOutStream();
        flatBinaryDoubleWrite.setDataWindow(new Window(testDataWindow));
        flatBinaryDoubleWrite.setData(testData);
        flatBinaryDoubleWrite.writeToStream();

        flatBinaryDoubleRead.setFile(flatBinaryDoubleWrite.file);
        flatBinaryDoubleRead.setDataWindow(new Window(testDataWindow));
        flatBinaryDoubleRead.setInStream();
        flatBinaryDoubleRead.readFromStream();

        double[][] testData_ACTUAL = flatBinaryDoubleRead.getData();

        Assert.assertArrayEquals(testData, testData_ACTUAL);

    }

    @Ignore
    @Test
    public void testWritingReadingLittleEndianData() throws Exception {

        flatBinaryDoubleLittleWrite.setOutStream();
        flatBinaryDoubleLittleWrite.setDataWindow(new Window(testDataWindow));
        flatBinaryDoubleLittleWrite.setData(testData);
        flatBinaryDoubleLittleWrite.writeToStream();

        flatBinaryDoubleLittleRead.setFile(flatBinaryDoubleLittleWrite.file);
        flatBinaryDoubleLittleRead.setDataWindow(new Window(testDataWindow));
        flatBinaryDoubleLittleRead.setInStream();
        flatBinaryDoubleLittleRead.readFromStream();

        double[][] testData_ACTUAL = flatBinaryDoubleLittleRead.getData();
        Assert.assertArrayEquals(testData, testData_ACTUAL);

    }

/*
    @Ignore
    @Test
    public void testCanRead() throws Exception {
    }

    @Ignore
    @Test
    public void testCanWrite() throws Exception {
    }
*/
}
