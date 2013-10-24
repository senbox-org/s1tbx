package org.jlinda.core.coregistration.legacy;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.jblas.*;
import org.jlinda.core.Constants;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.coregistration.LUT;
import org.jlinda.core.io.DataReader;
import org.jlinda.core.utils.LinearAlgebraUtils;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;
import org.jlinda.core.utils.SarUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static org.jblas.MatrixFunctions.pow;
import static org.jblas.MatrixFunctions.sqrt;
import static org.jlinda.core.utils.PolyUtils.normalize2;
import static org.jlinda.core.utils.PolyUtils.polyval;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CoregistrationTest {

    private static final double DELTA_08 = 1e-08;
    private static final double DELTA_06 = 1e-06;
    private static final double DELTA_04 = 1e-04;
    private static final double DELTA_01 = 1e-01;
    private static final double DELTA_00 = 1e-00;

    private String dataPath;
    private String processingPath;
    private String masterFileName;
    private String slaveFileName;
    private String masterMagnitudeFileName;
    private String slaveMagnitudeFileName;
    private String correlFileName;

    private ComplexDoubleMatrix masterCplx;
    private ComplexDoubleMatrix slaveCplx;
    private FloatMatrix masterMagnitude;
    private FloatMatrix slaveMagnitude;
    private FloatMatrix correlMasterSlave_EXPECTED;

    private ByteOrder littleEndian = ByteOrder.LITTLE_ENDIAN;
    private ByteOrder bigEndian = ByteOrder.BIG_ENDIAN;
    private int rows;
    private int rowsAcc;
    private int cols;
    private int colsAcc;

    private static final Logger logger = (Logger) LoggerFactory.getLogger(CoregistrationTest.class);

    private static StopWatch clock = new StopWatch();


    @BeforeClass
    public static void setUp() throws Exception {

        // setup logger
        logger.setLevel(Level.TRACE);

    }

    @Test
    public void firstTest_MagSpace() throws Exception {

        /*
        * 1) Test for computation of cross correlation between input magnitudes, estimation is in the space domain
        * 2) Optional - demonstrates the propagation of float vs double rounding errors into computations
        * --
        * Important(!): I had to byteswap data with C routines to parse them correctly!
        * */

        /* INITIALIZE */

        clock.start();

        // declare file names
        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "coarseCorr/magSpace/";
        masterFileName = "master.cr4";
        slaveFileName = "slave.cr4";
        masterMagnitudeFileName = "masterMagnitude.r4";
        slaveMagnitudeFileName = "slaveMagnitude.r4";
        // correlFileName = "correlMasterSlave.r4"; // cannot be correctly parsed
        correlFileName = "correlMasterSlave.SWAP.r4";

        // dimensions
        rows = 256 + 1;
        rowsAcc = 8;
        cols = 256 + 1;
        colsAcc = 8;

        int colsMaster = cols + 2 * rowsAcc;
        int rowsMaster = rows + 2 * colsAcc;
        int colsSlave = cols;
        int rowsSlave = rows;

        /* LOAD TEST DATA */

        // load all data
        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rowsMaster, colsMaster, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rowsSlave, colsSlave, littleEndian);

        masterMagnitude = DataReader.readFloatData(processingPath + dataPath + masterMagnitudeFileName, rowsMaster, colsMaster, littleEndian);
        slaveMagnitude = DataReader.readFloatData(processingPath + dataPath + slaveMagnitudeFileName, rowsSlave, colsSlave, littleEndian);

        // expected matrix
        // correlMasterSlave_EXPECTED = DataReader.readFloatData(processingPath + dataPath + correlFileName, rowsMaster, colsMaster, littleEndian);
        correlMasterSlave_EXPECTED = DataReader.readFloatData(processingPath + dataPath + correlFileName, rowsMaster, colsMaster, bigEndian);

        // report
        clock.stop();
        logger.info("Time to read data [ms]: {}", clock.getElapsedTime());

        /* START TESTING */

        clock.start();

        // Initialize Coregistration
        Coregistration coreg = new Coregistration();

        // Call test method
        DoubleMatrix correlateDouble = coreg.correlate(SarUtils.magnitude(masterCplx), SarUtils.magnitude(slaveCplx));

        clock.stop();
        logger.info("Time to Cross-correlate in SPACE domain [ms]: {}", clock.getElapsedTime());

        // Cast whats returned to floats
        float[][] correlateFloatArray = castToFloatMatrix(correlateDouble).toArray2();
        float[][] correlMasterSlaveArray = correlMasterSlave_EXPECTED.toArray2();

        // assert expected vs actual
        for (int i = 0; i < correlMasterSlaveArray.length; i++) {
            float[] floats = correlMasterSlaveArray[i];
            Assert.assertArrayEquals(floats, correlateFloatArray[i], (float) DELTA_04);
        }

/*
        // Check for possible effect of ROUNDING ERRORS when/if working with floats
        // ...if working with magnitude that is precomputed and saved as float, when cross correlating rounding errors
        // ...will propagate into computation

        // Log on screen some values to check whether everything is parsed right
        logger.trace("First value of Master Complex Input Matrix: {}",masterCplx.get(0, 0));
        logger.trace("First value of Slave Complex Input Matrix: {}",slaveCplx.get(0, 0));
        logger.trace("First value of Master Magnitude Matrix: {}",masterMagnitude.get(0, 0));
        logger.trace("First value of Internally Computed Magnitude Matrix {}", SarUtils.magnitude(masterCplx).get(0, 0));
        logger.trace("First value of Slave Magnitude Matrix: {}", slaveMagnitude.get(0, 0));
        logger.trace("First value of Expected Correlation Matrix: {}", correlMasterSlave_EXPECTED.get(0, 0));

        // Cast input complex data from Double to Float
        ComplexFloatMatrix masterCplx_FLOAT = castToComplexFloatMatrix(masterCplx);
        ComplexFloatMatrix slaveCplx_FLOAT = castToComplexFloatMatrix(slaveCplx);

        // Cast pre-computed magnitude from Float to Double
        DoubleMatrix masterMagnitudeDouble = castToDoubleMatrix(masterMagnitude);
        DoubleMatrix slaveMagnitudeDouble = castToDoubleMatrix(slaveMagnitude);
        logger.trace("First value of Casted to Double Master Magnitude: {}",masterMagnitudeDouble.get(0, 0));
        logger.trace("First value of Casted to Double Slave Magnitude: {}", slaveMagnitudeDouble.get(0, 0));

        // Compute cross correlation between:
        //   1. magnitude pre-computed as floats
        FloatMatrix correlateFloat_PreComputed = correlate(masterMagnitude, slaveMagnitude); // internal implementation
        //   2. internally computed magnitude on float data
        FloatMatrix correlateFloat_InternallyComputed = correlate(magnitude(masterCplx_FLOAT), magnitude(slaveCplx_FLOAT));

        logger.trace("Max value of Double Correlate: {}", correlateDouble.max());
        logger.trace("Max value of Float Correlate (PreComputed Magnitude - ROUNDING ERROR): {}", correlateFloat_PreComputed.max());
        logger.trace("Max value of Float Correlate (Internally Comp): {}", correlateFloat_InternallyComputed.max());
        logger.trace("Max value of Expected Correlate (PreComputed): {}", correlMasterSlave_EXPECTED.max());
*/

    }

    @Test
    public void secondTest_MagFFT() throws Exception {

        /*
        * 1) Test for cross correlation between input magnitudes in spectral domain (eg. by using FFTs)
        * */

        /* INITIALIZE */

        // declare file names
        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "coarseCorr/magFFT/";
        masterFileName = "master.cr4";
        slaveFileName = "slave.cr4";

        // dimensions
        rows = 256;
        cols = 256;

        /* LOAD TEST DATA */

        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rows, cols, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rows, cols, littleEndian);

        // report some values to check whether everything is parsed right
        logger.trace("First entry of Master Complex Data Input {}: ", masterCplx.get(0, 0));
        logger.trace("First entry of Slave Complex Data Input {}: ", slaveCplx.get(0, 0));
        logger.trace("Master Input Dimensions [{},{}]", masterCplx.rows, masterCplx.columns);
        logger.trace("Slave Input Dimensions [{},{}]", slaveCplx.rows, slaveCplx.columns);

        /* START TESTING */

        clock.start();

        // initialize
        Coregistration coreg = new Coregistration();

        // processing input
        int MasksizeP = rows;
        int MasksizeL = cols;

        double coherence = coreg.crosscorrelate(masterCplx, slaveCplx, 16, MasksizeL / 2, MasksizeP / 2, 0, 0);

        // report
        clock.stop();
        logger.info("Time to Cross-correlate in SPECTRAL domain [ms]: {}", clock.getElapsedTime());
        logger.info("Estimated peak coherence: {}", coherence);

    }


    @Test
    public void secondTest_fineMagFFT() throws Exception {

        /*
        * 1) Test for cross correlation between input magnitudes in spectral domain (eg. by using FFTs)
        * */

        /* INITIALIZE */

        // declare file names
        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "fineCoreg/magFFT/";
        masterFileName = "master_patch.32x32.cr4";
        slaveFileName = "slave_patch.32x32.cr4";

        // dimensions
        rows = 32;
        cols = 32;

        /* LOAD TEST DATA */

        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rows, cols, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rows, cols, littleEndian);

        // report some values to check whether everything is parsed right
        logger.trace("First entry of Master Complex Data Input {}: ", masterCplx.get(0, 0));
        logger.trace("First entry of Slave Complex Data Input {}: ", slaveCplx.get(0, 0));
        logger.trace("Master Input Dimensions [{},{}]", masterCplx.rows, masterCplx.columns);
        logger.trace("Slave Input Dimensions [{},{}]", slaveCplx.rows, slaveCplx.columns);

        /* START TESTING */

        clock.start();

        // initialize
        Coregistration coreg = new Coregistration();

        // processing parameters
        int MasksizeL = rows;
        int MasksizeP = cols;
        int ovsfactor;
        double coherence;

        for (int i = 0; i < 8; i++) {
            ovsfactor = (int) Math.pow(2, i);
            clock.start();
            coherence = coreg.crosscorrelate(masterCplx, slaveCplx, ovsfactor, MasksizeL / 2, MasksizeP / 2, 0, 0);
            clock.stop();
            logger.info("Estimated peak Coherence: {}, with OvsFactor: {}", coherence, ovsfactor);
            logger.info("Computation Time [ms]: {}", clock.getElapsedTime());
        }


    }

    @Test
    public void thirdTest_shiftSpectrum() throws Exception {

        /*
        * 1) Test for shifting spectra in azimuth direction as a function fDC
        * */

        /* INITIALIZE */

        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "fineCoreg/magFFT/";
        masterFileName = "master_patch.32x32.cr4";
        slaveFileName = "slave_patch.32x32.cr4";

        // dimensions
        rows = 32;
        cols = 32;

        /* LOAD TEST DATA */

        // load binary data
        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rows, cols, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rows, cols, littleEndian);

        // load metadata
        SLCImage minfo = new SLCImage();
        minfo.parseResFile(new File(processingPath + "01486.res"));

        SLCImage sinfo = new SLCImage();
        sinfo.parseResFile(new File(processingPath + "21159.res"));

        // setup input for testing
        final int m_pixlo = 1212;
        final double s_pixlo = 1214;// neg.shift -> 0
        final double mPrf = minfo.getPRF();
        final double sPrf = sinfo.getPRF();
        final double mRsr2x = minfo.getRsr2x();
        final double sRsr2x = sinfo.getRsr2x();

        double[] mFdc = new double[3];
        mFdc[0] = minfo.doppler.getF_DC_a0();
        mFdc[1] = minfo.doppler.getF_DC_a1();
        mFdc[2] = minfo.doppler.getF_DC_a2();

        double[] sFdc = new double[3];
        sFdc[0] = sinfo.doppler.getF_DC_a0();
        sFdc[1] = sinfo.doppler.getF_DC_a1();
        sFdc[2] = sinfo.doppler.getF_DC_a2();

        /* START TESTING */

        Coregistration coreg = new Coregistration();

        clock.start();
        coreg.shiftazispectrum(masterCplx, mPrf, mRsr2x, mFdc, -m_pixlo);// shift from fDC to zero
        clock.stop();
        logger.info("Computation Time for shifting of spectra [ms]: {}", clock.getElapsedTime());

        clock.start();
        coreg.shiftazispectrum(slaveCplx, mPrf, mRsr2x, mFdc, -m_pixlo);// shift from fDC to zero
        clock.stop();
        logger.info("Computation Time for shifting of spectra [ms]: {}", clock.getElapsedTime());

    }

    @Test
    public void fourthTest_shiftSpectrumAndMagFFT() throws Exception {

        /*
        * 1) Test cross correlation with spectra shift and FFTs
        * */

        /* INITIALIZE */

        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "fineCoreg/magFFT/";
        masterFileName = "master_patch.32x32.cr4";
        slaveFileName = "slave_patch.32x32.cr4";

        // dimensions
        rows = 32;
        cols = 32;

        /* LOAD TEST DATA */

        // load binary data
        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rows, cols, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rows, cols, littleEndian);

        // load metadata

        SLCImage minfo = new SLCImage();
        minfo.parseResFile(new File(processingPath + "01486.res"));

        SLCImage sinfo = new SLCImage();
        sinfo.parseResFile(new File(processingPath + "21159.res"));

        // setup input for testing
        final int m_pixlo = 1212;
        final double s_pixlo = 1214;// neg.shift -> 0

        double mPrf = minfo.getPRF();
        double sPrf = sinfo.getPRF();
        double mRsr2x = minfo.getRsr2x();
        double sRsr2x = sinfo.getRsr2x();

        double[] mFdc = new double[3];
        mFdc[0] = minfo.doppler.getF_DC_a0();
        mFdc[1] = minfo.doppler.getF_DC_a1();
        mFdc[2] = minfo.doppler.getF_DC_a2();

        double[] sFdc = new double[3];
        sFdc[0] = sinfo.doppler.getF_DC_a0();
        sFdc[1] = sinfo.doppler.getF_DC_a1();
        sFdc[2] = sinfo.doppler.getF_DC_a2();

        /* START TESTING */

        // initialize
        Coregistration coreg = new Coregistration();

        // shiftspectra of input tiles
        coreg.shiftazispectrum(masterCplx, mPrf, mRsr2x, mFdc, -m_pixlo);// shift from fDC to zero
        coreg.shiftazispectrum(slaveCplx, sPrf, sRsr2x, sFdc, -s_pixlo);// shift from fDC to zero

        // define processing parameters
        final int AccL = masterCplx.rows / 2;
        final int AccP = masterCplx.columns / 2;

        // oversample input data
        final int ovsFactor = 2;
        masterCplx = SarUtils.oversample(masterCplx, ovsFactor, ovsFactor);
        slaveCplx = SarUtils.oversample(slaveCplx, ovsFactor, ovsFactor);

        int ovsFactorCorrelate;
        double coherence;
        int offsetL = 0;
        int offsetP = 0;

        for (int i = 1; i < 8; i++) {
            ovsFactorCorrelate = (int) Math.pow(2, i);
            clock.start();
            coherence = coreg.crosscorrelate(masterCplx, slaveCplx, ovsFactorCorrelate / ovsFactor, 2 * AccL, 2 * AccP, offsetL, offsetP);
            clock.stop();
            logger.info("Estimated peak Coherence: {}, with ShiftSpectra and OvsFactor: {}", coherence, ovsFactorCorrelate);
            logger.info("Computation Time [ms]: {}", clock.getElapsedTime());
        }

    }

    @Test
    public void fifthTest_coherenceSpace() throws Exception {

        /*
         * 1) Test coherence computation in space domaion
         **/

        /* INITIALIZE */

        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "fineCoreg/magSpace/";
        masterFileName = "master_patch.48x48.cr4";
        slaveFileName = "slave_patch.48x48.cr4";

        // dimensions
        rows = 48;
        cols = 48;
        /* LOAD TEST DATA */

        // load binary data
        masterCplx = DataReader.readCplxFloatData(processingPath + dataPath + masterFileName, rows, cols, littleEndian);
        slaveCplx = DataReader.readCplxFloatData(processingPath + dataPath + slaveFileName, rows, cols, littleEndian);

        /* SETUP INPUT PARAMS FOR TESTING */

        final int ovsFactor = 8;
        final int AccL = 8;
        final int AccP = 8;
        int offsetL = 0;
        int offsetP = 0;

        /* START TESTING */

        // initialize
        Coregistration coreg = new Coregistration();

        clock.start();
        double coherence = coreg.coherencespace(AccL, AccP, ovsFactor, masterCplx, slaveCplx, offsetL, offsetP);// shift from fDC to zero
        clock.stop();
        logger.info("Time to compute cohrence in SPACE domain [ms]: {}", clock.getElapsedTime());
        logger.info("Estimated peak coherence: {}", coherence);

    }

    @Test
    public void sixthTest_Resampling() throws Exception {

        logger.trace("Start Resampling [development code]");

        // PARAMETERS
        // ----------------------------------
        processingPath = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";
        dataPath = "rsmp/";

        SLCImage master = new SLCImage();
        master.parseResFile(new File(processingPath + dataPath + "01486.res"));
        master.setOriginalWindow(new Window(1, 26292, 1, 4900));

        SLCImage slave = new SLCImage();
        slave.parseResFile(new File(processingPath + dataPath + "21159.res"));
/*
        // Estimated during CPM step: where inverse estimation is performed :
        // ...not really clear why estimation is performed?
        // ...I can just invert polynomials and apply them to slave?
        Deltaline_slave00_poly:                    	1.38941008e+02
        Deltapixel_slave00_poly:                   	-2.19844746e+00
        Deltaline_slave0N_poly:                    	1.38856333e+02
        Deltapixel_slave0N_poly:                   	-2.39968790e+00
        Deltaline_slaveN0_poly:                    	1.38911145e+02
        Deltapixel_slaveN0_poly:                   	-2.19893253e+00
        Deltaline_slaveNN_poly:                    	1.38936348e+02
        Deltapixel_slaveNN_poly:                   	-2.36860850e+00
*/
        slave.setSlaveMasterOffset(1.38941008e+02, -2.19844746e+00, 1.38856333e+02, -2.39968790e+00,
                1.38911145e+02, -2.19893253e+00, 1.38936348e+02, -2.36860850e+00);

        boolean shiftAziSpectra = true;
        boolean demAssisted = false;
        String method = "cc6p";

        int polyOrder = 2;
        int polyCoeffs = PolyUtils.numberOfCoefficients(polyOrder);
        // polynomial : 5th degree : AZIMUTH direction
        double[] cpmL = new double[polyCoeffs + 1];
        cpmL[0] = -138.60102699999999;
        cpmL[1] = -0.61342283399999997;
        cpmL[2] = 0.31677070099999999;
        cpmL[3] = 0.086886668400000006;
        cpmL[4] = -0.58635061099999997;
        cpmL[5] = -0.016647044699999999;

        // polynomial : 5th degree : RANGE direction
        double[] cpmP = new double[polyCoeffs + 1];
        cpmP[0] = 2.2027052999999999;
        cpmP[1] = 0.32394611499999998;
        cpmP[2] = -0.60784787200000001;
        cpmP[3] = -0.52443622899999998;
        cpmP[4] = -0.16831470000000001;
        cpmP[5] = -0.684955745;

        // PROCESSING
        // ----------------------------------
        if (shiftAziSpectra == true) {
            logger.info("Shifting kernel_L to data fDC");
        }

        final int Npoints = extractNumber(method); // #pnts interpolator
        logger.debug("Number of kernel points: {}", Npoints);

        if (MathUtils.isOdd(Npoints)) {
            logger.error("Resample only even point interpolators, defined number of points: {}", Npoints);
            throw new IllegalArgumentException();
        }

        final int Npointsd2 = Npoints / 2;
        final int Npointsd2m1 = Npointsd2 - 1;

        // Normalize data for polynomial
        final double minL = master.getOriginalWindow().linelo;
        final double maxL = master.getOriginalWindow().linehi;
        final double minP = master.getOriginalWindow().pixlo;
        final double maxP = master.getOriginalWindow().pixhi;

        logger.info("resample: polynomial normalized by factors [AZIMUTH]: {} {} to [-2,2]", minL, maxL);
        logger.info("resample: polynomial normalized by factors [RANGE]: {} {} to [-2,2]", minP, maxP);


        // For KNAB/Raised Cosine kernel if requested
        // ...Because kernel is same in az. and rg. min. must be used.
        final float CHI_az = (float) (slave.getPRF() / slave.getAzimuthBandwidth());// oversampling factor az
        final float CHI_rg = (float) ((slave.getRsr2x() / 2.0) / slave.getRangeBandwidth());// oversampling factor rg
        final float CHI = min(CHI_az, CHI_rg);// min. oversampling factor of data
        logger.info("Oversampling ratio azimuth (PRF/ABW): {}", CHI_az);
        logger.info("Oversampling ratio range (RSR/RBW): {}", CHI_rg);
        logger.info("KNAB/RC kernel uses: oversampling ratio: {}", CHI);

        if (CHI < 1.1) {
            logger.warn("Oversampling ratio: {} not optimal for KNAB/RC", CHI);
        }

        /** Create lookup table */
        /** Notes:
         *  ...Lookup table complex because of multiplication with complex
         *  ...Loopkup table for azimuth and range and
         *  ...shift spectrum of azi kernel with doppler centroid
         *  ...kernel in azimuth should be sampled higher
         *  ...and may be different from range due to different oversampling ratio and spectral shift (const)
         */
        LUT lut = new LUT(LUT.CC6P, Npoints);
        lut.constructLUT();
//        lut.overviewOfLut();

        final ComplexDoubleMatrix pntKernelAz = new ComplexDoubleMatrix(lut.getKernel());
        final ComplexDoubleMatrix pntKernelRg = new ComplexDoubleMatrix(lut.getKernel());
        final DoubleMatrix pntAxis = lut.getAxis();

//        // Degree of coregistration polynomial
//        final int degree_cpmL = PolyUtils.degreeFromCoefficients(cpmL.length);
//        final int degree_cpmP = PolyUtils.degreeFromCoefficients(cpmP.length);

        // Compute overlap between master and slave
        Window fullOverlap = Coregistration.getOverlap(master, slave, (double) Npointsd2, 0d, 0d);
        logger.info("Overlap window: " + fullOverlap.linelo + ":" + fullOverlap.linehi + ", " + fullOverlap.pixlo + ":" + fullOverlap.pixhi);

        Window tileOverlap = fullOverlap;

        /* WORK OUT RESAMPLING OUTPUT GEOMETRY */

        // overlap between master and slave in MASTER COORDINATE SYSTEM
        int firstTileLine;
        int lastTileLine;
        int firstTilePixel;
        int lastTilePixel;

        int line;
        int pixel;

        /* TILE MANAGEMENT THAT GOES INTO OPERATOR PART WHERE SLAVE DATA IS PULLED */
        // -------------------- slave tile management start here -----------------
        // - here part that is pulled from slave data is computed
        // - normalization is the same as used for the estimation of the coregistration polynomial
        // - in this test assume that the tile is the size of the whole buffer

        final double normPixLo = normalize2(fullOverlap.pixlo, minP, maxP);
        final double normPixHi = normalize2(fullOverlap.pixhi, minP, maxP);
        final double normLinLo = normalize2(fullOverlap.linelo, minL, maxL);
        final double normLinHi = normalize2(fullOverlap.linehi, minL, maxL);

        // upper/lower line --
        line = (int) tileOverlap.linelo;
        firstTileLine = (int) ((ceil(min(line + polyval(normalize2(line, minL, maxL), normPixLo, cpmL),
                line + polyval(normalize2(line, minL, maxL), normPixHi, cpmL))))
                - Npoints);

        final int line2 = (int) (line + tileOverlap.lines() - 1);
        lastTileLine = (int) ((ceil(min(line2 + polyval(normalize2(line2, minL, maxL),
                normPixLo, cpmL), line2 + polyval(normalize2(line2, minL, maxL), normPixHi, cpmL))))
                + Npoints);

        // upper/lower pixel --
        pixel = (int) tileOverlap.pixlo;
        firstTilePixel = (int) ((ceil(min(pixel + polyval(normLinLo, normalize2(pixel, minP, maxP), cpmP),
                line + polyval(normLinHi, normalize2(pixel, minP, maxP), cpmP))))
                - Npoints);

        int pixel2 = (int) (pixel + tileOverlap.pixels() - 1);
        lastTilePixel = (int) ((ceil(min(pixel2 + polyval(normLinLo, normalize2(pixel2, minP, maxP), cpmP),
                pixel2 + polyval(normLinHi, normalize2(pixel2, minP, maxP), cpmP))))
                - Npoints);

        final int FORSURE = 25; // buffer larger 2 x FORSURE start/end
        // azimuth direction + extra
        firstTileLine -= FORSURE;
        lastTileLine += FORSURE;
        // range direction + extra
        firstTilePixel -= FORSURE;
        lastTilePixel += FORSURE;

        // Account for edges
        if (firstTileLine < (int) (slave.getCurrentWindow().linelo))
            firstTileLine = (int) slave.getCurrentWindow().linelo;

        if (lastTileLine > (int) (slave.getCurrentWindow().linehi))
            lastTileLine = (int) slave.getCurrentWindow().linehi;

        if (firstTilePixel < (int) (slave.getCurrentWindow().pixlo))
            firstTilePixel = (int) slave.getCurrentWindow().pixlo;

        if (lastTilePixel > (int) (slave.getCurrentWindow().pixhi))
            lastTilePixel = (int) slave.getCurrentWindow().pixhi;

        // Fill slave BUFFER from data pool

        // part of slave loaded
        Window winSlaveFile = new Window(firstTileLine, lastTileLine, firstTilePixel, lastTilePixel);

        logger.debug("Reading slave: [" + winSlaveFile.linelo + ":" + winSlaveFile.linehi + ", "
                + winSlaveFile.pixlo + ":" + winSlaveFile.pixhi + "]");

        /* LOAD TEST DATA */

        // file name
        String fileName = "resample_buffer.cr4";

        // now I work with smaller buffer
        int nRows = (int) winSlaveFile.lines();
        int nCols = (int) winSlaveFile.pixels();

        ComplexDoubleMatrix BUFFER = DataReader.readCplxFloatData(processingPath + dataPath + fileName, nRows, nCols, littleEndian);
        logger.info("Loaded BUFFER size: {} rows, {} cols", nRows, nCols);

        // -------------------- slave tile management stop here -----------------

        /* SET OUTPUT/RESULT TILE */

//        ComplexDoubleMatrix RESULT = ComplexDoubleMatrix.zeros(bufferLines, (int) (overlap.pixhi - overlap.pixlo + 1));
        ComplexDoubleMatrix RESULT = ComplexDoubleMatrix.zeros((int) tileOverlap.lines(), (int) tileOverlap.pixels());

        // temp matrix for parsing interpolated results
        ComplexDoubleMatrix PART = new ComplexDoubleMatrix(Npoints, Npoints);

        /* ACTUAL DATA RESAMPLING */

        /* Evaluate coregistration polynomial */
        double interpL = 0;
        double interpP = 0;

        int pixelCnt = 0;
        int lineCnt = 0;


        // Progress messages
	    int percent = 0;
	    int tenpercent = (int)(Math.ceil(tileOverlap.lines() / 10.0)); // round
	    if (tenpercent == 0)
		    tenpercent = 1000; // avoid error: x%0


        clock.start();

        // loop that does the job!
        for (line = (int) tileOverlap.linelo; line <= tileOverlap.linehi; line++) {

        	// Progress messages
    		if (((line - tileOverlap.linelo) % tenpercent) == 0) {
                logger.info("RESAMPLE: {} %", percent);
                percent += 10;
            }

            for (pixel = (int) tileOverlap.pixlo; pixel <= (int) (tileOverlap.pixhi); pixel++) {

                if (!demAssisted) {
                    interpL = line + polyval(normalize2(line, minL, maxL), normalize2(pixel, minP, maxP), cpmL); // ~ 255.35432
                    interpP = pixel + polyval(normalize2(line, minL, maxL), normalize2(pixel, minP, maxP), cpmP); // ~ 2.5232
                }

                /* Get correct lines for interpolation */
                int fl_interpL = (int) (interpL);
                int fl_interpP = (int) (interpP);
                int firstL = fl_interpL - Npointsd2m1; // e.g. 254 (5 6 7)
                int firstP = fl_interpP - Npointsd2m1; // e.g. 1   (2 3 4)
                double interpLdec = interpL - fl_interpL; // e.g. .35432
                double interpPdec = interpP - fl_interpP; // e.g. .5232

                int kernelnoL = (int) (interpLdec * LUT.getInterval() + 0.5); // lookup table index
                int kernelnoP = (int) (interpPdec * LUT.getInterval() + 0.5); // lookup table index

                ComplexDoubleMatrix kernelL = pntKernelAz.getRow(kernelnoL);
                final DoubleMatrix pntAxisRow = pntAxis.getRow(kernelnoL);

                final ComplexDoubleMatrix kernelP = pntKernelRg.getRow(kernelnoP);

                // Shift azimuth kernel with fDC before interpolation ______
                if (shiftAziSpectra == true) {
                    // get Doppler centroid
                    double tmp = 2.0 * Constants.PI * slave.doppler.pix2fdc(interpP) / slave.getPRF();
                    // ...to shift spectrum of convolution kernel to fDC of data, multiply
                    // ...in the space domain with a phase trend of -2pi*t*fdc/prf
                    // ...(to shift back (no need) you would use +fdc), see manual;
                    for (int i = 0; i < Npoints; ++i) {

                        // Modify kernel, shift spectrum to fDC
                        double t = pntAxisRow.get(i) * tmp;
                        kernelL.put(i, kernelL.get(i).mul(new ComplexDouble(Math.cos(t), (-1) * Math.sin(t)))); // note '-' (see manual)
                    }
                }

                Window inWin = new Window(firstL - firstTileLine, firstL - firstTileLine + (Npoints - 1),
                        firstP - firstTilePixel, firstP - firstTilePixel + (Npoints - 1));

                LinearAlgebraUtils.setdata(PART, BUFFER, inWin);

//                logger.debug("Result (line,pixel): {},{}", line, pixel);
//                logger.debug("Result (line,pixel): {},{}", lineCnt, pixelCnt);

                RESULT.put(lineCnt, pixelCnt, LinearAlgebraUtils.matTxmat(PART.mmul(kernelP.transpose()), kernelL.transpose()).get(0, 0));
                pixelCnt++;

            }

            lineCnt++;
            pixelCnt = 0;

        }

        clock.stop();
        logger.info("Resampling time: {} [ms]", clock.getElapsedTime());


//        /* UNIT TEST */
//        // ..this will fail, the numbers are approx the same, but because of rounding error and small differences in
//        // ..the actual application the values are different on 0.1 - 0.01 scale
//
//        /* LOAD EXPECTED DATA */
//        String resampledSlaveFileName = "21159.rsmp.SWAP";
//
//        // load binary data
//        ComplexDoubleMatrix resampledSlave = DataReader.readCplxFloatData(processingPath + dataPath + resampledSlaveFileName, (int) tileOverlap.lines(), (int) tileOverlap.pixels(), ByteOrder.BIG_ENDIAN);
//
//        /* ASSERT PHASE */
//        // assert Phase of resampled image
//        double[][] phaseArray_EXPECTED = computeAbsPhase(resampledSlave);
//        double[][] phaseArray_ACTUAL = computeAbsPhase(RESULT);
//
//        for (int i = 0; i < phaseArray_ACTUAL.length; i++) {
//            double[] doubles_ACTUAL = phaseArray_ACTUAL[i];
//            double[] doubles_EXPECTED = phaseArray_EXPECTED[i];
//            Assert.assertArrayEquals(doubles_EXPECTED, doubles_ACTUAL, DELTA_01);
//        }

    }

    private double[][] computePhase(ComplexDoubleMatrix complexDoubleMatrix) {

        int nRows = complexDoubleMatrix.rows;
        int nCols = complexDoubleMatrix.columns;
        double[][] phaseArray = new double[nRows][nCols];
        ComplexDouble complexDouble;
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                complexDouble = complexDoubleMatrix.get(i, j);
                phaseArray[i][j] = Math.atan2(complexDouble.imag(), complexDouble.real());
            }
        }
        return phaseArray;
    }

    private double[][] computeAbsPhase(ComplexDoubleMatrix complexDoubleMatrix) {

        int nRows = complexDoubleMatrix.rows;
        int nCols = complexDoubleMatrix.columns;
        double[][] phaseArray = new double[nRows][nCols];
        ComplexDouble complexDouble;
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                complexDouble = complexDoubleMatrix.get(i, j);
                phaseArray[i][j] = Math.abs(Math.atan2(complexDouble.imag(), complexDouble.real()));
            }
        }
        return phaseArray;
    }


    /*public static ComplexDoubleMatrix readCplxIntData(final String fileName,
                                                      final int rows, final int columns,
                                                      final ByteOrder byteOrder) throws FileNotFoundException {

        final FlatBinaryInt inRealFile = new FlatBinaryInt();
        inRealFile.setFile(new File(fileName));
        inRealFile.setByteOrder(byteOrder);
        inRealFile.setDataWindow(new Window(0, (rows - 1), 0, (2 * columns - 1)));
        inRealFile.setInStream();
        inRealFile.readFromStream();

        // parse data from :: assume it is stored in "major-row order"
        DoubleMatrix realData = new DoubleMatrix(rows, columns);
        DoubleMatrix imgData = new DoubleMatrix(rows, columns);
        final float[][] data = inRealFile.getData();
        int cnt;
        for (int i = 0; i < rows; i++) {
            cnt = 0;
            for (int j = 0; j < 2 * columns; j = j + 2) {
                realData.put(i, cnt, data[i][j]);
                imgData.put(i, cnt, data[i][j + 1]);
                cnt++;
            }
        }

        return new ComplexDoubleMatrix(realData, imgData);

    }*/


    private int extractNumber(String line) {
        String numbers = new String();

        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(line);
        while (m.find()) {
            numbers = numbers + m.group();
        }

        return Integer.parseInt(numbers);
    }


    private DoubleMatrix castToDoubleMatrix(FloatMatrix matrixFloat) {
        int rows = matrixFloat.rows;
        int columns = matrixFloat.columns;
        DoubleMatrix matrixDouble = new DoubleMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrixDouble.put(i, j, matrixFloat.get(i, j));
            }
        }
        return matrixDouble;
    }

    private FloatMatrix castToFloatMatrix(DoubleMatrix matrixDouble) {
        int rows = matrixDouble.rows;
        int columns = matrixDouble.columns;
        FloatMatrix matrixFloat = new FloatMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrixFloat.put(i, j, (float) matrixDouble.get(i, j));
            }
        }
        return matrixFloat;
    }

    private ComplexFloatMatrix castToComplexFloatMatrix(ComplexDoubleMatrix matrixComplexDouble) {
        int rows = matrixComplexDouble.rows;
        int columns = matrixComplexDouble.columns;
        ComplexFloatMatrix matrixComplexFloat = new ComplexFloatMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                ComplexDouble complexDouble = matrixComplexDouble.get(i, j);
                matrixComplexFloat.put(i, j, new ComplexFloat((float) complexDouble.real(), (float) complexDouble.imag()));
            }
        }
        return matrixComplexFloat;
    }


    private FloatMatrix correlate_FLOAT(FloatMatrix A, FloatMatrix Mask) {

        double varM = 0.; // variance of Mask
        Mask.subi(Mask.mean());

        for (int ii = 0; ii < Mask.length; ii++) {
            varM += Math.pow(Mask.get(ii), 2); // 1/N later
        }

        // Compute correlation at these points
        int beginl = (Mask.rows - 1) / 2; // floor
        int beginp = (Mask.columns - 1) / 2; // floor

        FloatMatrix Result = FloatMatrix.zeros(A.rows, A.columns); // init to 0
        FloatMatrix Am = new FloatMatrix(Mask.rows, Mask.columns);

        // First Window of A, updated at end of loop______
        Window winA = new Window(0, Mask.rows - 1, 0, Mask.columns - 1);
        Window windef = new Window();// defaults to total Am

        // Correlate part of Result______
        for (int i = beginl; i < A.rows - beginl; i++) {
            for (int j = beginp; j < A.columns - beginp; j++) {

                // Am.setdata(windef, A, winA); // Am no allocs.
                setdata_FLOAT(Am, windef, A, winA);

                Am.subi(Am.mean()); // center around mean
                float covAM = (float) 0.; // covariance A,Mask
                float varA = (float) 0.; // variance of A(part)

                for (int l = 0; l < Mask.length; l++) {
                    covAM += (Mask.get(l) * Am.get(l));
                    varA += Math.pow(Am.get(l), 2);
                }

                Result.put(i, j, (float) (covAM / Math.sqrt(varM * varA)));
                winA.pixlo++;
                winA.pixhi++;
            }
            winA.linelo++;
            winA.linehi++;
            winA.pixlo = 0;
            winA.pixhi = winA.pixlo + Mask.columns - 1;
        }
        return Result;

    }


    /**
     * setdata(outMatrix, outWin, inMatrix, inWin):
     * set outWin of outMatrix to inWin of inMatrix
     * if outWin==0 defaults to totalB, inWin==0 defaults to totalA
     * first line matrix =0 (?)
     */
    private static void setdata_FLOAT(FloatMatrix outMatrix, Window outWin, FloatMatrix inMatrix, Window inWin) {

        if (outWin.linehi == 0 && outWin.pixhi == 0) {
            outWin.linehi = outMatrix.rows - 1;
            outWin.pixhi = outMatrix.columns - 1;
        }
        if (inWin.linehi == 0 && inWin.pixhi == 0) {
            inWin.linehi = inMatrix.rows - 1;
            inWin.pixhi = inMatrix.columns - 1;
        }

        if (((outWin.linehi - outWin.linelo) != (inWin.linehi - inWin.linelo)) ||
                ((outWin.pixhi - outWin.pixlo) != (inWin.pixhi - inWin.pixlo))) {
            throw new IllegalArgumentException("setdata: wrong input.");

        }
        if (outWin.linehi < outWin.linelo || outWin.pixhi < outWin.pixlo) {
            throw new IllegalArgumentException("setdata: wrong input.1");
        }

        if ((outWin.linehi > outMatrix.rows - 1) ||
                (outWin.pixhi > outMatrix.columns - 1)) {
            throw new IllegalArgumentException("setdata: wrong input.2");
        }

        if ((inWin.linehi > inMatrix.rows - 1) ||
                (inWin.pixhi > inMatrix.columns - 1)) {
            throw new IllegalArgumentException("setdata: wrong input.3");
        }

        //// Fill data ////
        int sizeLin = (int) inWin.lines();
        for (int i = (int) outWin.pixlo, j = (int) inWin.pixlo; i <= outWin.pixhi; i++, j++) {

            int startOut = (int) (i * outMatrix.rows + outWin.linelo);
            int startIn = (int) (j * inMatrix.rows + inWin.linelo);

            System.arraycopy(inMatrix.data, startIn, outMatrix.data, startOut, sizeLin);

        }
    }

    private FloatMatrix intensity_FLOAT(final ComplexFloatMatrix inputMatrix) {
        return pow(inputMatrix.real(), 2).add(pow(inputMatrix.imag(), 2));
    }

    private FloatMatrix magnitude_FLOAT(final ComplexFloatMatrix inputMatrix) {
        return sqrt(intensity_FLOAT(inputMatrix));
    }


}
