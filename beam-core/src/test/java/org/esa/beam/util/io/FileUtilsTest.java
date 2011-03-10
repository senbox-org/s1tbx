/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.util.io;

import junit.framework.TestCase;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.GlobalTestTools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class FileUtilsTest extends TestCase {

    public FileUtilsTest(String name) {
        super(name);
    }

    public void testGetExtension() {
        assertEquals(null, FileUtils.getExtension(new File("testfile")));
        assertEquals(".", FileUtils.getExtension(new File("testfile.")));
        assertEquals(".ext", FileUtils.getExtension(new File("testfile.ext")));
        assertEquals(".EXT", FileUtils.getExtension(new File("testfile.EXT")));
        assertEquals(".ext", FileUtils.getExtension(new File("directory\\testfile.ext")));
        assertEquals(".ext", FileUtils.getExtension(new File("directory/testfile.ext")));
        assertEquals(".ext", FileUtils.getExtension(new File("directory:testfile.ext")));
    }

    public void testGetExtensionDotPos() {
        assertEquals(-1, FileUtils.getExtensionDotPos("testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos(".testfile"));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile."));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile.ext"));
        assertEquals(8, FileUtils.getExtensionDotPos("testfile.EXT"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory\\testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory/testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("directory:testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext\\testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext/testfile.ext"));
        assertEquals(18, FileUtils.getExtensionDotPos("direc.ext:testfile.ext"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext\\testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext/testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext:testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext\\.testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext/.testfile"));
        assertEquals(-1, FileUtils.getExtensionDotPos("direc.ext:.testfile"));
    }

    public void testEnsureExtension_FileParameter() {
        File dirFile = new File("dir.with.dots");

        File current = FileUtils.ensureExtension(new File(dirFile, "file.smc"), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc.kdt"),
                     current.toString());

        current = FileUtils.ensureExtension(new File(dirFile, "file."), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.ensureExtension(new File(dirFile, "file"), ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());
    }

    public void testEnsureExtension_StringParameter() {
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc.kdt"),
                     FileUtils.ensureExtension(
                             GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.smc"), ".kdt"));
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"),
                     FileUtils.ensureExtension(
                             GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file."), ".kdt"));
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"),
                     FileUtils.ensureExtension(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file"),
                                               ".kdt"));
    }

    public void testChangeExtension_FileParameter() {
        File dirFile = new File("dir.with.dots");
        File fileWithExtension = new File(dirFile, "file.smc");
        File fileWithDot = new File(dirFile, "file.");
        File fileWithoutExtension = new File(dirFile, "file");

        File current = FileUtils.exchangeExtension(fileWithExtension, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.exchangeExtension(fileWithDot, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());

        current = FileUtils.exchangeExtension(fileWithoutExtension, ".kdt");
        assertEquals(GlobalTestTools.createPlatformIndependentFilePath("dir.with.dots/file.kdt"), current.toString());
    }

    public void testChangeExtension_StringParameter() {
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file.smc", ".kdt"));
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file.", ".kdt"));
        assertEquals("dir.with.dots/file.kdt", FileUtils.exchangeExtension("dir.with.dots/file", ".kdt"));
        assertEquals("dir.with.dots/file", FileUtils.exchangeExtension("dir.with.dots/file.txt", ""));
        assertEquals("dir.with.dots/fileo", FileUtils.exchangeExtension("dir.with.dots/file.txt", "o"));
    }

    public void testExtractFileName() {
        String path1 = "home" + File.separator + "tom" + File.separator + "tesfile1.dim";
        String path2 = "C:" + File.separator + "Data" + File.separator + "TestFiles" + File.separator + "tesfile2.dim";
        String path3 = File.separator + "tesfile3.dim";
        String expected1 = "tesfile1.dim";
        String expected2 = "tesfile2.dim";
        String expected3 = "tesfile3.dim";

        // check that null is not allowed as argument
        try {
            FileUtils.getFileNameFromPath(null);
            fail("Exception expected here!");
        } catch (IllegalArgumentException e) {
        }

        assertEquals(expected1, FileUtils.getFileNameFromPath(path1));
        assertEquals(expected2, FileUtils.getFileNameFromPath(path2));
        assertEquals(expected3, FileUtils.getFileNameFromPath(path3));
    }

    public void testDeleteFileTree() {
        File treeRoot = GlobalTestConfig.getBeamTestDataOutputDirectory();
        File firstDir = new File(treeRoot, "firstDir");
        File firstFile = new File(treeRoot, "firstFile");
        File secondDir = new File(firstDir, "secondDir");
        File secondFile = new File(firstDir, "secondFile");
        File thirdFile = new File(secondDir, "thirdFile");
        File writeProtectedFile = new File(secondDir, "protFile");

        try {
            treeRoot.mkdirs();
            assertTrue(treeRoot.exists());

            firstDir.mkdirs();
            assertTrue(firstDir.exists());

            firstFile.createNewFile();
            assertTrue(firstFile.exists());

            secondDir.mkdirs();
            assertTrue(secondDir.exists());

            secondFile.createNewFile();
            assertTrue(secondFile.exists());

            thirdFile.createNewFile();
            assertTrue(thirdFile.exists());

            writeProtectedFile.createNewFile();
            assertTrue(writeProtectedFile.setReadOnly());
            assertTrue(writeProtectedFile.exists());

        } catch (IOException e) {
            fail("unable to create file");
        }

        FileUtils.deleteTree(treeRoot);

        assertTrue(!treeRoot.exists());
        assertTrue(!firstDir.exists());
        assertTrue(!firstFile.exists());
        assertTrue(!secondDir.exists());
        assertTrue(!secondFile.exists());
        assertTrue(!thirdFile.exists());
        assertTrue(!writeProtectedFile.exists());
    }

    public void testDeleteFileTreeExceptions() {
        try {
            FileUtils.deleteTree(null);
            fail("invalid null argument");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDeleteFileTree_DeleteEmptyDirectories() {
        File treeRoot = GlobalTestConfig.getBeamTestDataOutputDirectory();
        treeRoot.mkdirs();
        assertTrue("treeRoot exists expected", treeRoot.exists());
        File firstDir = new File(treeRoot, "firstDir");
        firstDir.mkdirs();
        assertTrue("firstDir exists expected", firstDir.exists());
        File emptyDir = new File(firstDir, "emptyDir");
        emptyDir.mkdirs();
        assertTrue("emptyDir exists expected", emptyDir.exists());

        FileUtils.deleteTree(treeRoot);

        assertTrue("treeRoot not exists expected", !treeRoot.exists());
    }

    public void testGetFileAsURL() throws MalformedURLException {
        final File file = new File("/dummy/hugo/daten").getAbsoluteFile();

        final URL fileURL = FileUtils.getFileAsUrl(file);

        assertEquals(file.toURL(), fileURL);
        assertEquals(file.toURI().toURL(), fileURL);
    }

    public void testGetFileAsURLWithIllegalChars() throws MalformedURLException, URISyntaxException {
        final File file = new File("/dummy/hu\tgo/daten").getAbsoluteFile();

        final URL fileURL = FileUtils.getFileAsUrl(file);

        final URL url = file.toURL();
        assertFalse(url.equals(fileURL));

        final String expectedPath = new File("/dummy/hu%09go/daten").toURL().getPath();
        assertEquals(expectedPath, fileURL.getPath());
        assertEquals(file, new File(fileURL.toURI()));
    }

    public void testThatGetUrlAsFileIsTheInverseOfGetFileAsUrl() throws MalformedURLException,
            URISyntaxException {
        final File file1 = new File("/dummy/hugo/daten").getAbsoluteFile();
        final File file2 = FileUtils.getUrlAsFile(FileUtils.getFileAsUrl(file1));
        assertEquals(file1, file2);
    }

    public void testGetDisplayText() {
        try {
            FileUtils.getDisplayText(null, 50);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            FileUtils.getDisplayText(new File("alpha/bravo/charly"), 3);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
        //                   "5432109876543210987654321098765432109876543210987654321"
        File file = new File("alpha/bravo/charly/delta/echo/foxtrott/golf/hotel/india");
        char sep = File.separatorChar;
        assertEquals("...ndia",
                     FileUtils.getDisplayText(file, 7));
        assertEquals(".../india".replace('/', sep),
                     FileUtils.getDisplayText(file, 12));
        assertEquals(".../golf/hotel/india".replace('/', sep),
                     FileUtils.getDisplayText(file, 20));
        assertEquals("alpha/bravo/charly/delta/echo/foxtrott/golf/hotel/india".replace('/', sep),
                     FileUtils.getDisplayText(file, 80));
    }
}

