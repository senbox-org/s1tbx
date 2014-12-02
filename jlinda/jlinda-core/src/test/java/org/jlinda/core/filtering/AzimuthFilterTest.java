package org.jlinda.core.filtering;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.jlinda.core.io.DataReader.readCplxFloatData;
import static org.jlinda.core.io.DataReader.readFloatData;

public class AzimuthFilterTest {

    // etna processing!
    // assume all flatbinaries are swapped
    private static final String testDirectory = "/d2/etna_test/azimuthFilterTest/";

    private static FloatMatrix filterVector_HAMMING_EXPECTED;
    private static FloatMatrix filterMatrix_HAMMING_EXPECTED;
    private static FloatMatrix filterVector_RECT_EXPECTED;
    private static FloatMatrix filterFull;

    private static File masterResFile = new File(testDirectory + "master.res");
    private static SLCImage masterMetadata = new SLCImage();
    private static ComplexDoubleMatrix masterCplxData;
    private static ComplexDoubleMatrix filteredData_HAMM_EXPECTED;
    private static ComplexDoubleMatrix filteredData_HAMM_VARIABLE_EXPECTED;
    private static ComplexDoubleMatrix filteredData_RECT_EXPECTED;


    private static File slaveResFile =  new File(testDirectory + "slave.res");
    private static SLCImage slaveMetadata = new SLCImage();

    private static ComplexDoubleMatrix slaveCplxData;
    private static ComplexDoubleMatrix slaveCplxDataFiltered_EXPECTED;

    private static final double DELTA_04 = 1e-04;


    @BeforeClass
    public static void setUpTestData() throws Exception {

        // master data
        masterMetadata.parseResFile(masterResFile);
        String masterCplxDataFileName = testDirectory + "slc_image1_128_512.cr4.swap";
        masterCplxData = readCplxFloatData(masterCplxDataFileName, 128, 512);

        // filters vectors
        String filterVector_HAMMING_FileName = testDirectory + "hamming_filtervector_075_128_1.r4.swap";
        filterVector_HAMMING_EXPECTED = readFloatData(filterVector_HAMMING_FileName, 128, 1);

        String filterVector_RECT_FileName = testDirectory + "rect_filtervector_128_1.r4.swap";
        filterVector_RECT_EXPECTED = readFloatData(filterVector_RECT_FileName, 128, 1);

        // filters matrix
        String filterMatrix_HAMMING_FileName = testDirectory + "hamming_filtermatrix_075_128_512.r4.swap";
        filterMatrix_HAMMING_EXPECTED = readFloatData(filterMatrix_HAMMING_FileName, 128, 512);

        // filtered data
        String masterCplxFiltDataFileName_RECT = testDirectory + "slc_image1_filtered_rect_128_512.cr4.swap";
        filteredData_RECT_EXPECTED = readCplxFloatData(masterCplxFiltDataFileName_RECT, 128, 512);

        String masterCplxFiltDataFileName_HAMM = testDirectory + "slc_image1_filtered_hamming_128_512.cr4.swap";
        filteredData_HAMM_EXPECTED = readCplxFloatData(masterCplxFiltDataFileName_HAMM, 128, 512);

        String masterCplxFiltDataFileName_HAMM_VARIABLE = testDirectory + "slc_image1_filtered_hamming_variable_128_512.cr4.swap";
        filteredData_HAMM_VARIABLE_EXPECTED = readCplxFloatData(masterCplxFiltDataFileName_HAMM_VARIABLE, 128, 512);

        // slave data
        slaveMetadata.parseResFile(slaveResFile);
//        String slaveCplxDataFileName = testDirectory + "slave.raw";
//        slaveCplxData = readCplxFloatData(slaveCplxDataFileName, 128, 128);
//        String slaveCplxFiltDataFileName = testDirectory + "slave.raw.filt";
//        slaveCplxDataFiltered_EXPECTED = readCplxFloatData(slaveCplxFiltDataFileName, 128, 128);
    }


