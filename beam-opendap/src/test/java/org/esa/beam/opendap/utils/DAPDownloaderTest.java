package org.esa.beam.opendap.utils;

import org.esa.beam.opendap.ui.DownloadProgressBarPM;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dods.DODSNetcdfFile;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class DAPDownloaderTest {

    static final File TESTDATA_DIR = new File("target/dap_download_test");

    @Before
    public void setUp() throws Exception {
        TESTDATA_DIR.mkdirs();
        if (!TESTDATA_DIR.isDirectory()) {
            fail("Can't create test I/O directory: " + TESTDATA_DIR);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!FileUtils.deleteTree(TESTDATA_DIR)) {
            System.out.println("Warning: failed to completely delete test I/O directory:" + TESTDATA_DIR);
        }
    }

    @Test
    public void testDownloadFile() throws Exception {
        final Set<File> downloadedFiles = new HashSet<File>();
        NullDownloadContext fileCountProvider = new NullDownloadContext() {
            @Override
            public void notifyFileDownloaded(File downloadedFile) {
                downloadedFiles.add(downloadedFile);
            }
        };
        DAPDownloader dapDownloader = new DAPDownloader(new HashMap<String, Boolean>(), new ArrayList<String>(),
                                                        fileCountProvider, new NullLabelledProgressBarPM());
        String fileName = "fileToTextDownload.txt";
        assertFalse(getTestFile(fileName).exists());
        assertEquals(0, downloadedFiles.size());

        URL resource = getClass().getResource(fileName);
        dapDownloader.downloadFile(TESTDATA_DIR, resource.toString());

        assertEquals(1, downloadedFiles.size());
        assertEquals(fileName, ((File) downloadedFiles.toArray()[0]).getName());
        assertTrue(getTestFile(fileName).exists());
    }

    @Test
    public void testGetVariableNames() throws Exception {
        List<String> variableNames = DAPDownloader.getVariableNames(
                "iop_a_total_443[0:1:717][0:1:308],iop_a_ys_443[0:1:717][0:1:308]");

        String[] expected = {"iop_a_total_443", "iop_a_ys_443"};
        assertArrayEquals(expected, variableNames.toArray(new String[variableNames.size()]));

        variableNames = DAPDownloader.getVariableNames("");
        assertNull(variableNames);

        variableNames = DAPDownloader.getVariableNames(null);
        assertNull(variableNames);

        variableNames = DAPDownloader.getVariableNames("someUnconstrainedVariable");
        expected = new String[]{"someUnconstrainedVariable"};
        assertArrayEquals(expected, variableNames.toArray(new String[variableNames.size()]));

        variableNames = DAPDownloader.getVariableNames(
                "someUnconstrainedVariable,someConstrainedVariable[0:1:717][0:1:308]");
        expected = new String[]{"someUnconstrainedVariable", "someConstrainedVariable"};
        assertArrayEquals(expected, variableNames.toArray(new String[variableNames.size()]));
    }

    @Test
    public void testFilterVariables() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.open(resource.toString());
        final List<Variable> variables = netcdfFile.getVariables();
        final List<String> variableNames = new ArrayList<String>();
        for (Variable variable : variables) {
            variableNames.add(variable.getFullName());
        }
        String constraintExpression = null;
        List<String> filteredVariables = DAPDownloader.filterVariables(variableNames, constraintExpression);
        assertEquals(2, filteredVariables.size());
        assertEquals("sst", filteredVariables.get(0));
        assertEquals("wind", filteredVariables.get(1));

        constraintExpression = "sst[0:1:10][0:1:10]";
        filteredVariables = DAPDownloader.filterVariables(variableNames, constraintExpression);
        assertEquals(1, filteredVariables.size());
        assertEquals("sst", filteredVariables.get(0));

        constraintExpression = "bogusVariable[0:1:10][0:1:10]";
        filteredVariables = DAPDownloader.filterVariables(variableNames, constraintExpression);
        assertEquals(2, filteredVariables.size());
        assertEquals("sst", filteredVariables.get(0));
        assertEquals("wind", filteredVariables.get(1));

        constraintExpression = "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]";
        filteredVariables = DAPDownloader.filterVariables(variableNames, constraintExpression);
        assertEquals(2, filteredVariables.size());
        assertEquals("sst", filteredVariables.get(0));
        assertEquals("wind", filteredVariables.get(1));

        constraintExpression = "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10],sst";
        filteredVariables = DAPDownloader.filterVariables(variableNames, constraintExpression);
        assertEquals(2, filteredVariables.size());
        assertEquals("sst", filteredVariables.get(0));
        assertEquals("wind", filteredVariables.get(1));
    }

    @Test
    public void testFilterDimensions() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.open(resource.toString());

        List<String> variableNames = new ArrayList<String>();
        variableNames.add("sst");
        variableNames.add("wind");

        List<Dimension> dimensions = DAPDownloader.filterDimensions(variableNames, netcdfFile);
        Collections.sort(dimensions);
        assertEquals(3, dimensions.size());
        assertEquals("COADSX", dimensions.get(0).getShortName());
        assertEquals("COADSY", dimensions.get(1).getShortName());
        assertEquals("TIME", dimensions.get(2).getShortName());

        variableNames.clear();
        variableNames.add("wind");
        dimensions = DAPDownloader.filterDimensions(variableNames, netcdfFile);
        Collections.sort(dimensions);
        assertEquals(2, dimensions.size());
        assertEquals("COADSX", dimensions.get(0).getShortName());
        assertEquals("COADSY", dimensions.get(1).getShortName());

        variableNames.clear();
        variableNames.add("sst");
        dimensions = DAPDownloader.filterDimensions(variableNames, netcdfFile);
        Collections.sort(dimensions);
        assertEquals("COADSX", dimensions.get(0).getShortName());
        assertEquals("COADSY", dimensions.get(1).getShortName());
        assertEquals("TIME", dimensions.get(2).getShortName());
    }

    @Test
    public void testGetOrigin() throws Exception {
        int[] origin = DAPDownloader.getOrigin("sst", "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]", 3);
        assertArrayEquals(new int[]{0, 0, 0}, origin);

        origin = DAPDownloader.getOrigin("wind", "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]", 2);
        assertArrayEquals(new int[]{0, 0}, origin);

        origin = DAPDownloader.getOrigin("sst", "sst[5:1:10][10:1:10],wind[1:1:10][0:1:10]", 3);
        assertArrayEquals(new int[]{5, 10, 0}, origin);

        origin = DAPDownloader.getOrigin("sst", "", 3);
        assertArrayEquals(new int[]{0, 0, 0}, origin);
    }

    @Test
    public void testGetConstraintsExpressionForVariable() throws Exception {
        assertEquals("sst[0:1:10][0:1:10]",
                     DAPDownloader.getConstraintExpression("sst", "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]"));
        assertEquals("wind[0:1:10][0:1:10]",
                     DAPDownloader.getConstraintExpression("wind", "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]"));
        try {
            DAPDownloader.getConstraintExpression("pig_density", "sst[0:1:10][0:1:10],wind[0:1:10][0:1:10]");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("must be included"));
        }

        assertEquals("sst[0:1:10]", DAPDownloader.getConstraintExpression("sst",
                                                                          "sst_flag[0:1:10][0:1:10],wind[0:1:10][0:1:10],sst[0:1:10]"));
        assertEquals("sst[0:1:10]", DAPDownloader.getConstraintExpression("sst",
                                                                          "flag_sst[0:1:10][0:1:10],wind[0:1:10][0:1:10],sst[0:1:10]"));
    }

    @Test
    public void testGetDownloadSpeed() {
        assertEquals(1024.0 / 60.0, DAPDownloader.getDownloadSpeed(60 * 1000, 1024), 1E-4);
    }

    @Ignore
    @Test
    public void testActualWriting() throws Exception {
        final DAPDownloader dapDownloader = new DAPDownloader(null, null, new NullDownloadContext(), new NullLabelledProgressBarPM());
        final DODSNetcdfFile sourceNetcdfFile = new DODSNetcdfFile(
                "http://test.opendap.org:80/opendap/data/nc/coads_climatology.nc");
        dapDownloader.writeNetcdfFile(TESTDATA_DIR, "deleteme.nc", "", sourceNetcdfFile, false);

        final File testFile = getTestFile("deleteme.nc");
        assertTrue(testFile.exists());
        assertTrue(NetcdfFile.canOpen(testFile.getAbsolutePath()));
        final NetcdfFile netcdfFile = NetcdfFile.open(testFile.getAbsolutePath());
        assertNotNull(netcdfFile.findVariable("SST"));
    }

    @Ignore
    @Test
    public void testActualWriting_WithConstraint() throws Exception {
        final DAPDownloader dapDownloader = new DAPDownloader(null, null, new NullDownloadContext(), new NullLabelledProgressBarPM());
        final DODSNetcdfFile sourceNetcdfFile = new DODSNetcdfFile(
                "http://test.opendap.org:80/opendap/data/nc/coads_climatology.nc");
        dapDownloader.writeNetcdfFile(TESTDATA_DIR, "deleteme.nc", "COADSX[0:1:4]", sourceNetcdfFile, false);

        final File testFile = getTestFile("deleteme.nc");
        assertTrue(testFile.exists());
        assertTrue(NetcdfFile.canOpen(testFile.getAbsolutePath()));
        final NetcdfFile netcdfFile = NetcdfFile.open(testFile.getAbsolutePath());
        assertNull(netcdfFile.findVariable("SST"));
        assertNotNull(netcdfFile.findVariable("COADSX"));
    }

    static File getTestFile(String fileName) {
        return new File(TESTDATA_DIR, fileName);
    }

    private static class NullLabelledProgressBarPM extends DownloadProgressBarPM {

        public NullLabelledProgressBarPM() {
            super(null, null, null, null);
        }

        @Override
        public void setPreMessage(String preMessageText) {
        }

        @Override
        public void setPostMessage(String postMessageText) {
        }

        @Override
        public int getTotalWork() {
            return 0;
        }

        @Override
        public int getCurrentWork() {
            return 0;
        }

        @Override
        public void setTooltip(String tooltip) {
        }

        @Override
        public void beginTask(String taskName, int totalWork) {
        }

        @Override
        public void done() {
        }

        @Override
        public void internalWorked(double work) {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setCanceled(boolean canceled) {
        }

        @Override
        public void setTaskName(String taskName) {
        }

        @Override
        public void setSubTaskName(String subTaskName) {
        }

        @Override
        public void worked(int work) {
        }
    }

    private static class NullDownloadContext implements DAPDownloader.DownloadContext {

        @Override
        public int getAllFilesCount() {
            return 0;
        }

        @Override
        public int getAllDownloadedFilesCount() {
            return 0;
        }

        @Override
        public void notifyFileDownloaded(File downloadedFile) {
        }

        @Override
        public boolean mayOverwrite(String filename) {
            return true;
        }
    }
}
