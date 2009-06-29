package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

public class PlacemarkLayer extends Layer {

    private static final PlacemarkLayerType LAYER_TYPE = (PlacemarkLayerType) LayerType.getLayerType(
            PlacemarkLayerType.class.getName());

    public static final String PROPERTY_NAME_TEXT_FONT = "text.font";
    public static final String PROPERTY_NAME_TEXT_ENABLED = "text.enabled";
    public static final String PROPERTY_NAME_TEXT_FG_COLOR = "text.fg.color";
    public static final String PROPERTY_NAME_TEXT_BG_COLOR = "text.bg.color";

    public static final boolean DEFAULT_TEXT_ENABLED = false;
    public static final Font DEFAULT_TEXT_FONT = new Font("Helvetica", Font.BOLD, 14);
    public static final Color DEFAULT_TEXT_FG_COLOR = Color.WHITE;
    public static final Color DEFAULT_TEXT_BG_COLOR = Color.BLACK;

    private final MyProductNodeListenerAdapter pnl;
    private Product product;
    private PlacemarkDescriptor placemarkDescriptor;
    private final AffineTransform imageToModelTransform;

    public PlacemarkLayer(Product product, PlacemarkDescriptor placemarkDescriptor,
                          AffineTransform imageToModelTransform) {
        this(LAYER_TYPE, product, placemarkDescriptor, imageToModelTransform);
    }

