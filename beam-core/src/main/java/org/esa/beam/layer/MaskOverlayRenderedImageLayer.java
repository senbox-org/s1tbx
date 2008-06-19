package org.esa.beam.layer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.view.ViewModel;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;

public abstract class MaskOverlayRenderedImageLayer extends RenderedImageLayer {
    public static final Color DEFAULT_COLOR = Color.RED;
    public static final float DEFAULT_TRANSPARENCY = 0.5f;

    private RasterDataNode raster;
    private final String propertyNameColor;
    private final String propertyNameTransparency;

    public MaskOverlayRenderedImageLayer(RasterDataNode raster) {
        super(null);
        this.raster = raster;
        this.propertyNameColor = getPropertyName("color");
        this.propertyNameTransparency = getPropertyName("transparency");
        setColor(DEFAULT_COLOR);
        setTransparency(DEFAULT_TRANSPARENCY);
    }

    public RasterDataNode getRaster() {
        return raster;
    }

    @Override
    public void dispose() {
        raster = null;
        super.dispose();
    }

    public Color getColor() {
        return (Color) getPropertyValue(getPropertyNameColor());
    }

    public void setColor(Color color) {
        Assert.notNull(color, "color");
        setPropertyValue(getPropertyNameColor(), color);
    }

    public float getTransparency() {
        return (Float) getPropertyValue(getPropertyNameTransparency());
    }

    public void setTransparency(float color) {
        setPropertyValue(getPropertyNameTransparency(), color);
    }

    @Override
    public void draw(Graphics2D g2d, ViewModel viewModel) {
        if (getImage() == null) {
            return;
        }
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            // Alpha is expected to be included in the image's color model
            super.draw(g2d, viewModel);
        } else {
            final Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F - getTransparency()));
            super.draw(g2d, viewModel);
            g2d.setComposite(oldComposite);
        }
    }

    @Override
    protected void setStylePropertiesImpl(PropertyMap propertyMap) {
        super.setStylePropertiesImpl(propertyMap);

        final Color oldColor = getColor();
        final float oldTransparency = getTransparency();
        Color newColor = propertyMap.getPropertyColor(getPropertyNameColor(), getColor());
        float newTransparency = (float) propertyMap.getPropertyDouble(getPropertyNameTransparency(), getTransparency());
        if (!oldColor.equals(newColor) || Math.abs(oldTransparency - newTransparency) > 0.001f) {
            // Force image recreation
            setColor(newColor);
            setTransparency(newTransparency);
            try {
                updateImage(true, ProgressMonitor.NULL);
            } catch (Exception e) {
                // todo - add error handler
                Debug.trace(e);
            }
        }
    }

    public RenderedImage updateImage(boolean recreate, ProgressMonitor pm) throws Exception {
        RenderedImage image = null;
        if (getImage() == null || recreate) {
            image = createImage(pm);
        }
        setImage(image);
        return getImage();
    }

    protected abstract RenderedImage createImage(ProgressMonitor pm) throws Exception;

    protected String getPropertyNameColor() {
        return propertyNameColor;
    }

    protected String getPropertyNameTransparency() {
        return propertyNameTransparency;
    }
}

