/*
 * $Id: ProductSceneImage.java,v 1.1 2006/12/15 08:41:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.layer.DefaultLayerModel;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.layer.*;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.jai.ImageFactory;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class ProductSceneImage {

    private RasterDataNode[] rasters;
    private String histogramMatching;
    private RenderedImage image;
    private Rectangle modelArea;
    private LayerModel layerModel;
    private String name;


    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster the product raster, must not be null
     * @param pm     a monitor to inform the user about progress
     */
    public static ProductSceneImage create(RasterDataNode raster, ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("raster", raster);
        return new ProductSceneImage(new RasterDataNode[]{raster}, null, pm);
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param redRaster   the product raster used for the red color component, must not be null
     * @param greenRaster the product raster used for the green color component, must not be null
     * @param blueRaster  the product raster used for the blue color component, must not be null
     * @param pm          a monitor to inform the user about progress
     */
    public static ProductSceneImage create(RasterDataNode redRaster,
                                           RasterDataNode greenRaster,
                                           RasterDataNode blueRaster,
                                           ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("redRaster", redRaster);
        Guardian.assertNotNull("greenRaster", greenRaster);
        Guardian.assertNotNull("blueRaster", blueRaster);
        return new ProductSceneImage(new RasterDataNode[]{redRaster, greenRaster, blueRaster}, null, pm);
    }

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster     the product raster, must not be null
     * @param layerModel the layer model to be reused, must not be null
     * @param pm         a monitor to inform the user about progress
     */
    public static ProductSceneImage create(RasterDataNode raster, LayerModel layerModel, ProgressMonitor pm) throws
            IOException {
        Guardian.assertNotNull("raster", raster);
        Guardian.assertNotNull("layerModel", layerModel);
        return new ProductSceneImage(new RasterDataNode[]{raster}, layerModel, pm);
    }

    private ProductSceneImage(RasterDataNode[] rasters, LayerModel layerModel, ProgressMonitor pm) throws IOException {
        this.rasters = rasters;
        name = "";
        histogramMatching = ImageInfo.HISTOGRAM_MATCHING_OFF;
        if (layerModel == null) {
            image = createImage(pm);
            layerModel = createLayer(image);
        } else {
            image = ((RenderedImageLayer) layerModel.getLayer(0)).getImage();
        }
        initLayerModel(layerModel);
        this.layerModel = layerModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    RenderedImage getImage() {
        return image;
    }

    Rectangle getModelArea() {
        return modelArea;
    }

    LayerModel getLayerModel() {
        return layerModel;
    }

    RasterDataNode[] getRasters() {
        return rasters;
    }

    String getHistogramMatching() {
        return histogramMatching;
    }

    void setHistogramMatching(String histogramMatching) {
        this.histogramMatching = histogramMatching;
    }

    Layer getLayer(String name) {
        return layerModel.getLayer(name);
    }

    NoDataLayer getNoDataLayer() {
        return (NoDataLayer) layerModel.getLayer(1);
    }

    FigureLayer getFigureLayer() {
        return (FigureLayer) layerModel.getLayer(2);
    }

    ROILayer getROILayer() {
        return (ROILayer) layerModel.getLayer(3);
    }

    GraticuleLayer getGraticuleLayer() {
        return (GraticuleLayer) layerModel.getLayer(4);
    }

    PlacemarkLayer getPinLayer() {
        return (PlacemarkLayer) layerModel.getLayer(5);
    }

    PlacemarkLayer getGcpLayer() {
        return (PlacemarkLayer) layerModel.getLayer(6);
    }

    /**
     * Gets the product raster of a single banded view.
     *
     * @return the product raster, or <code>null</code> if this is a 3-banded RGB view
     */
    private RasterDataNode getRaster() {
        return rasters[0];
    }

    /**
     * Returns the associated product.
     */
    private Product getProduct() {
        return getRaster().getProduct();
    }

    private LayerModel createLayer(RenderedImage image) {
        DefaultLayerModel layerModel = new DefaultLayerModel();
        layerModel.addLayer(new RenderedImageLayer(image));
        layerModel.addLayer(new NoDataLayer(getRaster()));
        layerModel.addLayer(new FigureLayer());
        layerModel.addLayer(new ROILayer(getRaster()));
        layerModel.addLayer(new GraticuleLayer(getProduct(), getRaster()));
        layerModel.addLayer(new PlacemarkLayer(getProduct(), PinDescriptor.INSTANCE));
        layerModel.addLayer(new PlacemarkLayer(getProduct(), GcpDescriptor.INSTANCE));
        return layerModel;
    }

    private void initLayerModel(LayerModel layerModel) {
        final int size = ProductSceneView.DEFAULT_IMAGE_VIEW_BORDER_SIZE; // @todo 3 nf/** - use preferences to obtain image border size value
        modelArea = new Rectangle(-size, -size,
                                  image.getWidth() + 2 * size,
                                  image.getHeight() + 2 * size);

        RenderedImageLayer imageLayer = (RenderedImageLayer) layerModel.getLayer(0);
        NoDataLayer noDataLayer = (NoDataLayer) layerModel.getLayer(1);
        FigureLayer figureLayer = (FigureLayer) layerModel.getLayer(2);
        ROILayer roiLayer = (ROILayer) layerModel.getLayer(3);
        GraticuleLayer graticuleLayer = (GraticuleLayer) layerModel.getLayer(4);
        PlacemarkLayer pinLayer = (PlacemarkLayer) layerModel.getLayer(5);
        PlacemarkLayer gcpLayer = (PlacemarkLayer) layerModel.getLayer(6);

        imageLayer.setVisible(true);
        noDataLayer.setVisible(false);
        figureLayer.setVisible(true);
        roiLayer.setVisible(false);
        graticuleLayer.setVisible(false);
        pinLayer.setVisible(false);
        pinLayer.setTextEnabled(true);
        gcpLayer.setVisible(false);
        gcpLayer.setTextEnabled(false);
    }

    private RenderedImage createImage(ProgressMonitor pm) throws IOException {
        return createImage(this.rasters, this.histogramMatching, pm);
    }

    public static RenderedImage createImage(RasterDataNode[] rasters, String histogramMatching, ProgressMonitor pm) throws IOException {
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            return ImageFactory.createOverlayedRgbImage(rasters, histogramMatching, pm);
        } else {
            return ProductUtils.createOverlayedImage(rasters, histogramMatching, pm);
        }
    }

}
