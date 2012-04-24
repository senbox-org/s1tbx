package org.esa.beam.util.io;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Norman Fomferra
 */
public class WildcardMatcherTest {

    @Test
    public void testFileAssumptions() throws Exception {
        String baseDir = new File("").getCanonicalPath();
        assertEquals(baseDir, new File(".").getCanonicalPath());
        assertEquals(baseDir, new File("./").getCanonicalPath());
        assertEquals(baseDir + File.separator + "test.txt",
                     new File("./test.txt").getCanonicalPath());
        assertEquals(baseDir + File.separator + "test.txt",
                     new File("sub/../test.txt").getCanonicalPath());
        assertEquals(baseDir + File.separator + "sub" + File.separator + "test.txt",
                     new File("sub/./test.txt").getCanonicalPath());
    }


    @Test
    public void testSingleFileNoWildcardUsed() throws Exception {
        WildcardMatcher m = new WildcardMatcher("test.N1");
        assertEquals(m.isWindowsFs() ? "test\\.n1" : "test\\.N1", m.getRegex());

        assertTrue(m.matches("test.N1"));

        assertFalse(m.matches("test.jpg"));
        assertFalse(m.matches("rest.N1"));
        assertFalse(m.matches("x/test.N1"));
    }

    @Test
    public void testMultiDirectoryNoWildcardUsed_Unix() throws Exception {
        WildcardMatcher m = new WildcardMatcher("/home/norman/meris/data.nc", false);
        assertEquals("/home/norman/meris/data\\.nc", m.getRegex());

        assertTrue(m.matches("/home/norman/meris/data.nc"));

        assertFalse(m.matches("home/norman/meris/data.nc"));
        assertFalse(m.matches("/home/norman/MERIS/data.nc"));
        assertFalse(m.matches("/home/norman/meris/data.NC"));
    }

    @Test
    public void testMultiDirectoryNoWildcardUsed_Win() throws Exception {
        WildcardMatcher m = new WildcardMatcher("C:\\Users\\Norman\\MERIS\\data.nc", true);
        assertEquals("c:/users/norman/meris/data\\.nc", m.getRegex());

        assertTrue(m.matches("C:\\Users\\Norman\\MERIS\\data.nc"));
        assertTrue(m.matches("C:\\Users\\Norman\\meris\\data.nc"));
        assertTrue(m.matches("C:\\Users\\Norman\\meris\\data.NC"));
        assertTrue(m.matches("c:\\Users\\Norman\\meris\\data.nc"));

        assertTrue(m.matches("C:/Users/Norman/MERIS/data.nc"));
        assertTrue(m.matches("C:/Users/Norman/meris/data.nc"));
        assertTrue(m.matches("C:/Users/Norman/meris/data.NC"));
        assertTrue(m.matches("c:/Users/Norman/meris/data.nc"));

        assertFalse(m.matches("D:\\Users\\Norman\\MERIS\\data.nc"));
        assertFalse(m.matches("\\Users\\Norman\\MERIS\\data.nc"));
    }

    @Test
    public void testQuoteInFilename() throws Exception {
        WildcardMatcher m = new WildcardMatcher("te?t.N1");
        assertEquals(m.isWindowsFs() ? "te.t\\.n1" : "te.t\\.N1", m.getRegex());

        assertTrue(m.matches("test.N1"));
        assertTrue(m.matches("te?t.N1"));

        assertFalse(m.matches("tet.N1"));
    }

    @Test
    public void testStarInFilename() throws Exception {
        WildcardMatcher m = new WildcardMatcher("*.N1");
        assertEquals(m.isWindowsFs() ? "[^/:]*\\.n1" : "[^/:]*\\.N1", m.getRegex());

        assertTrue(m.matches("test.N1"));
        assertTrue(m.matches("MER_RR.N1"));

        assertFalse(m.matches("MER_RR"));
        assertFalse(m.matches("MER_RR.txt"));
    }

    @Test
    public void testStarInBetween() throws Exception {
        WildcardMatcher m = new WildcardMatcher("foo/*/test.txt");
        assertEquals("foo/[^/:]*/test\\.txt", m.getRegex());

        assertTrue(m.matches("foo//test.txt"));
        assertTrue(m.matches("foo/bar/test.txt"));

        assertFalse(m.matches("/foo/test.txt"));
        assertFalse(m.matches("foo/bar/doz/gna/test.txt"));
    }

    @Test
    public void testStarAtEnd() throws Exception {
        WildcardMatcher m = new WildcardMatcher("foo/*");
        assertEquals("foo/[^/:]*", m.getRegex());

        assertTrue(m.matches("foo/test.txt"));
        assertTrue(m.matches("foo/bar"));

        assertFalse(m.matches("foo"));
        assertFalse(m.matches("foo/bar/test.txt"));
        assertFalse(m.matches("/foo/"));
        assertFalse(m.matches("foo/bar/"));
        assertFalse(m.matches("foo/bar/doz/gna/test.txt"));
    }

