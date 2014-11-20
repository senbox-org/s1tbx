package org.jlinda.core.filtering;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jlinda.core.SLCImage;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.jlinda.core.io.DataReader.readCplxFloatData;
import static org.jlinda.core.io.DataReader.readFloatData;

public class RangeFilterTest {

    private static final double DELTA_04 = 1e-04;
    private static final double DELTA_03 = 1e-03;
    private static final String testDirectory = "/d2/etna_test/rangeFilterTest/";

    /// tile size ///
    private static int nRows = 128;
    private static int nCols = 128;

    private static ComplexDoubleMatrix masterCplx;
    private static ComplexDoubleMatrix slaveCplx;

    private static ComplexDoubleMatrix masterCplx_ACTUAL;
    private static ComplexDoubleMatrix slaveCplx_ACTUAL;

    /// define parameters
    final static int nlMean = 15;
    final static int SNRThreshold = 5;
    final static double RSR = 18962500.774137583;
    final static int RBW = 15550000;

    @BeforeClass
    public static void setUpTestData() throws FileNotFoundException {

        /// load Input Data
        String fileMasterDataName = "slc_image_128_128.cr4.swap";
        masterCplx = readCplxFloatData(testDirectory + fileMasterDataName, 128, 128);

        String fileSlaveDataName = "slc_image1_128_128.cr4.swap";
        slaveCplx = readCplxFloatData(testDirectory + fileSlaveDataName, 128, 128);

    }

    @Test
    public void testDefineFilter() throws Exception {

        // master metadata & data
        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
        masterCplx_ACTUAL = masterCplx.dup();

        // slave metadata & data
        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
        slaveCplx_ACTUAL = slaveCplx.dup();

        RangeFilter rangeFilter = new RangeFilter();

        rangeFilter.setMetadata(masterMetadata);
        rangeFilter.setData(masterCplx_ACTUAL);

        rangeFilter.setMetadata1(slaveMetadata);
        rangeFilter.setData1(slaveCplx_ACTUAL);

        rangeFilter.defineParameters();
        rangeFilter.defineFilter();
        DoubleMatrix rngFilter_ACTUAL = rangeFilter.getFilter();

        String fileNameFilter = "filter_hamm_1_OFF.r4.swap";
        FloatMatrix rngFilter_EXPECTED = readFloatData(testDirectory + fileNameFilter, nRows, nCols);

        Assert.assertEquals(rngFilter_EXPECTED, rngFilter_ACTUAL.toFloat());
    }


    @Test
    public void filterClass_HAMM() throws Exception {

        // master metadata & data
        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
        masterCplx_ACTUAL = masterCplx.dup();

        // slave metadata & data
        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
        slaveCplx_ACTUAL = slaveCplx.dup();

        RangeFilter rangeFilter = new RangeFilter();

        rangeFilter.setMetadata(masterMetadata);
        rangeFilter.setData(masterCplx_ACTUAL);

        rangeFilter.setMetadata1(slaveMetadata);
        rangeFilter.setData1(slaveCplx_ACTUAL);

        rangeFilter.defineParameters();

        rangeFilter.defineFilter();
        rangeFilter.applyFilter();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_1_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_1_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA1_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        Assert.assertEquals(rngFilter_DATA_EXPECTED, rangeFilter.getData());
        Assert.assertEquals(rngFilter_DATA1_EXPECTED, rangeFilter.getData1());

    }

    @Test
    public void filterClass_RECT() throws Exception {

        // master metadata & data
        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
        masterCplx_ACTUAL = masterCplx.dup();

        // slave metadata & data
        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
        slaveCplx_ACTUAL = slaveCplx.dup();

        RangeFilter rangeFilter = new RangeFilter();

        rangeFilter.setMetadata(masterMetadata);
        rangeFilter.setData(masterCplx_ACTUAL);

        rangeFilter.setMetadata1(slaveMetadata);
        rangeFilter.setData1(slaveCplx_ACTUAL);

        rangeFilter.setAlphaHamming(1);
        rangeFilter.defineParameters();

        rangeFilter.defineFilter();
        rangeFilter.applyFilter();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_rect_1_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_rect_1_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA1_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        Assert.assertEquals(rngFilter_DATA_EXPECTED, rangeFilter.getData());
        Assert.assertEquals(rngFilter_DATA1_EXPECTED, rangeFilter.getData1());

    }

