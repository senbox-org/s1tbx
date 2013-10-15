/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.GeoUtils;
import org.esa.nest.util.MathUtils;

import java.awt.*;

/**
 * Slant Range to Ground Range Conversion.
 */

@OperatorMetadata(alias="SRGR",
        category = "Geometric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Converts Slant Range to Ground Range")
public class SRGROp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "The order of WARP polynomial function", interval = "[1, *)", defaultValue = "4",
                label="Warp Polynomial Order")
    private int warpPolynomialOrder = 4;

//    @Parameter(description = "The number of range points used in computing WARP polynomial",
//               interval = "(1, *)", defaultValue = "100", label="Number of Range Points")
    private int numRangePoints = 100;

    @Parameter(valueSet = {nearestNeighbourStr, linearStr, cubicStr, cubic2Str, sincStr},
            defaultValue = linearStr, label="Interpolation Method")
    private String interpolationMethod = linearStr;

    private MetadataElement absRoot = null;
    private GeoCoding geoCoding = null;
    private boolean imageFlipped = false;
    private double slantRangeSpacing; // in m
    private double groundRangeSpacing; // in m
    private double nearRangeIncidenceAngle; // in degree
    private double[] slantRangeDistanceArray; // slant range distance from each selected range point to the 1st one
    private double[] groundRangeDistanceArray; // ground range distance from each selected range point to the first one
    private double[] warpPolynomialCoef; // coefficients for warp polynomial

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetImageWidth;
    private int targetImageHeight;

    private enum Interpolation { NEAREST_NEIGHBOR, LINEAR, CUBIC, CUBIC2, SINC }
    private Interpolation interpMethod = Interpolation.LINEAR;

    private static final String nearestNeighbourStr = "Nearest-neighbor interpolation";
    private static final String linearStr = "Linear interpolation";
    private static final String cubicStr = "Cubic interpolation";
    private static final String cubic2Str = "Cubic2 interpolation";
    private static final String sincStr = "Sinc interpolation";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (numRangePoints < warpPolynomialOrder + 2) {
                throw new OperatorException("numRangePoints must be greater than warpPolynomialOrder");
            }

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getSRGRFlag();

            getSlantRangePixelSpacing();

            getSourceImageDimension();

            geoCoding = sourceProduct.getGeoCoding();
            if(geoCoding == null) {
                throw new OperatorException("GeoCoding is null");
            }

            computeSlantRangeDistanceArray();

            getNearRangeIncidenceAngle();

            computeGroundRangeSpacing();

            computeWarpPolynomial();

            createTargetProduct();

            if (interpolationMethod.equals(nearestNeighbourStr)) {
                interpMethod = Interpolation.NEAREST_NEIGHBOR;
            } else if (interpolationMethod.equals(linearStr))  {
                interpMethod = Interpolation.LINEAR;
            } else if (interpolationMethod.equals(cubicStr)) {
                interpMethod = Interpolation.CUBIC;
            } else if (interpolationMethod.equals(cubic2Str)) {
                interpMethod = Interpolation.CUBIC2;
            } else if (interpolationMethod.equals(sincStr)) {
                interpMethod = Interpolation.SINC;
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get SRGR flag.
     * @throws Exception The exceptions.
     */
    private void getSRGRFlag() throws Exception {
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            throw new OperatorException("Slant range to ground range conversion has already been applied");
        }
    }

    /**
     * Get slant range pixel spacing.
     * @throws Exception The exceptions.
     */
    private void getSlantRangePixelSpacing() throws Exception {
        slantRangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
      try {
        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int tw = targetTileRectangle.width;
        final int th = targetTileRectangle.height;
        //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

        // compute ground range image pixel values
        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand); 
        final Rectangle sourceTileRectangle = getSourceRectangle(tx0, ty0, tw, th);
        final Tile sourceRaster = getSourceTile(sourceBand, sourceTileRectangle);

        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData srcData = sourceRaster.getDataBuffer();

        int p0 = 0, p1 = 0, p2 = 0, p3 = 0, p4 = 0;
        double v0 = 0.0, v1 = 0.0, v2 = 0.0, v3 = 0.0, v4 = 0.0, v = 0.0;
        double mu = 0.0;

        for (int x = tx0; x < tx0 + tw; x++) {
            final double p = getSlantRangePixelPosition((double)x);
            if (interpMethod.equals(Interpolation.NEAREST_NEIGHBOR)) {
                p0 = Math.min((int)(p + 0.5), sourceImageWidth - 1);
            } else if (interpMethod.equals(Interpolation.LINEAR))  {
                p0 = Math.min((int)p, sourceImageWidth - 2);
                p1 = p0 + 1;
                mu = p - p0;
            } else if (interpMethod.equals(Interpolation.CUBIC) || interpMethod.equals(Interpolation.CUBIC2)) {
                p1 = Math.min((int)p, sourceImageWidth - 1);
                p0 = Math.max(p1 - 1, 0);
                p2 = Math.min(p1 + 1, sourceImageWidth - 1);
                p3 = Math.min(p1 + 2, sourceImageWidth - 1);
                mu = Math.min(p - p1, 1.0);
            } else if (interpMethod.equals(Interpolation.SINC)) {
                p2 = Math.min((int)(p + 0.5), sourceImageWidth - 1);
                p0 = Math.max(p2 - 2, 0);
                p1 = Math.max(p2 - 1, 0);
                p3 = Math.min(p2 + 1, sourceImageWidth - 1);
                p4 = Math.min(p2 + 2, sourceImageWidth - 1);
                mu = p - p2;
            }

            for (int y = ty0; y < ty0 + th; y++) {
                if (interpMethod.equals(Interpolation.NEAREST_NEIGHBOR)) {
                    v = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p0, y));
                } else if (interpMethod.equals(Interpolation.LINEAR))  {
                    v0 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p0, y));
                    v1 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p1, y));
                    v = MathUtils.interpolationLinear(v0, v1, mu);
                } else if (interpMethod.equals(Interpolation.CUBIC))  {
                    v0 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p0, y));
                    v1 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p1, y));
                    v2 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p2, y));
                    v3 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p3, y));
                    v = MathUtils.interpolationCubic(v0, v1, v2, v3, mu);
                } else if (interpMethod.equals(Interpolation.CUBIC2))  {
                    v0 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p0, y));
                    v1 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p1, y));
                    v2 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p2, y));
                    v3 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p3, y));
                    v = MathUtils.interpolationCubic2(v0, v1, v2, v3, mu);
                } else if (interpMethod.equals(Interpolation.SINC)) {
                    v0 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p0, y));
                    v1 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p1, y));
                    v2 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p2, y));
                    v3 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p3, y));
                    v4 = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(p4, y));
                    v = MathUtils.interpolationSinc(v0, v1, v2, v3, v4, mu);
                }
                if (bandUnit == Unit.UnitType.INTENSITY) {
                    v = Math.max(v, 0.0);
                }
                trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y), v);
            }
        }
      } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int xMin = (int)(getSlantRangePixelPosition((double)Math.max(tx0-2, 0)));
        final int xMax = (int)getSlantRangePixelPosition((double)tx0 + tw + 2);
        final int sw = Math.min(xMax - xMin + 1, sourceImageWidth);
        return new Rectangle(xMin, ty0, sw, th);
    }

    /**
     * Get slant range pixel position given pixel index in the ground range image.
     *
     * @param x The pixel index in the ground range image.
     * @return The pixel index in the slant range image
     */
    private double getSlantRangePixelPosition(double x) {

        if (Double.compare(x, 0.0) == 0) {
            return 0.0;
        }

        final double dg = groundRangeSpacing * x;
        double ds = 0.0;
        for (int j = 0; j < warpPolynomialOrder + 1; j++) {
            ds += Math.pow(dg, (double)j) * warpPolynomialCoef[j];
        }
        return ds / slantRangeSpacing;
    }

    /**
     * Compute slant range distance from each selected range point to the 1st one.
     */
    private void computeSlantRangeDistanceArray() {

        slantRangeDistanceArray = new double[numRangePoints - 1];
        final int pixelsBetweenPoints = sourceImageWidth / numRangePoints;
        final double slantDistanceBetweenPoints = slantRangeSpacing * pixelsBetweenPoints;
        for (int i = 0; i < numRangePoints - 1; i++) {
            slantRangeDistanceArray[i] = slantDistanceBetweenPoints * (i+1);
        }
    }

    /**
     * Get near range incidence angle (in degree).
     */
    private void getNearRangeIncidenceAngle() {

        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        final double alphaFirst = incidenceAngle.getPixelFloat(0.5f, 0.5f);
        final double alphaLast = incidenceAngle.getPixelFloat(sourceImageWidth - 0.5f, 0.5f);
        if (alphaFirst <= alphaLast) {
            imageFlipped = false;
            nearRangeIncidenceAngle = alphaFirst;
        } else {
            imageFlipped = true;
            nearRangeIncidenceAngle = alphaLast;
        }
    }

    /**
     * Compute ground range spacing.
     */
    private void computeGroundRangeSpacing() {
        groundRangeSpacing = slantRangeSpacing / Math.sin(nearRangeIncidenceAngle*Math.PI/180.0);
    }

    /**
     * Create target product.
     * @throws Exception The exceptions.
     */
    private void createTargetProduct() throws Exception {

        computeTargetImageDimension();

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetImageWidth,
                                    targetImageHeight);

        addSelectedBands();

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        addGeoCoding();

        updateTargetProductMetadata();
    }

    /**
     * Compute target image dimension.
     */
    private void computeTargetImageDimension() {

        final double[] xyz = new double[3];
        GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        GeoUtils.geo2xyz(geoPos, xyz);
        double xP0 = xyz[0];
        double yP0 = xyz[1];
        double zP0 = xyz[2];

        double totalDistance = 0.0;
        for (int i = 1; i < sourceImageWidth; i++) {

            geoPos = geoCoding.getGeoPos(new PixelPos(i, 0), null);
            GeoUtils.geo2xyz(geoPos, xyz);
            totalDistance += Math.sqrt(Math.pow(xP0 - xyz[0], 2) +
                                       Math.pow(yP0 - xyz[1], 2) +
                                       Math.pow(zP0 - xyz[2], 2));

            xP0 = xyz[0];
            yP0 = xyz[1];
            zP0 = xyz[2];
        }

        targetImageWidth = (int)(totalDistance / groundRangeSpacing);
        targetImageHeight = sourceImageHeight;
    }

    /**
     * Update metadata in the target product.
     * @throws Exception The exceptions.
     */
    private void updateTargetProductMetadata() throws Exception {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.srgr_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, groundRangeSpacing);
        addSRGRCoefficients(absTgt);
    }

    /**
     * Add SRGR Coefficients to the abstract metadata.
     * @param absTgt The root of the target abstract metadata.
     * @throws Exception The exceptions.
     */
    private void addSRGRCoefficients(MetadataElement absTgt) throws Exception {

        final MetadataElement srgrCoefficientsElem = new MetadataElement(AbstractMetadata.srgr_coefficients);

        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);

        final ProductData.UTC utcTime = absTgt.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);

        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        final double r0 = AbstractMetadata.getAttributeDouble(absTgt, AbstractMetadata.slant_range_to_first_pixel);
        for (int i = 0; i < warpPolynomialCoef.length; i++) {
            if (i == 0) {
                addSRGRCoef(srgrListElem, warpPolynomialCoef[i] + r0, i+1);
            } else {
                addSRGRCoef(srgrListElem, warpPolynomialCoef[i], i+1);
            }
        }

        absTgt.addElement(srgrCoefficientsElem);
    }

    /**
     * Add a SRGR coefficient to the SRGR list.
     * @param srgrListElem The SRGR coefficients list.
     * @param srgrCoefficient The SRGR coefficient.
     * @param cnt the number to append to the coef name
     */
    private static void addSRGRCoef(final MetadataElement srgrListElem, final double srgrCoefficient, int cnt) {
        final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient+'.'+cnt);
        srgrListElem.addElement(coefElem);
        AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
        AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, srgrCoefficient);
    }

    /**
     * Add user selected bands to the target product.
     */
    private void addSelectedBands() {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for(Band srcBand : sourceBands) {
            if (srcBand.getUnit() != null && srcBand.getUnit().contains("phase")) {
                continue;
            }
            final Band targetBand = new Band(srcBand.getName(),
                                       ProductData.TYPE_FLOAT32,
                                       targetImageWidth,
                                       targetImageHeight);
            targetBand.setUnit(srcBand.getUnit());
            targetBand.setDescription(srcBand.getDescription());
            targetBand.setNoDataValue(srcBand.getNoDataValue());
            targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
            targetProduct.addBand(targetBand);
        }
    }

    /**
     * Add Geo coding to the target product.
     */
    private void addGeoCoding() {

        final TiePointGrid lat = OperatorUtils.getLatitude(sourceProduct);
        final TiePointGrid lon = OperatorUtils.getLongitude(sourceProduct);
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        final TiePointGrid slantRgTime = OperatorUtils.getSlantRangeTime(sourceProduct);
        if (lat == null || lon == null || incidenceAngle == null || slantRgTime == null) { // for unit test
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
            return;
        }

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = targetImageWidth / (gridWidth - 1.0f);
        final float subSamplingY = targetImageHeight / (gridHeight - 1.0f);
        final PixelPos[] newTiePointPos = new PixelPos[gridWidth*gridHeight];

        int k = 0;
        for (int j = 0; j < gridHeight; j++) {
            final float y = Math.min(j*subSamplingY, targetImageHeight - 1);
            for (int i = 0; i < gridWidth; i++) {
                final float tx = Math.min(i*subSamplingX, targetImageWidth - 1);
                final float x = (float)getSlantRangePixelPosition((double)tx);
                newTiePointPos[k] = new PixelPos(x, y);
                k++;
            }
        }

        OperatorUtils.createNewTiePointGridsAndGeoCoding(
                sourceProduct,
                targetProduct,
                gridWidth,
                gridHeight,
                subSamplingX,
                subSamplingY,
                newTiePointPos);
    }

    /**
     * Compute WARP polynomial coefficients.
     */
    private void computeWarpPolynomial() {

        final int y = sourceImageHeight / 2;
        computeGroundRangeDistanceArray(y);
        final Matrix A = MathUtils.createVandermondeMatrix(groundRangeDistanceArray, warpPolynomialOrder);
        final Matrix b = new Matrix(slantRangeDistanceArray, numRangePoints - 1);
        final Matrix x = A.solve(b);
        warpPolynomialCoef = x.getColumnPackedCopy();
    }

    /**
     * Compute ground range distance from each selected range point to the 1st one (in m).
     * @param y The y coordinate of a given range line.
     */
    private void computeGroundRangeDistanceArray(int y) {

        groundRangeDistanceArray = new double[numRangePoints - 1];
        final double[] xyz = new double[3];
        final int pixelsBetweenPoints = sourceImageWidth / numRangePoints;

        GeoPos geoPos;
        if (imageFlipped) {
            geoPos = geoCoding.getGeoPos(new PixelPos(sourceImageWidth-1, y), null);
        } else {
            geoPos = geoCoding.getGeoPos(new PixelPos(0, y), null);
        }

        GeoUtils.geo2xyz(geoPos, xyz);
        double xPos0 = xyz[0];
        double yPos0 = xyz[1];
        double zPos0 = xyz[2];

        for (int i = 0; i < numRangePoints - 1; i++) {

            if (imageFlipped) {
                geoPos = geoCoding.getGeoPos(new PixelPos(sourceImageWidth - 1 - pixelsBetweenPoints*(i+1), y), null);
            } else {
                geoPos = geoCoding.getGeoPos(new PixelPos(pixelsBetweenPoints*(i+1), y), null);
            }

            GeoUtils.geo2xyz(geoPos, xyz);
            final double pointToPointDistance = Math.sqrt((xPos0 - xyz[0])*(xPos0 - xyz[0]) +
                                                          (yPos0 - xyz[1])*(yPos0 - xyz[1]) +
                                                          (zPos0 - xyz[2])*(zPos0 - xyz[2]) );

            if (i == 0) {
                groundRangeDistanceArray[i] = pointToPointDistance;
            } else {
                groundRangeDistanceArray[i] = groundRangeDistanceArray[i-1] + pointToPointDistance;
            }

            xPos0 = xyz[0];
            yPos0 = xyz[1];
            zPos0 = xyz[2];
        }
    }

    /**
     * Set the number of range points used for creating warp function.
     * This function is used for unit test only.
     * @param numPoints The number of range points.
     */
    public void setNumOfRangePoints(int numPoints) {
        numRangePoints = numPoints;
    }

    /**
     * Set source band name.
     * This function is used for unit test only.
     * @param name The source band name.
     */
    public void setSourceBandName(String name) {
        sourceBandNames = new String[1];
        sourceBandNames[0] = name;
    }

    /**
     * Get SRGR coefficients.
     * @return The SRGR coefficients.
     */
    public double[] getWarpPolynomialCoef() {
        return warpPolynomialCoef;
    }
    
    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SRGROp.class);
        }
    }
}