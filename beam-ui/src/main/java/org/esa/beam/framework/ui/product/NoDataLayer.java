/*
 * $Id: NoDataLayer.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.layer.AbstractLayer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class NoDataLayer extends AbstractLayer {

    private final RasterDataNode _raster;
    private RenderedImage _noDataImage;
    private float _transparency;
    private Color _color;

    public NoDataLayer(RasterDataNode raster) {
        _raster = raster;
        _noDataImage = null;
        _transparency = 0.0F;
        _color = Color.ORANGE;
    }

    public RasterDataNode getRaster() {
        return _raster;
    }

    public void draw(Graphics2D g2d) {
        if (getNoDataImage() == null) {
            return;
        }
        final Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - _transparency));
        g2d.drawRenderedImage(getNoDataImage(), null);
        g2d.setComposite(oldComposite);
    }

    /**
     * Sets multiple ROI display properties.
     */
    public void setProperties(PropertyMap propertyMap) {
        final float oldTransparency = _transparency;
        final Color oldColor = _color;
        _transparency = (float) propertyMap.getPropertyDouble("noDataOverlay.transparency", 0);
        _color = propertyMap.getPropertyColor("noDataOverlay.color", Color.ORANGE);
        if (!_color.equals(oldColor) || _transparency != oldTransparency) {
            // Force no-data overlay image recreation
            try {
                setNoDataImage(createNoDataImage(ProgressMonitor.NULL));
            } catch (IOException e) {
                Debug.trace(e);
            }
        }
        fireLayerChanged();
    }

    public RenderedImage getNoDataImage() {
        return _noDataImage;
    }

    public void setNoDataImage(RenderedImage noDataImage) {
        _noDataImage = noDataImage;
        fireLayerChanged();
    }

    /**
     * Creates an image of the no-data layer.
     *
     * @return the image
     *
     * @throws IOException
     * @deprecated use {@link #createNoDataImage(com.bc.ceres.core.ProgressMonitor)} instead
     */
    public BufferedImage createNoDataImage() throws IOException {
        return createNoDataImage(ProgressMonitor.NULL);
    }

    /**
     * Creates an image of the no-data layer.
     *
     * @param pm a monitor to inform the user about progress
     *
     * @return the image
     *
     * @throws IOException
     */
    public BufferedImage createNoDataImage(ProgressMonitor pm) throws IOException {
        final byte b00 = (byte) 0;
        final byte b01 = (byte) 1;
        final int width = _raster.getSceneRasterWidth();
        final int height = _raster.getSceneRasterHeight();
        final IndexColorModel cm = new IndexColorModel(8, 2,
                                                       new byte[]{b00, (byte) _color.getRed()},
                                                       new byte[]{b00, (byte) _color.getGreen()},
                                                       new byte[]{b00, (byte) _color.getBlue()},
                                                       0);
        final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        final byte[] imageDataBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        pm.beginTask("Creating no-data image", height);
        try {
            getRaster().ensureDataMaskIsAvailable();
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