    protected PlacemarkLayer(LayerType layerType, ValueContainer configuration) {
        super(layerType, configuration);
        this.product = (Product) configuration.getValue(PlacemarkLayerType.PROPERTY_PRODUCT);
        this.placemarkDescriptor = (PlacemarkDescriptor) configuration.getValue(
                PlacemarkLayerType.PROPERTY_PLACEMARK_DESCRIPTOR);
        this.imageToModelTransform = (AffineTransform) configuration.getValue(
                PlacemarkLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
        this.pnl = new MyProductNodeListenerAdapter();
        product.addProductNodeListener(pnl);

    }


    private static ValueContainer initConfiguration(ValueContainer configurationTemplate, Product product,
                                                    PlacemarkDescriptor placemarkDescriptor,
                                                    AffineTransform imageToModelTransform) {
        try {
            configurationTemplate.setValue(PlacemarkLayerType.PROPERTY_PRODUCT, product);
            configurationTemplate.setValue(PlacemarkLayerType.PROPERTY_PLACEMARK_DESCRIPTOR, placemarkDescriptor);
            configurationTemplate.setValue(PlacemarkLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM, imageToModelTransform);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return configurationTemplate;
    }

    protected PlacemarkLayer(PlacemarkLayerType type, Product product, PlacemarkDescriptor placemarkDescriptor,
                             AffineTransform imageToModelTransform) {
        this(type,
             initConfiguration(type.getConfigurationTemplate(), product, placemarkDescriptor, imageToModelTransform));
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        final ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup();
        if (placemarkGroup.getNodeCount() == 0) {
            return null;
        }
        double x0 = Double.MAX_VALUE;
        double y0 = Double.MAX_VALUE;
        double x1 = Double.MIN_VALUE;
        double y1 = Double.MIN_VALUE;
        for (int i = 0; i < placemarkGroup.getNodeCount(); i++) {
            final Pin pin = placemarkGroup.get(i);
            final PixelPos pixelPos = pin.getPixelPos();
            x0 = Math.min(x0, pixelPos.getX());
            y0 = Math.min(y0, pixelPos.getY());
            x1 = Math.max(x1, pixelPos.getX());
            y1 = Math.max(y1, pixelPos.getY());
        }
        final Rectangle2D.Double bounds = new Rectangle2D.Double();
        bounds.setFrameFromDiagonal(x0, y0, x1, y1);
        return getImageToModelTransform().createTransformedShape(bounds).getBounds2D();
    }

    @Override
    public void disposeLayer() {
        if (product != null) {
            product.removeProductNodeListener(pnl);
            product = null;
        }
    }

    protected ProductNodeGroup<Pin> getPlacemarkGroup() {
        return placemarkDescriptor.getPlacemarkGroup(getProduct());
    }

    public Product getProduct() {
        return product;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    public AffineTransform getImageToModelTransform() {
        return new AffineTransform(imageToModelTransform);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        Graphics2D g2d = rendering.getGraphics();
        Viewport viewport = rendering.getViewport();
        AffineTransform oldTransform = g2d.getTransform();
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldTextAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final AffineTransform transform = new AffineTransform();
            transform.concatenate(oldTransform);
            transform.concatenate(viewport.getModelToViewTransform());
            transform.concatenate(imageToModelTransform);
            g2d.setTransform(transform);

            ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
            Pin[] placemarks = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
            for (final Pin placemark : placemarks) {
                final PixelPos pixelPos = placemark.getPixelPos();
                if (pixelPos != null) {
                    g2d.translate(pixelPos.getX(), pixelPos.getY());
                    final double scale = Math.sqrt(Math.abs(g2d.getTransform().getDeterminant()));
                    g2d.scale(1 / scale, 1 / scale);
                    g2d.rotate(viewport.getOrientation());

                    if (pinGroup.getSelectedNodes().contains(placemark)) {
                        placemark.getSymbol().drawSelected(g2d);
                    } else {
                        placemark.getSymbol().draw(g2d);
                    }

                    if (isTextEnabled()) {
                        drawTextLabel(g2d, placemark);
                    }

                    g2d.rotate(-viewport.getOrientation());
                    g2d.scale(scale, scale);
                    g2d.translate(-pixelPos.getX(), -pixelPos.getY());
                }
            }
        } finally {
            g2d.setTransform(oldTransform);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAntialiasing);
        }
    }

    private void drawTextLabel(Graphics2D g2d, Pin placemark) {

        final String label = placemark.getLabel();

        g2d.setFont(getTextFont());

        GlyphVector glyphVector = getTextFont().createGlyphVector(g2d.getFontRenderContext(), label);
        Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
        float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
        float ty = (float) (1.0 + logicalBounds.getHeight());
        Shape outline = glyphVector.getOutline(tx, ty);

        int[] alphas = new int[]{64, 128, 192, 255};
        for (int i = 0; i < alphas.length; i++) {
            BasicStroke selectionStroke = new BasicStroke((alphas.length - i));
            Color selectionColor = new Color(getTextBgColor().getRed(),
                                             getTextBgColor().getGreen(),
                                             getTextBgColor().getBlue(),
                                             alphas[i]);
            g2d.setStroke(selectionStroke);
            g2d.setPaint(selectionColor);
            g2d.draw(outline);
        }

        g2d.setPaint(getTextFgColor());
        g2d.fill(outline);
    }

    public boolean isTextEnabled() {
        return getConfigurationProperty(PROPERTY_NAME_TEXT_ENABLED, DEFAULT_TEXT_ENABLED);
    }

    public final void setTextEnabled(boolean enabled) {
        try {
            getConfiguration().setValue(PROPERTY_NAME_TEXT_ENABLED, enabled);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Font getTextFont() {
        return getConfigurationProperty(PROPERTY_NAME_TEXT_FONT, DEFAULT_TEXT_FONT);
    }

    public final void setTextFont(Font font) {
        try {
            getConfiguration().setValue(PROPERTY_NAME_TEXT_FONT, font);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Color getTextFgColor() {
        return getConfigurationProperty(PROPERTY_NAME_TEXT_FG_COLOR, DEFAULT_TEXT_FG_COLOR);
    }

    public final void setTextFgColor(Color color) {
        try {
            getConfiguration().setValue(PROPERTY_NAME_TEXT_FG_COLOR, color);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Color getTextBgColor() {
        return getConfigurationProperty(PROPERTY_NAME_TEXT_BG_COLOR, DEFAULT_TEXT_BG_COLOR);
    }

    public final void setTextBgColor(Color color) {
        try {
            getConfiguration().setValue(PROPERTY_NAME_TEXT_BG_COLOR, color);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public void setOutlineColor(Color color) {
        final Pin[] placemarks = getPlacemarks();

        if (placemarks != null) {
            for (Pin pin : placemarks) {
                pin.getSymbol().setOutlineColor(color);
            }
            fireLayerDataChanged(getLayerModelBounds());
        }
    }

    public Color getOutlineColor() {
        Pin[] placemarks = getPlacemarks();
        Color color = null;
        if (placemarks != null) {
            color = placemarks[0].getSymbol().getOutlineColor();
            for (int i = 1; i < placemarks.length; i++) {
                Pin placemark = placemarks[i];
                if (color != null && !color.equals(placemark.getSymbol().getOutlineColor())) {
                    return null;
                }
            }
        }
        return color;
    }

    public void setFillColor(Color color) {
        final Pin[] placemarks = getPlacemarks();
        if (placemarks != null) {
            for (Pin pin : placemarks) {
                pin.getSymbol().setFillPaint(color);
            }
            fireLayerDataChanged(getLayerModelBounds());
        }
    }

    public Color getFillColor() {
        Pin[] placemarks = getPlacemarks();
        Color color = null;
        if (placemarks != null) {
            color = (Color) placemarks[0].getSymbol().getFillPaint();
            for (int i = 1; i < placemarks.length; i++) {
                Pin placemark = placemarks[i];
                if (color != null && !color.equals(placemark.getSymbol().getFillPaint())) {
                    return null;
                }
            }
        }
        return color;
    }

    private Pin[] getPlacemarks() {
        final ProductNodeGroup<Pin> placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
        Collection<Pin> pinCollection = placemarkGroup.getSelectedNodes();

        Pin[] placemarks;
        if (pinCollection.isEmpty()) {
            placemarks = placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]);
        } else {
            placemarks = pinCollection.toArray(new Pin[pinCollection.size()]);
        }
        if (placemarks.length >= 1) {
            return placemarks;
        }
        return null;
    }


    private class MyProductNodeListenerAdapter extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            maybeFireLayerDataChanged(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            maybeFireLayerDataChanged(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            maybeFireLayerDataChanged(event);
        }

        private void maybeFireLayerDataChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Pin &&
                !ProductNode.PROPERTY_NAME_OWNER.equals(event.getPropertyName())) {
                fireLayerDataChanged(getLayerModelBounds());
            }
        }

    }
}
