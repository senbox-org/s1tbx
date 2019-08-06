
package org.csa.rstb.soilmoisture.gpf;

import au.com.bytecode.opencsv.CSVReader;
import com.bc.ceres.core.ProgressMonitor;
import net.sf.javaml.core.kdtree.KDTree;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.*;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.runtime.Config;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * There is a strong correlation between soil dielectric and radar backscatter. By inverting the Integral
 * Equation Model (IEM), the real dielectric constant (RDC) can be obtained from the radar backscatter
 * coefficient.
 * There are three approaches:
 * (1) Hybrid
 * (2) Multi-polarization
 * (3) Multi-angle
 * <p>
 * This class serves as the superclass for the different approaches.
 */

@OperatorMetadata(alias = "IEM-Inversion-Base",
        category = "Soil-Moisture",
        authors = "Cecilia Wong",
        description = "Superclass for IEM inversion operators")
public class IEMInverBase extends Operator {

    public static final String MAT_FILE_EXTENSION = "mat";
    public static final String CSV_FILE_EXTENSION = "csv";
    public static final String HDR_FILE_EXTENSION = "luthdr";
    // The range of cl in LUT is [1, 25] (cm)
    // The range of rms in LUT is [0.3, 2.6] (cm)
    // The range of RDC in LUT is [3.125824, 38.6884] (Farad/m)
    protected static final double INVALID_OUTPUT_VALUE = -999d;
    protected static final int INVALID_OUTLIER_VALUE = -1;
    private static final String OUTPUT_OUTLIER = "snap.soil_moisture.output_outlier";
    private final static String[] SIGMA_HH_BANDNAME_KEYWORDS = {"Sigma0_HH", "HH"};
    private final static String[] SIGMA_VV_BANDNAME_KEYWORDS = {"Sigma0_VV", "VV"};
    private final static String[] LIA_BANDNAME_KEYWORDS = {"incidenceAngle", "LIA"};
    private static final double underFlowFloat = 1.0e-30;
    private static boolean doDebug = false;
    // Since both images are in one source product, then the sigmaHH band in one image must have a different
    // name than the sigmaHH band in the other image.
    // Index points to which image
    protected String[] sigmaHHBandName = null; // source
    protected String[] sigmaVVBandName = null; // source
    protected String[] thetaBandName = null; // source
    // Index points to which image
    protected Band[] sigmaHHBand = null; // source
    protected Band[] sigmaVVBand = null; // source
    protected Band[] thetaBand = null; // source
    // Index points to which image
    protected boolean[] sigmaHHUnitIsDecibels = null; // source
    protected boolean[] sigmaVVUnitIsDecibels = null; // source

    // All three approaches have these two bands as output.
    protected Band rmsBand = null; // target
    protected Band rdcBand = null; // target
    protected Band outlierBand = null; // target; -1 = no data; 0 = not replaced; 1 = replaced with RDC from LUT; 2 = replaced with average of neighbours
    protected Band debugBand = null; // target
    // Number of angles covered by the LUT.
    protected int NUM_ANGLE_SECTIONS = 0;
    // number of nearest from KD tree
    @Parameter(description = "# closest sigma match from LUT search", defaultValue = "5", label = "N")
    protected Integer N = 5;
    protected double rdcThreshold;
    // There can be an AM image and/or a PM image depending on the inversion approach.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    // Some bands in a source product may not be used.
    // See extended classes for more details.
    // All images MUST be in one source product.
    private Product sourceProduct = null;
    // There is just one target product containing multiple bands one of which is the RDC (in Farad/m).
    // Other possible bands are rms and/or cl.
    // rms is RMS roughness height (in cm). cl is Roughness correlation length (in cm).
    // See extended classes for more details.
    private Product targetProduct = null;
    private boolean outputRMS = false;
    // Index points to which image
    private double[] invalidSigmaHHValue = null; // source
    private double[] invalidSigmaVVValue = null; // source
    private double[] invalidThetaValue = null; // source
    // LUT
    // It is assumed that the columns of the LUT in the file are:
    // rms, cl, RDC, sigmaHH, sigmaVV (for Hybrid)
    // rms, RDC, sigmaHH, sigmaVV (for Multi-pol and Multi-angle)
    // The rows are divided into sections. Each section has equal number of rows.
    // Each section is for an integer valued LIA.
    // If the first section is for LIA == n degrees. Then the next section is for (n+1) degrees.
    // For each row, the last two columns (i.e., sigmaHH and sigmaVV) are the backscatter computed
    // (using forward model) for the LIA that the row belongs to using the rms etc from that row.
    // It is assumed that rms in row k in one section has the same value as it counterpart in
    // row k in another section, k = 0, ... , M where M is the number of rows for one angle. Ditto for cl and RDC.
    // I.e., given a LIA, we compute the sigma for the same (M+1) combinations of rms, cl (if applicable), and RDC
    // values. This assumption is needed for Hybrid and Multi-angle approaches to work.
    // This assumption is not necessary for Multi-pol to work, but it is assumed to be true anyways so
    // that the (M+1) combinations of values need to be saved only once rather than N times where N is the
    // number of LIA sections in the LUT.
    private File lutFile = null;
    // This is the number of rows in the LUT for one angle. To be read from metadata of LUT.
    private int NUM_ROWS_PER_ANGLE_DEGREE = 0;
    // These are the start and end angles for the LUT. To be read from metadata of LUT.
    private int startIntThetaDeg = 0; // degrees
    private int endIntThetaDeg = 0; // degrees
    // This is set by extended class.
    // Hybrid and Multi-angle approaches have 2 images (one AM, one PM).
    // Multi-polarization approach has 1 image (AM or PM).
    private int numberOfImages = 0;
    // paramLUT is m rows by n columns where m is NUM_ROWS_PER_ANGLE_DEGREE and n is the
    // number of variable input parameters that feed the model that computes the backscatter.
    // n is 3 for Hybrid approach (the 3 parameters are rms, cl and RDC).
    // n is 2 for the remaining 2 approaches (the 2 parameters are rms and RDC).
    // Once paramLUT is populated, its contents do not change.
    private double[][] paramLUT = null;
    // Used by LUT CSV file only.
    // sigmaLUT is m rows by 2 columns where m is total number of rows in the LUT.
    // The 2 columns are sigmaHH and sigmaVV.
    private double[][] sigmaLUT = null;
    // Number of columns in the LUT.
    private int lutWidth = 0;
    private ProductData lutData = null;
    // It is inefficient to build and destroy a KD tree for each pixel.
    // Also, there are multiple threads processing the pixels.
    // Thus, all possible KD trees are built at the beginning.
    // E.g., for Hybrid or Multi-angle, if the angle range for AM image is 18 to 20 degrees and 19 to 21 degrees
    // for PM image, there are 9 combinations, thus 9 KD trees.
    // kdTreeMap will map a unique integer for each such combination to a KD tree.
    // (See method convertToKDTreeMapIntKey() in this class.)
    // For Multi-pol, it is simpler since only one image is involved. If the angle range for the AM or PM image is
    // 30 to 32 degrees, only 3 KD trees are needed.
    private TreeMap<Integer, KDTree> kdTreeMap = null;
    private int srcRasterWidth;
    private int srcRasterHeight;
    // length of the side of the square neighbourhood, must be odd and >= 3
    @Parameter(description = "Length (pixels) of side of square neighbourhood (M)", defaultValue = "5", label = "M")
    private Integer M = 5;
    @Parameter(description = "Replace remaining outlier with neighbours's average", defaultValue = "true", label = "Filter remaining outliers")
    private Boolean doRemainingOutliersFilter = true;
    private int halfM;

