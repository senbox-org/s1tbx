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
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;

import java.awt.*;
import java.util.HashMap;

/**
 * Original SAR images generally appears with inherent speckle noise. Multi-look integration is one category
 * of methods to reduce this inherent speckle noise. The frequency-domain method consis of
 *
 * (1) dividing the bandwidth of the azimuth spectrum of the image into L segments (called looks),
 * (2) forming L independent images from these spectra, and
 * (3) incoherently averaing them.
 *
 * There is also a time-domain method which produce the multi-looked image by averaging the single look image
 * with a small sliding window.
 *
 * This operator implements the simple time-domain method. The multi-looked image is produced according to a
 * user specified factor and a default factor determined by range and azimuth spacings of the original image.
 * As a result, image with equal pixel spacing is produced.
 */

@OperatorMetadata(alias="Multilook",
        category = "SAR Tools",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Averages the power across a number of lines in both the azimuth and range directions")
public final class MultilookOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "The user defined number of range looks", interval = "[1, *)", defaultValue = "1",
                label="Number of Range Looks")
    private int nRgLooks = 1;

    @Parameter(description = "The user defined number of azimuth looks", interval = "[1, *)", defaultValue = "1",
                label="Number of Azimuth Looks")
    private int nAzLooks = 1;

    @Parameter(description = "For complex product output intensity or i and q", defaultValue = "true",
                label="Output Intensity")
    private boolean outputIntensity = true;

    @Parameter(defaultValue="Currently, detection for complex data is performed without any resampling", label="Note")
    String note;

    private MetadataElement absRoot = null;

    private double azimuthLooks; // original azimuth_looks from metadata
    private double rangeLooks;   // original range_looks from metadata
    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetImageWidth;
    private int targetImageHeight;

    private double rangeSpacing;
    private double azimuthSpacing;

    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();

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
            GeoCoding sourceGeoCoding = sourceProduct.getGeoCoding();
            if (sourceGeoCoding instanceof CrsGeoCoding) {
                throw new OperatorException("Multilook is not intended for map projected products");
            }

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getRangeAzimuthSpacing();

            getRangeAzimuthLooks();

            getSourceImageDimension();

            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
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

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int tx0 = targetTileRectangle.x;
        final int ty0 = targetTileRectangle.y;
        final int tw  = targetTileRectangle.width;
        final int th  = targetTileRectangle.height;

        final int x0 = tx0 * nRgLooks;
        final int y0 = ty0 * nAzLooks;
        final int w  = tw * nRgLooks;
        final int h  = th * nAzLooks;
        final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);

        //System.out.println(targetBand.getName()+ " tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            Tile sourceRaster1;
            Tile sourceRaster2 = null;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand1;
            if (srcBandNames.length == 1) {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
                if (sourceRaster1 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            } else {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
                sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
                sourceRaster2 = getSourceTile(sourceBand2, sourceTileRectangle);
                if (sourceRaster1 == null || sourceRaster2 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            }

            final ProductData trgData = targetTile.getDataBuffer();

            final ProductData srcData1 = sourceRaster1.getDataBuffer();
            final ProductData srcData2 = sourceRaster2 != null ? sourceRaster2.getDataBuffer() : null;

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceRaster1);

            final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
            final boolean isdB = bandUnit == Unit.UnitType.INTENSITY_DB || bandUnit == Unit.UnitType.AMPLITUDE_DB;
            final boolean isComplex = outputIntensity && (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY);

            double meanValue;
            int offset;
            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            if(nRgLooks == 1 && nAzLooks == 1) {
                //no mean
                if(!isComplex && targetTile.getDataBuffer().getType() == sourceRaster1.getDataBuffer().getType()) {
                    targetTile.setRawSamples(sourceRaster1.getRawSamples());
                } else {
                    for (int ty = ty0; ty < maxy; ty++) {
                        offset = trgIndex.calculateStride(ty);
                        for (int tx = tx0; tx < maxx; tx++) {
                            final int index = tx - offset;
                            final double i = srcData1.getElemDoubleAt(index);
                            if(srcData2 != null) {
                                final double q = srcData2.getElemDoubleAt(index);
                                trgData.setElemDoubleAt(index, i*i + q*q);
                            } else {
                                trgData.setElemDoubleAt(index, i);
                            }
                        }
                    }
                }
            } else {
                for (int ty = ty0; ty < maxy; ty++) {
                    trgIndex.calculateStride(ty);
                    for (int tx = tx0; tx < maxx; tx++) {
                        meanValue = getMeanValue(tx, ty, srcData1, srcData2, srcIndex, nRgLooks, nAzLooks, isdB, isComplex);
                        trgData.setElemDoubleAt(trgIndex.getIndex(tx), meanValue);
                    }
                }
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get the range and azimuth spacings (in meter).
     */
    private void getRangeAzimuthSpacing() {

        rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 1);
        azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);
        //System.out.println("Range spacing is " + rangeSpacing);
        //System.out.println("Azimuth spacing is " + azimuthSpacing);
    }

    /**
     * Get azimuth and range looks.
     */
    private void getRangeAzimuthLooks() {

        azimuthLooks = absRoot.getAttributeDouble(AbstractMetadata.azimuth_looks, 1);
        rangeLooks = absRoot.getAttributeDouble(AbstractMetadata.range_looks, 1);
        //System.out.println("Azimuth looks is " + azimuthLooks);
        //System.out.println("Range looks is " + rangeLooks);
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
        //System.out.println("Source image width = " + sourceImageWidth);
        //System.out.println("Source image height = " + sourceImageHeight);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetImageWidth = sourceImageWidth / nRgLooks;
        targetImageHeight = sourceImageHeight / nAzLooks;

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    targetImageWidth,
                                    targetImageHeight);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, outputIntensity, false);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        //ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        //ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setDescription(sourceProduct.getDescription());
        OperatorUtils.copyIndexCodings(sourceProduct, targetProduct);

        addGeoCoding();

        updateTargetProductMetadata();
    }

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
            final float y = (nAzLooks - 1)/2 + Math.min(j*subSamplingY, targetImageHeight - 1)*nAzLooks;
            for (int i = 0; i < gridWidth; i++) {
                final float x = (nRgLooks - 1)/2 + Math.min(i*subSamplingX, targetImageWidth - 1)*nRgLooks;
                newTiePointPos[k] = new PixelPos();
                newTiePointPos[k].x = x;
                newTiePointPos[k].y = y;
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
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.multilook_flag, 1);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_looks, azimuthLooks*nAzLooks);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_looks, rangeLooks*nRgLooks);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.azimuth_spacing, azimuthSpacing*nAzLooks);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.range_spacing, rangeSpacing*nRgLooks);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetImageHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetImageWidth);

        final float oldLineTimeInterval = (float)absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.line_time_interval, oldLineTimeInterval*nAzLooks);

        final double oldNearEdgeSlantRange = absTgt.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel);
        final double newNearEdgeSlantRange = oldNearEdgeSlantRange + rangeSpacing*(nRgLooks - 1)/2.0;
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.slant_range_to_first_pixel, newNearEdgeSlantRange);

        double oldFirstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        double newFirstLineUTC = oldFirstLineUTC + oldLineTimeInterval*((nAzLooks - 1)/2.0) / Constants.secondsInDay;
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_line_time, new ProductData.UTC(newFirstLineUTC));
    }

    /**
     * Compute the mean value of pixels of the source image in the sliding window.
     * @param tx The x coordinate of a pixel in the current target tile.
     * @param ty The y coordinate of a pixel in the current target tile.
     * @param srcData1 The product data for i band in case of complex product.
     * @param srcData2 The product data for q band in case of complex product.
     * @param nRgLooks number of range looks
     * @param nAzLooks number of azimuth looks
     * @return The mean value.
     */
    private static double getMeanValue(final int tx, final int ty,
                                       final ProductData srcData1, final ProductData srcData2,
                                       final TileIndex srcIndex,
                                       final int nRgLooks, final int nAzLooks,
                                       final boolean isdB, final boolean isComplex) {

        final int xStart = tx * nRgLooks;
        final int yStart = ty * nAzLooks;
        final int xEnd = xStart + nRgLooks;
        final int yEnd = yStart + nAzLooks;

        double meanValue = 0.0;
        int offset;
        if (isdB) {
            for (int y = yStart; y < yEnd; y++) {
                offset = srcIndex.calculateStride(y);
                for (int x = xStart; x < xEnd; x++) {
                    meanValue += Math.pow(10, srcData1.getElemDoubleAt(x-offset) / 10.0); // dB to linear
                }
            }

            meanValue /= (nRgLooks * nAzLooks);
            return 10.0*Math.log10(meanValue); // linear to dB
        } else if (isComplex) { // COMPLEX
            double i, q;
            int index;
            for (int y = yStart; y < yEnd; y++) {
                offset = srcIndex.calculateStride(y);
                for (int x = xStart; x < xEnd; x++) {
                    index = x-offset;
                    i = srcData1.getElemDoubleAt(index);
                    q = srcData2.getElemDoubleAt(index);
                    meanValue += i*i + q*q;
                }
            }
        } else {
            for (int y = yStart; y < yEnd; y++) {
                offset = srcIndex.calculateStride(y);
                for (int x = xStart; x < xEnd; x++) {
                    meanValue += srcData1.getElemDoubleAt(x-offset);
                }
            }
        }

        return meanValue / (nRgLooks * nAzLooks);
    }

    /**
     * Compute number of azimuth looks and the mean ground pixel spacings for given number of range looks.
     * @param srcProduct The source product.
     * @param nRgLooks The number of range looks.
     * @param param The computed parameters.
     * @throws Exception The exception.
     */
    public static void getDerivedParameters(Product srcProduct, int nRgLooks, DerivedParams param) throws Exception {

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        double rangeSpacing = abs.getAttributeDouble(AbstractMetadata.range_spacing, 1);
        double azimuthSpacing = abs.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);

        double groundRangeSpacing = rangeSpacing;
        if(rangeSpacing == AbstractMetadata.NO_METADATA) {
            azimuthSpacing = 1;
            groundRangeSpacing = 1;
        } else if (!srgrFlag) {
            final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
            if(incidenceAngle != null) {
                final double incidenceAngleAtCentreRangePixel = getIncidenceAngleAtCentreRangePixel(srcProduct,
                                                                                                    incidenceAngle);
                groundRangeSpacing /= Math.sin(incidenceAngleAtCentreRangePixel*MathUtils.DTOR);
            }
        }

        final int nAzLooks = Math.max(1, (int)((double)nRgLooks * groundRangeSpacing / azimuthSpacing + 0.5));
        final float meanGRSqaurePixel = (float)((nRgLooks*groundRangeSpacing + nAzLooks*azimuthSpacing)*0.5);
        param.nAzLooks = nAzLooks;
        param.meanGRSqaurePixel = meanGRSqaurePixel;
    }

    /**
     * Get incidence angle at centre range pixel (in degree).
     * @param srcProduct the source product
     * @param incidenceAngle The incidenceAngle tie point grid.
     * @throws OperatorException if incidenceAngle is null
     * @return The incidence angle.
     */
    private static double getIncidenceAngleAtCentreRangePixel(final Product srcProduct,
                                                              final TiePointGrid incidenceAngle) throws OperatorException {

        final int sourceImageWidth = srcProduct.getSceneRasterWidth();
        final int sourceImageHeight = srcProduct.getSceneRasterHeight();
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        return incidenceAngle.getPixelFloat((float)x, (float)y);
    }

    static class DerivedParams {
        int nAzLooks = 0;
        float meanGRSqaurePixel = 0;
    }

    /**
     * Set the number of range looks. This method is for unit test only.
     * @param numRangelooks The number of range looks.
     */
    public void setNumRangeLooks(int numRangelooks) {
        nRgLooks = numRangelooks;
    }

    /**
     * Set the number of azimuth looks. This method is for unit test only.
     * @param numAzimuthlooks The number of azimuth looks.
     */
    public void setNumAzimuthLooks(int numAzimuthlooks) {
        nAzLooks = numAzimuthlooks;
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
            super(MultilookOp.class);
            super.setOperatorUI(MultilookOpUI.class);
        }
    }
}