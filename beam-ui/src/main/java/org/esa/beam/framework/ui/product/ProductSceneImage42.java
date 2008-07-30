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
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.layer.DefaultLayerModel;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.layer.*;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

class ProductSceneImage42 extends ProductSceneImage {

    private RenderedImage baseImage;
    private Rectangle modelArea;
    private LayerModel layerModel;

    ProductSceneImage42(RasterDataNode raster, ProductSceneView42 view) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
        layerModel = view.getLayerModel();
        setBaseImage(((RenderedImageLayer) layerModel.getLayer(0)).getImage());
        initModelArea();
    }

    ProductSceneImage42(RasterDataNode raster, ProgressMonitor pm) throws IOException {
        this(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo(), pm);
    }

    ProductSceneImage42(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) throws IOException {
        this("RGB", rasterDataNodes, null, pm);
    }

    private ProductSceneImage42(String name, RasterDataNode[] rasterDataNodes, ImageInfo imageInfo, ProgressMonitor pm) throws IOException {
        super(name, rasterDataNodes, imageInfo);
        setBaseImage(createBaseImage(pm));
        layerModel = createLayerModel(getBaseImage());
        initModelArea();
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

    private LayerModel createLayerModel(RenderedImage image) {
        DefaultLayerModel layerModel = new DefaultLayerModel();

        layerModel.addLayer(new RenderedImageLayer(image));
        layerModel.addLayer(new NoDataLayer(getRaster()));
        layerModel.addLayer(new FigureLayer());
        layerModel.addLayer(new ROILayer(getRaster()));
        layerModel.addLayer(new GraticuleLayer(getProduct(), getRaster()));
        layerModel.addLayer(new PlacemarkLayer(getProduct(), PinDescriptor.INSTANCE));
        layerModel.addLayer(new PlacemarkLayer(getProduct(), GcpDescriptor.INSTANCE));

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

        return layerModel;
    }

    private void initModelArea() {
        final int size = ProductSceneView.DEFAULT_IMAGE_VIEW_BORDER_SIZE;
        modelArea = new Rectangle(-size, -size,
                                  getBaseImage().getWidth() + 2 * size,
                                  getBaseImage().getHeight() + 2 * size);
    }

    RenderedImage getBaseImage() {
        return baseImage;
    }

    void setBaseImage(RenderedImage baseImage) {
        this.baseImage = baseImage;
    }


    RenderedImage createBaseImage(ProgressMonitor pm) throws IOException {
        if (getImageInfo() != null) {
            return ProductUtils.createRgbImage(getRasters(), getImageInfo(), pm);
        } else {
            pm.beginTask("Computing image", 1 + 3);
            try {
                ImageInfo imageInfo = ProductUtils.createImageInfo(getRasters(), true, SubProgressMonitor.create(pm, 1));
                setImageInfo(imageInfo);
                return ProductUtils.createRgbImage(getRasters(), getImageInfo(), SubProgressMonitor.create(pm, 3));
            } finally {
                pm.done();
            }
        }
    }

}