    /**
     * Check validity of a double.
     *
     * @param val The double value.
     * @return @p true if it is not NaN and not infinite.
     */
    protected static boolean isValid(final double val) {

        return (!Double.isNaN(val) && !Double.isInfinite(val));
    }

    // Among the bands, there is a "master" and the rest are "slaves".
    // The "master" has the highest rank.
    // The "slaves" are numbered 1, 2, 3 and so on. "Slave m" has higher rank than "Slave n" where m < n.
    // The band name of the "master" contains "mst".
    // The band name of a "slave" contains "slvx" where x = 1, 2, 3, ...
    // Check if s1 has higher rank then s2.
    // E.g., Band name "Sigma0_HH_slv2_26Oct2011" has higher rank than band name "Sigma0_HH_slv7_26Oct2911".
    private static boolean hasHigherRank(final String s1, final String s2) {

        if (s1.isEmpty() || s2.isEmpty()) {

            return false;
        }

        if (s1.contains("mst")) {

            if (s2.contains("mst")) {
                throw new OperatorException("Both band names contain mst");
            }

            return true;
        }

        if (s2.contains("mst")) {

            if (s1.contains("mst")) {
                throw new OperatorException("Both band names contain mst");
            }

            return false;
        }

        // The substring "slv..._" should be in the same location in s1 and s2 but we look for it
        // separately anyways.

        final int slvStartIdx1 = s1.indexOf("slv");
        if (slvStartIdx1 < 0) {
            throw new OperatorException("Can't find slv in band name 1 " + s1 + ' ' + s2);
        }

        final int slvEndIdx1 = s1.indexOf('_', slvStartIdx1);
        if (slvEndIdx1 < 0) {
            throw new OperatorException("Can't find _ after slv in band name 1 " + s1 + ' ' + s2);
        }

        if (slvEndIdx1 <= slvStartIdx1 + 3) {
            throw new OperatorException("No number after slv 1 " + s1 + ' ' + s2);
        }

        final int slvStartIdx2 = s2.indexOf("slv");
        if (slvStartIdx2 < 0) {
            throw new OperatorException("Can't find slv in band name 2 " + s1 + ' ' + s2);
        }

        final int slvEndIdx2 = s2.indexOf('_', slvStartIdx2);
        if (slvEndIdx2 < 0) {
            throw new OperatorException("Can't find _ after slv in band name 2 " + s1 + ' ' + s2);
        }

        if (slvEndIdx2 <= slvStartIdx2 + 3) {
            throw new OperatorException("No number after slv 2 " + s1 + ' ' + s2);
        }

        final String slvStr1 = s1.substring(slvStartIdx1 + 3, slvEndIdx1);

        if (!StringUtils.isNumeric(slvStr1)) {

            throw new OperatorException("Can't extract slave number from band name 1 " + s1 + ' ' + s2);
        }

        final int rank1 = Integer.valueOf(slvStr1);

        final String slvStr2 = s2.substring(slvStartIdx2 + 3, slvEndIdx2);

        if (!StringUtils.isNumeric(slvStr2)) {

            throw new OperatorException("Can't extract slave number from band name 2 " + s1 + ' ' + s2);
        }

        final int rank2 = Integer.valueOf(slvStr2);

        if (rank1 == rank2) {

            throw new OperatorException("Possible duplicate band names " + s1 + ' ' + s2);
        }

        return rank1 < rank2; // slv1 ranks higher than slv5
    }

    private static void swap(String[] a) {

        String tmp = a[0];
        a[0] = a[1];
        a[1] = tmp;
    }

    private static boolean isDecibel(final String unit) {

        // If no unit is specified, we assume it is not dB.

        if (unit == null) {
            return false;
        }

        final String unitInLowerCase = unit.toLowerCase();

        return (unitInLowerCase.contains("db") || unitInLowerCase.contains("decibel"));
    }

    // This needs to be called by the extended class before initialize() is called.
    // This is the number of images (1 or 2) used in the approach.

    static double toDecibels(final double sigma) {

        if (sigma < underFlowFloat) {

            return -underFlowFloat;

        } else {

            return 10.0d * Math.log10(sigma);
        }
    }

    private static int getIntegerTheta(final double theta) {

        return (int) Math.round(theta);
    }

    private static double demoteToFloatPrecision(double a) {

        final float b = (float) a;
        return (double) b;
    }

    private static double[] demoteToFloatPrecision(double[] a) {

        double[] b = new double[a.length];

        for (int i = 0; i < a.length; i++) {
            b[i] = demoteToFloatPrecision(a[i]);
        }

        return b;
    }

    /**
     *
     */
    public static File initializeLUTFolder() {
        File lutFolder;
        try {
            lutFolder = new File(Settings.instance().getAuxDataFolder(), "sm_luts");
            if (!lutFolder.exists()) {
                if (!lutFolder.mkdirs()) {
                    SystemUtils.LOG.severe("Unable to create folders in " + lutFolder);
                }
            }

            File[] files = lutFolder.listFiles();
            if (files == null || files.length == 0) {
                installDefaultLUTs(lutFolder);
            }
        } catch (Exception e) {
            lutFolder = new File(".");
        }
        return lutFolder;
    }

