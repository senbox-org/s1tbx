package $groupId;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MyProductReaderTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MyProductReaderTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MyProductReaderTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testMyProducWriter() {
        assertTrue(true);
    }
}