    @Test
    public void filterClass_HAMM_OVSMP() throws Exception {

        // master metadata & data
        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
        masterCplx_ACTUAL = masterCplx.dup();

        // slave metadata & data
        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
        slaveCplx_ACTUAL = slaveCplx.dup();

        RangeFilter rangeFilter = new RangeFilter();

        rangeFilter.setMetadata(masterMetadata);
        rangeFilter.setData(masterCplx_ACTUAL);

        rangeFilter.setMetadata1(slaveMetadata);
        rangeFilter.setData1(slaveCplx_ACTUAL);

        rangeFilter.setOvsFactor(2);
        rangeFilter.defineParameters();

        rangeFilter.defineFilter();
        rangeFilter.applyFilter();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA1_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        Assert.assertEquals(rngFilter_DATA_EXPECTED, rangeFilter.getData());
        Assert.assertEquals(rngFilter_DATA1_EXPECTED, rangeFilter.getData1());

    }

    @Test
    public void filterClass_HAMM_OVSMP_SPLIT() throws Exception {

        // master metadata & data
        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
        masterCplx_ACTUAL = masterCplx.dup();

        // slave metadata & data
        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
        slaveCplx_ACTUAL = slaveCplx.dup();

        RangeFilter rangeFilter = new RangeFilter();

        rangeFilter.setMetadata(masterMetadata);
        rangeFilter.setData(masterCplx_ACTUAL);

        rangeFilter.setMetadata1(slaveMetadata);
        rangeFilter.setData1(slaveCplx_ACTUAL);

        rangeFilter.setOvsFactor(2);
        rangeFilter.defineParameters();

        rangeFilter.defineFilter();
        rangeFilter.applyFilterMaster();
        rangeFilter.applyFilterSlave();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix rngFilter_DATA1_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        Assert.assertEquals(rngFilter_DATA_EXPECTED, rangeFilter.getData());
        Assert.assertEquals(rngFilter_DATA1_EXPECTED, rangeFilter.getData1());

    }



    @Test
    public void filterBlock_RECT() throws Exception {

        final double alphaHamming = 1;
        final int ovsFactor = 1;
        final boolean doWeightCorrelFlag = false;

        masterCplx_ACTUAL = masterCplx.dup();
        slaveCplx_ACTUAL = slaveCplx.dup();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_rect_1_OFF.cr4.swap";
        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_rect_1_OFF.cr4.swap";
        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        /// range filter data block
        RangeFilter.filterBlock(masterCplx_ACTUAL, slaveCplx_ACTUAL, nlMean, SNRThreshold, RSR, RBW, alphaHamming, ovsFactor, doWeightCorrelFlag);

        assertFilterBlock(masterCplx_rngFilter_EXPECTED, slaveCplx_rngFilter_EXPECTED);

    }

    @Test
    public void filterBlock_HAMM() throws Exception {

        final double alphaHamming = 0.75;
        final int ovsFactor = 1;
        final boolean doWeightCorrelFlag = false;

        masterCplx_ACTUAL = masterCplx.dup();
        slaveCplx_ACTUAL = slaveCplx.dup();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_1_OFF.cr4.swap";
        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_1_OFF.cr4.swap";
        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        /// range filter data block
        RangeFilter.filterBlock(masterCplx_ACTUAL, slaveCplx_ACTUAL, nlMean, SNRThreshold, RSR, RBW, alphaHamming, ovsFactor, doWeightCorrelFlag);

        assertFilterBlock(masterCplx_rngFilter_EXPECTED, slaveCplx_rngFilter_EXPECTED);

    }

    @Test
    public void filterBlock_HAMM_WEIGHT() throws Exception {

        final double alphaHamming = 0.75;
        final int ovsFactor = 1;
        final boolean doWeightCorrelFlag = true;

        masterCplx_ACTUAL = masterCplx.dup();
        slaveCplx_ACTUAL = slaveCplx.dup();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_1_ON.cr4.swap";
        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_1_ON.cr4.swap";
        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        /// range filter data block
        RangeFilter.filterBlock(masterCplx_ACTUAL, slaveCplx_ACTUAL, nlMean, SNRThreshold, RSR, RBW, alphaHamming, ovsFactor, doWeightCorrelFlag);

        assertFilterBlock(masterCplx_rngFilter_EXPECTED, slaveCplx_rngFilter_EXPECTED);

    }

