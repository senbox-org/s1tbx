package org.jlinda.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.lang.ArrayUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.SystemUtils;
import org.jlinda.core.Constants;
import org.jlinda.core.GeoPoint;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;
import org.jlinda.core.coregistration.LUT;
import org.jlinda.core.coregistration.SimpleLUT;
import org.jlinda.core.coregistration.cross.CrossGeometry;

import javax.media.jai.BorderExtender;
import javax.media.jai.InterpolationTable;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.WarpGeneralPolynomial;
import javax.media.jai.WarpPolynomial;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Image resampling for Cross Interferometry
 */
@OperatorMetadata(alias = "CrossResampling",
        category = "SAR Processing/Coregistration",
        authors = "Petar Marinkovic",
        copyright = "Copyright (C) 2013 by PPO.labs",
        description = "Estimate Resampling Polynomial using SAR Image Geometry, and Resample Input Images")
public class CrossResamplingOp extends Operator {

    private static final Logger logger = SystemUtils.LOG;

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The order of polynomial function", valueSet = {"1", "2", "3"}, defaultValue = "2",
            label = "Interpolation Polynomial Order")
    private int warpPolynomialOrder = 2;

    // only complex data accepted
    @Parameter(valueSet = {LUT.CC4P, LUT.CC6P, LUT.TS6P, LUT.TS8P, LUT.TS16P}, defaultValue = LUT.CC6P, label = "Interpolation Method")
    private String interpolationMethod = LUT.CC6P;

    // only complex data accepted
    @Parameter(valueSet = {"ERS", "Envisat ASAR"}, defaultValue = "ERS", label = "Target Geometry")
    private String targetGeometry = "ERS";

    private InterpolationTable interpTable = null;

    // Processing Variables
    // target
    private double targetPRF;
    private double targetRSR;
    // source
    private double sourcePRF;
    private double sourceRSR;

    private SLCImage slcMetadata = null;

    private WarpPolynomial warpPolynomial;
    private WarpPolynomial reverseWarpPolynomial;

    // ERS NOMINAL PRF and RSR
    private final static double ERS_PRF_NOMINAL = 1679.902;  // [Hz]
    private final static double ERS_RSR_NOMINAL = 18.962468; // [MHz]

    // ASAR NOMINAL PRF and RSR
    private final static double ASAR_PRF_NOMINAL = 1652.4156494140625;  // [Hz]
    private final static double ASAR_RSR_NOMINAL = 19.20768;            // [MHz]

    // Metadata
    MetadataElement absSrc;
    MetadataElement absTgt;
    SLCImage targetMeta;
    SLCImage sourceMeta;
    Orbit targetOrbit;

    // Grids
    TiePointGrid longitudeTPG;
    TiePointGrid latitudeTPG;
    TiePointGrid incidenceAngleTPG;
    TiePointGrid slantRangeTimeTPG;
    int targetTPGWidth;
    int targetTPGHeight;
    
    // Source & Target dimensions
    int sourceImageWidth;
    int sourceImageHeight;
    int targetImageWidth;
    int targetImageHeight;
    
    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);

    private CrossGeometry crossGeometry;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CrossResamplingOp() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product} annotated with the
     * {@link org.esa.snap.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */

    @Override
    public void initialize() throws OperatorException {

        try {

            absSrc = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            slcMetadata = new SLCImage(absSrc);

            final String sourceMission = slcMetadata.getMission().toLowerCase();

            boolean ersMission = sourceMission.contains("ers");
            boolean asarMission = sourceMission.contains("asar") || sourceMission.contains("envisat");

            if (!ersMission && !asarMission) {
                throw new OperatorException("The Cross Interferometry operator is for ERS 1/2 and Envisat ASAR products only");
            }

            if (sourceMission.contains(targetGeometry.toLowerCase())) {
//            if (targetGeometry.toLowerCase().contains(sourceMission)) {
                throw new OperatorException("You selected the same target geometry as of input image");
            }

            // declare source
            sourcePRF = slcMetadata.getPRF();
            sourceRSR = slcMetadata.getRsr2x() / 2 / Constants.MEGA;

            // declare target ~ conditionally
            if (ersMission) {
                targetPRF = ASAR_PRF_NOMINAL;
                targetRSR = ASAR_RSR_NOMINAL;
            } else if (asarMission) {
                targetPRF = ERS_PRF_NOMINAL;
                targetRSR = ERS_RSR_NOMINAL;
            }

            constructPolynomial();
            constructReversePolynomial();
            constructInterpolationTable(interpolationMethod);
            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void constructPolynomial() {

        crossGeometry = new CrossGeometry();

        crossGeometry.setPrfOriginal(sourcePRF);
        crossGeometry.setRsrOriginal(sourceRSR);
        crossGeometry.setPrfTarget(targetPRF);
        crossGeometry.setRsrTarget(targetRSR);

        crossGeometry.setDataWindow(slcMetadata.getOriginalWindow());

        crossGeometry.setPolyDegree(warpPolynomialOrder);
        crossGeometry.setNormalizeFlag(false);

        // use grids for computing polynomial using JAI Warp
        crossGeometry.computeCoeffsFromCoords_JAI();

        // cast coefficients to floats
        double[] xCoeffsDouble = crossGeometry.getCoeffsRg();
        double[] yCoeffsDouble = crossGeometry.getCoeffsAz();
        float[] xCoeffsFloat = new float[xCoeffsDouble.length];
        float[] yCoeffsFloat = new float[yCoeffsDouble.length];
        for (int i = 0; i < xCoeffsFloat.length; i++) {
            yCoeffsFloat[i] = (float) yCoeffsDouble[i];
            xCoeffsFloat[i] = (float) xCoeffsDouble[i];
        }
        // show polynomials
        logger.info("coeffsY : {}"+ ArrayUtils.toString(yCoeffsDouble));
        logger.info("coeffsX : {}"+ ArrayUtils.toString(xCoeffsDouble));

        // construct polynomial <- this is strange! Some inconsistency!!!
        warpPolynomial = new WarpGeneralPolynomial(yCoeffsFloat, xCoeffsFloat);

    }

    private void constructReversePolynomial() {

        CrossGeometry crossReverseGeometry = new CrossGeometry();

        crossReverseGeometry.setPrfOriginal(targetPRF);
        crossReverseGeometry.setRsrOriginal(targetRSR);
        crossReverseGeometry.setPrfTarget(sourcePRF);
        crossReverseGeometry.setRsrTarget(sourceRSR);

        crossReverseGeometry.setDataWindow(slcMetadata.getOriginalWindow());

        crossReverseGeometry.setPolyDegree(warpPolynomialOrder);
        crossReverseGeometry.setNormalizeFlag(false);

        // use grids for computing polynomial using JAI Warp
        crossReverseGeometry.computeCoeffsFromCoords_JAI();

        // cast coefficients to floats
        double[] xCoeffsDouble = crossReverseGeometry.getCoeffsRg();
        double[] yCoeffsDouble = crossReverseGeometry.getCoeffsAz();
        float[] xCoeffsFloat = new float[xCoeffsDouble.length];
        float[] yCoeffsFloat = new float[yCoeffsDouble.length];
        for (int i = 0; i < xCoeffsFloat.length; i++) {
            yCoeffsFloat[i] = (float) yCoeffsDouble[i];
            xCoeffsFloat[i] = (float) xCoeffsDouble[i];
        }
        // show polynomials
        logger.info("coeffsY : {}"+ ArrayUtils.toString(yCoeffsDouble));
        logger.info("coeffsX : {}"+ ArrayUtils.toString(xCoeffsDouble));

        // construct polynomial
        reverseWarpPolynomial = new WarpGeneralPolynomial(yCoeffsFloat, xCoeffsFloat);

    }


    private void constructInterpolationTable(String interpolationMethod) {

        // construct interpolation LUT
        SimpleLUT lut = new SimpleLUT(interpolationMethod);
        lut.constructLUT();

        int kernelLength = lut.getKernelLength();

        // get LUT and cast it to float for JAI
        double[] lutArrayDoubles = lut.getKernelAsArray();
        float lutArrayFloats[] = new float[lutArrayDoubles.length];
        int i = 0;
        for (double lutElement : lutArrayDoubles) {
            lutArrayFloats[i++] = (float) lutElement;
        }

        // construct interpolation table for JAI resampling
        final int subsampleBits = 7;
        final int precisionBits = 32;
        int padding = kernelLength / 2 - 1;

        interpTable = new InterpolationTable(padding, kernelLength, subsampleBits, precisionBits, lutArrayFloats);

    }

    /**
     * Create target product.
     */
    private void createTargetProduct() throws Exception {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetImageWidth = (int) Math.ceil(sourceImageWidth * crossGeometry.getRatioRSR());
        targetImageHeight = (int) Math.ceil(sourceImageHeight * crossGeometry.getRatioPRF());

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                targetImageWidth, 
                targetImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (final Band band : targetProduct.getBands()) {
            targetProduct.removeBand(band);
        }

        Band srcBandI = sourceProduct.getBandAt(0);
        Band srcBandQ = sourceProduct.getBandAt(1);

        Band targetBandI = targetProduct.addBand(sourceProduct.getBandAt(0).getName(), ProductData.TYPE_FLOAT32);
        Band targetBandQ = targetProduct.addBand(sourceProduct.getBandAt(1).getName(), ProductData.TYPE_FLOAT32);

        sourceRasterMap.put(targetBandI, srcBandI);
        sourceRasterMap.put(targetBandQ, srcBandQ);

        ProductUtils.copyRasterDataNodeProperties(srcBandI, targetBandI);
        ProductUtils.copyRasterDataNodeProperties(srcBandQ, targetBandQ);

        ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, "_cross");
        ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, "_cross");

        updateTargetProductMetadata();

    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() throws Exception {

        absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        double azSpacingSrc = absTgt.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        double rngSpacingSrc = absTgt.getAttributeDouble(AbstractMetadata.range_spacing);
        
        double azSpacingTgt = azSpacingSrc * (1 / crossGeometry.getRatioPRF()); // ratios inverted because of resampling semantics
        double rngSpacingTgt = rngSpacingSrc * (1 / crossGeometry.getRatioRSR());

        // 1. RSR of source replaced with RSR of target
        // 2. PRF of source replaced with PRF of target
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.pulse_repetition_frequency, targetPRF);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_sampling_rate, targetRSR);

        // 3. resolution cell sampling
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, azSpacingTgt);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, rngSpacingTgt);

        // TODO - here goes update of metadata
        // 4. update of geocodings to allow 'old' coregistration to work
        updateTargetProductGEOCoding();

    }

    private void updateTargetProductGEOCoding() throws Exception {

        targetMeta = new SLCImage(absTgt);
        sourceMeta = new SLCImage(absSrc);
        targetOrbit = new Orbit(absTgt, 3);

        latitudeTPG = OperatorUtils.getLatitude(sourceProduct);
        longitudeTPG = OperatorUtils.getLongitude(sourceProduct);
        slantRangeTimeTPG = OperatorUtils.getSlantRangeTime(sourceProduct);
        incidenceAngleTPG = OperatorUtils.getIncidenceAngle(sourceProduct);

        targetTPGWidth = latitudeTPG.getRasterWidth();
        targetTPGHeight = latitudeTPG.getRasterHeight();

        final float[] targetLatTiePoints = new float[targetTPGHeight * targetTPGWidth];
        final float[] targetLonTiePoints = new float[targetTPGHeight * targetTPGWidth];
        final float[] targetIncidenceAngleTiePoints = new float[targetTPGHeight * targetTPGWidth];
        final float[] targetSlantRangeTimeTiePoints = new float[targetTPGHeight * targetTPGWidth];

        final int subSamplingX = sourceImageWidth / (targetTPGWidth - 1);
        final int subSamplingY = sourceImageHeight / (targetTPGHeight - 1);

        // Create new tie point grid
        int k = 0;
        for (int r = 0; r < targetTPGHeight; r++) {

            // get the zero Doppler time for the rth line
            int y;
            if (r == targetTPGHeight - 1) { // last row
                y = sourceImageHeight - 1;
            } else { // other rows
                y = r * subSamplingY;
            }

            for (int c = 0; c < targetTPGWidth; c++) {

                final int x = getSampleIndex(c, subSamplingX);
                targetIncidenceAngleTiePoints[k] = (float)incidenceAngleTPG.getPixelDouble(x, y);
                targetSlantRangeTimeTiePoints[k] = (float)slantRangeTimeTPG.getPixelDouble(x, y);

                final GeoPoint geoPos = computeLatLon(x, y);
                targetLatTiePoints[k] = (float) geoPos.lat;
                targetLonTiePoints[k] = (float) geoPos.lon;
                k++;
            }
        }

        final TiePointGrid angleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, targetTPGWidth, targetTPGHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetIncidenceAngleTiePoints);
        angleGrid.setUnit(Unit.DEGREES);

        final TiePointGrid slrgtGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, targetTPGWidth, targetTPGHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetSlantRangeTimeTiePoints);
        slrgtGrid.setUnit(Unit.NANOSECONDS);

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, targetTPGWidth, targetTPGHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLatTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, targetTPGWidth, targetTPGHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        for (TiePointGrid tpg : targetProduct.getTiePointGrids()) {
            targetProduct.removeTiePointGrid(tpg);
        }

        targetProduct.addTiePointGrid(angleGrid);
        targetProduct.addTiePointGrid(slrgtGrid);
        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.setGeoCoding(tpGeoCoding);

    }
    
    private int getSampleIndex(final int colIdx, final int subSamplingX) {

        if (colIdx == targetTPGWidth - 1) { // last column
            return sourceImageWidth - 1;
        } else { // other columns
            return colIdx * subSamplingX;
        }
    }
    
    private GeoPoint computeLatLon(final int x, final int y) throws Exception {

        final double[] ell = new double[3];

        ell[0] = latitudeTPG.getPixelDouble(x, y) * Constants.DTOR;
        ell[1] = longitudeTPG.getPixelDouble(x, y)  * Constants.DTOR;
        ell[2] = 0;

        Point posPixSrc = targetOrbit.ell2lp(ell, sourceMeta);
        Point posXYZSrc = targetOrbit.lp2xyz(posPixSrc, sourceMeta);
        Point posPixTgt = targetOrbit.xyz2lp(posXYZSrc, targetMeta);
        double[] posGeoTgt = targetOrbit.lp2ell(posPixTgt, targetMeta);

        return new GeoPoint(posGeoTgt[0] * Constants.RTOD, posGeoTgt[1] * Constants.RTOD);
    }
    
    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        System.out.println("Target Rectangle: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Rectangle sourceRectangle = getSourceRectangle(targetRectangle);
        System.out.println("Source Rectangle: x0 = " + sourceRectangle.x + ", y0 = " + sourceRectangle.y + ", w = " + sourceRectangle.width + ", h = " + sourceRectangle.height);

        System.out.println("------");
        
        
        final BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

        try {

            final Band srcBand = sourceRasterMap.get(targetBand);

            final Tile sourceRaster = getSourceTile(srcBand, targetRectangle, borderExtender);

            if (pm.isCanceled())
                return;

            // get source image
            final RenderedImage srcImage = sourceRaster.getRasterDataNode().getSourceImage();

            // get warped image
            final RenderedOp warpedImage = createWarpImage(warpPolynomial, srcImage);

            // copy warped image data to target
            float[] dataArray = warpedImage.getData(targetRectangle).getSamples(x0, y0, w, h, 0, (float[]) null);

            // set samples in target
            targetTile.setRawSamples(ProductData.createInstance(dataArray));

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Create warped image.
     *
     * @param warp     The WARP polynomial.
     * @param srcImage The source image.
     * @return The warped image.
     */
    private RenderedOp createWarpImage(WarpPolynomial warp, final RenderedImage srcImage) {

        // reformat source image by casting pixel values from ushort to float
        final ParameterBlock pb1 = new ParameterBlock();
        pb1.addSource(srcImage);
        pb1.add(DataBuffer.TYPE_FLOAT);
        final RenderedImage srcImageFloat = JAI.create("format", pb1);

        // get warped image
        final ParameterBlock pb2 = new ParameterBlock();

        pb2.addSource(srcImageFloat);
        pb2.add(warp);
        pb2.add(interpTable);

        RenderedOp warpOutput = JAI.create("warp", pb2);

        return warpOutput;
    }

    private Rectangle getSourceRectangle(Rectangle rect) {

        Point2D lowerLeftSrc = new Point2D.Double(rect.x, rect.y);
        Point2D upperRightSrc = new Point2D.Double(rect.x + rect.width - 1, rect.y + rect.height - 1);

        Point2D lowerLeftTgt = reverseWarpPolynomial.mapDestPoint(lowerLeftSrc);
        Point2D upperRightTgt = reverseWarpPolynomial.mapDestPoint(upperRightSrc);

        int x = (int) Math.ceil(lowerLeftTgt.getX());
        int y = (int) Math.ceil(lowerLeftTgt.getY());
        int w = (int) Math.ceil(upperRightTgt.getX() - lowerLeftTgt.getX() + 1);
        int h = (int) Math.ceil(upperRightTgt.getY() - lowerLeftTgt.getY() + 1);

        return new Rectangle(x, y, w, h);
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CrossResamplingOp.class);
        }
    }

}
