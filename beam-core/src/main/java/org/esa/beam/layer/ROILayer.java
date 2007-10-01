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
import org.esa.beam.util.jai.ImageFactory;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class ROILayer extends MaskOverlayRenderedImageLayer {
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
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            return createImageJAI(getRaster(), getColor(), getTransparency());
        } else {
            return createBufferedImage(pm);
        }
    }

    private BufferedImage createBufferedImage(ProgressMonitor pm) throws Exception {
        return getRaster().createROIImage(getColor(), pm);
    }

    private static PlanarImage createImageJAI(RasterDataNode raster, Color roiColor, float transparency) {
        ROI roi = ImageFactory.createROI(raster);
        if (roi == null) {
            return null;
        }
        int alpha = MathUtils.crop((int) (255.0f * (1.0f - transparency)), 0, 255);
        roiColor = new Color(roiColor.getRed(),
                             roiColor.getGreen(),
                             roiColor.getBlue(),
                             alpha);
        return ImageFactory.createROIImage(roi, roiColor);
    }

    public Figure getRasterROIShapeFigure() {
        if (getRaster().getROIDefinition() != null) {
            return getRaster().getROIDefinition().getShapeFigure();
        }
        return null;
    }
}
