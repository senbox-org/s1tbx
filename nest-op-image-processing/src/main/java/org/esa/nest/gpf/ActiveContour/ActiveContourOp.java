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
package org.esa.nest.gpf.ActiveContour;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.gpf.OperatorUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@OperatorMetadata(alias = "ActiveContour",
    category = "Image Processing",
    authors = "Emanuela Boros",
    copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
    description = "ActiveContour", internal=true)
public class ActiveContourOp extends Operator {

    public static float[] probabilityHistogram;
    final static int MAX_VALUE = 0;
    final static int MIN_VALUE = 256;
    public static int N;
    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
    rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;
    @Parameter(description = "HighThreshold", defaultValue = "100", label = "HighThreshold")
    private float highThreshold = 100f;
    @Parameter(description = "LowThreshold", defaultValue = "10", label = "LowThreshold")
    private float lowThreshold = 10f;
    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private int sourceImageWidth;
    private int sourceImageHeight;
    private static boolean processed = false;
    private int halfSizeX;
    private int halfSizeY;
    private int filterSizeX = 3;
    private int filterSizeY = 3;
    private static ImagePlus fullImagePlus;
    ActiveContourConfigurationDriver configDriver;
    @Parameter(description = "Number of Iterations", defaultValue = "100", label = "nIterations")
    int nIterations = 50;
    // step to display snake
    @Parameter(description = "Step", defaultValue = "1", label = "Step")
    int step = 1;
    // threshold of edges
    @Parameter(description = "Gradient Threshold", defaultValue = "100", label = "GradientThreshold")
    private int gradientThreshold = 5;
    // how far to look for edges
    @Parameter(description = "Maximum Distance", defaultValue = "100", label = "MaximumDistance")
    int maxDistance = Prefs.getInt("ABSnake_DistSearch.int", 100);
    // maximum displacement
    @Parameter(description = "Maximum Displacement", defaultValue = "2.0", label = "MaximumDisplacement")
    double maxDisplacement = 5.0;
    // regularization factors, min and max
    @Parameter(description = "Regularization Factor", defaultValue = "5.0", label = "RegularizationFactor")
    double dRegularization = 5.0;
    double dMinRegularization, dMaxRegularization;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type
     * {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct}
     * annotation or by calling {@link #setTargetProduct} method.</p> <p>The
     * framework calls this method after it has created this operator. Any
     * client code that must be performed before computation of tile data should
     * be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during operator initialization.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        dMinRegularization = dRegularization / 2.0;
        dMaxRegularization = dRegularization;

        configDriver = new ActiveContourConfigurationDriver();

        setAdvancedParameters();

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            halfSizeX = filterSizeX / 2;
            halfSizeY = filterSizeY / 2;

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
    }

    /**
     * Called by the framework in order to compute a tile for the given target
     * band. <p>The default implementation throws a runtime exception with the
     * message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param pm A progress monitor which should be used to determine
     * computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;

            final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
            Tile sourceRaster;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster = getSourceTile(sourceBand, sourceTileRectangle);
            if (sourceRaster == null) {
                throw new OperatorException("Cannot get source tile");
            }

            initializeActiveContoursProcessing(sourceBand, sourceRaster, targetTile, x0, y0, w, h, pm);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }
    /**
     * Apply Otsu Thresholding
     *
     * @param sourceRaster The source tile for the band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param x0 X coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param pm A progress monitor which should be used to determine
     * computation cancellation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the filtered value.
     */
    private static ArrayList<Roi> currentROIs = new ArrayList<Roi>();
    private final Object lock = new Object();

