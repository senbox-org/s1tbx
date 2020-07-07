/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.s1tbx.sentinel1.gpf.util.ArcDataIntegration;
import org.esa.s1tbx.sentinel1.gpf.util.GraphUtils;
import org.esa.s1tbx.sentinel1.gpf.util.OverlapUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.SLCImage;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.core.utils.BandUtilsDoris;
import org.jlinda.core.utils.CplxContainer;
import org.jlinda.core.utils.ProductContainer;
import org.jlinda.core.utils.TileUtilsDoris;
import org.json.simple.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Estimate range and azimuth offsets for each burst using cross-correlation with a 512x512 block in
 * the center of the burst. Then average the offsets computed for all bursts in the same sub-swath to
 * get one constant offset for the whole sub-swath.
 * <p>
 * Perform range shift for all bursts in a sub-swath with the constant range offset computed above using
 * a frequency domain method.
 * <p>
 * <p>
 * For the azimuth shift estimation, this operator uses the Network Enhanced Spectral Diversity (NESD) method.
 * <p>
 * Reference:
 * H. Fattahi, P. Agram, and M. Simons. "A network-based enhanced spectral diversity approach for TOPS time-series
 * analysis". In: IEEE Transactions on Geoscience and Remote Sensing, vol. 55, no. 2, pp. 777-786. February 2017.
 * DOI:10.1109/TGRS.2016.2614925
 * <p>
 * <p>
 * ESD between pairs can be computed with one of two methods: Weighted average or Periodogram. Both are described in:
 * <p>
 * Reference:
 * N. Yague-Martinez, P. Prats-Iraola, F. Rodriguez Gonzalez, R. Brcic, R. Shau, D. Geudtner, M. Eineder, and
 * R. Bamler. “Interferometric Processing of Sentinel-1 TOPS Data”. In: IEEE Transactions on
 * Geoscience and Remote Sensing, vol. 54, no. 4, pp. 2220–2234, April 2016. ISSN:0196-2892.
 * DOI:10.1109/TGRS.2015.2497902
 */
@OperatorMetadata(alias = "Enhanced-Spectral-Diversity",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "David A. Monge, Reinier Oost, Esteban Aguilera, Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SENSAR B.V.\nCopyright (C) 2016 by Array Systems Computing Inc.",
        description = "Estimate constant range and azimuth offsets for a stack of images")
public class SpectralDiversityOp extends Operator {

    // ESD estimators
    private final static String ESD_AVERAGE = "Average";
    private final static String ESD_PERIODOGRAM = "Periodogram";

    // Weight functions
    private final static String WEIGHT_FN_NONE = "None";
    private final static String WEIGHT_FN_LINEAR = "Linear";
    private final static String WEIGHT_FN_QUAD = "Quadratic";
    private final static String WEIGHT_FN_INVQUAD = "Inv Quadratic";

    // Optimization criteria for Peridogram
    private final static String OPT_CRITERION_MIN_ARG = "Min. argument";
    private final static String OPT_CRITERION_MAX_REAL = "Max. real part";

    // Integration network distance functions
    private final static String INT_NETWORK_DAYS_BASELINE = "Number of days";
    private final static String INT_NETWORK_IMAGES_BASELINE = "Number of images";

    // Integration network method
    private final static String INT_METHOD_L1 = "L1";
    private final static String INT_METHOD_L2 = "L2";
    private final static String INT_METHOD_L1_AND_L2 = "L1 and L2";


    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "512",
            label = "Registration Window Width")
    private String fineWinWidthStr = "512";

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "512",
            label = "Registration Window Height")
    private String fineWinHeightStr = "512";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16",
            label = "Search Window Accuracy in Azimuth Direction")
    private String fineWinAccAzimuth = "16";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16",
            label = "Search Window Accuracy in Range Direction")
    private String fineWinAccRange = "16";

    @Parameter(valueSet = {"32", "64", "128", "256"}, defaultValue = "128",
            label = "Window oversampling factor")
    private String fineWinOversampling = "128";

    @Parameter(description = "The peak cross-correlation threshold", interval = "(0, *)", defaultValue = "0.1",
            label = "Cross-Correlation Threshold")
    private double xCorrThreshold = 0.1;

    @Parameter(description = "The coherence threshold for outlier removal", interval = "(0, 1]", defaultValue = "0.3",
            label = "Coherence Threshold for Outlier Removal")
    private double cohThreshold = 0.3;

    @Parameter(description = "The number of windows per overlap for ESD", interval = "[1, 20]", defaultValue = "10",
            label = "Number of Windows Per Overlap for ESD")
    private int numBlocksPerOverlap = 10;

    @Parameter(label = "ESD Estimator",
            valueSet = {ESD_AVERAGE, ESD_PERIODOGRAM},
            defaultValue = ESD_PERIODOGRAM,
            description = "ESD estimator used for azimuth shift computation")
    private String esdEstimator = ESD_PERIODOGRAM;

    @Parameter(label = "Weight function",
            valueSet = {WEIGHT_FN_NONE, WEIGHT_FN_LINEAR, WEIGHT_FN_QUAD, WEIGHT_FN_INVQUAD},
            defaultValue = WEIGHT_FN_INVQUAD,
            description = "Weight function of the coherence to use for azimuth shift estimation")
    private String weightFunc = WEIGHT_FN_INVQUAD;

    @Parameter(label = "Temporal baseline type",
            valueSet = {INT_NETWORK_IMAGES_BASELINE, INT_NETWORK_DAYS_BASELINE},
            defaultValue = INT_NETWORK_IMAGES_BASELINE,
            description = "Baseline type for building the integration network")
    private String temporalBaselineType = INT_NETWORK_IMAGES_BASELINE;

    @Parameter(label = "Maximum temporal baseline (inclusive)",
            defaultValue = "4",
            description = "Maximum temporal baseline (in days or number of images depending on the Temporal " +
                    "baseline type) between pairs of images to construct the network. Any number < 1 will generate a network " +
                    "with all of the possible pairs.")
    private int maxTemporalBaseline = 4;

    @Parameter(label = "Integration method",
            valueSet = {INT_METHOD_L1, INT_METHOD_L2, INT_METHOD_L1_AND_L2},
            defaultValue = INT_METHOD_L1_AND_L2,
            description = "Method used for integrating the shifts network.")
    private String integrationMethod = INT_METHOD_L1_AND_L2;

    // TODO(David): uncomment for showing in the GUI
//    @Parameter(label = "Optimization criterion",
//            valueSet = {OPT_CRITERION_MIN_ARG, OPT_CRITERION_MAX_REAL},
//            defaultValue = OPT_CRITERION_MAX_REAL,
//            description = "Optimization criterion for azimuth shift estimation")
    private String optObjective = OPT_CRITERION_MAX_REAL;

    // TODO(David): uncomment for showing in the GUI
//    @Parameter(label = "Number of candidate solutions",
//            defaultValue = "9",
//            description = "Number of solutions to consider at each iteration in the optimization process")
    private int noOfCandidateSolutions = 9;

    // TODO(David): uncomment for showing in the GUI
//    @Parameter(label = "Optimization method tolerance",
//            defaultValue = "0.0001",
//            description = "Tolerance used by the optimization method to consider that solution has enough quality")
    private double optTolerance = 0.0001;

    // TODO(David): uncomment for showing in the GUI