    @Test
    public void testDoubleStarInBetween() throws Exception {
        WildcardMatcher m = new WildcardMatcher("foo/**/test.txt");
        assertEquals("foo((/.*/)?|/)test\\.txt", m.getRegex());

        assertTrue(m.matches("foo/test.txt"));
        assertTrue(m.matches("foo/bar/test.txt"));
        assertTrue(m.matches("foo/bar/doz/test.txt"));
        assertTrue(m.matches("foo/bar/doz/gna/test.txt"));

        assertFalse(m.matches("/foo/test.txt"));
        assertFalse(m.matches("foo/bar/doz/gna/test.zip"));
    }

    @Test
    public void testDoubleStarAtEnd() throws Exception {
        WildcardMatcher m = new WildcardMatcher("foo/**");
        assertEquals("foo(/.*)?", m.getRegex());

        assertTrue(m.matches("foo"));
        assertTrue(m.matches("foo/"));
        assertTrue(m.matches("foo/bar/doz/test.txt"));
        assertTrue(m.matches("foo/bar/doz/gna/test.txt"));
        assertTrue(m.matches("foo/test.txt"));
        assertTrue(m.matches("foo/bar/doz/gna/test.zip"));
        if (m.isWindowsFs()) {
            assertTrue(m.matches("foo\\bar\\doz\\gna\\test.txt"));
        }

        assertFalse(m.matches("/foo/bar/doz/gna/test.zip"));
        assertFalse(m.matches("bar/doz/gna/test.zip"));
    }

    @Test
    public void testGlobWithDoubleStar() throws Exception {
        String dir = getTestdataDir();
        File[] files = WildcardMatcher.glob(dir + "/**/*.txt");
        assertNotNull(files);
        for (File file : files) {
            //System.out.println("file = " + file);
        }
        assertEquals(3, files.length);
        Arrays.sort(files);
        assertEquals(new File(dir, "foo/bar/test1.txt"), files[0]);
        assertEquals(new File(dir, "foo/bar/test3.txt"), files[1]);
        assertEquals(new File(dir, "foo/test1.txt"), files[2]);
    }

    @Test
    public void testGlobStarAtEnd() throws Exception {
        String dir = getTestdataDir();
        File[] files = WildcardMatcher.glob(dir + "/foo/bar/*");
        assertNotNull(files);
        for (File file : files) {
            //System.out.println("file = " + file);
        }
        assertEquals(3, files.length);
        Arrays.sort(files);
        assertEquals(new File(dir, "foo/bar/test1.txt"), files[0]);
        assertEquals(new File(dir, "foo/bar/test2.dat"), files[1]);
        assertEquals(new File(dir, "foo/bar/test3.txt"), files[2]);
    }

    @Test
    public void testGlobDoubleStarAtEnd() throws Exception {
        String dir = getTestdataDir();
        File[] files = WildcardMatcher.glob(dir + "/foo/**");
        assertNotNull(files);
        for (File file : files) {
            //System.out.println("file = " + file);
        }
        assertEquals(4, files.length);
        Arrays.sort(files);
        assertEquals(new File(dir, "foo/bar"), files[0]);
        assertEquals(new File(dir, "foo/test1.txt"), files[1]);
        assertEquals(new File(dir, "foo/test2.dat"), files[2]);
        assertEquals(new File(dir, "foo/test3.dat"), files[3]);
    }

    @Test
    public void testGlobAllFiles() throws Exception {
        String dir = getTestdataDir();
        File[] files = WildcardMatcher.glob(dir + "/foo/**/*.*");
        assertNotNull(files);
        for (File file : files) {
            //System.out.println("file = " + file);
        }
        assertEquals(6, files.length);
        Arrays.sort(files);
        assertEquals(new File(dir, "foo/bar/test1.txt"), files[0]);
        assertEquals(new File(dir, "foo/bar/test2.dat"), files[1]);
        assertEquals(new File(dir, "foo/bar/test3.txt"), files[2]);
        assertEquals(new File(dir, "foo/test1.txt"), files[3]);
        assertEquals(new File(dir, "foo/test2.dat"), files[4]);
        assertEquals(new File(dir, "foo/test3.dat"), files[5]);
    }

    @Test
    public void testGlobExistingDir() throws Exception {
        String dir = getTestdataDir();
        File[] files = WildcardMatcher.glob(dir + "/foo");
        assertNotNull(files);
        for (File file : files) {
            //System.out.println("file = " + file);
        }
        assertEquals(1, files.length);
        assertEquals(new File(dir, "foo"), files[0]);
    }

    private String getTestdataDir() throws URISyntaxException {
        URL resource = WildcardMatcherTest.class.getResource("WildcardMatcherTest");
        return new File(resource.toURI()).getPath();
    }
}
