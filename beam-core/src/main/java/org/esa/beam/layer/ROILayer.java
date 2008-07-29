/*
 * $Id: ROILayer.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.layer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class ROILayer extends MaskOverlayRenderedImageLayer {
    // TODO: IMAGING 4.5: Layer.getStyle(), SVG property names!
    public static final Color DEFAULT_COLOR = Color.RED;
    public static final float DEFAULT_TRANSPARENCY = 0.5f;

    public ROILayer(RasterDataNode raster) {
        super(raster);
        setColor(DEFAULT_COLOR);
        setTransparency(DEFAULT_TRANSPARENCY);
    }


    @Override
    public String getPropertyNamePrefix() {
        return "roi";
    }

    @Override
    protected RenderedImage createImage(ProgressMonitor pm) throws Exception {
        return createBufferedImage(pm);
    }

    private BufferedImage createBufferedImage(ProgressMonitor pm) throws Exception {
        return getRaster().createROIImage(getColor(), pm);
    }

    public Figure getRasterROIShapeFigure() {
        if (getRaster().getROIDefinition() != null) {
            return getRaster().getROIDefinition().getShapeFigure();
        }
        return null;
    }
}