    private synchronized void initializeActiveContoursProcessing(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm) {

        if (!processed) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
            ImageProcessor imageProcessor = fullImagePlus.getProcessor().convertToByte(true);
            ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
            contrastEnhancer.equalize(imageProcessor);

            fullImagePlus.setProcessor(imageProcessor);

            ProductNodeGroup<VectorDataNode> productNodeGroup = sourceProduct.getVectorDataGroup();
            for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
                FeatureCollection<SimpleFeatureType, SimpleFeature> features =
                        productNodeGroup.get(i).getFeatureCollection();
                FeatureIterator<SimpleFeature> featureIterator = features.features();

                while (featureIterator.hasNext()) {
                    SimpleFeature feature = featureIterator.next();
                    Object value = feature.getDefaultGeometry();
                    Polygon polygon = (Polygon) value;
                    int x[] = new int[polygon.getCoordinates().length];
                    int y[] = new int[polygon.getCoordinates().length];

                    for (int j = 0; j < polygon.getCoordinates().length; j++) {
                        Coordinate coordinate = polygon.getCoordinates()[j];
                        x[j] = (int) (coordinate.x);
                        y[j] = (int) (coordinate.y);
                    }

                    PolygonRoi currentROI = new PolygonRoi(x, y,
                            polygon.getCoordinates().length - 1, Roi.FREEROI);
                    currentROIs.add(new Roi(currentROI.getBounds()));
                }
            }
            RoiManager managerROI = RoiManager.getInstance();