    @Test
    public void testFilter_RECTANGULAR() throws Exception {

        ComplexDoubleMatrix masterCplxDataTemp = masterCplxData.dup();

        final AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxDataTemp);
        testAziFilter.setHammingAlpha(1);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();

        DoubleMatrix filterVector_RECT_ACTUAL = testAziFilter.getFilterVector();

        final float[] actual = filterVector_RECT_ACTUAL.toFloat().toArray();
        final float[] expected = filterVector_RECT_EXPECTED.toArray();

        Assert.assertArrayEquals(expected, actual, (float) DELTA_04);

    }

    @Test
    public void testDataFilter_RECTANGULAR() throws Exception {

        ComplexDoubleMatrix masterCplxDataTemp = masterCplxData.dup();

        final AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxDataTemp);
        testAziFilter.setHammingAlpha(1);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();
        testAziFilter.applyFilter();

        // get filtered data
        ComplexDoubleMatrix filteredData_RECT_ACTUAL = testAziFilter.getData();

        // assert
        Assert.assertEquals(filteredData_RECT_EXPECTED, filteredData_RECT_ACTUAL);
//        Assert.assertEquals(testAziFilter.data, filteredData_RECT_ACTUAL);

    }

    @Test
    public void testFilter_HAMMING() throws Exception {

        ComplexDoubleMatrix masterCplxDataTemp = masterCplxData.dup();

        AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxDataTemp);
        testAziFilter.setHammingAlpha(0.75);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();

        DoubleMatrix filterVector_HAMMING_ACTUAL = testAziFilter.getFilterVector();

        final float[] actual = filterVector_HAMMING_ACTUAL.toFloat().toArray();
        final float[] expected = filterVector_HAMMING_EXPECTED.toArray();

        Assert.assertArrayEquals(expected, actual, (float) DELTA_04);

    }

    @Test
    public void testDataFilter_HAMMING() throws Exception {

        ComplexDoubleMatrix masterCplxDataTemp = masterCplxData.dup();

        AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxDataTemp);
        testAziFilter.setHammingAlpha(0.75);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();
        testAziFilter.applyFilter();

        // get filtered data
        ComplexDoubleMatrix filteredData_HAMM_ACTUAL = testAziFilter.getData();

        // assert
        Assert.assertEquals(filteredData_HAMM_EXPECTED, filteredData_HAMM_ACTUAL);
        Assert.assertEquals(testAziFilter.data, filteredData_HAMM_ACTUAL);

    }

    @Test
    public void testFilter_HAMMING_VARIABLE() throws Exception {

        ComplexDoubleMatrix masterCplxDataTemp = masterCplxData.dup();

        AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.metadata1.setCoarseOffsetP(2);
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxDataTemp);
        testAziFilter.setHammingAlpha(0.75);
        testAziFilter.setVariableFilter(true);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();

        // get filtered data
        DoubleMatrix filterMatrix_HAMMING_ACTUAL = testAziFilter.getFilter();

        final float[] actual = filterMatrix_HAMMING_ACTUAL.toFloat().toArray();
        final float[] expected = filterMatrix_HAMMING_EXPECTED.toArray();

        Assert.assertArrayEquals(expected, actual, (float) DELTA_04);

    }

    @Test
    public void testDataFilter_HAMMING_VARIABLE() throws Exception {

        AzimuthFilter testAziFilter = new AzimuthFilter();
        testAziFilter.setMetadata(masterMetadata);
        testAziFilter.setMetadata1(slaveMetadata);
        testAziFilter.metadata1.setCoarseOffsetP(2); // hardcoded!
        testAziFilter.setTile(new Window(0, 0, 0, 0));
        testAziFilter.setData(masterCplxData);
        testAziFilter.setHammingAlpha(0.75);
        testAziFilter.setVariableFilter(true);
        testAziFilter.defineParameters();

        testAziFilter.defineFilter();
        testAziFilter.applyFilter();

        // get filtered data
        ComplexDoubleMatrix filterData_HAMMING_ACTUAL = testAziFilter.getData();

        Assert.assertEquals(filteredData_HAMM_VARIABLE_EXPECTED, filterData_HAMMING_ACTUAL);

    }



}
