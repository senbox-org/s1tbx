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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.layer.DefaultLayerModel;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.layer.*;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class ProductSceneImage {

    private String name;
    private ImageInfo imageInfo;
    private RasterDataNode[] rasters;
    private RenderedImage image;
    private Rectangle modelArea;
    private LayerModel layerModel;

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster the product raster, must not be null
     * @param pm     a monitor to inform the user about progress
     * @return a color indexed product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode raster, ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("raster", raster);
        return new ProductSceneImage(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo(), null, pm);
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param redRaster   the product raster used for the red color component, must not be null
     * @param greenRaster the product raster used for the green color component, must not be null
     * @param blueRaster  the product raster used for the blue color component, must not be null
     * @param pm          a monitor to inform the user about progress
     * @return an RGB product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode redRaster,
                                           RasterDataNode greenRaster,
                                           RasterDataNode blueRaster,
                                           ProgressMonitor pm) throws IOException {
        Assert.notNull(redRaster, "redRaster");
        Assert.notNull(greenRaster, "greenRaster");
        Assert.notNull(blueRaster, "blueRaster");
        return new ProductSceneImage("RGB", new RasterDataNode[]{redRaster, greenRaster, blueRaster}, null, null, pm);
    }

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster     the product raster, must not be null
     * @param imageInfo  the image information
     *@param layerModel the layer model to be reused, must not be null
     * @param pm         a monitor to inform the user about progress @return a color indexed product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode raster, ImageInfo imageInfo, LayerModel layerModel, ProgressMonitor pm) throws
            IOException {
        Assert.notNull(raster, "raster");
        Assert.notNull(layerModel, "layerModel");
        return new ProductSceneImage(raster.getDisplayName(), new RasterDataNode[]{raster}, imageInfo, layerModel, pm);
    }

    private ProductSceneImage(String name, RasterDataNode[] rasters, ImageInfo imageInfo, LayerModel layerModel, ProgressMonitor pm) throws IOException {
        this.name = name;
        this.imageInfo = imageInfo;
        this.rasters = rasters;
        if (layerModel == null) {
            image = createImage(pm);
            layerModel = createLayerModel(image);
        } else {
            image = ((RenderedImageLayer) layerModel.getLayer(0)).getImage();
        }
        initLayerModel(layerModel);
        this.layerModel = layerModel;
    }

    /**
     * @return the associated product.
     */
    private Product getProduct() {
        return getRaster().getProduct();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public RasterDataNode[] getRasters() {
        return rasters;
    }

    public void setRasters(RasterDataNode[] rasters) {
        this.rasters = rasters;
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

    private LayerModel createLayerModel(RenderedImage image) {
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
        final int size = ProductSceneView.DEFAULT_IMAGE_VIEW_BORDER_SIZE;
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

    RenderedImage createImage(ProgressMonitor pm) throws IOException {
        if (imageInfo != null) {
            return ProductUtils.createRgbImage(rasters, imageInfo, pm);
        } else {
            pm.beginTask("Computing image", 1+3);
            try {
                imageInfo = ProductUtils.createImageInfo(rasters, true, SubProgressMonitor.create(pm, 1));
                return ProductUtils.createRgbImage(rasters, imageInfo, SubProgressMonitor.create(pm, 3));
            } finally {
                pm.done();
            }
        }
    }
}