    private static void installDefaultLUTs(final File lutFolder) throws IOException {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(IEMInverBase.class);
        final Path sourcePath = moduleBasePath.resolve("auxdata/sm_luts/");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourcePath, lutFolder.toPath());
        resourceInstaller.install(".*", ProgressMonitor.NULL);
    }

    public static double getSigmaDistance(final double[] sigma1, final double[] sigma2) {

        double disSq = 0.0;
        for (int i = 0; i < sigma1.length; i++) {
            final double diff = sigma1[i] - sigma2[i];
            disSq += diff * diff;
        }
        return Math.sqrt(disSq);
    }

    protected static void initResults(final double[][] result, final double[][] resultSigma) {
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = INVALID_OUTPUT_VALUE;
            }
            resultSigma[i][0] = INVALID_OUTPUT_VALUE;
            resultSigma[i][1] = INVALID_OUTPUT_VALUE;
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            initializeLUTFolder();

            getSourceBandNames();

            createTargetProduct();

            if (N < 1) {
                throw new OperatorException("N = " + N + "; it must be >= 1");
            }

            if (M < 3 || M % 2 == 0) {
                throw new OperatorException("M = " + M + "; it must odd and >= 3");
            }

            srcRasterWidth = sourceProduct.getSceneRasterWidth();
            srcRasterHeight = sourceProduct.getSceneRasterHeight();
            halfM = (M - 1) / 2;

            /*System.out.println("N = " + N + " M = " + M + " halfM = " + halfM + " RDC threshold = " + rdcThreshold
                    + " filter remaining outliers = " + doRemainingOutliersFilter
                    + " output outlier = " + (outlierBand != null));*/

        } catch (Throwable e) {

            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Initialize parameters.
     *
     * @param srcProduct   The source product
     * @param lutInputFile The input LUT file
     */
    protected void initBaseParams(final Product srcProduct, final File lutInputFile, final boolean outputRMS) {

        sourceProduct = srcProduct;
        lutFile = lutInputFile;
        this.outputRMS = outputRMS;
    }

    /**
     * Get the target product.
     *
     * @return The target product
     */
    protected Product getTargetProductFromBase() {

        return targetProduct;
    }

    protected void updateMetadata(final String inversion) {
        final MetadataElement root = AbstractMetadata.getAbstractedMetadata(targetProduct);
        MetadataElement productElem = root.getElement("Product_Information");
        if (productElem == null) {
            productElem = new MetadataElement("Product_Information");
            root.addElement(productElem);
        }

        AbstractMetadata.addAbstractedAttribute(productElem, "Inversion type", ProductData.TYPE_ASCII, "", "");
        AbstractMetadata.setAttribute(productElem, "Inversion type", inversion);
    }

    /**
     * Set the number of images.
     *
     * @param numImages The number of images.
     */
    protected void setNumberOfImages(int numImages) {

        if (numImages != 1 && numImages != 2) {

            throw new OperatorException("Wrong number of images");
        }

        numberOfImages = numImages;

        sigmaHHBandName = new String[numImages];
        sigmaVVBandName = new String[numImages];
        thetaBandName = new String[numImages];

        for (int i = 0; i < numImages; i++) {

            sigmaHHBandName[i] = "";
            sigmaVVBandName[i] = "";
            thetaBandName[i] = "";
        }
    }

    /**
     * Initialize the LUT.
     *
     * @param numParams The number of input parameters input to the forward model, i.e., number of columns in
     *                  the LUT (not counting the rightmost 2 columns that are sigmaHH and sigmaVV).
     */
    protected void initLUT(final int numParams) throws IOException {

        getLUTMetadata();

        // paramLUT contains only rms, (cl if applicable) and RDC
        paramLUT = new double[NUM_ROWS_PER_ANGLE_DEGREE][numParams];

        final String filename = lutFile.getName();
        final String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);

        //System.out.println(fileExtension);

        if (filename.endsWith("." + MAT_FILE_EXTENSION)) {

            initLUTFromMatlabFile();

        } else if (filename.endsWith("." + CSV_FILE_EXTENSION)) {

            initLUTFromCSVFile();

        } else {

            throw new OperatorException("LUT files with extension ." + fileExtension + " are not supported. Only ." + MAT_FILE_EXTENSION + " and ." + CSV_FILE_EXTENSION + " are supported (case sensitive)");
        }
    }

    private void initLUTFromMatlabFile() {

        //System.out.println("initLUTFromMatlabFile");

        Product lutProd = null;

        try {

            lutProd = CommonReaders.readProduct(lutFile);

        } catch (Throwable e) {

            OperatorUtils.catchOperatorException(getId(), e);
        }

        if (lutProd == null) {

            throw new OperatorException("LUT product is null");
        }

        // The LUT file should have (NUM_ROWS_PER_ANGLE_DEGREE * NUM_ANGLE_SECTIONS) rows.
        // Each row contains the parameters and the sigma values.
        // E.g., for Hybrid approach, each row contains rms, cl, RDC, sigmaHH, sigmaVV.

        lutWidth = lutProd.getSceneRasterWidth();
        final int lutHeight = lutProd.getSceneRasterHeight();

        //System.out.println("lutWidth = " + lutWidth + " lutHeight = " + lutHeight);

        if (lutWidth != (paramLUT[0].length + 2)) {

            throw new OperatorException("LUT has wrong width = " + lutWidth);
        }

        if (lutHeight != (NUM_ROWS_PER_ANGLE_DEGREE * NUM_ANGLE_SECTIONS)) {

            throw new OperatorException("LUT has wrong height = " + lutHeight + "; expecting it to be " + NUM_ANGLE_SECTIONS * NUM_ROWS_PER_ANGLE_DEGREE);
        }

        if (lutProd.getNumBands() != 1) {

            throw new OperatorException("Too many bands in LUT product");
        }

        final Band lutBand = lutProd.getBandAt(0);

        if (lutBand == null) {

            throw new OperatorException("LUT band is null");
        }

        /*
        final int datatype = lutBand.getDataType();
        if (datatype != ProductData.TYPE_FLOAT64) {
            System.out.println("WARNING: lutBand data type = " + datatype);
        }  else {
            System.out.println("lutBand is doubles");
        }
        */

        // Populate paramLUT by reading in the first n columns of the first NUM_ROWS_PER_ANGLE_DEGREE rows of the LUT.
        // E.g., for Hybrid approach, n = 3 and we would read in rms, cl and RDC.
        // The contents of paramLUT will not change during the processing.

        lutData = lutBand.createCompatibleProductData((int) lutBand.getNumDataElems());

        if (lutData == null) {

            throw new OperatorException("LUT data is null");
        }

        try {

            lutBand.readRasterData(0, 0, lutWidth, lutHeight, lutData, ProgressMonitor.NULL);

        } catch (Throwable e) {

            OperatorUtils.catchOperatorException(getId(), e);
        }

        if (lutData.getNumElems() != (NUM_ANGLE_SECTIONS * NUM_ROWS_PER_ANGLE_DEGREE * (paramLUT[0].length + 2))) {

            throw new OperatorException("Wrong number of elements in LUT " + lutData.getNumElems());
        }

        for (int i = 0; i < paramLUT.length; i++) { // loop through rows of LUT

            for (int j = 0; j < paramLUT[i].length; j++) { // loop through columns of LUT

                paramLUT[i][j] = lutData.getElemDoubleAt(i * lutWidth + j);
            }
        }
    }

    private void initLUTFromCSVFile() {

        //System.out.println("initLUTFromCSVFile");

        sigmaLUT = new double[NUM_ANGLE_SECTIONS * NUM_ROWS_PER_ANGLE_DEGREE][2];
        final int numParams = paramLUT[0].length;

        try {

            final CSVReader cvsReader = new CSVReader(new FileReader(lutFile));

            // loop through rows in first angle section
            for (int i = 0; i < NUM_ROWS_PER_ANGLE_DEGREE; i++) {

                // read one row
                String[] row = cvsReader.readNext();

                if (row == null || row.length != numParams + 2) {

                    throw new OperatorException("Error reading row " + i + " (zero-based) in LUT (LUT has wrong height or width?");
                }

                // the left non-sigma columns
                for (int j = 0; j < numParams; j++) {

                    if (row[j].isEmpty()) {

                        throw new OperatorException("Error reading row " + i + " (zero-based) in LUT");
                    }
                    paramLUT[i][j] = Double.parseDouble(row[j]);
                }

                // the two sigma columns
                for (int j = 0; j < 2; j++) {

                    if (row[numParams + j].isEmpty()) {

                        throw new OperatorException("Error reading row " + i + " (zero-based) in LUT");
                    }
                    sigmaLUT[i][j] = Double.parseDouble(row[numParams + j]);
                }
            }

            // loop through remaining rows
            for (int i = NUM_ROWS_PER_ANGLE_DEGREE; i < (NUM_ANGLE_SECTIONS * NUM_ROWS_PER_ANGLE_DEGREE); i++) {

                String[] row = cvsReader.readNext();

                if (row == null || row.length != numParams + 2) {

                    throw new OperatorException("Error reading row " + i + " (zero-based) in LUT (LUT has wrong height or width?");
                }

                for (int j = 0; j < 2; j++) {

                    if (row[numParams + j].isEmpty()) {

                        throw new OperatorException("Error reading row " + i + " (zero-based) in LUT");
                    }
                    sigmaLUT[i][j] = Double.parseDouble(row[numParams + j]);
                }
            }

            // Have already read all the rows, so next row should be null
            String[] row = cvsReader.readNext();

            if (row != null) {

                throw new OperatorException("LUT has more rows than expected (expecting only " + NUM_ANGLE_SECTIONS * NUM_ROWS_PER_ANGLE_DEGREE + " rows)");
            }

        } catch (Throwable e) {

            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    // sigma contains the measured values to be used in the search.
    // theta[i] is the LIA of sigma[i].
    // pol[i] is polarization of sigma[i], 0 means HH, 1 means VV.
    // results and resultSigmas will not be filled if something goes wrong, so caller should initialize them.
    protected void searchLUTForN(final double[] sigma, final double[] theta, final int[] pol, double[][] results,
                                 double[][] resultSigmas) {

        final int numSigma = sigma.length;

        if (numSigma == 0) {

            throw new OperatorException("No sigmas");
        }

        if (theta.length != numSigma) {

            throw new OperatorException("Wrong theta array length");
        }

        if (pol.length != numSigma) {

            throw new OperatorException("Wrong pol array length");
        }

        int[] sectionIdx = new int[numSigma];

        for (int i = 0; i < sectionIdx.length; i++) {

            if (pol[i] != 0 && pol[i] != 1) {

                throw new OperatorException("Wrong polarization value");
            }

            sectionIdx[i] = getAngleSectionIndex(theta[i]);

            if (sectionIdx[i] < 0) {

                return;
            }
        }

        int rowIndices[] = null;
        try {
            rowIndices = doKDTreeSearchForN(sigma, sectionIdx, pol);
        } catch (Exception e) {
            System.out.println("caught doKDTreeSearchForN exception");
        }

        if (rowIndices == null) return;

        if (results.length != rowIndices.length || resultSigmas.length != rowIndices.length) {
            throw new OperatorException("Wrong results length");
        }

        for (int i = 0; i < rowIndices.length; i++) {
            int rowIdx = rowIndices[i];
            // rowIdx should never be out of range, but do a paranoid check anyways
            if (rowIdx >= 0 && rowIdx < NUM_ROWS_PER_ANGLE_DEGREE) {

                if (results[i].length != paramLUT[rowIdx].length) {
                    throw new OperatorException("Wrong results array length");
                }

                if (resultSigmas[i].length != pol.length) {
                    throw new OperatorException("Wrong resultSigmas array length");
                }

                System.arraycopy(paramLUT[rowIdx], 0, results[i], 0, results[i].length);

                for (int j = 0; j < pol.length; j++) {
                    resultSigmas[i][j] = getSigmaFromLUT(sectionIdx[j], rowIdx, pol[j]);
                }
            }
        }

        // Check to make sure...
        /*
        double dis  = getSigmaDistance(sigma, resultSigmas[0]);
        for (int i = 1; i < resultSigmas.length; i++) {
            final double dis1 = getSigmaDistance(sigma, resultSigmas[i]);
            if (dis1 < dis) {
                throw new OperatorException("sigmas from KD tree not in order");
            }
            dis = dis1;
        } */
    }

    // sigma contains the measured values to be used in the search.
    // theta[i] is the LIA of sigma[i].
    // pol[i] is polarization of sigma[i], 0 means HH, 1 means VV.
    // results will not be filled if something goes wrong, so caller should initialize results.
    protected void searchLUT(final double[] sigma, final double[] theta, final int[] pol, double[] results) {

        final int numSigma = sigma.length;

        if (numSigma == 0) {

            throw new OperatorException("No sigmas");
        }

        if (theta.length != numSigma) {

            throw new OperatorException("Wrong theta array length");
        }

        if (pol.length != numSigma) {

            throw new OperatorException("Wrong pol array length");
        }

        int[] sectionIdx = new int[numSigma];

        for (int i = 0; i < sectionIdx.length; i++) {

            if (pol[i] != 0 && pol[i] != 1) {

                throw new OperatorException("Wrong polarization value");
            }

            sectionIdx[i] = getAngleSectionIndex(theta[i]);

            if (sectionIdx[i] < 0) {

                return;
            }
        }

        //final int rowIdx = doLinearSearch(sigma, sectionIdx, pol);
        final int rowIdx = doKDTreeSearch(sigma, sectionIdx, pol);

        // rowIdx should never be out of range, but do a paranoid check anyways
        if (rowIdx >= 0 && rowIdx < NUM_ROWS_PER_ANGLE_DEGREE) {

            if (results.length != paramLUT[rowIdx].length) {

                throw new OperatorException("Wrong results array length");
            }

            System.arraycopy(paramLUT[rowIdx], 0, results, 0, results.length);
            //results[0] = (double) rowIdx; // for debugging, output the row index
            //results[0] = (double) (sectionIdx[0] + startIntThetaDeg); // for debugging, output the integral theta1
            //results[1] = (double) (sectionIdx[2] + startIntThetaDeg); // for debugging, output the integral theta2
        }
    }

    protected boolean isValidSigmaHH(final double sigmaHH, final int imageIdx) {

        return (isValid(sigmaHH) && sigmaHH != invalidSigmaHHValue[imageIdx]);
    }

    protected boolean isValidSigmaVV(final double sigmaVV, final int imageIdx) {

        return (isValid(sigmaVV) && sigmaVV != invalidSigmaVVValue[imageIdx]);
    }

    protected boolean isValidTheta(final double theta, final int imageIdx) {

        return (isValid(theta) && theta != invalidThetaValue[imageIdx]);
    }

    private ArrayList<String> findAllSourceBandNames(String keyword) {

        final String[] sourceBandNames = sourceProduct.getBandNames();

        ArrayList<String> names = new ArrayList<>();

        for (String s : sourceBandNames) {

            if (s.toLowerCase().contains(keyword.toLowerCase())) {

                names.add(s);
            }
        }

        return names;
    }

    private void findSourceBandNames(final String[] keywords, final String[] bandnames) {

        int numFound = 0;

        for (String s : keywords) {

            ArrayList<String> names = findAllSourceBandNames(s);

            if (names.size() + numFound <= bandnames.length) {

                for (int i = 0; i < names.size(); i++) {
                    bandnames[numFound + i] = names.get(i);
                }

                numFound += names.size();

                if (bandnames.length == numFound) {
                    break;
                }

            } else {

                throw new OperatorException("Too many bands containing \"" + s + "\" (case insensitive) in their names");
            }
        }
    }

    private void getSourceBandNames() {

        if (numberOfImages == 0) {

            throw new OperatorException("Extended class needs to set numberOfImages");
        }

        // This method does not check if a band is missing. It is checked in checkSourceBands().

        findSourceBandNames(SIGMA_HH_BANDNAME_KEYWORDS, sigmaHHBandName);
        findSourceBandNames(SIGMA_VV_BANDNAME_KEYWORDS, sigmaVVBandName);
        findSourceBandNames(LIA_BANDNAME_KEYWORDS, thetaBandName);

        if (numberOfImages == 2) {

            // Group the band names according to image so that
            // sigmaHHBandBane[n], sigmaVVBandName[n] and thetaBandBand[n] belong to the same image where n = 1 or 2.
            // E.g., Band names "Sigma0_HH_slv2_26Oct2011", "Sigma0_VV_mst_26Oct2911" and
            // "incidenceAngleFromEllipsoid_slv4_26Oct2011"
            // belong to one image and
            // Band names "Sigma0_HH_slv7_26Oct2911", "Sigma0_VV_slv5_26Oct2911" and
            // "incidenceAngleFromEllipsoid_slv9_26Oct2011"
            // belong to another image.

            if (hasHigherRank(sigmaHHBandName[1], sigmaHHBandName[0])) {

                swap(sigmaHHBandName);
            }

            if (hasHigherRank(sigmaVVBandName[1], sigmaVVBandName[0])) {

                swap(sigmaVVBandName);
            }

            if (hasHigherRank(thetaBandName[1], thetaBandName[0])) {

                swap(thetaBandName);
            }
        }
    }

    protected void getSourceBands() {

        sigmaHHBand = new Band[numberOfImages];
        sigmaVVBand = new Band[numberOfImages];
        thetaBand = new Band[numberOfImages];

        invalidSigmaHHValue = new double[numberOfImages];
        invalidSigmaVVValue = new double[numberOfImages];
        invalidThetaValue = new double[numberOfImages];

        sigmaHHUnitIsDecibels = new boolean[numberOfImages];
        sigmaVVUnitIsDecibels = new boolean[numberOfImages];

        for (int i = 0; i < numberOfImages; i++) {

            if (sigmaHHBandName[i].isEmpty()) {
                sigmaHHBand[i] = null;
            } else {
                sigmaHHBand[i] = sigmaHHBandName[i] == null ? null : sourceProduct.getBand(sigmaHHBandName[i]);
            }

            if (sigmaHHBand[i] != null) {

                invalidSigmaHHValue[i] = sigmaHHBand[i].getNoDataValue();
                sigmaHHUnitIsDecibels[i] = isDecibel(sigmaHHBand[i].getUnit());

                /*
                final int datatype = sigmaHHBand[i].getDataType();
                if (datatype != ProductData.TYPE_FLOAT64) {
                    System.out.println("sigmaHHBand[" + i + "] data type = " + datatype);
                }
                */
            }

            if (sigmaVVBandName[i].isEmpty()) {
                sigmaVVBand[i] = null;
            } else {
                sigmaVVBand[i] = sigmaVVBandName[i] == null ? null : sourceProduct.getBand(sigmaVVBandName[i]);
            }

            if (sigmaVVBand[i] != null) {

                invalidSigmaVVValue[i] = sigmaVVBand[i].getNoDataValue();
                sigmaVVUnitIsDecibels[i] = isDecibel(sigmaVVBand[i].getUnit());

                /*
                final int datatype = sigmaVVBand[i].getDataType();
                if (datatype != ProductData.TYPE_FLOAT64) {
                    System.out.println("sigmaVVBand[" + i + "] data type = " + datatype);
                }
                */
            }

            if (thetaBandName[i].isEmpty()) {
                thetaBand[i] = null;
            } else {
                thetaBand[i] = thetaBandName[i] == null ? null : sourceProduct.getBand(thetaBandName[i]);
            }

            if (thetaBand[i] != null) {

                invalidThetaValue[i] = thetaBand[i].getNoDataValue();

                /*
                final int datatype = thetaBand[i].getDataType();
                if (datatype != ProductData.TYPE_FLOAT64) {
                    System.out.println("thetaBand[" + i + "] data type = " + datatype);
                }
                */
            }
        }

        checkSourceBands();
    }

    // Extended class should override
    // This base class will try to get the band names for sigmaHH, sigmaVV and theta.
    // Different extended class may need different bands, so it is up to each extended class to check if any
    // required band is missing.
    protected void checkSourceBands() {

    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        // The target product has the same dimensions as the source products.

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        // Add the bands that appear in the target product for all approaches.
        // The target bands all have the same dimensions as the target product.
        // Assume the target bands are all doubles.

        if (outputRMS) {

            rmsBand = targetProduct.addBand("rms", ProductData.TYPE_FLOAT32);
            rmsBand.setUnit("cm");
            rmsBand.setNoDataValue(INVALID_OUTPUT_VALUE);
            rmsBand.setNoDataValueUsed(true);
        }

        rdcBand = targetProduct.addBand("RDC", ProductData.TYPE_FLOAT32);
        rdcBand.setUnit("Farad/m");
        rdcBand.setNoDataValue(INVALID_OUTPUT_VALUE);
        rdcBand.setNoDataValueUsed(true);

        if (N > 1) {
            boolean outputOutlier = Config.instance().preferences().getBoolean(OUTPUT_OUTLIER, false);
            if (outputOutlier) {
                outlierBand = targetProduct.addBand("outlier", ProductData.TYPE_INT8);
                outlierBand.setUnit("boolean flag");
                outlierBand.setNoDataValue(INVALID_OUTLIER_VALUE);
                outlierBand.setNoDataValueUsed(true);
            }
            if (doDebug) {
                debugBand = targetProduct.addBand("debug", ProductData.TYPE_FLOAT32);
                debugBand.setNoDataValue(INVALID_OUTPUT_VALUE);
                debugBand.setNoDataValueUsed(true);
            }
        }

        // If there are sand and clay bands, pass them onto the target

        final String[] sourceBandNames = sourceProduct.getBandNames();

        String bandName = "";

        for (String s : sourceBandNames) {

            if (s.toLowerCase().contains("clay") || s.toLowerCase().contains("sand")) {

                ProductUtils.copyBand(s, sourceProduct, targetProduct, true);
            }
        }

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private boolean isNotInRange(int intTheta) {

        return (intTheta < startIntThetaDeg || intTheta > endIntThetaDeg);
    }

    protected int getAngleSectionIndex(final double theta) {

        final int intTheta = getIntegerTheta(theta);

        if (isNotInRange(intTheta)) {

            return -1;
        }

        return (intTheta - startIntThetaDeg);
    }

    // sectionIdx points to the 13252-row section of the LUT belonging to a theta.
    //
    // One theta corresponds to one sigma pair. One sigma pair contains one sigmaHH and one sigmaVV.
    //
    // Multi-pol:
    // There is one sigma pair corresponding to a theta.
    // sigma, sectionIdx and sigmaColIdx should all have length 2.
    // sectionIdx[0] == sectionIdx[1] == the section index of the theta corresponding to
    // sigma[0] (of one polarization) and sigma[1] (of the other polarization).
    // sigmaColIdx should be of length 2. Column index 0 is HH and 1 is VV.
    // In general, sigma[0] should be HH and sigma[1] is VV which means
    // sigmaColIdx[0] == 0 and sigmaColIdx[1] == 1.
    //
    // Multi-angle:
    // There are two thetas and each theta has one corresponding sigma value that can be of either polarization.
    // (4 possible combinations total).
    // sigma, sectionIdx and sigmaColIdx should all have length 2.
    // sectionIdx[i] corresponds to sigma[i], i = 0, 1.
    // We need to know if sigma[i] is HH or VV, or more specific, which column of the LUT to use.
    // 1) HH1-HH2: sigmaColIdx[0] == 0, sigmaColIdx[1] == 0
    // 2) HH1-VV2: sigmaColIdx[0] == 0, sigmaColIdx[1] == 1
    // 3) VV1-VV2: sigmaColIdx[0] == 1, sigmaColIdx[1] == 1
    // 4) VV1-HH2: sigmaColIdx[0] == 1, sigmaColIdx[1] == 0
    //
    // Hybrid:
    // There are two sigma pairs each corresponding to a theta.
    // sigma, sectionIdx and sigmaColIdx should all have length 4.
    // In general,
    // sigma[0] and sigma[1] is one pair; and
    // sigma[2] and sigma[3] is another pair.
    // sigma[0] and sigma[2] are HH and sigma[1] and sigma[3] are VV.
    // So sectionIdx[0] == sectionIdx[1] and sectionIdx[2] == sectionIdx[3] and
    // sigmaColIdx[0] == 0 (HH)
    // sigmaColIdx[1] == 1 (VV)
    // sigmaColIdx[2] == 0 (HH)
    // sigmaColIdx[3] == 1 (VV)
    //
    private int doLinearSearch(final double[] sigma, final int[] sectionIdx, final int[] sigmaColIdx) {

        // Assume the input arrays are of correct length.

        int result = -1;
        double savedDis = Double.MAX_VALUE;

        for (int rowIdx = 0; rowIdx < NUM_ROWS_PER_ANGLE_DEGREE; rowIdx++) {

            double dis = 0.0;

            // Loop through each sigma value
            for (int j = 0; j < sigma.length; j++) {

                final int secIdx = sectionIdx[j];
                final int colIdx = sigmaColIdx[j];
                final double diff = sigma[j] - getSigmaFromLUT(secIdx, rowIdx, colIdx);
                dis += diff * diff;
            }

            dis = Math.sqrt(dis);

            if (dis < savedDis) {

                savedDis = dis;
                result = rowIdx;
            }
        }

        return result;
    }

    private int[] doKDTreeSearchForN(final double[] sigma, final int[] sectionIdx, final int[] sigmaColIdx) {

        // This error checking is really redundant but safe.

        if (sectionIdx.length != sigma.length) {

            throw new OperatorException("sigma.length = " + sigma.length + " and sectionIdx.length = " +
                    sectionIdx.length + " should be equal");
        }

        if (sigmaColIdx.length != sigma.length) {

            throw new OperatorException("sigma.length = " + sigma.length + " and sigmaColIdx.length = " +
                    sigmaColIdx.length + " should be equal");
        }

        KDTree kdTree = null;

        if (kdTreeMap == null) {

            kdTree = buildOneKDTRee(sectionIdx, sigmaColIdx);

        } else {

            final KDTreeInfo info = new KDTreeInfo(sectionIdx, sigmaColIdx);

            kdTree = kdTreeMap.get(convertToKDTreeMapIntKey(info));
        }

        if (kdTree == null) { // This should never happen

            return null;
        }

        Object object[] = kdTree.nearest(sigma, N);
        if (object == null || object.length == 0) { // This should never happen
            return null;
        }

        RowIndex result[] = new RowIndex[object.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (RowIndex) object[i];
        }

        int[] rowIndices = new int[result.length];
        for (int i = 0; i < rowIndices.length; i++) {
            rowIndices[i] = result[i].getIndex();
        }

        return rowIndices;
    }

    private int doKDTreeSearch(final double[] sigma, final int[] sectionIdx, final int[] sigmaColIdx) {

        // This error checking is really redundant but safe.

        if (sectionIdx.length != sigma.length) {

            throw new OperatorException("sigma.length = " + sigma.length + " and sectionIdx.length = " +
                    sectionIdx.length + " should be equal");
        }

        if (sigmaColIdx.length != sigma.length) {

            throw new OperatorException("sigma.length = " + sigma.length + " and sigmaColIdx.length = " +
                    sigmaColIdx.length + " should be equal");
        }

        KDTree kdTree = null;

        if (kdTreeMap == null) {

            kdTree = buildOneKDTRee(sectionIdx, sigmaColIdx);

        } else {

            final KDTreeInfo info = new KDTreeInfo(sectionIdx, sigmaColIdx);

            kdTree = kdTreeMap.get(convertToKDTreeMapIntKey(info));
        }

        if (kdTree == null) { // This should never happen

            return -1;
        }

        //RowIndex result = (RowIndex) kdTree.nearest(demoteToFloatPrecision(sigma)); // For comparing with matlab
        RowIndex result = (RowIndex) kdTree.nearest(sigma);

        if (result == null) { // This should never happen

            return -1;

        } else {

            return result.getIndex();
        }
    }

    // Builds a map that maps a key (which can be derived from KDTReeInfo) to the KD Tree built based
    // on KDTreeInfo.
    // This will build all the KD trees that are needed.
    protected void buildKDTreeMap(final KDTreeInfo[] infos) {

        kdTreeMap = new TreeMap<>();

        for (KDTreeInfo info : infos) {

            //info.dumpContents();

            final KDTree kdtree = buildOneKDTRee(info.getSectionIdx(), info.getSigmaColIdx());

            kdTreeMap.put(convertToKDTreeMapIntKey(info), kdtree);
        }

        /*
        We cannot delete the sigma LUTs here because in searchLUTForN(), we need to access the LUT to get the sigmas of
        the N closest match from the KD tree.

        // lutData or sigmaLUT can be deleted after all the KD trees are built.
        // However, if linear search is to be used, then lutData cannot be deleted here.
        if (lutData == null) {

            sigmaLUT = null;

        } else {

            lutData.dispose();
            lutData = null;
        }
        */
    }

    // See explanation above doLinearSearch() and doKDTreeSearch() on sectionIdx and sigmaColIdx.
    // The sigmas comprise the key and the LUT row index (relative to a section) of the sigmas is the value stored
    // in the KD tree for that key.
    private KDTree buildOneKDTRee(final int[] sectionIdx, final int[] sigmaColIdx) {

        if (sectionIdx.length != sigmaColIdx.length) {

            throw new OperatorException("sectionIdx.length = " + sectionIdx.length + " and sigmaColIdx.length = " +
                    sigmaColIdx.length + " should be equal");
        }

        // Tree dimension is the number of sigmas being compared.
        // Hybrid: It is 4. SigmaHH_AM, SigmaVV_AM, SigmaHH_PM and SigmaVV_PM
        // Multi-pol: It is 2. (SigmaHH_AM and SigmaVV_AM) or (SigmaHH_PM and SigmaVV_PM)
        // Multi-angle: It is 2. (SigmaHH_AM and SigmaHH_PM) or (SigmaHH_AM and SigmaVV_PM) or
        // (SigmaVV_AM and SigmaVV_PM) or (SigmaVV_AM and SigmaHH_PM)
        final int treeDim = sectionIdx.length;

        final KDTree kdtree = new KDTree(treeDim);
        final double[] sigmas = new double[treeDim];
        for (int i = 0; i < NUM_ROWS_PER_ANGLE_DEGREE; i++) {

            for (int j = 0; j < treeDim; j++) {

                sigmas[j] = getSigmaFromLUT(sectionIdx[j], i, sigmaColIdx[j]);
            }

            //kdtree.insert(demoteToFloatPrecision(sigmas), new RowIndex(i)); // For comparing with matlab
            kdtree.insert(sigmas, new RowIndex(i));
        }

        return kdtree;
    }

    private double getSigmaFromLUT(int sectionIdx, int rowIdx, int colIdx) {

        if (lutData == null) {

            return getSigmaFromCSV(sectionIdx, rowIdx, colIdx);

        } else {

            return getSigmaFromMatlab(sectionIdx, rowIdx, colIdx);
        }
    }

    private double getSigmaFromMatlab(int sectionIdx, int rowIdx, int colIdx) {

        // See description of LUT above lutFile.
        // sectionIdx is zero-based index of the angle section.
        // rowIdx is zero-based index relative to the start of the angle section.
        // colIdx can be or 0 or 1. 0 means sigmaHH and 1 means sigmaVV.

        final int startRowIdx = sectionIdx * NUM_ROWS_PER_ANGLE_DEGREE;
        final int idx = (startRowIdx + rowIdx) * lutWidth + (lutWidth - 2);

        return lutData.getElemDoubleAt(idx + colIdx);
    }

    private double getSigmaFromCSV(int sectionIdx, int rowIdx, int colIdx) {

        return sigmaLUT[sectionIdx * NUM_ROWS_PER_ANGLE_DEGREE + rowIdx][colIdx];
    }

    // This will provide a KD tree with a unique value that can be used as a key to a map.
    // should be overridden by extended class
    protected int convertToKDTreeMapIntKey(KDTreeInfo info) {

        return -1;
    }

    private void getLUTMetadata() throws IOException {

        String metadataFullPath = lutFile.getAbsolutePath();
        //System.out.println("lutFile.getAbsolutePath() = " + metadataFullPath);

        int dotIdx = metadataFullPath.lastIndexOf(".");
        metadataFullPath = metadataFullPath.substring(0, dotIdx) + "." + HDR_FILE_EXTENSION;
        //System.out.println("metadata full path " + metadataFullPath);

        final File metadataFile = new File(metadataFullPath);
        final PropertyMap metadataPref = new DefaultPropertyMap();
        metadataPref.load(metadataFile.toPath());

        NUM_ROWS_PER_ANGLE_DEGREE = metadataPref.getPropertyInt("Number_of_rows_per_angle");
        startIntThetaDeg = metadataPref.getPropertyInt("Start_angle");
        endIntThetaDeg = metadataPref.getPropertyInt("Stop_angle");
        NUM_ANGLE_SECTIONS = endIntThetaDeg - startIntThetaDeg + 1;

        if (endIntThetaDeg < startIntThetaDeg) {

            throw new OperatorException("In LUT metadata (." + HDR_FILE_EXTENSION + " file), Start_angle = " + startIntThetaDeg + " should be <= Stop_angle = " + endIntThetaDeg);
        }

        /*
        System.out.println("NUM_ROWS_PER_ANGLE_DEGREE = " + NUM_ROWS_PER_ANGLE_DEGREE);
        System.out.println("startIntThetaDeg = " + startIntThetaDeg);
        System.out.println("endIntThetaDeg = " + endIntThetaDeg);

        System.out.println("Filename = " + metadataPref.getPropertyString("Filename"));
        System.out.println("LUT_format = " + metadataPref.getPropertyString("LUT_format"));
        System.out.println("Data_set_format = " + metadataPref.getPropertyString("Data_set_format"));
        System.out.println("Version = " + metadataPref.getPropertyString("Version"));
        System.out.println("Date_Time_of_creation = " + metadataPref.getPropertyString("Date_Time_of_creation"));
        System.out.println("Description = " + metadataPref.getPropertyString("Description"));
        System.out.println("Mission = " + metadataPref.getPropertyString("Mission"));
        System.out.println("Number_of_rows = " + metadataPref.getPropertyInt("Number_of_rows"));
        System.out.println("Number_of_columns = " + metadataPref.getPropertyInt("Number_of_columns"));
        System.out.println("Start_angle = " + metadataPref.getPropertyInt("Start_angle"));
        System.out.println("Stop_angle = " + metadataPref.getPropertyInt("Stop_angle"));
        System.out.println("Number_of_rows_per_angle = " + metadataPref.getPropertyInt("Number_of_rows_per_angle"));
        System.out.println("Reference_to_input_data_source = " + metadataPref.getPropertyString("Reference_to_input_data_source"));
        */
    }

    protected void getMinMaxIncidenceAngles(final double[] minThetas, final double[] maxThetas) {

        final int len = thetaBand.length;

        if (len < 1 || minThetas.length != len || maxThetas.length != len) {

            throw new OperatorException("IEMInverBase::getMinMaxIncidenceAngles: wrong array length");
        }

        final double NO_DATA = AbstractMetadata.NO_METADATA;

        for (int i = 0; i < len; i++) {

            minThetas[i] = NO_DATA;
            maxThetas[i] = NO_DATA;
        }

        // First try to get min/max thetas from meta data

        final MetadataElement metadata1 = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        if (metadata1 != null) {

            minThetas[0] = metadata1.getAttributeDouble(AbstractMetadata.incidence_near, NO_DATA);
            maxThetas[0] = metadata1.getAttributeDouble(AbstractMetadata.incidence_far, NO_DATA);
        }

        if (len > 1) {

            MetadataElement metadata2 = null;

            final MetadataElement slvMetaData = AbstractMetadata.getSlaveMetadata(sourceProduct.getMetadataRoot());

            if (slvMetaData != null) {

                if (slvMetaData.getNumElements() > 0) {

                    metadata2 = slvMetaData.getElementAt(0);

                    if (metadata2 != null) {

                        minThetas[1] = metadata2.getAttributeDouble(AbstractMetadata.incidence_near, NO_DATA);
                        maxThetas[1] = metadata2.getAttributeDouble(AbstractMetadata.incidence_far, NO_DATA);
                    }
                }
            }
        }

        for (int i = 0; i < len; i++) {

            if (minThetas[i] == NO_DATA || maxThetas[i] == NO_DATA) {

                // Get min/max thetas from band if failed to get them from meta data

                final Stx stx = thetaBand[i].getStx();
                minThetas[i] = stx.getMinimum();
                maxThetas[i] = stx.getMaximum();
            }
        }
    }

    // This is for unit test only.
    protected boolean compareLUTs(final String csvLUTFilePath, final String matlabLUTFilePath, final int numParams, final double epsilon) throws IOException {

        boolean isOK = true;

        lutFile = new File(csvLUTFilePath);
        getLUTMetadata();
        paramLUT = new double[NUM_ROWS_PER_ANGLE_DEGREE][numParams];
        initLUTFromCSVFile();
        final double csvParamLUT[][] = paramLUT.clone();
        paramLUT = null;

        lutFile = new File(matlabLUTFilePath);
        getLUTMetadata();
        paramLUT = new double[NUM_ROWS_PER_ANGLE_DEGREE][numParams];
        initLUTFromMatlabFile();
        final double matlabParamLUT[][] = paramLUT.clone();
        paramLUT = null;

        System.out.println("csvParamLUT.length = " + csvParamLUT.length);
        System.out.println("matlabParamLUT.length = " + matlabParamLUT.length);

        double maxParamDiff[] = new double[numParams];
        for (int j = 0; j < numParams; j++) {
            maxParamDiff[j] = 0.0d;
        }

        for (int i = 0; i < NUM_ROWS_PER_ANGLE_DEGREE; i++) {

            for (int j = 0; j < numParams; j++) {

                final double diff = Math.abs(csvParamLUT[i][j] - matlabParamLUT[i][j]);
                if (diff > maxParamDiff[j]) {
                    maxParamDiff[j] = diff;
                }
            }
        }

        String paramsStr[] = new String[numParams];
        paramsStr[0] = "max abs rms diff";
        if (numParams == 2) {
            paramsStr[1] = "max abs RDC diff";
        } else {
            paramsStr[1] = "max abs cl diff";
            paramsStr[2] = "max abs RDC diff";
        }
        for (int i = 0; i < numParams; i++) {
            System.out.println(paramsStr[i] + " = " + maxParamDiff[i]);
            if (maxParamDiff[i] > epsilon) {
                isOK = false;
            }
        }

        double maxSigmaDiff[] = new double[2];
        maxSigmaDiff[0] = 0.0d;
        maxSigmaDiff[1] = 0.0d;

        for (int i = 0; i < NUM_ANGLE_SECTIONS; i++) {

            for (int j = 0; j < NUM_ROWS_PER_ANGLE_DEGREE; j++) {

                for (int k = 0; k < 2; k++) {

                    final double diff = Math.abs(getSigmaFromCSV(i, j, k) - getSigmaFromMatlab(i, j, k));
                    if (diff > maxSigmaDiff[k]) {
                        maxSigmaDiff[k] = diff;
                    }
                }
            }
        }

        String sigmaStr[] = {"max sigmaHH abs diff", "max sigmaVV abs diff"};
        for (int i = 0; i < 2; i++) {
            System.out.println(sigmaStr[i] + " = " + maxSigmaDiff[0]);
            if (maxSigmaDiff[i] > epsilon) {
                isOK = false;
            }
        }

        return isOK;
    }

    private void getExtendedDims(final int tx0, final int tw, final int rasterWidth, final int[] result) {

        // works for height too

        int tw1 = tw + M - 1;
        int tx1 = tx0 - halfM;
        if (tx1 < 0) {
            tw1 = tw1 + tx1;
            tx1 = 0;
        }
        if (tx1 + tw1 > rasterWidth) {
            tw1 = rasterWidth - tx1;
        }

        result[0] = tx1;
        result[1] = tw1;
    }

    protected Rectangle getExtendedRectangle(final int tx0, final int ty0, final int tw, final int th) {

        final int result[] = new int[2];

        getExtendedDims(tx0, tw, srcRasterWidth, result);
        final int tx1 = result[0];
        final int tw1 = result[1];

        getExtendedDims(ty0, th, srcRasterHeight, result);
        final int ty1 = result[0];
        final int th1 = result[1];

        //System.out.println("tx1 = " + tx1 + " ty1 = " + ty1 + " tw1 = " + tw1 + " th1 = " + th1);

        //return new Rectangle(tx0, ty0, tw, th);
        return new Rectangle(tx1, ty1, tw1, th1);
    }

    private double getAverageRDC(final int x, final int y, KDTreeNearestNeighbours[][] nearestNeighbours,
                                 final double[] otherAvg) {

        // otherAvg[0] is average rms
        // otherAvg[1] is average cl

        final int w = nearestNeighbours.length;
        final int h = nearestNeighbours[0].length;

        double sum = 0.0;
        double[] otherSums = new double[otherAvg.length];
        for (int i = 0; i < otherSums.length; i++) {
            otherSums[0] = 0.0;
        }
        int cnt = 0;

        for (int i = x - halfM; i <= x + halfM; i++) {
            for (int j = y - halfM; j <= y + halfM; j++) {

                if (i < 0 || j < 0 || i >= w || j >= h || (i == x && j == y)) continue;

                if (nearestNeighbours[i][j] != null) {
                    sum += nearestNeighbours[i][j].rdc.get(0);
                    otherSums[0] += nearestNeighbours[i][j].rms.get(0);
                    if (otherSums.length > 1) {
                        KDTreeNearestNeighbours1 nn = (KDTreeNearestNeighbours1) nearestNeighbours[i][j];
                        otherSums[1] += nn.cl.get(0);
                    }
                    cnt++;
                }
            }
        }

        if (cnt > 0) {
            otherAvg[0] = otherSums[0] / (double) cnt;
            if (otherAvg.length > 1) {
                otherAvg[1] = otherSums[1] / (double) cnt;
            }
            return sum / (double) cnt;
        } else {
            otherAvg[0] = nearestNeighbours[x][y].rms.get(0);
            if (otherAvg.length > 1) {
                KDTreeNearestNeighbours1 nn = (KDTreeNearestNeighbours1) nearestNeighbours[x][y];
                otherAvg[1] = nn.cl.get(0);
            }
            return nearestNeighbours[x][y].rdc.get(0);
        }
    }

    protected int getBestKDTreeNeighbour(final int x, final int y, KDTreeNearestNeighbours[][] nearestNeighbours,
                                         final double outParams[]) {

        final ArrayList<Double> nnRDC = nearestNeighbours[x][y].rdc;

        // outParams[0] is outlier status: 0, 1 or 2
        // outParams[1] is average RDC
        // outParams[2] is average rms
        // outParams[3] is average cl
        // otherAvg[0] is average rms
        // otherAvg[1] is average cl
        final double[] otherAvg = new double[outParams.length - 2];
        final double avgRDC = getAverageRDC(x, y, nearestNeighbours, otherAvg);

        int bestIdx = 0;
        double deviation = Math.abs(avgRDC - nnRDC.get(bestIdx)) / Math.abs(avgRDC);
        outParams[0] = 0; // not outlier
        if (deviation > rdcThreshold) {
            outParams[0] = 1; // still outlier; could replaced with mean RDC
            for (int i = 1; i < nnRDC.size(); i++) {
                final double newDeviation = Math.abs(avgRDC - nnRDC.get(i)) / Math.abs(avgRDC);
                if (newDeviation < deviation) {
                    bestIdx = i;
                    deviation = newDeviation;
                    if (deviation <= rdcThreshold) {
                        outParams[0] = 2; // no longer outlier
                    }
                }
            }
            if (doRemainingOutliersFilter && (outParams[0] == 1)) {
                outParams[1] = avgRDC;
                outParams[2] = otherAvg[0]; // rms
                if (outParams.length > 3) {
                    outParams[3] = otherAvg[1]; // cl
                }
                bestIdx = -1;
            }
        }

        return bestIdx;
    }

    protected KDTreeNearestNeighbours setUpNearestNeighbours(final double[][] result, final double[][] resultSigma) {
        // result[.][0] is rms
        // result[.][1] is RDC
        ArrayList<Double> rdcList = new ArrayList<>();
        ArrayList<Double> rmsList = new ArrayList<>();
        ArrayList<double[]> sigmaList = new ArrayList<>();
        for (int i = 0; i < result.length; i++) {
            rmsList.add(result[i][0]);
            rdcList.add(result[i][1]);
            sigmaList.add(resultSigma[i]);
        }
        return new KDTreeNearestNeighbours(rdcList, rmsList, sigmaList);
    }

    protected KDTreeNearestNeighbours1 setUpNearestNeighbours1(final double[][] result, final double[][] resultSigma) {
        // result[.][0] is rms
        // result[.][1] is cl
        // result[.][2] is RDC
        ArrayList<Double> rdcList = new ArrayList<>();
        ArrayList<Double> rmsList = new ArrayList<>();
        ArrayList<Double> clList = new ArrayList<>();
        ArrayList<double[]> sigmaList = new ArrayList<>();
        for (int i = 0; i < result.length; i++) {
            rmsList.add(result[i][0]);
            clList.add(result[i][1]);
            rdcList.add(result[i][2]);
            sigmaList.add(resultSigma[i]);
        }
        return new KDTreeNearestNeighbours1(rdcList, rmsList, clList, sigmaList);
    }

    protected void printDebugMsg(final int x, final int y, final double rdc, final int bestIdx, final double[] sigma,
                                 final KDTreeNearestNeighbours nn) {

        if (bestIdx != 0) {
            double dis1 = IEMInverBase.getSigmaDistance(sigma, nn.sigmas.get(0));
            double dis2 = IEMInverBase.getSigmaDistance(sigma, nn.sigmas.get(bestIdx));
            System.out.println("x = " + x + " y = " + y + " rdc from " + nn.rdc.get(0) + " to " + rdc +
                    " sigma dis from " + dis1 + " to " + dis2);
        }
    }

    private static class RowIndex {

        private final int idx;

        public RowIndex(int index) {
            this.idx = index;
        }

        public int getIndex() {
            return this.idx;
        }
    }

    // KDTreeInfo completely defines a KDTRee.
    // Use convertToKDTreeMapIntKey() to derive a key from KDTReeInfo that can be used as a key to a map.
    protected static class KDTreeInfo {

        private int[] sectionIndex;
        private int[] sigmaColIndex;

        // See explanation above doLinearSearch() and doKDTreeSearch() on sectionIdx and sigmaColIdx.
        public KDTreeInfo(final int[] sectionIdx, final int[] sigmaColIdx) {
            sectionIndex = sectionIdx;
            sigmaColIndex = sigmaColIdx;
        }

        public int[] getSectionIdx() {
            return sectionIndex;
        }

        public int[] getSigmaColIdx() {
            return sigmaColIndex;
        }

        public void dumpContents() {

            System.out.print("sectionIndex: ");
            for (int aSectionIndex : sectionIndex) {
                System.out.print(aSectionIndex + " ");
            }
            System.out.println("");

            System.out.print("sigmaColIndex: ");
            for (int aSigmaColIndex : sigmaColIndex) {
                System.out.print(aSigmaColIndex + " ");
            }
            System.out.println("");
        }
    }

    protected class KDTreeNearestNeighbours {

        final protected ArrayList<Double> rdc;
        final protected ArrayList<Double> rms;
        final protected ArrayList<double[]> sigmas;

        KDTreeNearestNeighbours(final ArrayList<Double> rdc, final ArrayList<Double> rms, ArrayList<double[]> sigmas) {
            this.rdc = rdc;
            this.rms = rms;
            this.sigmas = sigmas;
        }
    }

    protected class KDTreeNearestNeighbours1 extends KDTreeNearestNeighbours {

        final protected ArrayList<Double> cl;

        KDTreeNearestNeighbours1(final ArrayList<Double> rdc, final ArrayList<Double> rms, final ArrayList<Double> cl,
                                 ArrayList<double[]> sigmas) {
            super(rdc, rms, sigmas);
            this.cl = cl;
        }
    }
}