    @Test
    public void filterBlock_HAMM_OVSMP() throws Exception {

        /// define parameters parameters
        final double alphaHamming = 0.75;
        final int ovsFactor = 2;
        final boolean doWeightCorrelFlag = false;

        masterCplx_ACTUAL = masterCplx.dup();
        slaveCplx_ACTUAL = slaveCplx.dup();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_2_OFF.cr4.swap";
        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        /// range filter data block
        RangeFilter.filterBlock(masterCplx_ACTUAL, slaveCplx_ACTUAL, nlMean, SNRThreshold, RSR, RBW, alphaHamming, ovsFactor, doWeightCorrelFlag);

        assertFilterBlock(masterCplx_rngFilter_EXPECTED, slaveCplx_rngFilter_EXPECTED);

    }

    @Test
    public void filterBlock_HAMM_OVSMP_WEIGHT() throws Exception {

        /// define parameters parameters
        final double alphaHamming = 0.75;
        final int ovsFactor = 2;
        final boolean doWeightCorrelFlag = true;

        masterCplx_ACTUAL = masterCplx.dup();
        slaveCplx_ACTUAL = slaveCplx.dup();

        /// load Expected Data
        String fileMasterDataNameFiltered = "slc_image_filtered_hamm_2_ON.cr4.swap";
        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);

        String fileSlaveDataNameFiltered = "slc_image1_filtered_hamm_2_ON.cr4.swap";
        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);

        /// range filter data block
        RangeFilter.filterBlock(masterCplx_ACTUAL, slaveCplx_ACTUAL, nlMean, SNRThreshold, RSR, RBW, alphaHamming, ovsFactor, doWeightCorrelFlag);

        assertFilterBlock(masterCplx_rngFilter_EXPECTED, slaveCplx_rngFilter_EXPECTED);
    }

//    @Ignore
//    @Test
//    public void filterClass_RECT() throws Exception {
//
//        // master metadata & data
//        SLCImage masterMetadata = new SLCImage();
//        masterMetadata.parseResFile(new File(testDirectory + "master.res"));
//        masterCplx_ACTUAL = masterCplx.dup();
//
//        // slave metadata & data
//        SLCImage slaveMetadata = new SLCImage();
//        slaveMetadata.parseResFile(new File(testDirectory + "slave.res"));
//        slaveCplx_ACTUAL = slaveCplx.dup();
//
//        RangeFilter rangeFilter = new RangeFilter();
//
//        rangeFilter.setMetadata(masterMetadata);
//        rangeFilter.setData(masterCplx_ACTUAL);
//
//        rangeFilter.setMetadata1(slaveMetadata);
//        rangeFilter.setData1(slaveCplx_ACTUAL);
//
//        rangeFilter.defineParameters();
//
//        rangeFilter.defineFilter();
//        rangeFilter.applyFilter();
//
//        rangeFilter.getData();
//
//        /// load Expected Data
//        String fileMasterDataNameFiltered = "slc_image_filtered_rect_1_OFF.cr4.swap";
//        ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED = loadExpectedData(fileMasterDataNameFiltered);
//
//        String fileSlaveDataNameFiltered = "slc_image1_filtered_rect_1_OFF.cr4.swap";
//        ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED = loadExpectedData(fileSlaveDataNameFiltered);
//
//        Assert.assertArrayEquals(masterCplx_rngFilter_EXPECTED.toDoubleArray(), rangeFilter.getData().toDoubleArray(), DELTA_03);
//        Assert.assertArrayEquals(slaveCplx_rngFilter_EXPECTED.toDoubleArray(), rangeFilter.getData1().toDoubleArray(), DELTA_03);
//
//    }


    /// HELPER FUNCTIONS ///

    private ComplexDoubleMatrix loadExpectedData(String fileMasterDataNameFiltered) throws FileNotFoundException {
        return readCplxFloatData(testDirectory + fileMasterDataNameFiltered, nRows, nCols);
    }

    private void assertFilterBlock(ComplexDoubleMatrix masterCplx_rngFilter_EXPECTED, ComplexDoubleMatrix slaveCplx_rngFilter_EXPECTED) {
        Assert.assertArrayEquals(masterCplx_rngFilter_EXPECTED.toDoubleArray(), masterCplx_ACTUAL.toDoubleArray(), DELTA_03);
        Assert.assertArrayEquals(slaveCplx_rngFilter_EXPECTED.toDoubleArray(), slaveCplx_ACTUAL.toDoubleArray(), DELTA_03);
    }

}