//    @Parameter(label = "Maximum number of iterations",
//            description = "Maximum number of iterations for the optimization method",
//            defaultValue = "10000")
    private int optMaxIterations = 10000;

    @Parameter(description = "Do not write target bands", defaultValue = "false",
            label = "Do not write target bands (store range and azimuth offsets in json files).")
    private boolean doNotWriteTargetBands = false;

    @Parameter(description = "Use user supplied range shift", defaultValue = "false",
            label = "Use user supplied range shift (please enter it below)")
    private boolean useSuppliedRangeShift = false;

    @Parameter(description = "The overall range shift", defaultValue = "0.0",
            label = "The overall range shift in pixels")
    private double overallRangeShift = 0.0;

    @Parameter(description = "Use user supplied azimuth shift", defaultValue = "false",
            label = "Use user supplied azimuth shift (please enter it below)")
    private boolean useSuppliedAzimuthShift = false;

    @Parameter(description = "The overall azimuth shift", defaultValue = "0.0",
            label = "The overall azimuth shift in pixels")
    private double overallAzimuthShift = 0.0;

    private int fineWinWidth = 0;
    private int fineWinHeight = 0;
    private int fineWinAccY = 0;
    private int fineWinAccX = 0;
    private int fineWinOvsFactor = 0;

    private boolean isRangeOffsetAvailable = false;
    private boolean isAzimuthOffsetAvailable = false;
    private Double noDataValue = -9999.0;
    private Sentinel1Utils su;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;
    private double azimuthTimeInterval;

    private String swathIndexStr = null;
    private String[] subSwathNames = null;
    private String[] polarizations = null;

    private Map<String, CplxContainer> masterMap = new HashMap<>();  // master complex image map: master images indexed by <date>_<swath>_<polarization>
    private Map<String, CplxContainer> slaveMap = new HashMap<>();  // slave complex images map: slave images indexed by <date>_<swath>_<polarization>
    private Map<String, ProductContainer> targetMap = new HashMap<>();  // image pairs for the target bands: master-slave pairs indexed by masterKey_slave<i>Key (keys are the same in masterMap and slaveMap)
    private Map<String, AzRgOffsets> targetOffsetMap = new HashMap<>();  // range and azimuth offsets for the target bands

    private static final int cohWin = 5; // window size for coherence calculation
    private static final int maxRangeShift = 1;

    private boolean outputESDEstimationToFile = true;

    // ESD
    private boolean usePeriodogram;
    private WeightFunction weightFunction;

    // integration network
    private Map<String, List<CplxContainer>> complexImages = new HashMap<>(); // map with lists of complex images (master is first), indexed by swath-polarization
    private int[][] arcs;


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SpectralDiversityOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
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

        usePeriodogram = esdEstimator.equalsIgnoreCase(ESD_PERIODOGRAM);
        weightFunction = WeightFunction.fromString(weightFunc);

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfSentinel1Product();

            if (doNotWriteTargetBands && useSuppliedRangeShift && useSuppliedAzimuthShift) {
                throw new OperatorException("If you choose not to write the target bands you should let the operator " +
                                                    "estimate range shift, azimuth shift or both.");
            }

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            su.computeReferenceTime();
            subSwath = su.getSubSwath();
            polarizations = su.getPolarizations();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
                swathIndexStr = subSwathNames[0].substring(2);
                azimuthTimeInterval = subSwath[subSwathIndex - 1].azimuthTimeInterval;
            }

            if (useSuppliedRangeShift) {
                isRangeOffsetAvailable = true;
            } else {
                fineWinWidth = Integer.parseInt(fineWinWidthStr);
                fineWinHeight = Integer.parseInt(fineWinHeightStr);
                fineWinAccY = Integer.parseInt(fineWinAccAzimuth);
                fineWinAccX = Integer.parseInt(fineWinAccRange);
                fineWinOvsFactor = Integer.parseInt(fineWinOversampling);

                if (subSwath[subSwathIndex - 1].samplesPerBurst < fineWinWidth) {
                    throw new OperatorException("Registration window width should not be grater than burst width " +
                                                        subSwath[subSwathIndex - 1].samplesPerBurst);
                }

                if (subSwath[subSwathIndex - 1].linesPerBurst < fineWinHeight) {
                    throw new OperatorException("Registration window height should not be grater than burst height " +
                                                        subSwath[subSwathIndex - 1].linesPerBurst);
                }
            }

            if (useSuppliedAzimuthShift) {
                isAzimuthOffsetAvailable = true;
            }

            constructSourceMetadata();
            constructTargetMetadata();
            createTargetProduct();
            // System.out.println("SpectralDiversity.initialize: targetProduct name = " + targetProduct.getName());

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void constructSourceMetadata() throws Exception {

        // master image
        MetadataElement mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;

        metadataMapPut(StackUtils.MST, mstRoot, sourceProduct, masterMap, complexImages);

        // slave images
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot);
        if (slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        if (slaveElem == null) {
            throw new OperatorException("Product must be coregistered (missing Slave_Metadata in Metadata)");
        }
        MetadataElement[] slaveRoot = slaveElem.getElements();
        for (MetadataElement meta : slaveRoot) {
            if (!meta.getName().equals(AbstractMetadata.ORIGINAL_PRODUCT_METADATA))
                metadataMapPut(StackUtils.SLV, meta, sourceProduct, slaveMap, complexImages);
        }
    }

    /**
     * Fills the map with the product's metadata and adds the complex image(s) to a list.
     *
     * @param tag           either  "_mst" or "_slv". For differentiating master and slave bands in the product.
     * @param root          Abstracted_Metadata for tag "_mst" and one of the slave meta data under Slave_Metadata for tag "_slv".
     * @param product       source product.
     * @param map           map of complex images.
     * @param complexImages list of complex images for each polarization-swath combination.
     * @throws Exception
     */
    private void metadataMapPut(final String tag,
                                final MetadataElement root,
                                final Product product,
                                final Map<String, CplxContainer> map,
                                final Map<String, List<CplxContainer>> complexImages) throws Exception {

        // There is really just one subswath, i.e., subSwathNames.length() is 1
        // Polarization can be 1 or more
        // "ABS_ORBIT" is from root so it is expected to be unique for each master and slave product
        // Say #polarizations is N and #slaves is M.
        // We are expecting to have only N elements (one element for each pol) in masterMap and
        // N*M elements in the slaveMap?
        for (String swath : subSwathNames) {
            // Can swath ever be empty??
            final String subswath = swath.isEmpty() ? "" : '_' + swath.toUpperCase();

            for (String polarization : polarizations) {
                final String pol = polarization.isEmpty() ? "" : '_' + polarization.toUpperCase();

                // String mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT) + subswath + pol;
                final String date = OperatorUtils.getAcquisitionDate(root);
                String mapKey = date + subswath + pol;
                // System.out.println("SpectralDiversity.metadataMapPut: tag = " + tag + "; mapKey = " + mapKey);

                // final String date = OperatorUtils.getAcquisitionDate(root);
                final SLCImage meta = new SLCImage(root, product);

                // Set Multilook factor
                meta.setMlAz(1);
                meta.setMlRg(1);

                Band bandReal = null;
                Band bandImag = null;
                for (String bandName : product.getBandNames()) {
                    // When looking for the master band, tag is sufficient
                    // date is needed to pick the correct slave band
                    if (bandName.contains(tag) && bandName.contains(date)) {
                        if (subswath.isEmpty() || bandName.contains(subswath)) {
                            if (pol.isEmpty() || bandName.contains(pol)) {
                                final Band band = product.getBand(bandName);
                                if (BandUtilsDoris.isBandReal(band)) {
                                    bandReal = band;
                                } else if (BandUtilsDoris.isBandImag(band)) {
                                    bandImag = band;
                                }
                            }
                        }
                    }
                }
                if (bandReal != null && bandImag != null) {
                    // System.out.println("SpectralDiversity.metadataMapPut: tag = " + tag + "; mapKey = " + mapKey + " add to map");
                    // map.put(mapKey, new CplxContainer(date, meta, null, bandReal, bandImag));

                    // add to map
                    CplxContainer container = new CplxContainer(date, meta, null, bandReal, bandImag);
                    map.put(mapKey, container);

                    // add to images list
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    List<CplxContainer> imagesList = complexImages.get(imagesKey);
                    if (imagesList == null) {
                        imagesList = new ArrayList<>();
                        complexImages.put(imagesKey, imagesList);
                    }
                    imagesList.add(container);
                }
            }
        }
    }

    private void constructTargetMetadata() {

        for (String keyMaster : masterMap.keySet()) {
            CplxContainer master = masterMap.get(keyMaster);
            for (String keySlave : slaveMap.keySet()) {
                final CplxContainer slave = slaveMap.get(keySlave);
                if (master.polarisation == null || master.polarisation.equals(slave.polarisation)) {
                    final String productName = keyMaster + '_' + keySlave;
                    final ProductContainer product = new ProductContainer(productName, master, slave, true);
                    // System.out.println("SpectralDiversity.constructTargetMetadata: productName = " + productName + " add to map");
                    targetMap.put(productName, product);
                }
            }
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if (!doNotWriteTargetBands) {
            final String[] srcBandNames = sourceProduct.getBandNames();
            for (String srcBandName : srcBandNames) {
                final Band band = sourceProduct.getBand(srcBandName);
                if (band instanceof VirtualBand) {
                    continue;
                }

                Band targetBand;
                if (StackUtils.isMasterBand(srcBandName, sourceProduct)) {
                    targetBand = ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
                } else if (srcBandName.contains("azOffset") || srcBandName.contains("rgOffset") ||
                        srcBandName.contains("derampDemod")) {
                    continue;
                } else {
                    targetBand = new Band(srcBandName,
                                          band.getDataType(),
                                          band.getRasterWidth(),
                                          band.getRasterHeight());

                    targetBand.setUnit(band.getUnit());
                    targetProduct.addBand(targetBand);
                }

                if (targetBand != null && srcBandName.startsWith("q_")) {
                    final String suffix = srcBandName.substring(1);
                    ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand('i' + suffix), targetBand, suffix);
                }
            }
        }

        targetProduct.setPreferredTileSize(512, subSwath[subSwathIndex - 1].linesPerBurst);
    }

    private void updateTargetMetadata(int[][] arcs) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        MetadataElement esdMeasurement = new MetadataElement("ESD Measurement");

        // generate metadata for master-slave pairs
        for (String key : targetMap.keySet()) {
            final CplxContainer master = targetMap.get(key).sourceMaster;
            final CplxContainer slave = targetMap.get(key).sourceSlave;
            final String mstSlvTag = getImagePairTag(master, slave);
            // System.out.println("SpectralDiversity.updateTargetMetadata: mstSlvTag = " + mstSlvTag);
            final MetadataElement mstSlvTagElem = new MetadataElement(mstSlvTag);
            esdMeasurement.addElement(mstSlvTagElem);

            final MetadataElement overallRgAzShiftElem = new MetadataElement("Overall_Range_Azimuth_Shift");
            overallRgAzShiftElem.addElement(new MetadataElement(subSwathNames[0]));
            mstSlvTagElem.addElement(overallRgAzShiftElem);
        }
        absTgt.addElement(esdMeasurement);

        // generate metadata for every pair considering: subswaths, polarizations and arcs in the network
        for (String swath : subSwathNames) {
            for (String polarization : polarizations) {
                String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                List<CplxContainer> imagesList = complexImages.get(imagesKey);

                for (int i = 0; i < arcs.length; i++) {
                    final CplxContainer image1 = imagesList.get(arcs[i][0]);
                    final CplxContainer image2 = imagesList.get(arcs[i][1]);
                    final String imagePairTag = getImagePairTag(image1, image2);
                    // System.out.println("SpectralDiversity.updateTargetMetadata: imagePairTag = " + imagePairTag);

                    final MetadataElement imagePairTagElem = getOrCreateElement(esdMeasurement, imagePairTag);

                    if (!useSuppliedRangeShift) {
                        final MetadataElement rgShiftPerBurstElem = new MetadataElement("Range_Shift_Per_Burst");
                        rgShiftPerBurstElem.addElement(new MetadataElement(subSwathNames[0]));
                        imagePairTagElem.addElement(rgShiftPerBurstElem);

                        final MetadataElement azShiftPerBurstElem = new MetadataElement("Azimuth_Shift_Per_Burst");
                        azShiftPerBurstElem.addElement(new MetadataElement(subSwathNames[0]));
                        imagePairTagElem.addElement(azShiftPerBurstElem);
                    }

                    if (!useSuppliedAzimuthShift) {
                        final MetadataElement azShiftPerOverlapElem = new MetadataElement("Azimuth_Shift_Per_Overlap");
                        azShiftPerOverlapElem.addElement(new MetadataElement(subSwathNames[0]));
                        imagePairTagElem.addElement(azShiftPerOverlapElem);

                        final MetadataElement azShiftPerBlockElem = new MetadataElement("Azimuth_Shift_Per_Block");
                        azShiftPerBlockElem.addElement(new MetadataElement(subSwathNames[0]));
                        imagePairTagElem.addElement(azShiftPerBlockElem);
                    }
                }
            }
        }

        if (useSuppliedRangeShift) {
            for (String key : targetMap.keySet()) {
                final CplxContainer master = targetMap.get(key).sourceMaster;
                final CplxContainer slave = targetMap.get(key).sourceSlave;
                final String mstSlvTag = getImagePairTag(master, slave);
                saveOverallRangeShift(mstSlvTag, overallRangeShift);
            }
        }

        if (useSuppliedAzimuthShift) {
            for (String key : targetMap.keySet()) {
                final CplxContainer master = targetMap.get(key).sourceMaster;
                final CplxContainer slave = targetMap.get(key).sourceSlave;
                final String mstSlvTag = getImagePairTag(master, slave);
                saveOverallAzimuthShift(mstSlvTag, overallAzimuthShift);
            }
        }
    }

    private String getImagePairTag(final CplxContainer image1, final CplxContainer image2) {
        return getImageTag(image1) + "_" + getImageTag(image2);
    }

    private String getImageTag(final CplxContainer image) {
        final String bandName = image.realBand.getName();
        return bandName.substring(bandName.indexOf("i_") + 2);
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        // compute network
        arcs = buildImagesGraph(maxTemporalBaseline);

        SystemUtils.LOG.fine("Arcs\n" + Arrays.deepToString(arcs));

        updateTargetMetadata(arcs);

        if (doNotWriteTargetBands) {  // if we choose not to write the target bands, we must perform the range/azimuth shift estimation here
            SystemUtils.LOG.info("Starting SpectralDiversity processing (target bands won't be written)");

            estimateRangeOffset();

            estimateAzimuthOffset();
        }
    }

    /**
     * Creates a graph of images whose temporal baseline is less or equal to the supplied max temporal baseline.
     * <p>
     * Given `N` images, the maximum number of arcs is: `N (N - 1) / 2`.
     *
     * @param maxTemporalBaseline the maximum amount of days between pairs of images or the number of images apart
     *                            (sorted by date) depending on the selected baseline type. If 0 or a negative value is
     *                            supplied, then all possible pairs are generated. When considering days, a baseline
     *                            that is too low might lead to an empty network, which produces an exception.
     * @return a graph represented as an array of image-index pairs.
     * @throws OperatorException if a connected graph could not be built.
     */
    private int[][] buildImagesGraph(int maxTemporalBaseline) {
        boolean baselineInDays;
        Map<CplxContainer, Integer> imagesOrder = null;

        List<CplxContainer> complexImages = this.complexImages.values().iterator().next();  // get any of the list of images to build the images graph

        // temporal baseline
        if (temporalBaselineType.equalsIgnoreCase(INT_NETWORK_IMAGES_BASELINE)) {
            imagesOrder = mapIndicesOfSortedImages(complexImages);
            baselineInDays = false;
        } else if (temporalBaselineType.equalsIgnoreCase(INT_NETWORK_DAYS_BASELINE)) {
            baselineInDays = true;
        } else {
            throw new OperatorException("Unrecognized temporal baseline type: " + temporalBaselineType);
        }

        // correct baseline threshold if necessary
        if (maxTemporalBaseline < 1) {
            maxTemporalBaseline = Integer.MAX_VALUE;  // keep all possible pairs.
        }

        // generate graph
        int noOfImages = complexImages.size();
        ArrayList<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < noOfImages - 1; i++) {
            for (int j = i + 1; j < noOfImages; j++) {

                CplxContainer image1 = complexImages.get(i);
                CplxContainer image2 = complexImages.get(j);
                int baseline;
                if (baselineInDays) {
                    baseline = computeTemporalBaselineInDays(image1, image2);
                } else {
                    baseline = computeTemporalBaselineInNumberOfImages(image1, image2, imagesOrder);
                }
                if (baseline <= maxTemporalBaseline) {
                    pairs.add(new int[]{i, j});
                }
            }
        }

        // validate graph
        if (pairs.size() < 1) {
            throw new OperatorException("Generated network of images does not contain any pair. " +
                                                "Max temporal baseline provided: " + maxTemporalBaseline);
        }

        if (!GraphUtils.isConnectedGraph(pairs, noOfImages)) {
            throw new OperatorException("Generated graph is not connected. Max temporal baseline provided: " +
                                                maxTemporalBaseline);
        }

        return pairs.toArray(new int[][]{});
    }

    /**
     * Builds a mapping from complex images to indices. Index of an image corresponds to the ordering number of such
     * image in the list of images sorted by date.
     *
     * @param images list of images.
     * @return the map of indices for each image.
     */
    private Map<CplxContainer, Integer> mapIndicesOfSortedImages(List<CplxContainer> images) {
        SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy");

        // read indices and times
        Integer[] indices = new Integer[images.size()];
        long[] times = new long[images.size()];
        for (int i = 0; i < indices.length; i++) {
            CplxContainer image = images.get(i);
            try {
                indices[i] = i;
                times[i] = format.parse(image.date).getTime();
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }

        // sort indices according to date
        Arrays.sort(indices, Comparator.comparingLong(i -> times[i]));

        Map<CplxContainer, Integer> imagesOrder = new HashMap<>();
        for (int i = 0; i < images.size(); i++) {
            imagesOrder.put(images.get(indices[i]), i);
        }

        return imagesOrder;
    }

    /**
     * Computes the temporal baseline, between two images, in number of days.
     *
     * @param image1
     * @param image2
     * @return
     */
    private int computeTemporalBaselineInDays(CplxContainer image1, CplxContainer image2) {
        SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy");

        int baseline = -1;
        try {
            Date date1 = format.parse(image1.date);
            Date date2 = format.parse(image2.date);
            long diff = date2.getTime() - date1.getTime();
            baseline = (int) Math.abs(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return baseline;
    }

    /**
     * Computes the temporal baseline between two images, in number of images.
     *
     * @param image1
     * @param image2
     * @param imageOrder A map of the chronological order number for each image.
     * @return
     */
    private int computeTemporalBaselineInNumberOfImages(CplxContainer image1, CplxContainer image2, Map<CplxContainer,
            Integer> imageOrder) {

        int baseline = -1;
        try {
            int order1 = imageOrder.get(image1);
            int order2 = imageOrder.get(image2);
            baseline = Math.abs(order1 - order2);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return baseline;
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            // offset estimations
            if (!isRangeOffsetAvailable) {
                estimateRangeOffset();
            }
            if (!isAzimuthOffsetAvailable) {
                estimateAzimuthOffset();
            }

            // apply offsets to target tiles
            if (!doNotWriteTargetBands) {
                for (String key : targetMap.keySet()) {
                    final CplxContainer slave = targetMap.get(key).sourceSlave;

                    final AzRgOffsets azRgOffsets = targetOffsetMap.get(key);
                    double rgOffset = useSuppliedRangeShift ? overallRangeShift : azRgOffsets.rgOffset;
                    double azOffset = useSuppliedAzimuthShift ? overallAzimuthShift : azRgOffsets.azOffset;

                    performRangeAzimuthShift(azOffset, rgOffset, slave.realBand, slave.imagBand, targetRectangle,
                                             targetTileMap);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Estimate range and azimuth offset using cross-correlation.
     * <p>
     * Steps:
     * <ol>
     * <li>estimate range shifts for each arc (including all polarizations) using cross-correlation,</li>
     * <li>integrate range shifts for each image using the network, and</li>
     * <li>save shifts and network metadata.</li>
     * </ol>
     */
    private synchronized void estimateRangeOffset() {

        if (isRangeOffsetAvailable) {
            return;
        }

        try {
            final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);

            // compute range shift for every subswath
            for (String swath : subSwathNames) {
                JSONObject rangeShifts = new JSONObject();

                // 1. estimate shift for each arc
                int noOfPolarizations = polarizations.length;
                List<int[]> arcsList = new ArrayList<>(arcs.length * noOfPolarizations);
                List<ShiftData> arcShiftsList = new ArrayList<>(arcs.length * noOfPolarizations);
                List<String> arcPolarizationsList = new ArrayList<>(arcs.length * noOfPolarizations);

                for (String polarization : polarizations) {
                    // get list of complex images
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    List<CplxContainer> complexImages = this.complexImages.get(imagesKey);
                    SystemUtils.LOG.fine("Estimating range offset for: " + imagesKey);

                    // estimate range shift image pair
                    status.beginTask("Range shift: Cross-correlation for image pairs (" + imagesKey + ")...", arcs.length);
                    for (int arcIndex = 0; arcIndex < arcs.length; arcIndex++) {  // for each pair
                        // estimate range shift for each pair using cross-correlation
                        CplxContainer image1 = complexImages.get(arcs[arcIndex][0]);
                        CplxContainer image2 = complexImages.get(arcs[arcIndex][1]);
                        String pairKey = getCanonicalId(image1) + "_" + getCanonicalId(image2);
                        SystemUtils.LOG.fine("Estimating range shift for pair " + pairKey +
                                                     "\t arc:" + arcs[arcIndex][0] + " -> " + arcs[arcIndex][1]);
                        ShiftData rangeShift = crossCorrelatePair(image1, image2);

                        // save network data
                        arcsList.add(arcs[arcIndex]);
                        arcShiftsList.add(rangeShift);
                        arcPolarizationsList.add(polarization);

                        status.worked(1);
                    }
                    status.done();
                }

                // 2. integration of arcs
                int[][] extendedArcs = arcsList.toArray(new int[0][]);
                double[] relativeRangeShifts = new double[extendedArcs.length];
                double[] rangeWeights = new double[extendedArcs.length];

                // get range shifts and weights
                for (int i = 0; i < relativeRangeShifts.length; i++) {
                    ShiftData rangeShift = arcShiftsList.get(i);
                    relativeRangeShifts[i] = rangeShift.shift;
                    rangeWeights[i] = rangeShift.weight;
                }

                double[] imageShifts = integrateImageShifts(extendedArcs, relativeRangeShifts, rangeWeights);

                // 3. save shifts
                for (String polarization : polarizations) {
                    // get list of complex images
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    List<CplxContainer> complexImages = this.complexImages.get(imagesKey);
                    SystemUtils.LOG.fine("Saving range offset for: " + imagesKey);

                    CplxContainer masterImage = complexImages.get(0);
                    for (int i = 1; i < imageShifts.length; i++) {
                        CplxContainer slaveImage = complexImages.get(i);

                        String pairKey = getCanonicalId(masterImage) + "_" + getCanonicalId(slaveImage);
                        if (targetOffsetMap.get(pairKey) == null) {
                            targetOffsetMap.put(pairKey, new AzRgOffsets(0.0, imageShifts[i]));
                        } else {
                            targetOffsetMap.get(pairKey).setRgOffset(imageShifts[i]);
                        }

                        // Although shifts are computed considering all pairs in the network, tag names are kept with
                        // the same old structure (master-slave) for backward compatibility with ESD generated metadata
                        CplxContainer master = complexImages.get(0);
                        CplxContainer slave = complexImages.get(i);
                        String mstSlvTag = getImagePairTag(master, slave);
                        saveOverallRangeShift(mstSlvTag, imageShifts[i]);

                        // add to json object for writing to a file
                        rangeShifts.put(mstSlvTag, imageShifts[i]);
                    }
                }

                // save shifts to file
                saveShiftsToFile(rangeShifts, swath + "_range_shifts.json");

                // save integration network
                saveIntegrationNetwork(extendedArcs, relativeRangeShifts, rangeWeights, complexImages, imageShifts,
                                       arcPolarizationsList, false);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateRangeOffset", e);
        }

        isRangeOffsetAvailable = true;
    }

    private void estimateAzRgOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                     final int burstIndex, final double[] offset) {

        final int burstHeight = subSwath[subSwathIndex - 1].linesPerBurst;
        final int burstWidth = subSwath[subSwathIndex - 1].samplesPerBurst;
        final int x0 = burstWidth / 2;
        final int y0 = burstHeight / 2 + burstIndex * burstHeight;
        final PixelPos mGCP = new PixelPos(x0, y0);
        final PixelPos sGCP = new PixelPos(x0, y0);

        getFineOffsets(mBandI, mBandQ, sBandI, sBandQ, mGCP, sGCP, offset);
    }

    private void getFineOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos, final double[] offset) {

        try {
            ComplexDoubleMatrix mI = getComplexDoubleMatrix(
                    mBandI, mBandQ, mGCPPixelPos, fineWinWidth, fineWinHeight);

            ComplexDoubleMatrix sI = getComplexDoubleMatrix(
                    sBandI, sBandQ, sGCPPixelPos, fineWinWidth, fineWinHeight);

            final double[] fineOffset = {0, 0};

            final double coherence = CoregistrationUtils.crossCorrelateFFT(
                    fineOffset, mI, sI, fineWinOvsFactor, fineWinAccY, fineWinAccX);

            if (coherence < xCorrThreshold) {
                offset[0] = noDataValue;
                offset[1] = noDataValue;
            } else {
                offset[0] = -fineOffset[0];
                offset[1] = -fineOffset[1];
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getFineOffsets ", e);
        }
    }

    private ComplexDoubleMatrix getComplexDoubleMatrix(
            final Band band1, final Band band2, final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {

        Rectangle rectangle = defineRectangleMask(pixelPos, fineWinWidth, fineWinHeight);
        Tile tileReal = getSourceTile(band1, rectangle);
        Tile tileImag = getSourceTile(band2, rectangle);
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private static Rectangle defineRectangleMask(final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {
        int l0 = (int) (pixelPos.y - fineWinHeight / 2);
        int lN = (int) (pixelPos.y + fineWinHeight / 2 - 1);
        int p0 = (int) (pixelPos.x - fineWinWidth / 2);
        int pN = (int) (pixelPos.x + fineWinWidth / 2 - 1);
        return new Rectangle(p0, l0, pN - p0 + 1, lN - l0 + 1);
    }

    /**
     * Estimate azimuth offset using Network ESD approach.
     * <p>
     * Steps:
     * <ol>
     * <li>estimate azimuth shifts for each block stack (including all polarizations) using ESD,</li>
     * <li>integrate azimuth shifts for each image using the network, and</li>
     * <li>save shifts and network metadata.</li>
     * </ol>
     */
    private synchronized void estimateAzimuthOffset() {

        if (isAzimuthOffsetAvailable) {
            return;
        }

        try {
            final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);

            final int numOverlaps = subSwath[subSwathIndex - 1].numOfBursts - 1;

            // shifts for each block, shape: (overlaps, blocks, arcs)
            ShiftData[][][] shiftAllBlocks = new ShiftData[numOverlaps][numBlocksPerOverlap][arcs.length];

            // compute azimuth shift for every sub-swath
            for (String swath : subSwathNames) {
                JSONObject azimuthShifts = new JSONObject();

                // 1. estimate azimuth shifts for each block
                int noOfPolarizations = polarizations.length;
                List<int[]> arcsList = new ArrayList<>(arcs.length * noOfPolarizations);
                List<ShiftData> arcShiftsList = new ArrayList<>(arcs.length * noOfPolarizations);
                List<String> arcPolarizationsList = new ArrayList<>(arcs.length * noOfPolarizations);

                // compute azimuth shift for every polarization
                for (String polarization : polarizations) {
                    double[] totalOffsets = new double[arcs.length];
                    double[] totalWeights = new double[arcs.length];

                    // get list of complex images
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    List<CplxContainer> complexImages = this.complexImages.get(imagesKey);
                    SystemUtils.LOG.fine("Estimating azimuth offset for: " + imagesKey);

                    // estimate shift for each overlap
                    status.beginTask("Azimuth shift: ESD for overlap blocks (" + imagesKey + ")...", numOverlaps * numBlocksPerOverlap);
                    for (int i = 0; i < numOverlaps; i++) {
                        final ThreadExecutor executor = new ThreadExecutor();
                        SystemUtils.LOG.info("Estimating azimuth offset for blocks in overlap: " +
                                                     (i + 1) + "/" + numOverlaps);
                        final Rectangle overlapInBurstOneRectangle = new Rectangle();
                        final Rectangle overlapInBurstTwoRectangle = new Rectangle();

                        OverlapUtils.getOverlappedRectangles(i, overlapInBurstOneRectangle, overlapInBurstTwoRectangle,
                                                             subSwath[subSwathIndex - 1]);

                        final int w = overlapInBurstOneRectangle.width / numBlocksPerOverlap;  // block width
                        final int h = overlapInBurstOneRectangle.height;
                        final int x0BurstOne = overlapInBurstOneRectangle.x;
                        final int y0BurstOne = overlapInBurstOneRectangle.y;
                        final int y0BurstTwo = overlapInBurstTwoRectangle.y;
                        final int overlapIndex = i;

                        final double[] spectralSeparation = computeSpectralSeparation(i);
                        final double searchBoundary = getSearchSpaceBoundary(spectralSeparation);

                        // estimate shift for each block stack
                        for (int j = 0; j < numBlocksPerOverlap; j++) {
                            final int x0 = x0BurstOne + j * w;
                            final int blockIndex = j;
                            final Rectangle blockRectangle1 = new Rectangle(x0, y0BurstOne, w, h);
                            final Rectangle blockRectangle2 = new Rectangle(x0, y0BurstTwo, w, h);

                            // apply ESD for pairs in this block stack
                            final ThreadRunnable worker = new ThreadRunnable() {
                                @Override
                                public void process() {
                                    ShiftData[] azimuthShiftsPerBlock = applyESDToBlockStack(
                                            swath,
                                            polarization,
                                            overlapIndex,
                                            blockIndex,
                                            blockRectangle1,
                                            blockRectangle2,
                                            spectralSeparation,
                                            searchBoundary,
                                            usePeriodogram);
                                    synchronized (shiftAllBlocks) {
                                        shiftAllBlocks[overlapIndex][blockIndex] = azimuthShiftsPerBlock;
                                    }
                                }
                            };
                            executor.execute(worker);
                            status.worked(1);
                        }
                        executor.complete();
                    }
                    status.done();

                    // compute average offset for images of this polarization
                    double[] azOffsets = new double[arcs.length];
                    for (int arcIndex = 0; arcIndex < arcs.length; arcIndex++) {
                        CplxContainer image1 = complexImages.get(arcs[arcIndex][0]);
                        CplxContainer image2 = complexImages.get(arcs[arcIndex][1]);
                        final String imagePairTag = getImagePairTag(image1, image2);

                        // all block shifts for this overlap
                        List<ShiftData> azShiftArray = new ArrayList<>();
                        final double[][] shiftLUT = new double[numOverlaps][numBlocksPerOverlap];
                        for (int i = 0; i < numOverlaps; i++) {
                            for (int j = 0; j < numBlocksPerOverlap; j++) {
                                azShiftArray.add(shiftAllBlocks[i][j][arcIndex]);
                                shiftLUT[i][j] = shiftAllBlocks[i][j][arcIndex].shift;
                            }
                        }

                        // Find average shift per block, using average block weights
                        final double[] averagedAzShiftArray = new double[numOverlaps];
                        final double[] averagedWeight = new double[numOverlaps];
                        final double[] overlapSearchBoundary = new double[numOverlaps];
                        for (int i = 0; i < numOverlaps; i++) {
                            double sumAzOffset = 0.0;
                            double sumWeight = 0.0;
                            double blockSearchBoundary = 0.0;

                            // for each block in this overlap
                            for (int j = 0; j < numBlocksPerOverlap; j++) {
                                ShiftData shiftData = shiftAllBlocks[i][j][arcIndex];
                                sumAzOffset += shiftData.shift * shiftData.weight;
                                sumWeight += shiftData.weight;
                                blockSearchBoundary = shiftData.searchBoundary;

                            }
                            // average for this overlap
                            if (sumWeight != 0) {
                                averagedAzShiftArray[i] = sumAzOffset / sumWeight;
                            } else {
                                averagedAzShiftArray[i] = 0.0;
                                SystemUtils.LOG.warning("NetworkESD (azimuth shift): arc = " + imagePairTag +
                                                                " overlap area = " + i + ", weight for this overlap is 0.0");
                            }
                            averagedWeight[i] = sumWeight / numBlocksPerOverlap;
                            overlapSearchBoundary[i] = blockSearchBoundary;

                            // sum to compute overall average shift
                            totalOffsets[arcIndex] += sumAzOffset;
                            totalWeights[arcIndex] += sumWeight;

                            SystemUtils.LOG.fine("NetworkESD (azimuth shift): arc = " + imagePairTag + " overlap area = " + i +
                                                         ", azimuth offset = " + averagedAzShiftArray[i]);
                        }

                        // overall average shift
                        if (totalWeights[arcIndex] != 0) {
                            azOffsets[arcIndex] = -totalOffsets[arcIndex] / totalWeights[arcIndex];
                            SystemUtils.LOG.fine("NetworkESD (azimuth shift): arc = " + imagePairTag +
                                                         ", overall azimuth shift for this arc = " +
                                                         azOffsets[arcIndex]);
                        } else {
                            azOffsets[arcIndex] = 0.0;
                            SystemUtils.LOG.warning("NetworkESD (azimuth shift): arc = " + imagePairTag +
                                                            ", weight for this band is 0.0, setting azimuth offset " +
                                                            "to 0.0");
                        }


                        // save overlap metadata
                        saveAzimuthShiftPerOverlap(imagePairTag, averagedAzShiftArray, averagedWeight, overlapSearchBoundary);

                        // save block metadata
                        saveAzimuthShiftPerBlock(imagePairTag, azShiftArray);

                        // save to file
                        if (outputESDEstimationToFile) {
                            final String fileName = imagePairTag + "_azimuth_shift.txt";
                            outputESDEstimationToFile(fileName, shiftLUT, azOffsets[arcIndex]);
                        }

                        // save network data
                        arcsList.add(arcs[arcIndex]);
                        arcShiftsList.add(new ShiftData(-1, -1, azOffsets[arcIndex], totalWeights[arcIndex], -1));
                        arcPolarizationsList.add(polarization);
                    }
                }

                // 2. integration of arcs
                int[][] extendedArcs = arcsList.toArray(new int[0][]);
                double[] relativeAzimuthShifts = new double[extendedArcs.length];
                double[] azimuthWeights = new double[extendedArcs.length];

                // get azimuth shifts and weights
                for (int i = 0; i < relativeAzimuthShifts.length; i++) {
                    ShiftData azimuthShift = arcShiftsList.get(i);
                    relativeAzimuthShifts[i] = azimuthShift.shift;
                    azimuthWeights[i] = azimuthShift.weight;
                }

                double[] imageShifts = integrateImageShifts(extendedArcs, relativeAzimuthShifts, azimuthWeights);

                // 3. save shifts
                for (String polarization : polarizations) {
                    // get list of complex images
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    List<CplxContainer> complexImages = this.complexImages.get(imagesKey);
                    SystemUtils.LOG.fine("Saving azimuth offset for: " + imagesKey);

                    CplxContainer masterImage = complexImages.get(0);
                    for (int i = 1; i < imageShifts.length; i++) {
                        CplxContainer slaveImage = complexImages.get(i);

                        String pairKey = getCanonicalId(masterImage) + "_" + getCanonicalId(slaveImage);
                        if (targetOffsetMap.get(pairKey) == null) {
                            targetOffsetMap.put(pairKey, new AzRgOffsets(imageShifts[i], 0.0));
                        } else {
                            targetOffsetMap.get(pairKey).setAzOffset(imageShifts[i]);
                        }

                        // Although shifts are computed considering all pairs in the network, tag names are kept with
                        // the same old structure (master-slave) for backward compatibility with ESD generated metadata
                        CplxContainer master = complexImages.get(0);
                        CplxContainer slave = complexImages.get(i);
                        String mstSlvTag = getImagePairTag(master, slave);
                        saveOverallAzimuthShift(mstSlvTag, imageShifts[i]);

                        // add to json object for writing to a file
                        azimuthShifts.put(mstSlvTag, imageShifts[i]);
                    }
                }

                // save shifts to file
                saveShiftsToFile(azimuthShifts, swath + "_azimuth_shifts.json");

                // save integration network
                saveIntegrationNetwork(extendedArcs, relativeAzimuthShifts, azimuthWeights, complexImages, imageShifts,
                                       arcPolarizationsList, true);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateAzimuthOffset", e);
        }

        isAzimuthOffsetAvailable = true;
    }

    /**
     * Writes a json file in the SNAP reports directory.
     *
     * @param shifts   json object to save.
     * @param fileName file name.
     */
    private void saveShiftsToFile(JSONObject shifts, String fileName) {

        File file = new File(ResourceUtils.getReportFolder(), fileName);

        try (FileWriter writer = new FileWriter(new File(ResourceUtils.getReportFolder(), fileName))) {
            writer.write(shifts.toJSONString());
            writer.flush();

            SystemUtils.LOG.info("Shifts written to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            SystemUtils.LOG.warning("Error trying to write shifts to file: " + file.getAbsolutePath());
        }
    }

    /**
     * Estimate azimuth offset of a block in the second image with respect to the same block in the first one using the
     * ESD approach. This method is applied to all the pairs of images (blocks) according to the arcs in the integration
     * network.
     *
     * @param swath              swath name.
     * @param polarization       polarization name.
     * @param overlapIndex       index of this overlap.
     * @param blockIndex         index of this block in the overlap.
     * @param blockRectangle1    rectangle for this block in the first burst.
     * @param blockRectangle2    rectangle for this block in the second burst.
     * @param spectralSeparation spectral separation.
     * @param searchBoundary     boundaries of the search space for the azimuth shift estimation.
     * @param usePeriodogram     flag to indicate the ESD estimation method: Periodogram (true) | Average (false).
     * @return an array of ShiftData objects describing the offsets for all the image (block) pairs
     */
    private ShiftData[] applyESDToBlockStack(String swath, String polarization, int overlapIndex, int blockIndex, Rectangle blockRectangle1, Rectangle blockRectangle2, double[] spectralSeparation, double searchBoundary, boolean usePeriodogram) {
        ShiftData[] azimuthShifts = new ShiftData[arcs.length];
        final int w = blockRectangle1.width;
        final int h = blockRectangle1.height;

        try {
            for (int i = 0; i < arcs.length; i++) {  // for each pair
                final int arcIndex = i;

                checkForCancellation();

                // get images and bands for this arc
                String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                List<CplxContainer> complexImages = this.complexImages.get(imagesKey);
                CplxContainer image1 = complexImages.get(arcs[i][0]);
                CplxContainer image2 = complexImages.get(arcs[i][1]);

                final Band mBandI = image1.realBand;
                final Band mBandQ = image1.imagBand;
                final Band sBandI = image2.realBand;
                final Band sBandQ = image2.imagBand;

                try {

                    // Chop spectralSeparation to fit the block
                    double[] blockSpectralSeparation = chopSpectralSeparation(blockIndex,
                                                                              w, h,
                                                                              spectralSeparation);

                    // Transform 2D coherence to 1D coherence only for the block
                    final double[] blockCoherence = ravel(computeCoherence(blockRectangle1,
                                                                           mBandI, mBandQ, sBandI, sBandQ,
                                                                           cohWin));

                    // Transform coherence into weights
                    final double[] blockWeight = getBlockWeight(blockCoherence, weightFunction);
                    double avgBlockWeight = getAverageBlockWeight(blockWeight);

                    // Calculate ESD phase
                    final double[] esdPhase = estimateESDPhase(mBandI, mBandQ, sBandI, sBandQ,
                                                               blockRectangle2, blockRectangle1);

                    // Estimate the shift
                    double azShift;
                    if (usePeriodogram) {
                        // Apply the azimuth shift retrieval estimator
                        azShift = estimateAzimuthShiftWithPeriodogram(esdPhase,
                                                                      blockWeight,
                                                                      blockSpectralSeparation,
                                                                      searchBoundary);
                    } else {
                        // Apply an estimator based on the average esd
                        azShift = estimateAzimuthShiftWithAverage(esdPhase,
                                                                  blockWeight,
                                                                  blockSpectralSeparation);
                    }

                    // Save shift to azShiftArray
                    ShiftData shiftData =
                            new ShiftData(overlapIndex, blockIndex, azShift, avgBlockWeight, searchBoundary);
                    azimuthShifts[arcIndex] = shiftData;

                } catch (Throwable e) {
                    OperatorUtils.catchOperatorException("estimateOffset", e);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateAzimuthOffset (applyESDToBlockStack)", e);
        }

        return azimuthShifts;
    }

    /**
     * Gets the canonical id from a complex image container.
     *
     * @param container
     * @return
     */
    private String getCanonicalId(CplxContainer container) {
        return container.date + "_" + container.subswath.toUpperCase() + "_" + container.polarisation.toUpperCase();
    }

    /**
     * Computes the azimuth or range shifts for each image according to the graph, relative azimuth shifts between
     * images and weights.
     *
     * @param arcs    description of the graph of images.
     * @param shifts  contains the relative (azimuth or range) shifts per arc.
     * @param weights contains the shift weights per arc.
     * @return an array of integrated shifts per image.
     */
    public double[] integrateImageShifts(int[][] arcs, double[] shifts, double[] weights) {

        // integrate
        double[] integratedShifts;

        try {
            if (integrationMethod.equalsIgnoreCase(INT_METHOD_L1)) {
                integratedShifts = ArcDataIntegration.integrateArcsL1(arcs, shifts, weights);

            } else if (integrationMethod.equalsIgnoreCase(INT_METHOD_L2)) {
                integratedShifts = ArcDataIntegration.integrateArcsL2(arcs, shifts, weights);

            } else if (integrationMethod.equalsIgnoreCase(INT_METHOD_L1_AND_L2)) {
                integratedShifts = ArcDataIntegration.integrateArcsL1AndL2(arcs, shifts, weights);

            } else {
                throw new OperatorException("Unrecognized integration method: " + integrationMethod);
            }
        } catch (Throwable e) {
            throw new OperatorException("Integration problem using method: " + integrationMethod, e);
        }

        return integratedShifts;
    }


    /**
     * Estimate range offset of the second image with respect to the first one using the average cross-correlation.
     *
     * @param image1 first image used as reference.
     * @param image2 second image.
     * @return range shift and weights for each pair of images.
     */
    private ShiftData crossCorrelatePair(CplxContainer image1, CplxContainer image2) {

        double rgOffset = Double.NaN;

        final int numBursts = subSwath[subSwathIndex - 1].numOfBursts;

        // SystemUtils.LOG.info("crossCorrelatePair numBursts = " + numBursts);

        final String imagePairTag = getImagePairTag(image1, image2);

        try {
            final List<Double> azOffsetArray = new ArrayList<>(numBursts);
            final List<Double> rgOffsetArray = new ArrayList<>(numBursts);
            final List<Integer> burstIndexArray = new ArrayList<>(numBursts);

            final ThreadExecutor executor = new ThreadExecutor();
            for (int i = 0; i < numBursts; i++) {
                checkForCancellation();
                final int burstIndex = i;

                final ThreadRunnable worker = new ThreadRunnable() {
                    @Override
                    public void process() {
                        try {
                            final double[] offset = new double[2]; // az/rg offset

                            estimateAzRgOffsets(image1.realBand, image1.imagBand, image2.realBand, image2.imagBand,
                                                burstIndex, offset);

                            synchronized (azOffsetArray) {
                                azOffsetArray.add(offset[0]);
                                rgOffsetArray.add(offset[1]);
                                burstIndexArray.add(burstIndex);
                            }
                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException("estimateOffset", e);
                        }
                    }
                };
                executor.execute(worker);
            }
            executor.complete();

            double sumRgOffset = 0.0;
            int count = 0;
            for (int i = 0; i < azOffsetArray.size(); i++) {
                final double azShift = azOffsetArray.get(i);
                final double rgShift = rgOffsetArray.get(i);

                SystemUtils.LOG.fine("SpectralDiversity (range shift): burst = " + burstIndexArray.get(i) +
                                             ", range offset = " + rgShift + ", azimuth offset = " + azShift);

                if (noDataValue.equals(azShift) || noDataValue.equals(rgShift)) {
                    continue;
                }

                if (Math.abs(rgShift) > maxRangeShift) {
                    continue;
                }

                sumRgOffset += rgShift;
                count++;
            }

            if (count > 0) {
                rgOffset = sumRgOffset / (double) count;
            } else {
                rgOffset = 0.0;
                SystemUtils.LOG.warning("SpectralDiversity (range shift): Cross-correlation failed for all bursts, " +
                                                "set range shift to 0.0");
            }

            // save metadata
            saveRangeShiftPerBurst(imagePairTag, rgOffsetArray, burstIndexArray);

            saveAzimuthShiftPerBurst(imagePairTag, azOffsetArray, burstIndexArray);

            SystemUtils.LOG.fine("SpectralDiversity (range shift): Overall range shift = " + rgOffset);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateRangeOffset (crossCorrelatePair)", e);
        }

        // validate and return azimuth shift
        if (Double.isNaN(rgOffset)) {
            rgOffset = 0.0;
            SystemUtils.LOG.warning("SpectralDiversity (range shift): arc = " + imagePairTag +
                                            ", range offset is NaN, setting to 0.0");
        }
        return new ShiftData(-1, -1, rgOffset, 1, -1);
    }

    private double[] computeSpectralSeparation(int overlapIndex) {
        final int numOverlappedLines = computeBurstOverlapSize(overlapIndex);
        int burstIndex = overlapIndex;  // Approximation: consider only the doppler rate of the first burst per overlap

        final double tCycle = (subSwath[subSwathIndex - 1].linesPerBurst - numOverlappedLines) * azimuthTimeInterval;

        double[] maxSpectralSeparation = new double[subSwath[subSwathIndex - 1].samplesPerBurst];
        for (int p = 0; p < subSwath[subSwathIndex - 1].samplesPerBurst; p++) {
            maxSpectralSeparation[p] = subSwath[subSwathIndex - 1].dopplerRate[burstIndex][p] * tCycle;
        }
        return maxSpectralSeparation;
    }

    /**
     * Computes the boundaries of the search space for the azimuth shift estimation. The search space is
     * <code>[-s, s]</code> where <code>s</code> is half one ambiguity band.
     *
     * @param spectralSeparation
     * @return Half of the ambiguity band.
     */
    private double getSearchSpaceBoundary(double[] spectralSeparation) {
        double maxSpectralSeparation = 0.0;

        for (int i = 0; i < spectralSeparation.length; i++) {
            maxSpectralSeparation = FastMath.max(spectralSeparation[i], maxSpectralSeparation);
        }

        // Calculate boundary of the search space
        return 0.5 / (azimuthTimeInterval * maxSpectralSeparation);
    }

    private double[] chopSpectralSeparation(int blockIndex, int blockWidth, int blockHeight,
                                            double[] spectralSeparation) {
        double[] choppedSpectralSeparation = new double[blockWidth * blockHeight];

        for (int i = 0; i < choppedSpectralSeparation.length; i++) {
            final int r = i / blockWidth;
            final int c = blockIndex * blockWidth + i - r * blockWidth;
            choppedSpectralSeparation[i] = spectralSeparation[c];
        }

        return choppedSpectralSeparation;
    }

    private static double[] ravel(double[][] matrix) {
        int height = matrix.length;
        int width = matrix[0].length;
        final double[] vector = new double[height * width];

        for (int i = 0; i < vector.length; i++) {
            final int r = i / width;
            final int c = i - r * width;
            vector[i] = matrix[r][c];
        }

        return vector;
    }

    private static double[] getBlockCoherence(final int blockIndex, final int blockWidth, final int blockHeight,
                                              final double[][] coherence) {

        final double[] blockCoherence = new double[blockWidth * blockHeight];

        for (int i = 0; i < blockCoherence.length; i++) {
            final int r = i / blockWidth;
            final int c = blockIndex * blockWidth + i - r * blockWidth;
            blockCoherence[i] = coherence[r][c];
        }
        return blockCoherence;
    }

    private double[] getBlockWeight(double[] coherence, WeightFunction weightFunction) {
        double[] weight = new double[coherence.length];

        for (int i = 0; i < coherence.length; i++) {
            weight[i] = weightFunction.getWeight(coherence[i], cohThreshold);
        }
        return weight;
    }

    private double getAverageBlockWeight(double[] blockWeight) {
        double sum = 0;
        for (int i = 0; i < blockWeight.length; i++) {
            sum += blockWeight[i];
        }
        return sum / blockWeight.length;
    }

    private void saveOverallRangeShift(final String mstSlvPairTag, final double rangeShift) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement OverallRgAzShiftElem = mstSlvPairElem.getElement("Overall_Range_Azimuth_Shift");
        final MetadataElement swathElem = OverallRgAzShiftElem.getElement(subSwathNames[0]);

        final MetadataAttribute rangeShiftAttr = new MetadataAttribute("rangeShift", ProductData.TYPE_FLOAT32);
        rangeShiftAttr.setUnit("pixel");
        swathElem.addAttribute(rangeShiftAttr);
        swathElem.setAttributeDouble("rangeShift", rangeShift);
    }

    private void saveOverallAzimuthShift(final String mstSlvPairTag, final double azimuthShift) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement OverallRgAzShiftElem = mstSlvPairElem.getElement("Overall_Range_Azimuth_Shift");
        final MetadataElement swathElem = OverallRgAzShiftElem.getElement(subSwathNames[0]);

        final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
        azimuthShiftAttr.setUnit("pixel");
        swathElem.addAttribute(azimuthShiftAttr);
        swathElem.setAttributeDouble("azimuthShift", azimuthShift);
    }

    private void saveRangeShiftPerBurst(
            final String mstSlvPairTag, final List<Double> rangeShiftArray, final List<Integer> burstIndexArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement RangeShiftPerBurstElem = mstSlvPairElem.getElement("Range_Shift_Per_Burst");
        final MetadataElement swathElem = RangeShiftPerBurstElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", rangeShiftArray.size());

        for (int i = 0; i < rangeShiftArray.size(); i++) {
            final MetadataElement burstListElem = new MetadataElement("RangeShiftList." + i);
            final MetadataAttribute rangeShiftAttr = new MetadataAttribute("rangeShift", ProductData.TYPE_FLOAT32);
            rangeShiftAttr.setUnit("pixel");
            burstListElem.addAttribute(rangeShiftAttr);
            burstListElem.setAttributeDouble("rangeShift", rangeShiftArray.get(i));
            burstListElem.addAttribute(new MetadataAttribute("burstIndex", ProductData.TYPE_INT16));
            burstListElem.setAttributeInt("burstIndex", burstIndexArray.get(i));
            swathElem.addElement(burstListElem);
        }
    }

    private void saveAzimuthShiftPerBurst(
            final String mstSlvPairTag, final List<Double> rangeShiftArray, final List<Integer> burstIndexArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement AzimuthShiftPerBurstElem = mstSlvPairElem.getElement("Azimuth_Shift_Per_Burst");
        final MetadataElement swathElem = AzimuthShiftPerBurstElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", rangeShiftArray.size());

        for (int i = 0; i < rangeShiftArray.size(); i++) {
            final MetadataElement burstListElem = new MetadataElement("AzimuthShiftList." + i);
            final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
            azimuthShiftAttr.setUnit("pixel");
            burstListElem.addAttribute(azimuthShiftAttr);
            burstListElem.setAttributeDouble("azimuthShift", rangeShiftArray.get(i));
            burstListElem.addAttribute(new MetadataAttribute("burstIndex", ProductData.TYPE_INT16));
            burstListElem.setAttributeInt("burstIndex", burstIndexArray.get(i));
            swathElem.addElement(burstListElem);
        }
    }

    private void saveAzimuthShiftPerOverlap(final String imagePairTag, final double[] averagedAzShiftArray,
                                            final double[] averagedWeightArray, final double[] overlapSearchBoundary) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement esdMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = esdMeasurement.getElement(imagePairTag);
        final MetadataElement azShiftPerOverlapElem = mstSlvPairElem.getElement("Azimuth_Shift_Per_Overlap");
        final MetadataElement swathElem = azShiftPerOverlapElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", averagedAzShiftArray.length);

        for (int i = 0; i < averagedAzShiftArray.length; i++) {
            final MetadataElement overlapListElem = new MetadataElement("AzimuthShiftList." + i);
            final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
            azimuthShiftAttr.setUnit("pixel");
            overlapListElem.addAttribute(azimuthShiftAttr);
            overlapListElem.setAttributeDouble("azimuthShift", averagedAzShiftArray[i]);
            overlapListElem.addAttribute(new MetadataAttribute("overlapIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("overlapIndex", i);
            final MetadataAttribute weightAttr = new MetadataAttribute("weight", ProductData.TYPE_FLOAT32);
            overlapListElem.addAttribute(weightAttr);
            overlapListElem.setAttributeDouble("weight", averagedWeightArray[i]);
            final MetadataAttribute searchBoundaryAttr = new MetadataAttribute("searchBoundary", ProductData.TYPE_FLOAT32);
            overlapListElem.addAttribute(searchBoundaryAttr);
            overlapListElem.setAttributeDouble("searchBoundary", overlapSearchBoundary[i]);
            swathElem.addElement(overlapListElem);
        }
    }

    private void saveAzimuthShiftPerBlock(final String mstSlvPairTag, final List<ShiftData> azShiftArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement AzShiftPerBlockElem = mstSlvPairElem.getElement("Azimuth_Shift_Per_Block");
        final MetadataElement swathElem = AzShiftPerBlockElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", azShiftArray.size());

        for (int i = 0; i < azShiftArray.size(); i++) {
            final MetadataElement overlapListElem = new MetadataElement("AzimuthShiftList." + i);
            final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
            azimuthShiftAttr.setUnit("pixel");
            overlapListElem.addAttribute(azimuthShiftAttr);
            overlapListElem.setAttributeDouble("azimuthShift", azShiftArray.get(i).shift);
            overlapListElem.addAttribute(new MetadataAttribute("overlapIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("overlapIndex", azShiftArray.get(i).overlapIndex);
            overlapListElem.addAttribute(new MetadataAttribute("blockIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("blockIndex", azShiftArray.get(i).blockIndex);
            overlapListElem.addAttribute(new MetadataAttribute("weight", ProductData.TYPE_FLOAT32));
            overlapListElem.setAttributeDouble("weight", azShiftArray.get(i).weight);
            swathElem.addElement(overlapListElem);
        }
    }

    /**
     * Saves the integration network to the metadata.
     *
     * @param arcs             arcs in the network.
     * @param relativeShifts   shift per pair.
     * @param weights          weight of the arc.
     * @param complexImages    list of complex images for each polarization.
     * @param integratedShifts results of the integration process.
     */
    private void saveIntegrationNetwork(int[][] arcs, double[] relativeShifts, double[] weights,
                                        Map<String, List<CplxContainer>> complexImages, double[] integratedShifts,
                                        List<String> arcPolarizations, boolean isAzimuthShift) {

        String shiftType = isAzimuthShift ? "azimuthShift" : "rangeShift";
        String weightType = isAzimuthShift ? "azimuthWeight" : "rangeWeight";
        String shiftDescription = isAzimuthShift ?
                "Computed using ESD" :
                "Computed using Cross-correlation";

        int noOfNodes = complexImages.values().iterator().next().size();
        int noOfArcs = arcs.length;

        // root
        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement esdMeasurement = absTgt.getElement("ESD Measurement");

        // network
        final MetadataElement networkRootElem = getOrCreateElement(esdMeasurement, "Network");
        networkRootElem.setAttributeString("temporalBaselineType", temporalBaselineType);
        networkRootElem.setAttributeInt("maxTemporalBaseline", maxTemporalBaseline);
        networkRootElem.setAttributeInt("noOfNodes", noOfNodes);
        networkRootElem.setAttributeInt("noOfArcs", noOfArcs);

        // arcs
        final MetadataElement arcsElem = getOrCreateElement(networkRootElem, "Arcs");
        arcsElem.setAttributeInt("count", noOfArcs);
        for (int i = 0; i < arcs.length; i++) {
            final MetadataElement arcElem = getOrCreateElement(arcsElem, "Arcs." + i);
            arcElem.setAttributeInt("index", i);
            arcElem.setAttributeInt("sourceNodeIndex", arcs[i][0]);
            arcElem.setAttributeInt("targetNodeIndex", arcs[i][1]);
            arcElem.setAttributeString("polarization", arcPolarizations.get(i));

            arcElem.setAttributeDouble(shiftType, relativeShifts[i]);
            arcElem.getAttribute(shiftType).setDescription(shiftDescription);
            arcElem.setAttributeDouble(weightType, weights[i]);
            arcElem.getAttribute(weightType).setDescription("Arc weight");
        }

        // nodes
        final MetadataElement nodesElem = getOrCreateElement(networkRootElem, "Nodes");
        nodesElem.setAttributeInt("count", noOfNodes);
        for (int i = 0; i < noOfNodes; i++) {
            final MetadataElement nodeElem = getOrCreateElement(nodesElem, "Node." + i);
            nodeElem.setAttributeInt("nodeIndex", i);
            int j = 0;
            for (String swath : subSwathNames) {
                for (String polarization : polarizations) {
                    String imagesKey = polarization.toUpperCase() + "_" + swath.toUpperCase();
                    nodeElem.setAttributeString("imageName." + j++,
                                                getImageTag(complexImages.get(imagesKey).get(i)));
                }

            }
            nodeElem.setAttributeDouble(shiftType, integratedShifts[i]);
            nodeElem.getAttribute(shiftType).setDescription("Integrated shift");
        }
    }

    /**
     * Get or create a sub-element in the provided element.
     *
     * @param element the root element.
     * @param name    the name of the element to retrieve or create.
     * @return the sub-element.
     */
    private MetadataElement getOrCreateElement(MetadataElement element, String name) {
        MetadataElement subElement = element.getElement(name);
        if (subElement == null) {
            subElement = new MetadataElement(name);
            element.addElement(subElement);
        }
        return subElement;
    }

    /**
     * Compute the number of lines in the overlapped area of given adjacent bursts.
     *
     * @return The number of lines in the overlapped area.
     */
    private int computeBurstOverlapSize(final int overlapIndex) {

        final double endTime = subSwath[subSwathIndex - 1].burstLastLineTime[overlapIndex];
        final double startTime = subSwath[subSwathIndex - 1].burstFirstLineTime[overlapIndex + 1];
        return (int) ((endTime - startTime) / subSwath[subSwathIndex - 1].azimuthTimeInterval);
    }

    /**
     * Estimates the ESD phase per pixel for the overlap of two bursts.
     * <p>
     * Reference:
     * N. Yague-Martinez, P. Prats-Iraola, F. Rodriguez Gonzalez, R. Brcic, R. Shau, D. Geudtner, M. Eineder, and
     * R. Bamler. “Interferometric Processing of Sentinel-1 TOPS Data”. In: IEEE Transactions on
     * Geoscience and Remote Sensing, vol. 54, no. 4, pp. 2220–2234, April 2016. ISSN:0196-2892.
     * DOI:10.1109/TGRS.2015.2497902
     * <p>
     * Computes:
     * <code>
     * \phi_\textup{ESD} = \arg{\left \{ (m_i \cdot s^*_i) (m_{i+1} \cdot s^*_{i+1})^* \right \}}
     * </code>
     *
     * @param mBandI            The band with the real part of the master image.
     * @param mBandQ            The band with the imaginary part of the master image.
     * @param sBandI            The band with the real part of the slave image.
     * @param sBandQ            The band with the imaginary part of the slave image.
     * @param backwardRectangle First burst rectangle.
     * @param forwardRectangle  Second burst rectangle.
     * @return ESD phase.
     */
    private double[] estimateESDPhase(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                      final Rectangle backwardRectangle, final Rectangle forwardRectangle) {

        final double[] mIBackArray = getSourceData(mBandI, backwardRectangle);
        final double[] mQBackArray = getSourceData(mBandQ, backwardRectangle);
        final double[] sIBackArray = getSourceData(sBandI, backwardRectangle);
        final double[] sQBackArray = getSourceData(sBandQ, backwardRectangle);

        final double[] mIForArray = getSourceData(mBandI, forwardRectangle);
        final double[] mQForArray = getSourceData(mBandQ, forwardRectangle);
        final double[] sIForArray = getSourceData(sBandI, forwardRectangle);
        final double[] sQForArray = getSourceData(sBandQ, forwardRectangle);

        final int arrayLength = mIBackArray.length;
        final double[] backIntReal = new double[arrayLength];
        final double[] backIntImag = new double[arrayLength];
        complexArrayMultiplication(mIBackArray, mQBackArray, sIBackArray, sQBackArray, backIntReal, backIntImag);

        final double[] forIntReal = new double[arrayLength];
        final double[] forIntImag = new double[arrayLength];
        complexArrayMultiplication(mIForArray, mQForArray, sIForArray, sQForArray, forIntReal, forIntImag);

        final double[] diffIntReal = new double[arrayLength];
        final double[] diffIntImag = new double[arrayLength];
        complexArrayMultiplication(forIntReal, forIntImag, backIntReal, backIntImag, diffIntReal, diffIntImag);

        final double[] phase = new double[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            phase[i] = Math.atan2(diffIntImag[i], diffIntReal[i]);
        }

        return phase;
    }

    /**
     * Estimates azimuth shift as a weighted average.
     * <p>
     * Reference:
     * N. Yague-Martinez, P. Prats-Iraola, F. Rodriguez Gonzalez, R. Brcic, R. Shau, D. Geudtner, M. Eineder, and
     * R. Bamler. “Interferometric Processing of Sentinel-1 TOPS Data”. In: IEEE Transactions on
     * Geoscience and Remote Sensing, vol. 54, no. 4, pp. 2220–2234, April 2016. ISSN:0196-2892.
     * DOI:10.1109/TGRS.2015.2497902
     * <p>
     * Computes:
     * <code>
     * \widehat{\Delta y} = \frac{f_\textup{az}}{2 \pi} \cdot
     * \frac{\arg{\left \{ \left \langle e^{j\phi_{\textup{ESD},p}} \right \rangle \right \}}}
     * {\left \langle \Delta f^{\textup{ovl}}_{\textup{DC},p} \right \rangle},
     * </code>
     * where <code>\left \langle \cdot \right \rangle</code> is the weighted average using the weight vector
     * <code>w</code>
     *
     * @param esdPhase           ESD phase per pixel. (<code>\phi_{\textup{ESD},p}</code>)
     * @param weight             Weights for the estimation. (<code>w</code>)
     * @param spectralSeparation Doppler centroid frequency difference.
     *                           (<code>\Delta f^{\textup{ovl}}_{\textup{DC},p}</code>)
     * @return Azimuth shift estimation.
     */
    public double estimateAzimuthShiftWithAverage(double[] esdPhase, double[] weight, double[] spectralSeparation) {
        double sumDCFrequencyDiffs = 0;  // sum of Doppler centroid frequency differences
        double sumReal = 0;
        double sumImaginary = 0;
        int noOfSamples = esdPhase.length;

        // Set-up summation
        for (int i = 0; i < noOfSamples; i++) {
            sumReal += FastMath.cos(esdPhase[i]) * weight[i];
            sumImaginary += FastMath.sin(esdPhase[i]) * weight[i];
            sumDCFrequencyDiffs += spectralSeparation[i] * weight[i];
        }

        // Calculate average
        double avgReal = sumReal / noOfSamples;
        double avgImag = sumImaginary / noOfSamples;
        double avgDfDC = sumDCFrequencyDiffs / noOfSamples;

        // Calculate angle
        final double phase = Math.atan2(avgImag, avgReal);

        // Return shift
        return phase / (2 * Math.PI * avgDfDC * azimuthTimeInterval);
    }

    /**
     * Estimates the azimuth shift using Periodogram.
     * <p>
     * Reference:
     * N. Yague-Martinez, P. Prats-Iraola, F. Rodriguez Gonzalez, R. Brcic, R. Shau, D. Geudtner, M. Eineder, and
     * R. Bamler. “Interferometric Processing of Sentinel-1 TOPS Data”. In: IEEE Transactions on
     * Geoscience and Remote Sensing, vol. 54, no. 4, pp. 2220–2234, April 2016. ISSN:0196-2892.
     * DOI:10.1109/TGRS.2015.2497902
     * <p>
     * The azimuth shift is estimated by solving:
     * <code>
     * \widehat{\Delta y} = {\arg \min}_{\Delta y}
     * \left \{
     * \left |
     * \sum_{p}
     * w_p \cdot e^{j(\phi_{\textup{ESD},p}
     * - 2 \pi \Delta f^{\textup{ovl}}_{\textup{DC},p}
     * \frac{\Delta y}{f_\textup{az}}
     * )}
     * \right |
     * \right \}
     * </code>
     *
     * @param esdPhase           ESD phase. (<code> \phi_{\textup{ESD},p}</code>)
     * @param weight             Weights for computing the simulated ESD phase (w).
     * @param spectralSeparation Doppler centroid frequency difference in the overlap area for each burst
     *                           (<code>\Delta f^{\textup{ovl}}_{\textup{DC},p}</code>)
     * @param boundary           The value of boundary, <code>b</code>, determines that the search for the shift will be carried
     *                           out in the space <code>[-b, b]</code>.
     * @return Azimuth shift estimation.
     */
    public double estimateAzimuthShiftWithPeriodogram(double[] esdPhase, double[] weight, double[] spectralSeparation,
                                                      double boundary) {
        double azShift;
        boolean findMinArgument;  // flag for the optimization criterion
        double initialBestValue;

        // Check if arrays are equally long
        if (esdPhase.length != weight.length || esdPhase.length != spectralSeparation.length ||
                weight.length != spectralSeparation.length) {
            throw new OperatorException("Arrays must have the same length.");
        }

        // Check optimization criterion
        if (optObjective.equalsIgnoreCase(OPT_CRITERION_MAX_REAL)) {  // maximize real part
            findMinArgument = false;
            initialBestValue = Double.MIN_VALUE;  // will be overwritten in the first comparison
        } else if (optObjective.equalsIgnoreCase(OPT_CRITERION_MIN_ARG)) {  // minimize phase
            findMinArgument = true;
            initialBestValue = Double.MAX_VALUE;  // will be overwritten in the first comparison
        } else {
            throw new OperatorException("Optimization criterion should be either '" + OPT_CRITERION_MIN_ARG +
                                                "' or '" + OPT_CRITERION_MAX_REAL + "'");
        }

        // Create n search subSpaces
        double lowerBound = -boundary;  // overall lower bound
        double searchSpaceSpan = boundary * 2;
        double solutionsSeparation = searchSpaceSpan / (noOfCandidateSolutions - 1);

        // Start search
        double previousBestValue = initialBestValue;
        int iteration = 0;

        while (true) {
            iteration++;
            SystemUtils.LOG.fine("Starting iteration: " + iteration);

            // Calculate residuals between simulated and actual ESD phase
            double bestValue = initialBestValue;  // an extreme value that will be overwritten in the first comparison
            int bestIndex = -1;
            double azimuthShift;

            // Find best value among the candidates
            for (int i = 0; i < noOfCandidateSolutions; i++) {
                // Simulate shift and ESD phase
                azimuthShift = lowerBound + solutionsSeparation * i;  // lowest shift value in subspace

                // Loop over all the data points and fit the simulated ESD phase
                double imag = 0;
                double real = 0;
                for (int j = 0; j < esdPhase.length; j++) {
                    double simPhase = 2 * Constants.PI * spectralSeparation[j] * azimuthShift * azimuthTimeInterval;
                    double resPhase = esdPhase[j] - simPhase;
                    imag += FastMath.sin(resPhase) * weight[j];
                    real += FastMath.cos(resPhase) * weight[j];
                }

                // Find shift according to optimization criterion
                double currentValue;
                if (findMinArgument) {  // minArgument: phase minimization
                    currentValue = FastMath.abs(FastMath.atan2(imag, real));
                    if (currentValue < bestValue) {
                        bestValue = currentValue;
                        bestIndex = i;
                        SystemUtils.LOG.finer(currentValue + " is the new minimum");
                    }
                } else {  // maxReal: real-part maximization
                    currentValue = real;
                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestIndex = i;
                        SystemUtils.LOG.finer(currentValue + " is the new maximum");
                    }
                }
            }

            // Check if the tolerance is met
            double difference = FastMath.abs(previousBestValue - bestValue);
            if (difference < optTolerance && solutionsSeparation < optTolerance) {
                SystemUtils.LOG.fine(new StringBuilder("Iteration: ").append(iteration)
                                             .append(". Tolerance threshold met. Difference ").append(difference)
                                             .append(" < ").append(optTolerance)
                                             .append(". Solutions separation ").append(solutionsSeparation)
                                             .append(" < ").append(optTolerance).toString());
                azShift = lowerBound + solutionsSeparation * bestIndex;
                break;  // Exit loop: tolerance met
            }

            if (iteration > optMaxIterations) {  // Check if the number of iterations has exceeded the maximum
                SystemUtils.LOG.warning("Maximum number of iterations reached: " + optMaxIterations);
                azShift = lowerBound + solutionsSeparation * bestIndex;
                break;  // Exit loop: max iterations
            }

            // Threshold not met, refine search for next iteration
            previousBestValue = bestValue;  // Update the bestValue with the current one
            azimuthShift = lowerBound + solutionsSeparation * bestIndex;  // Take current 'best' shift as center
            // Decrease search space span to twice the length of the current separation between solutions
            searchSpaceSpan = solutionsSeparation * 2;

            // The first value is the center offset minus the interval
            lowerBound = azimuthShift - solutionsSeparation;
            solutionsSeparation = searchSpaceSpan / (noOfCandidateSolutions - 1);

            SystemUtils.LOG.fine(new StringBuilder("Iteration: ").append(iteration)
                                         .append(". Tolerance threshold not met! ")
                                         .append("\nBest value found so far: ").append(previousBestValue)
                                         .append("\nBest value index: ").append(bestIndex)
                                         .append("\nBest azimuth shift estimation: ").append(azimuthShift)
                                         .append("\nNew search space span: ").append(searchSpaceSpan)
                                         .append("\nLower bound at ").append(lowerBound)
                                         .append(" with solutions separation: ").append(solutionsSeparation).toString());
        }
        SystemUtils.LOG.fine("Estimated azimuth shift: " + azShift);

        return azShift;
    }

    private double[] getSourceData(final Band srcBand, final Rectangle rectangle) {

        final int dataType = srcBand.getDataType();
        final Tile srcTile = getSourceTile(srcBand, rectangle);

        double[] dataArray;
        if (dataType == ProductData.TYPE_INT16) {
            final short[] dataArrayShort = (short[]) srcTile.getDataBuffer().getElems();
            dataArray = new double[dataArrayShort.length];
            for (int i = 0; i < dataArrayShort.length; i++) {
                dataArray[i] = (double) dataArrayShort[i];
            }
        } else if (dataType == ProductData.TYPE_FLOAT32) {
            final float[] dataArrayFloat = (float[]) srcTile.getDataBuffer().getElems();
            dataArray = new double[dataArrayFloat.length];
            for (int i = 0; i < dataArrayFloat.length; i++) {
                dataArray[i] = (double) dataArrayFloat[i];
            }
        } else {
            dataArray = (double[]) srcTile.getDataBuffer().getElems();
        }

        return dataArray;
    }

    private static void complexArrayMultiplication(final double[] realArray1, final double[] imagArray1,
                                                   final double[] realArray2, final double[] imagArray2,
                                                   final double[] realOutput, final double[] imagOutput) {

        final int arrayLength = realArray1.length;
        if (imagArray1.length != arrayLength || realArray2.length != arrayLength || imagArray2.length != arrayLength ||
                realOutput.length != arrayLength || imagOutput.length != arrayLength) {
            throw new OperatorException("Arrays of the same length are expected.");
        }

        for (int i = 0; i < arrayLength; i++) {
            realOutput[i] = realArray1[i] * realArray2[i] + imagArray1[i] * imagArray2[i];
            imagOutput[i] = imagArray1[i] * realArray2[i] - realArray1[i] * imagArray2[i];
        }
    }

    private Band getBand(final String suffix, final String prefix, final String swathIndexStr, final String polarization) {

        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (bandName.contains(suffix) && bandName.contains(prefix) &&
                    bandName.contains(swathIndexStr) && bandName.contains(polarization)) {
                return sourceProduct.getBand(bandName);
            }
        }
        return null;
    }

    private static void computeShiftPhaseArray(final double shift, final int signalLength, final double[] phaseArray) {

        int k2;
        double phaseK;
        final double phase = -2.0 * Math.PI * shift / (double) signalLength;
        final int halfSignalLength = (int) (signalLength * 0.5 + 0.5);

        for (int k = 0; k < signalLength; ++k) {
            if (k < halfSignalLength) {
                phaseK = phase * k;
            } else {
                phaseK = phase * (k - signalLength);
            }
            k2 = k * 2;
            phaseArray[k2] = FastMath.cos(phaseK);
            phaseArray[k2 + 1] = FastMath.sin(phaseK);
        }
    }

    private static void multiplySpectrumByShiftFactor(final double[] array, final double[] phaseArray) {

        int k2;
        double c, s;
        double real, imag;
        final int signalLength = array.length / 2;
        for (int k = 0; k < signalLength; ++k) {
            k2 = k * 2;
            c = phaseArray[k2];
            s = phaseArray[k2 + 1];
            real = array[k2];
            imag = array[k2 + 1];
            array[k2] = real * c - imag * s;
            array[k2 + 1] = real * s + imag * c;
        }
    }

    private double[][] computeCoherence(final Rectangle rectangle, final Band mBandI, final Band mBandQ,
                                        final Band sBandI, final Band sBandQ, final int cohWin) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        final int halfWindowSize = cohWin / 2;
        final double[][] coherence = new double[h][w];

        final Tile mstTileI = getSourceTile(mBandI, rectangle);
        final Tile mstTileQ = getSourceTile(mBandQ, rectangle);
        final ProductData mstDataBufferI = mstTileI.getDataBuffer();
        final ProductData mstDataBufferQ = mstTileQ.getDataBuffer();

        final Tile slvTileI = getSourceTile(sBandI, rectangle);
        final Tile slvTileQ = getSourceTile(sBandQ, rectangle);
        final ProductData slvDataBufferI = slvTileI.getDataBuffer();
        final ProductData slvDataBufferQ = slvTileQ.getDataBuffer();

        final TileIndex srcIndex = new TileIndex(mstTileI);

        final double[][] cohReal = new double[h][w];
        final double[][] cohImag = new double[h][w];
        final double[][] mstPower = new double[h][w];
        final double[][] slvPower = new double[h][w];
        for (int y = y0; y < yMax; ++y) {
            srcIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int srcIdx = srcIndex.getIndex(x);
                final int xx = x - x0;

                final float mI = mstDataBufferI.getElemFloatAt(srcIdx);
                final float mQ = mstDataBufferQ.getElemFloatAt(srcIdx);
                final float sI = slvDataBufferI.getElemFloatAt(srcIdx);
                final float sQ = slvDataBufferQ.getElemFloatAt(srcIdx);

                cohReal[yy][xx] = mI * sI + mQ * sQ;
                cohImag[yy][xx] = mQ * sI - mI * sQ;
                mstPower[yy][xx] = mI * mI + mQ * mQ;
                slvPower[yy][xx] = sI * sI + sQ * sQ;
            }
        }

        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final int rowSt = Math.max(yy - halfWindowSize, 0);
                final int rowEd = Math.min(yy + halfWindowSize, h - 1);
                final int colSt = Math.max(xx - halfWindowSize, 0);
                final int colEd = Math.min(xx + halfWindowSize, w - 1);

                double cohRealSum = 0.0f, cohImagSum = 0.0f, mstPowerSum = 0.0f, slvPowerSum = 0.0f;
                int count = 0;
                for (int r = rowSt; r <= rowEd; r++) {
                    for (int c = colSt; c <= colEd; c++) {
                        cohRealSum += cohReal[r][c];
                        cohImagSum += cohImag[r][c];
                        mstPowerSum += mstPower[r][c];
                        slvPowerSum += slvPower[r][c];
                        count++;
                    }
                }

                if (count > 0 && mstPowerSum != 0.0 && slvPowerSum != 0.0) {
                    final double cohRealMean = cohRealSum / (double) count;
                    final double cohImagMean = cohImagSum / (double) count;
                    final double mstPowerMean = mstPowerSum / (double) count;
                    final double slvPowerMean = slvPowerSum / (double) count;
                    coherence[yy][xx] = Math.sqrt((cohRealMean * cohRealMean + cohImagMean * cohImagMean) /
                                                          (mstPowerMean * slvPowerMean));
                }
            }
        }
        return coherence;
    }

    private void performRangeAzimuthShift(final double azOffset, final double rgOffset,
                                          final Band slvBandI, final Band slvBandQ,
                                          final Rectangle targetRectangle, Map<Band, Tile> targetTileMap) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int burstIndex = y0 / subSwath[subSwathIndex - 1].linesPerBurst;

        final float noDataValue = (float) slvBandI.getNoDataValue();
        final Tile slvTileI = getSourceTile(slvBandI, targetRectangle);
        final Tile slvTileQ = getSourceTile(slvBandQ, targetRectangle);
        final float[] slvArrayI = (float[]) slvTileI.getDataBuffer().getElems();
        final float[] slvArrayQ = (float[]) slvTileQ.getDataBuffer().getElems();

        // Perform range shift

        final double[] line = new double[2 * w];
        final double[] phaseRg = new double[2 * w];
        final DoubleFFT_1D row_fft = new DoubleFFT_1D(w);
        final double[][] rangeShiftedI = new double[h][w];
        final double[][] rangeShiftedQ = new double[h][w];

        computeShiftPhaseArray(rgOffset, w, phaseRg);

        for (int r = 0; r < h; r++) {
            final int rw = r * w;
            for (int c = 0; c < w; c++) {
                int c2 = c * 2;
                line[c2] = slvArrayI[rw + c];
                line[c2 + 1] = slvArrayQ[rw + c];
            }

            row_fft.complexForward(line);

            multiplySpectrumByShiftFactor(line, phaseRg);

            row_fft.complexInverse(line, true);

            for (int c = 0; c < w; c++) {
                int c2 = c * 2;
                rangeShiftedI[r][c] = line[c2];
                rangeShiftedQ[r][c] = line[c2 + 1];
            }
        }

        // Perform azimuth Shift

        // get deramp/demodulation phase and perform deramp and demodulation
        final double[][] derampDemodPhase = su.computeDerampDemodPhase(subSwath, subSwathIndex, burstIndex, targetRectangle);
        final double[][] derampDemodI = new double[h][w];
        final double[][] derampDemodQ = new double[h][w];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                final double cosPhase = FastMath.cos(derampDemodPhase[r][c]);
                final double sinPhase = FastMath.sin(derampDemodPhase[r][c]);
                derampDemodI[r][c] = rangeShiftedI[r][c] * cosPhase - rangeShiftedQ[r][c] * sinPhase;
                derampDemodQ[r][c] = rangeShiftedI[r][c] * sinPhase + rangeShiftedQ[r][c] * cosPhase;
            }
        }

        // compute shift phase
        final double[] phaseAz = new double[2 * h];
        computeShiftPhaseArray(azOffset, h, phaseAz);

        // perform azimuth shift using FFT, and perform reramp and remodulation
        final Band tgtBandI = targetProduct.getBand(slvBandI.getName());
        final Band tgtBandQ = targetProduct.getBand(slvBandQ.getName());
        final Tile tgtTileI = targetTileMap.get(tgtBandI);
        final Tile tgtTileQ = targetTileMap.get(tgtBandQ);
        final ProductData tgtDataI = tgtTileI.getDataBuffer();
        final ProductData tgtDataQ = tgtTileQ.getDataBuffer();

        final double[] col1 = new double[2 * h];
        final double[] col2 = new double[2 * h];
        final DoubleFFT_1D col_fft = new DoubleFFT_1D(h);
        for (int c = 0; c < w; c++) {
            final int x = x0 + c;
            for (int r = 0; r < h; r++) {
                int r2 = r * 2;
                col1[r2] = derampDemodI[r][c];
                col1[r2 + 1] = derampDemodQ[r][c];

                col2[r2] = derampDemodPhase[r][c];
                col2[r2 + 1] = 0.0;
            }

            col_fft.complexForward(col1);
            col_fft.complexForward(col2);

            multiplySpectrumByShiftFactor(col1, phaseAz);
            multiplySpectrumByShiftFactor(col2, phaseAz);

            col_fft.complexInverse(col1, true);
            col_fft.complexInverse(col2, true);

            for (int r = 0; r < h; r++) {
                if (slvArrayI[r * w + c] != noDataValue) {
                    int r2 = r * 2;
                    final int y = y0 + r;
                    final int idx = tgtTileI.getDataBufferIndex(x, y);

                    final double cosPhase = FastMath.cos(col2[r2]);
                    final double sinPhase = FastMath.sin(col2[r2]);
                    tgtDataI.setElemDoubleAt(idx, col1[r2] * cosPhase + col1[r2 + 1] * sinPhase);
                    tgtDataQ.setElemDoubleAt(idx, -col1[r2] * sinPhase + col1[r2 + 1] * cosPhase);
                }
            }
        }
    }

    private static void outputESDEstimationToFile(
            final String fileName, final double[][] shiftLUT, final double overallAzShift) throws OperatorException {

        final File logESDFile = new File(ResourceUtils.getReportFolder(), fileName);
        final int numOverlaps = shiftLUT.length;
        final int numBlocksPerOverlap = shiftLUT[0].length;
        PrintStream p = null;

        try {
            final FileOutputStream out = new FileOutputStream(logESDFile.getAbsolutePath(), false);
            p = new PrintStream(out);

            for (double[] aShiftLUT : shiftLUT) {
                for (int j = 0; j < numBlocksPerOverlap; ++j) {
                    p.format("%13.6f ", aShiftLUT[j]);
                }
                p.println();
            }

            p.println();
            p.print("Mean azimuth shift = " + overallAzShift);

        } catch (IOException exc) {
            throw new OperatorException(exc);
        } finally {
            if (p != null)
                p.close();
        }
    }

    //////////////////////
    // Auxiliary classes

    public enum WeightFunction {
        linear(WEIGHT_FN_LINEAR, (coherence, threshold) -> (coherence > threshold) ? coherence : 0),
        quadratic(WEIGHT_FN_QUAD, (coherence, threshold) -> (coherence > threshold) ? coherence * coherence : 0),
        inverseQuadratic(WEIGHT_FN_INVQUAD, (coherence, threshold) -> (coherence > threshold) ? FastMath.sqrt(coherence) : 0),
        none(WEIGHT_FN_NONE, (coherence, threshold) -> (coherence > threshold) ? 1 : 0);

        final private String caption;
        private WeightFunction.WFInterface function;

        private static final Map<String, WeightFunction> lookup = new HashMap<>();

        // Populate the lookup table
        static {
            for (WeightFunction env : WeightFunction.values()) {
                lookup.put(env.getCaption(), env);
            }
        }

        public static WeightFunction fromString(String string) {
            return lookup.get(string);
        }

        WeightFunction(String caption, WeightFunction.WFInterface function) {
            this.caption = caption;
            this.function = function;
        }

        private interface WFInterface {
            double compute(double coherence, double threshold);
        }

        public String getCaption() {
            return caption;
        }

        public double getWeight(double coherence, double threshold) {
            return function.compute(coherence, threshold);
        }
    }

    /**
     * Class for handling azimuth and range shift data.
     * For range, only <code>shift</code> and <code>weight</code> attributes are meaningful.
     */
    private static class ShiftData {
        int overlapIndex;
        int blockIndex;
        double shift;
        double weight;
        double searchBoundary;

        public ShiftData(final int overlapIndex, final int blockIndex, final double shift,
                         final double weight, final double searchBoundary) {
            this.overlapIndex = overlapIndex;
            this.blockIndex = blockIndex;
            this.shift = shift;
            this.weight = weight;
            this.searchBoundary = searchBoundary;
        }
    }

    private static class AzRgOffsets {
        double azOffset;
        double rgOffset;

        public AzRgOffsets(final double azOffset, final double rgOffset) {
            this.azOffset = azOffset;
            this.rgOffset = rgOffset;
        }

        public void setAzOffset(final double azOffset) {
            this.azOffset = azOffset;
        }

        public void setRgOffset(final double rgOffset) {
            this.rgOffset = rgOffset;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SpectralDiversityOp.class);
        }
    }

}
