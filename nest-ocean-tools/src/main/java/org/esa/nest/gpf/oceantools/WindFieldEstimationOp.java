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
package org.esa.nest.gpf.oceantools;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.eo.Constants;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Document;
import org.jdom.Element;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.operator.MedianFilterDescriptor;
import javax.media.jai.operator.MedianFilterShape;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * The wind field retrieval operator.
 *
 * The operator retrieves wind speed and direction from C-band SAR imagery. The wind direction is estimated
 * from the wind roll using a frequency domain method and the wind speed is estimated by using CMOD5 model
 * for the Normalized Radar Cross Section (NRCS).
 *
 * This operator supports only ERS and ENVISAT products. It is asssumed that the product has been calibrated
 * before applying this operator.
 * 
 * [1] H. Hersbach, CMOD5, “An Improved Geophysical Model Function for ERS C-Band Scatterometry”, Report of
 *     the European Centre Medium-Range Weather Forecasts (ECMWF), 2003.
 *
 * [2] C. C. Wackerman, W. G. Pichel, P. Clemente-Colon, “Automated Estimation of Wind Vectors from SAR”,
 *     12th Conference on Interactions of the Sea and Atmosphere, 2003.
 */

@OperatorMetadata(alias = "Wind-Field-Estimation", category = "Ocean-Tools", description = "Estimate wind speed and direction")
public class WindFieldEstimationOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Window size", defaultValue = "20.0", label="Window Size (km)")
    private double windowSizeInKm = 20.0;

    private String mission = null;
    private int windowSize = 0;
    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;

    private TiePointGrid latitudeTPG = null;
    private TiePointGrid longitudeTPG = null;
    private TiePointGrid incidenceAngle = null;

    private MetadataElement absRoot = null;
    private File windFieldReportFile = null;
    private boolean windFieldEstimated  = false;
    private final HashMap<String, List<WindFieldRecord>> bandWindFieldRecord = new HashMap<String, List<WindFieldRecord>>();

    @Override
    public void initialize() throws OperatorException {
        try {

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getMission();

            checkCalibrationFlag();

            getPixelSpacing();

            computeWindowSize();

            getSourceImageDimension();

            getTiePointGrid();
            
            setTargetReportFilePath();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get mission from the metadata of the product.
     */
    private void getMission() {
        mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if(!mission.equals("ERS") && !mission.equals("ENVISAT") && !mission.equals("ERS1") && !mission.equals("ERS2")
           && !mission.contains("SENTINEL-1") && !mission.contains("RS2")) {
            throw new OperatorException("Currently only C-Band SAR products are supported");
        }
    }

    /**
     * Check calibration flag from the metadata of the product.
     * @throws Exception The exceptions.
     */
    private void checkCalibrationFlag() throws Exception {
        if (!AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.abs_calibration_flag)) {
            throw new OperatorException("The product must be calibrated first.");
        }
    }

    /**
     * Get the range and azimuth spacings (in meter).
     * @throws Exception when metadata is missing or equal to default no data value
     */
    private void getPixelSpacing() throws Exception {

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        //System.out.println("Range spacing is " + rangeSpacing);
        //System.out.println("Azimuth spacing is " + azimuthSpacing);
    }

    private void computeWindowSize() {
        windowSize = (int)(windowSizeInKm*1000 / Math.min(rangeSpacing, azimuthSpacing));
        halfWindowSize = windowSize / 2;
    }

    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Get latitude anf longitude tie point grid.
     */
    private void getTiePointGrid() {
        latitudeTPG = OperatorUtils.getLatitude(sourceProduct);
        longitudeTPG = OperatorUtils.getLongitude(sourceProduct);
        incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
    }

    /**
     * Set absolute path for outputing target report file.
     */
    private void setTargetReportFilePath() {
        final String fileName = sourceProduct.getName() + "_wind_field_report.xml";
        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if(!appUserDir.exists()) {
            appUserDir.mkdirs();
        }
        windFieldReportFile = new File(appUserDir.toString(), fileName);
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        updateTargetProductMetadata();
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for (Band srcBand : sourceBands) {
            final String srcBandName = srcBand.getName();
            final String unit = srcBand.getUnit();
            if(unit == null) {
                throw new OperatorException("band " + srcBandName + " requires a unit");
            }

            final Band targetBand = new Band(srcBandName,
                                             srcBand.getDataType(),
                                             sourceImageWidth,
                                             sourceImageHeight);

            targetBand.setUnit(unit);
            targetProduct.addBand(targetBand);
            bandWindFieldRecord.put(srcBandName, new ArrayList<WindFieldRecord>());
        }
    }

    /**
     * Save wind field report file path in the metadata.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absTgt.setAttributeString(AbstractMetadata.wind_field_report_file, windFieldReportFile.getAbsolutePath());
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
        //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

        final String targetBandName = targetBand.getName();
        final List<WindFieldRecord> windFieldRecordList = bandWindFieldRecord.get(targetBandName);

        final Band sourceBand = sourceProduct.getBand(targetBandName);
        final double noDataValue = sourceBand.getNoDataValue();
        final String pol = OperatorUtils.getBandPolarization(targetBandName, absRoot);
        Tile sourceTile;

        if (mission.equals("ENVISAT")) {
            if (pol != null && !pol.contains("hh") && !pol.contains("vv")) {
                throw new OperatorException("Polarization " + pol + " is not supported. Please select HH or VV.");
            }
        }
        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand);
        if (bandUnit != Unit.UnitType.INTENSITY && bandUnit != Unit.UnitType.INTENSITY_DB) {
            throw new OperatorException("Please select calibrated amplitude or intensity band for wind field estimation");
        }

        // copy the original band data
        targetTile.setRawSamples(getSourceTile(sourceBand, targetTile.getRectangle()).getRawSamples());

        final boolean normlizeSigma = (mission.equals("ENVISAT") && pol.contains("hh"));

        // loop through the center pixel of each frame in the target tile
        int xStart = halfWindowSize;
        while (xStart < tx0) {
            xStart += windowSize;
        }

        int yStart = halfWindowSize;
        while (yStart < ty0) {
            yStart += windowSize;
        }

        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        final int halfWindowArea = windowSize*windowSize / 2;
        final int arrowSize = halfWindowSize*2/3;
        for (int y = yStart; y < maxY; y += windowSize) {
            for (int x = xStart; x < maxX; x += windowSize) {

                // get source data for the frame
                final Rectangle sourceTileRectangle = getSourceRectangle(x, y);
                if (sourceTileRectangle == null) {
                    continue;
                }

                final double lat = latitudeTPG.getPixelFloat(x, y);
                final double lon = longitudeTPG.getPixelFloat(x, y);
                final double theta = incidenceAngle.getPixelFloat(x, y);

                sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
                final int numLandPixels = getNumLandPixels(sourceTile, noDataValue);
                if (numLandPixels >= halfWindowArea) {
                    continue;
                }
                
                final double nrcs = getNormalizedRadarCrossSection(sourceTile, bandUnit, x, y, normlizeSigma, theta);

                // estimate wind direction for the frame
                final double[] direction = {0.0, 0.0};
                double ratio = estimateWindDirection(sourceTile, numLandPixels, noDataValue, direction);
                /*
                if (ratio < 0.2 || ratio > 0.8) { 
                    continue;
                }
                */
                // estimate wind speed for the frame
                final double speed = estimateWindSpeed(nrcs, direction, theta);

                // save wind field info
                final WindFieldRecord record =
                        new WindFieldRecord(lat, lon, speed, arrowSize*direction[0], arrowSize*direction[1], ratio);

                windFieldRecordList.add(record);
            }
        }

        windFieldEstimated = true;
    }

    /**
     * Get the source tile rectangle centered at a given point.
     * @param x The x coordinate of the given pixel.
     * @param y The y coordinate of the given pixel.
     * @return The rectangle.
     */
    private Rectangle getSourceRectangle(final int x, final int y) {
        final int x0 = x - halfWindowSize;
        final int y0 = y - halfWindowSize;
        final int w  = windowSize;
        final int h  = windowSize;

        if (x0 < 0 || y0 < 0 || x0 + w > sourceImageWidth || y0 + h > sourceImageHeight) {
            return null;
        }

        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Get the number of land exists in the given window.
     * @param sourceTile The source tile.
     * @param noDataValue The NoDataValue for the source band.
     * @return The number of land pixels.
     */
    private int getNumLandPixels(final Tile sourceTile, final double noDataValue) {

        final Rectangle sourceTileRectangle = sourceTile.getRectangle();
        final int x0 = sourceTileRectangle.x;
        final int y0 = sourceTileRectangle.y;
        final int w  = sourceTileRectangle.width;
        final int h  = sourceTileRectangle.height;
        if (w != windowSize || h != windowSize) {
            throw new OperatorException("Source tile size does not match window size.");
        }

        final int maxY = y0 + windowSize;
        final int maxX = x0 + windowSize;
        int numberOfLandPixels = 0;
        for (int y = y0; y < maxY; y++) {
            for (int x = x0; x < maxX; x++) {
                if (sourceTile.getDataBuffer().getElemDoubleAt(sourceTile.getDataBufferIndex(x, y)) == noDataValue) {
                    numberOfLandPixels++;
                }
            }
        }

        return numberOfLandPixels;
    }

    /**
     * Compute normalized radar cross section for given pixel.
     * @param sourceTile The source tile.
     * @param bandUnit The source band unit.
     * @param x The X coordinate for the given pixel.
     * @param y The Y coordinate for the given pixel.
     * @param normalizeSigma if mission.contains("ENVISAT") && pol.contains("hh")
     * @param theta The incidence angle in degree.
     * @return The normalized radar cross section.
     */
    private static double getNormalizedRadarCrossSection(final Tile sourceTile, final Unit.UnitType bandUnit,
                                     final int x, final int y, final boolean normalizeSigma, final double theta) {

        double sigma = sourceTile.getDataBuffer().getElemDoubleAt(sourceTile.getDataBufferIndex(x, y));
        if (bandUnit == Unit.UnitType.INTENSITY_DB) {
            sigma = Math.pow(10.0, sigma/10);
        }

        if (normalizeSigma) {
            final double alpha = 1.0; // in range [0.4, 1.0]
            final double tanTheta = Math.tan(theta*org.esa.beam.util.math.MathUtils.DTOR);
            sigma *= Math.pow((1 + 2.0*tanTheta*tanTheta)/(1 + alpha*tanTheta*tanTheta), 2.0);
        }
        return sigma;
    }

    /**
     * Estimate wind direction for a given window.
     * @param sourceTile The source tile.
     * @param numLandPixels The number of land pixels in a given window.
     * @param noDataValue Source band No Data Value.
     * @param direction The direction vector.
     * @return ratio The ratio of the minimum quadratic coefficient of the 2D polynomial over the maximum coefficient.
     */
    private double estimateWindDirection(final Tile sourceTile, final int numLandPixels,
                                       final double noDataValue, final double[] direction) {

        // 1. For each window within which a wind direction will be estimated, a local FFT size is determined.
        //    The FFT size is 2/3 of the window size, therefore four spectra can be computed in the window with
        //    each spectra region has a 50% overlap with the neighboring spectrum.
        //
        // 2. Each window is flattened by applying a large average filter, then dividing by the filtered image.
        //    The filter size for this implementation is set to 11x11.
        //
        // 3. The FFT’s are applied and the four resulting spectra are averaged.
        //
        // 4. An annulus is applied to the spectrum to zero out any energy outside of a wavenumber region.
        //    The limits of the annulus are set to wave lengths of 3 km to 15 km.
        //
        // 5. A 3x3 median filter is then applied to the spectrum to remove noise.
        //
        // 6. A 2D polynomial is fit to the resulting spectral samples and the direction through the origin
        //    which has the largest quadratic term (i.e. the widest extent) is determined. The wind direction
        //    is then assumed to be 90 degree from this direction.

        final double[][] imagette = new double[windowSize][windowSize];
        getImagette(sourceTile, numLandPixels, noDataValue, imagette);

        final double[][] dcRemovedImage = new double[windowSize][windowSize];
        removeDCComponent(imagette, dcRemovedImage);

        int fftSize = windowSize * 2 / 3;
        if (fftSize % 2 == 0) {
            fftSize++;
        }

        final double[][] spec = new double[fftSize][fftSize];
        computeSpectrum(dcRemovedImage, fftSize, spec);

        final double delta_k = 1.0 / (windowSizeInKm * 1000.0 * 2.0/3.0); // 1 / window_size_in_m
        final int n3 = Math.min((int)(Constants.TWO_PI/(2500.0*delta_k)), fftSize/2);
        final RenderedImage annulusAppliedSpec = applyAnnulusToSpec(spec, fftSize, n3, delta_k);

        final RenderedImage filteredImage = medianFilteringSpec(annulusAppliedSpec);

        final double[][] array = new double[2*n3+1][2*n3+1];
        final double peakValue = getPeakSpectrumValue(filteredImage, array, n3);

        return getDirection(array, peakValue, n3, direction);
    }

    private void getImagette(final Tile sourceTile, final int numLandPixels,
                             final double noDataValue, final double[][] imagette) {

        final Rectangle sourceTileRectangle = sourceTile.getRectangle();
        final int x0 = sourceTileRectangle.x;
        final int y0 = sourceTileRectangle.y;
        final int w  = sourceTileRectangle.width;
        final int h  = sourceTileRectangle.height;
        if (w != windowSize || h != windowSize) {
            throw new OperatorException("Source tile size does not match window size.");
        }
        
        final int maxY = y0 + windowSize;
        final int maxX = x0 + windowSize;
        if (numLandPixels > 0) {

            double mean = 0.0;
            for (int y = y0; y < maxY; y++) {
                for (int x = x0; x < maxX; x++) {
                    final double v = sourceTile.getDataBuffer().getElemDoubleAt(sourceTile.getDataBufferIndex(x, y));
                    if (v != noDataValue) {
                        mean += v;
                    }
                }
            }
            mean /= windowSize*windowSize - numLandPixels;

            for (int y = y0; y < maxY; y++) {
                for (int x = x0; x < maxX; x++) {
                    final double v = sourceTile.getDataBuffer().getElemDoubleAt(sourceTile.getDataBufferIndex(x, y));
                    if (v == noDataValue) {
                        imagette[y-y0][x-x0] = mean;
                    } else {
                        imagette[y-y0][x-x0] = v;
                    }
                }
            }

        } else {

            for (int y = y0; y < maxY; y++) {
                for (int x = x0; x < maxX; x++) {
                    imagette[y-y0][x-x0] = sourceTile.getDataBuffer().getElemDoubleAt(sourceTile.getDataBufferIndex(x, y));
                }
            }
        }

        /*
        // generate simulated data for test
        Random generator = new Random();
        for (int y = y0; y < y0 + windowSize; y++) {
            int r = y - y0;
            for (int x = x0; x < x0 + windowSize; x++) {
                int c = x - x0;
//                if (r == 30 || r == 60) {
//                if (c == 30 || c == 60) {
                if (c == r) {
//                if (c == windowSize - 1 - r) {
                    imagette[r][c] = 20*generator.nextDouble();
                } else {
                    imagette[r][c] = generator.nextDouble();
                }
            }
        }
        */
//        dumpData("Imagette", imagette);
    }

    private void removeDCComponent(final double[][] imagette, final double[][] dcRemovedImage) {

        final int filter_size = 11;
        final int half_filter_size = filter_size / 2;
        for (int r = 0; r < windowSize; r++) {
            final int rMin = Math.max(r - half_filter_size, 0);
            final int rMax = Math.min(r + half_filter_size, windowSize - 1);
            for (int c = 0; c < windowSize; c++) {
                final int cMin = Math.max(c - half_filter_size, 0);
                final int cMax = Math.min(c + half_filter_size, windowSize - 1);
                dcRemovedImage[r][c] = imagette[r][c] / getMean(imagette, rMin, rMax, cMin, cMax);
            }
        }
    }

    private static double getMean(final double[][] imagette,
                                  final int rMin, final int rMax, final int cMin, final int cMax) {

        double mean = 0.0;
        for (int r = rMin; r <= rMax; r++) {
            for (int c = cMin; c <= cMax; c++) {
                mean += imagette[r][c];
            }
        }
        return mean / ((rMax - rMin + 1)*(cMax - cMin + 1));
    }

    private void computeSpectrum(final double[][] srcImage, final int fftSize, final double[][] spec) {

        final double[][] F1 = new double[fftSize][fftSize];
        final double[][] F2 = new double[fftSize][fftSize];
        final double[][] F3 = new double[fftSize][fftSize];
        final double[][] F4 = new double[fftSize][fftSize];
        perform2DFFT(srcImage, 0, fftSize - 1, 0, fftSize - 1, F1);
        perform2DFFT(srcImage, 0, fftSize - 1, windowSize - fftSize, windowSize - 1, F2);
        perform2DFFT(srcImage, windowSize - fftSize, windowSize - 1, 0, fftSize - 1, F3);
        perform2DFFT(srcImage, windowSize - fftSize, windowSize - 1, windowSize - fftSize, windowSize - 1, F4);

        for (int r = 0; r < fftSize; r++) {
            for (int c = 0; c < fftSize; c++) {
                spec[r][c] = (F1[r][c] + F2[r][c] + F3[r][c] + F4[r][c]) / 4.0;
            }
        }
    }

    private static void perform2DFFT(final double[][] srcImage, final int xMin, final int xMax,
                              final int yMin, final int yMax, final double[][] spec) {

        // perform 1-D FFT to each row
        final int rowFFTSize = xMax - xMin + 1;
        final int colFFTSize = yMax - yMin + 1;
        final DoubleFFT_1D row_fft = new DoubleFFT_1D(rowFFTSize);
        final double[][] complexDataI = new double[colFFTSize][rowFFTSize];
        final double[][] complexDataQ = new double[colFFTSize][rowFFTSize];
        final double[] rowArray = new double[2*rowFFTSize];
        for (int y = yMin; y <= yMax; y++) {
            int k = 0;
            for (int x = xMin; x <= xMax; x++) {
                rowArray[k++] = srcImage[y][x];
                rowArray[k++] = 0.0;
            }
            row_fft.complexForward(rowArray);
            for (int c = 0; c < rowFFTSize; c++) {
                complexDataI[y - yMin][c] = rowArray[c+c];
                complexDataQ[y - yMin][c] = rowArray[c+c+1];
            }
        }
        // dumpData("complexDataI after row FFT", complexDataI);
        // dumpData("complexDataQ after row FFT", complexDataQ);

        // perform 1-D FFT to each column
        final DoubleFFT_1D col_fft = new DoubleFFT_1D(colFFTSize);
        final double[] colArray = new double[2*colFFTSize];
        for (int x = xMin; x <= xMax; x++) {
            int k = 0;
            for (int y = yMin; y <= yMax; y++) {
                colArray[k++] = complexDataI[y - yMin][x - xMin];
                colArray[k++] = complexDataQ[y - yMin][x - xMin];
            }
            col_fft.complexForward(colArray);
            for (int r = 0; r < colFFTSize; r++) {
                complexDataI[r][x - xMin] = colArray[r+r];
                complexDataQ[r][x - xMin] = colArray[r+r+1];
            }
        }
        // dumpData("complexDataI after col FFT", complexDataI);
        // dumpData("complexDataQ after col FFT", complexDataQ);

        // get spectrum magnitude and perform fftshift
        final int secondHalfColFFTSize = colFFTSize/2;
        final int firstHalfColFFTSize = colFFTSize - secondHalfColFFTSize;
        final int secondHalfRowFFTSize = rowFFTSize/2;
        final int firstHalfRowFFTSize = rowFFTSize - secondHalfRowFFTSize;
        int rr, cc;
        for (int y = yMin; y <= yMax; y++) {
            int r = y - yMin;
            if (r < firstHalfColFFTSize) {
                rr = r + secondHalfColFFTSize;
            } else {
                rr = r - firstHalfColFFTSize;
            }

            for (int x = xMin; x <= xMax; x++) {
                int c = x - xMin;
                if (c < firstHalfRowFFTSize) {
                    cc = c + secondHalfRowFFTSize;
                } else {
                    cc = c - firstHalfRowFFTSize;
                }
                spec[rr][cc] = complexDataI[r][c]*complexDataI[r][c] + complexDataQ[r][c]*complexDataQ[r][c];
            }
        }
        // dumpData("spec", spec);
    }

    private static RenderedImage createRenderedImage(double[] array, int width, int height) {

        // create rendered image with demension being width by height
        final SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1);
        final ColorModel colourModel = PlanarImage.createColorModel(sampleModel);
        final DataBufferDouble dataBuffer = new DataBufferDouble(array, array.length);
        final WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, dataBuffer, new Point(0,0));
        return new BufferedImage(colourModel, raster, false, new Hashtable());
    }

    private static RenderedImage applyAnnulusToSpec(
            final double[][] spec, final int fftSize, final int n3, final double delta_k) {

        final int halfFFTSize = fftSize / 2;
        int n15 = (int)(Constants.TWO_PI/(15000.0*delta_k));
        if (n15 >= halfFFTSize) {
            n15 = 1;
        }
        final double[] array = new double[(2*n3+1)*(2*n3+1)];
        int k = 0;
        for (int r = halfFFTSize - n3; r < halfFFTSize + n3 + 1; r++) {
            for (int c = halfFFTSize - n3; c < halfFFTSize + n3 + 1; c++) {
                if (r >= halfFFTSize - n15 && r <= halfFFTSize + n15 && c >= halfFFTSize - n15 && c <= halfFFTSize + n15) {
                    array[k++] = 0.0;
                } else {
                    array[k++] = spec[r][c];
                }
            }
        }
//        dumpData("before median filtering:", spec);

        return createRenderedImage(array, 2*n3+1, 2*n3+1);
    }

    private static RenderedImage medianFilteringSpec(final RenderedImage annulusAppliedSpec) {

        final int size = 3;
        final MedianFilterShape shape = MedianFilterDescriptor.MEDIAN_MASK_SQUARE;
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(annulusAppliedSpec);
        pb.add(shape);
        pb.add(size);
        return JAI.create("medianfilter", pb);
    }

    private static double getPeakSpectrumValue(final RenderedImage filteredImage, final double[][] array, final int n3) {

        final Raster data = filteredImage.getData();
        double peakValue = 0.0;
        final int length = 2*n3 + 1;
        for (int y = 0; y < length; y++) {
            for (int x = 0; x < length; x++) {
                array[y][x] = data.getSampleDouble(x, y, 0);
                if (peakValue < array[y][x]) {
                    peakValue = array[y][x];
                }
            }
        }
        return peakValue;
    }

    /**
     * Compute wind direction by performing 2D polynomial fitting to the spectral samples.
     * @param array Array holding the spectrum samples.
     * @param peakValue The peak spectrum sample.
     * @param n3 Spectrum size is 2*n3+1.
     * @param direction Wind direction (dx, dy).
     * @return The ratio of the minor semi axes over the major semi axes of the 2D polynomial.
     */
    private static double getDirection(final double[][] array, final double peakValue,
                                       final int n3, final double[] direction) {

//        dumpData("spec", array);

        double m00 = 0.0, m01 = 0.0, m02 = 0.0;
        double m10 = 0.0, m11 = 0.0, m12 = 0.0;
        double m20 = 0.0, m21 = 0.0, m22 = 0.0;
        double s0  = 0.0, s1  = 0.0, s2  = 0.0;

        final int length = 2*n3+1;
        for (int y = 0; y < length; y++) {
            final int yy = y - n3;
            for (int x = 0; x < length; x++) {
                final int xx = x - n3;
                final double v = array[y][x] - peakValue;
                m00 += xx*xx*xx*xx;
                m01 += xx*xx*yy*yy;
                m02 += xx*xx*xx*yy;
                m11 += yy*yy*yy*yy;
                m12 += xx*yy*yy*yy;
                s0  += xx*xx*v;
                s1  += yy*yy*v;
                s2  += xx*yy*v;
            }
        }

        m10 = m01;
        m20 = m02;
        m21 = m12;
        m22 = m01;

        final Matrix M = new Matrix(3, 3);
        M.set(0, 0, m00); M.set(0, 1, m01); M.set(0, 2, m02);
        M.set(1, 0, m10); M.set(1, 1, m11); M.set(1, 2, m12);
        M.set(2, 0, m20); M.set(2, 1, m21); M.set(2, 2, m22);

        final Matrix s = new Matrix(3, 1);
        s.set(0, 0, s0);
        s.set(1, 0, s1);
        s.set(2, 0, s2);

        final Matrix c = M.solve(s);
        final double c0 = c.get(0,0); // c0*x^2 + c1*y^2 + c2*x*y
        final double c1 = c.get(1,0);
        final double c2 = -c.get(2,0); // flip y axis pointing up

        double d = Math.sqrt((c0 - c1)*(c0 - c1) + c2*c2);
        double d2 = 2.0*d;
        double tmp = Math.abs(c0 - c1);
        double cos_theta_2 = (d + tmp) / d2;
        double sin_theta_2 = (d - tmp) / d2;
        double sin_cos = c2*tmp / ((c0 - c1)*d2);
        double a =  (c0*(d + tmp) + c1*(d - tmp) + c2*c2*tmp/(c0 - c1)) / d2;
        double b =  (c0*(d - tmp) + c1*(d + tmp) - c2*c2*tmp/(c0 - c1)) / d2;

        if (cos_theta_2 == 0.0) {
            if (Math.abs(a) > Math.abs(b)) {
                direction[0] = 1.0;
                direction[1] = 0.0;
            } else {
                direction[0] = 0.0;
                direction[1] = 1.0;
            }
        } else if (sin_theta_2 == 0.0) {
            if (Math.abs(a) > Math.abs(b)) {
                direction[0] = 0.0;
                direction[1] = 1.0;
            } else {
                direction[0] = 1.0;
                direction[1] = 0.0;
            }
        } else {
            double k = (sin_cos / Math.abs(sin_cos)) * Math.sqrt(sin_theta_2 / cos_theta_2);
            if (k > 0) {
                if (Math.abs(a) > Math.abs(b)) {
                    direction[0] = -k/Math.sqrt(1 + k*k);
                    direction[1] = 1/Math.sqrt(1 + k*k);
                } else {
                    direction[0] = 1/Math.sqrt(1 + k*k);
                    direction[1] = k/Math.sqrt(1 + k*k);
                }
            } else { // k < 0
                if (Math.abs(a) > Math.abs(b)) {
                    direction[0] = -k/Math.sqrt(1 + k*k);
                    direction[1] = 1/Math.sqrt(1 + k*k);
                } else {
                    direction[0] = -1/Math.sqrt(1 + k*k);
                    direction[1] = -k/Math.sqrt(1 + k*k);
                }
            }
        }

        // wind direction is 90 degree from this direction
        tmp = direction[0];
        direction[0] = -direction[1];
        direction[1] = tmp;

        return Math.min(Math.abs(a), Math.abs(b)) / Math.max(Math.abs(a), Math.abs(b));
    }


    /**
     * Dump data. This function is for debugging use only.
     * @param title The title of the data.
     * @param data 2-D array holding the data.
     */
    private synchronized static void dumpData(final String title, final double[][] data) {
        System.out.println();
        System.out.println(title + ";");
        final int h = data.length;
        final int w = data[0].length;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                System.out.print(data[r][c] + ",");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Estimate wind speed using CMOD5 model.
     * @param nrcs The normalized radar cross section.
     * @param direction The wind direction vector.
     * @param theta The incidence angle in degree.
     * @return The wind speed in m/s.
     */
    private static double estimateWindSpeed(final double nrcs, final double[] direction, final double theta) {

        final double fi = Math.atan2(direction[1], direction[0])*org.esa.beam.util.math.MathUtils.RTOD;
        final double cosFI = Math.cos(fi*org.esa.beam.util.math.MathUtils.DTOR);

        // try wind speed from 0.1 m/s to 20 m/s with step size 0.1
        final double[] err = new double[200];
        err[0] = Math.abs(nrcs - CMOD5.compute(0.1, cosFI, theta));
        double errMin = err[0];
        int errMinIndex = 0;
        for (int i = 1; i < 200; i++) {
            final double v = (i + 1)*0.1; // speed
            err[i] = Math.abs(nrcs - CMOD5.compute(v, cosFI, theta));
            if (err[i] < errMin) {
                errMin = err[i];
                errMinIndex = i;
            }
        }

        return (errMinIndex + 1)*0.1;
    }

    private static class CMOD5 {

        private final static double c1 = -0.688;  private final static double c15 = 0.007;
        private final static double c2 = -0.793;  private final static double c16 = 0.33;
        private final static double c3 = 0.338;   private final static double c17 = 0.012;
        private final static double c4 = -0.173;  private final static double c18 = 22.0;
        private final static double c5 = 0.0;     private final static double c19 = 1.95;
        private final static double c6 = 0.004;   private final static double c20 = 3.0;
        private final static double c7 = 0.111;   private final static double c21 = 8.39;
        private final static double c8 = 0.0162;  private final static double c22 = -3.44;
        private final static double c9 = 6.34;    private final static double c23 = 1.36;
        private final static double c10 = 2.57;   private final static double c24 = 5.35;
        private final static double c11 = -2.18;  private final static double c25 = 1.99;
        private final static double c12 = 0.4;    private final static double c26 = 0.29;
        private final static double c13 = -0.6;   private final static double c27 = 3.80;
        private final static double c14 = 0.045;  private final static double c28 = 1.53;

        private final static double THETM = 40.0;
        private final static double THETHR = 25.0;
        private final static double ZPOW = 1.6;

        private final static double y0 = c19;
        private final static double n = c20;
        private final static double a = y0 - (y0 - 1) / n;
        private final static double b = 1 / (n * Math.pow(y0 - 1, n - 1));

        /**
         * Compute normalized radar cross section (NRCS) using CMOD5 model.
         * @param v The wind speed in m/s.
         * @param cosFI The cos of the angle between radar look direction and wind direction (in degree).
         * @param incidenceAngle The incidence angle in degree.
         * @return The NRCS.
         */
        static double compute(final double v, final double cosFI, final double incidenceAngle) {

            final double x = (incidenceAngle - THETM) / THETHR;
            final double xx = x*x;
            final double a0 = c1 + c2*x + c3*xx + c4*x*xx;
            final double a1 = c5 + c6*x;
            final double a2 = c7 + c8*x;
            final double gamma = c9 + c10*x + c11*xx;
            final double s0 = c12 + c13*x;
            final double s = a2*v;
            double a3 = 1.0 / (1.0 + FastMath.exp(-Math.max(s, s0)));
            if (s < s0) {
                a3 = a3*Math.pow((s/s0), s0*(1.0 - a3));
            }

            final double b0 = Math.pow(a3, gamma) * Math.pow(10.0, a0 + a1*v);
            double b1 = c15*v*(0.5 + x - FastMath.tanh(4.0*(x + c16 + c17*v)));
            b1 = (c14*(1.0 + x) - b1) / (FastMath.exp(0.34*(v - c18) ) + 1);
            final double v0 = c21 + c22*x + c23*xx;
            final double d1 = c24 + c25*x + c26*xx;
            final double d2 = c27 + c28*x;
            double v2 = v/v0 + 1.0;
            if (v2 < y0) {
                v2 = a + b*Math.pow(v2 - 1.0, n);
            }
            final double b2 = (-d1 + d2*v2)*Math.exp(-v2);

            return b0*Math.pow(1.0 + b1*cosFI + b2*(2.0*cosFI*cosFI - 1.0), ZPOW);
        }
    }

    /**
     * Output cluster information to file.
     */
    @Override
    public void dispose() {

        if (!windFieldEstimated) {
            return;
        }

        outputWindFieldInfoToFile();
    }

    /**
     * Output wind fielld information to file.
     * @throws OperatorException when can't save metadata
     */
    private void outputWindFieldInfoToFile() throws OperatorException {
        /*
        double dxMean = 0.0;
        double dyMean = 0.0;
        int counter = 0;
        for (String bandName : bandWindFieldRecord.keySet())  {
            final java.util.List<WindFieldRecord> recordList = bandWindFieldRecord.get(bandName);
            for (WindFieldRecord rec : recordList) {
                dxMean += rec.dx;
                dyMean += rec.dy;
                counter++;
            }
        }
        dxMean /= counter;
        dyMean /= counter;
        */
        final Element root = new Element("Detection");
        final Document doc = new Document(root);

        for (String bandName : bandWindFieldRecord.keySet())  {
            final Element elem = new Element("windFieldEstimated");
            elem.setAttribute("bandName", bandName);
            final java.util.List<WindFieldRecord> recordList = bandWindFieldRecord.get(bandName);
            for (WindFieldRecord rec : recordList) {
                /*
                if (rec.dx*dxMean + rec.dy*dyMean <= 0.707) {
                    continue;
                }
                */
                final Element subElem = new Element("windFieldInfo");
                subElem.setAttribute("lat", String.valueOf(rec.lat));
                subElem.setAttribute("lon", String.valueOf(rec.lon));
                subElem.setAttribute("speed", String.valueOf(rec.speed));
                subElem.setAttribute("dx", String.valueOf(rec.dx));
                subElem.setAttribute("dy", String.valueOf(rec.dy));
                subElem.setAttribute("ratio", String.valueOf(rec.ratio));
                elem.addContent(subElem);
            }
            root.addContent(elem);
        }
        XMLSupport.SaveXML(doc, windFieldReportFile.getAbsolutePath());
    }

    public static class WindFieldRecord {
        public final double lat;
        public final double lon;
        public final double speed;
        public final double dx;
        public final double dy;
        public final double ratio;

        public WindFieldRecord(final double lat, final double lon, final double speed, final double dx, final double dy,
                               final double ratio) {
            this.lat = Math.round(lat*100.0)/100.0;
            this.lon = Math.round(lon*100.0)/100.0;
            this.speed = Math.round(speed*100.0)/100.0;
            this.dx = Math.round(dx*100.0)/100.0;
            this.dy = Math.round(dy*100.0)/100.0;
            this.ratio = Math.round(ratio*100.0)/100.0;
        }
    }


    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WindFieldEstimationOp.class);
        }
    }
}