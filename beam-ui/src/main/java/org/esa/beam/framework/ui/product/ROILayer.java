/*
 * $Id: ROILayer.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.layer.impl.RenderedImageLayer;
import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.jai.ImageFactory;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class ROILayer extends RenderedImageLayer {
    public static final float DEFAULT_TRANSPARENCY = 0.5f;
    public static final Color DEFAULT_COLOR = Color.RED;

    private RasterDataNode _raster;
    private Color _roiColor;
    private float _roiTransparency;

    public ROILayer(RasterDataNode raster) {
        super(createROIImage(raster, DEFAULT_COLOR, DEFAULT_TRANSPARENCY));
        _raster = raster;
        _roiColor = DEFAULT_COLOR;
        _roiTransparency = DEFAULT_TRANSPARENCY;
    }

    public RenderedImage getROIImage() {
        return getImage();
    }

    public void setROIImage(RenderedImage roiImage) {
        setImage(roiImage);
    }

    public void updateROIImage(boolean recreate) throws IOException {
        updateROIImage(recreate, ProgressMonitor.NULL);
    }

    public void updateROIImage(boolean recreate, ProgressMonitor pm) throws IOException {
        RenderedImage roiImage = null;
        if (isVisible() && (getROIImage() == null || recreate)) {
            // JAIJAIJAI
            if (Boolean.getBoolean("beam.imageTiling.enabled")) {
                roiImage = createROIImage(_raster, _roiColor, _roiTransparency);
            } else {
                roiImage = _raster.createROIImage(_roiColor, pm);
            }
        }
        setROIImage(roiImage);
    }

    public void draw(Graphics2D g2d, ViewModel viewModel) {
        if (getImage() == null) {
            return;
        }

        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            // The PlanarImage ROI already has alpha
            super.draw(g2d, viewModel);
        } else {
            final Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - _roiTransparency));
            super.draw(g2d, viewModel);
            g2d.setComposite(oldComposite);
        }
    }

    private static PlanarImage createROIImage(RasterDataNode raster, Color roiColor, float transparency) {
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

    public Figure getRasterROIShapeFigure() {
        if (_raster.getROIDefinition() != null) {
            return _raster.getROIDefinition().getShapeFigure();
        }
        return null;
    }
}