            if (currentROIs.size() > 0 && managerROI == null) {
                managerROI = new RoiManager();
                managerROI.setVisible(true);

                final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

                for (int i = 0; i < currentROIs.size(); i++) {

                    Roi currentROI = currentROIs.get(i);
                    fullImageProcessor.setRoi(new Roi(currentROI.getBounds()));

                    ImageProcessor roiImageProcessor = fullImageProcessor.crop();

                    ImagePlus currentImagePlus = new ImagePlus(sourceBand.getName() + "#" + i,
                            roiImageProcessor);
                    currentImagePlus.setProcessor(roiImageProcessor);
                    if (roiImageProcessor.getRoi() == null) {
                        IJ.showMessage("Roi required");
                    } else {
                        managerROI.add(currentImagePlus,
                                new Roi(roiImageProcessor.getRoi()), 0);
                    }

                    class ActiveContourThread extends Thread {

                        ImagePlus currentImagePlus;
                        RoiManager managerROI;
                        public boolean hasROIs = false;

                        public ActiveContourThread(ImagePlus currentImagePlus,
                                RoiManager managerROI) {
                            this.currentImagePlus = currentImagePlus;
                            this.managerROI = managerROI;
                            currentImagePlus.show();
                        }

                        @Override
                        public void run() {
                            synchronized (lock) {
                                while (!hasROIs) {
                                    int nbRois = managerROI.getCount();

                                    if (nbRois > 1) {
                                        JOptionPane.showMessageDialog(null, "saving.. " + nbRois,
                                                "getImagePlus", JOptionPane.INFORMATION_MESSAGE);
                                        final Roi[] originalROIs = managerROI.getRoisAsArray();
                                        for (int i = 1; i < nbRois; i++) {
                                            ActiveContour currentActivecontour = processActiveContour(
                                                    currentImagePlus,
                                                    originalROIs[i], currentImagePlus.getProcessor(), i,
                                                    x0 + "," + y0);
                                            //                    roiImageProcessor = currentActivecontour.drawContour(roiImageProcessor, Color.white, 2);
                                            //                    Roi resultROI = currentActivecontour.createROI();
                                            //                    currentImagePlus.setRoi(resultROI);
                                            //                    currentImagePlus.setProcessor(roiImageProcessor);
                                            //                    currentImagePlus.show();
                                            hasROIs = true;
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        protected void finalize() throws Throwable {
                            currentImagePlus.close();
                        }
                    }

                    ActiveContourThread thread = new ActiveContourThread(currentImagePlus,
                            managerROI);
                    thread.start();
                    synchronized (lock) {
                        while (!thread.hasROIs) {
                            try {
                                lock.wait();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ActiveContourOp.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                }
                processed = true;
            }
        }
//        final Rectangle srcTileRectangle = sourceRaster.getRectangle();
//
//        Roi currentROI = new Roi(sourceRaster.getRectangle());
//        ImageProcessor aPartProcessor = fullByteProcessor.duplicate();
//        aPartProcessor.setRoi(srcTileRectangle);
//        ImageProcessor roiImageProcessor = aPartProcessor.crop();
        final ProductData trgData = targetTile.getDataBuffer();
//        final ProductData sourceData = ProductData.createInstance((byte[]) fullImagePlus.getPixels());
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0;
                y < maxY;
                ++y) {
            for (int x = x0; x < maxX; ++x) {
//                trgData.setElemFloatAt(targetTile.getDataBufferIndex(x, y),
//                        sourceData.getElemFloatAt(sourceRaster.getDataBufferIndex(x, y)));
            }
        }
    }

    private void setAdvancedParameters() {
        configDriver.setMaxDisplacement(Prefs.get("ABSnake_DisplMin.double", 0.1), Prefs.get("ABSnake_DisplMax.double", 2.0));
        configDriver.setInvAlphaD(Prefs.get("ABSnake_InvAlphaMin.double", 0.5), Prefs.get("ABSnake_InvAlphaMax.double", 2.0));
        configDriver.setReg(Prefs.get("ABSnake_RegMin.double", 0.1), Prefs.get("ABSnake_RegMax.double", 2.0));
        configDriver.setStep(Prefs.get("ABSnake_MulFactor.double", 0.99));
    }

    /**
     * do the snake algorithm on all images
     *
     * @param imagePlus RGB image to display the snake
     * @param numSlice which image of the stack
     */
    public ActiveContour processActiveContour(ImagePlus imagePlus, Roi currentROI,
            ImageProcessor roiImageProcessor, int numSlice, String numRoi) {

//        imagePlus.show();

        ActiveContour activeContour = new ActiveContour();
        activeContour.initActiveContour(currentROI);
        activeContour.setOriginalImage(imagePlus.getProcessor());

        if (step > 0) {
            imagePlus.show();
        }

        double invAlphaD = configDriver.getInvAlphaD(false);
        double regMax = configDriver.getReg(false);
        double regMin = configDriver.getReg(true);
        double maxDisplacement = configDriver.getMaxDisplacement(false);
        double mul = configDriver.getStep();

        ActiveContourConfiguration config = new ActiveContourConfiguration(
                gradientThreshold, maxDisplacement,
                maxDistance, regMin, regMax, 1.0 / invAlphaD);
        activeContour.setConfiguration(config);

        activeContour.computeGradient(roiImageProcessor);

        IJ.resetEscape();

        double dist0 = 0.0;
        double dist;
        for (int i = 0; i < nIterations; i++) {
            if (IJ.escapePressed()) {
                break;
            }
            dist = activeContour.process();
            if ((dist >= dist0) && (dist < this.maxDisplacement)) {
                activeContour.computeGradient(roiImageProcessor);
                config.update(mul);
            }
            dist0 = dist;

            if ((step > 0) && ((i % step) == 0)) {
                IJ.showStatus("Show intermediate result (iteration n" + (i + 1) + ")");
                ByteProcessor image2 = (ByteProcessor) roiImageProcessor.duplicate();
                activeContour.drawContour(image2, Color.WHITE, 3);
                imagePlus.setProcessor(image2);
                imagePlus.setTitle(fullImagePlus.getTitle() + " roi " + numRoi
                        + " (iteration n" + (i + 1) + ")");
                imagePlus.updateAndRepaintWindow();
            }
        }
        return activeContour;
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param w The stackWidth of the target tile rectangle.
     * @param h The stackHeight of the target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfSizeX) {
            sx0 -= halfSizeX;
            sw += halfSizeX;
        }

        if (y0 >= halfSizeY) {
            sy0 -= halfSizeY;
            sh += halfSizeY;
        }

        if (x0 + w + halfSizeX <= sourceImageWidth) {
            sw += halfSizeX;
        }

        if (y0 + h + halfSizeY <= sourceImageHeight) {
            sh += halfSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);


    }

    /**
     * The SPI is used to register this operator in the graph processing
     * framework via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}. This
     * class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ActiveContourOp.class);
            setOperatorUI(ActiveContourOpUI.class);
        }
    }
}
