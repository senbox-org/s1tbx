package Jama;

import junit.framework.TestCase;

/**
 * Wraps the original Jama test class into a JUnit test case.
 */
public class MatrixTest extends TestCase {
    public void testJamaApi() {
        int errorCount = TestMatrix.main2(new String[0]);
        assertEquals("JAMA test(s) failed, see console output for details", 0, errorCount);
    }
}
