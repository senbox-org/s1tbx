/*
 * $Id: NoDataLayer.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.layer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.jai.ImageFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

public class NoDataLayer extends MaskOverlayRenderedImageLayer {
    public static final float DEFAULT_TRANSPARENCY = 0.5f;
    public static final Color DEFAULT_COLOR = Color.ORANGE;

    public NoDataLayer(RasterDataNode raster) {
        super(raster);
        setColor(DEFAULT_COLOR);
        setTransparency(DEFAULT_TRANSPARENCY);
    }

    @Override
    public String getPropertyNamePrefix() {
        return "noDataOverlay";
    }

    @Override
    protected RenderedImage createImage(ProgressMonitor pm) throws Exception {
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            return createImageJAI();
        } else {
            return createBufferedImage(pm);
        }
    }

    private  RenderedImage createImageJAI() throws Exception {
        return ImageFactory.createNoDataImage(getRaster(), getColor());
    }

    public BufferedImage createBufferedImage(ProgressMonitor pm) throws Exception {
        return createBufferedImage(getRaster(), getColor(), pm);
    }

    private BufferedImage createBufferedImage(RasterDataNode raster, Color color, ProgressMonitor pm) throws Exception {
        final byte b00 = (byte) 0;
        final byte b01 = (byte) 1;
        final int width = raster.getSceneRasterWidth();
        final int height = raster.getSceneRasterHeight();
        final IndexColorModel cm = new IndexColorModel(8, 2,
                                                       new byte[]{b00, (byte) color.getRed()},
                                                       new byte[]{b00, (byte) color.getGreen()},
                                                       new byte[]{b00, (byte) color.getBlue()},
                                                       0);
        final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        final byte[] imageDataBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        pm.beginTask("Creating no-data image", height);
        try {
            getRaster().ensureValidMaskComputed(ProgressMonitor.NULL);
            for (int y = 0; y < height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                for (int x = 0; x < width; x++) {
                    if (!getRaster().isPixelValid(x, y)) {
                        imageDataBuffer[y * width + x] = b01;
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return bi;
    }
}
