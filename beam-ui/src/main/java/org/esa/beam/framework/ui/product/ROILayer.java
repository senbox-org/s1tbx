/*
 * $Id: ROILayer.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.layer.AbstractLayer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class ROILayer extends AbstractLayer {

    private RasterDataNode _raster;
    private Color _roiColor = Color.red;
    private float _roiTransparency = 0.5F;
    private RenderedImage _roiImage;

    public ROILayer(RasterDataNode raster) throws IOException {
        _raster = raster;
        updateROIImage(true, ProgressMonitor.NULL);
    }

    public RenderedImage getROIImage() {
        return _roiImage;
    }

    public void setROIImage(RenderedImage roiImage) {
        _roiImage = roiImage;
        fireLayerChanged();
    }

    /**
     * @deprecated use {@link #updateROIImage(boolean, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public void updateROIImage(boolean recreate) throws IOException {
        updateROIImage(recreate, ProgressMonitor.NULL);
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws IOException {
        BufferedImage roiImage = null;
        if (isVisible() && (getROIImage() == null || recreate)) {
            roiImage = _raster.createROIImage(_roiColor, pm);
        }
        setROIImage(roiImage);
    }

    /**
     * Sets multiple ROI display properties.
     */
    public void setProperties(PropertyMap propertyMap) {
        Color oldROICOlor = _roiColor;

        _roiColor = propertyMap.getPropertyColor("roi.color", Color.red);
        _roiTransparency = (float) propertyMap.getPropertyDouble("roi.transparency", 0.5);

        if (!_roiColor.equals(oldROICOlor)) {
            // Force ROI overlay image recreation
            try {
                updateROIImage(true);
            } catch (IOException e) {
                Debug.trace(e);
            }
        }
        fireLayerChanged();
    }

    public void draw(Graphics2D g2d) {
        if (_roiImage == null) {
            return;
        }

        final Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - _roiTransparency));
        g2d.drawRenderedImage(_roiImage, null);
        g2d.setComposite(oldComposite);
    }

    public Figure getRasterROIShapeFigure() {
        if (_raster.getROIDefinition() != null) {
            return _raster.getROIDefinition().getShapeFigure();
        }
        return null;
    }
}
