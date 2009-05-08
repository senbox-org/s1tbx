/*
 * $Id: ExportImageAction.java,v 1.2 2007/02/09 11:05:57 marcop Exp $
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
package org.esa.beam.visat.actions;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditorsPane;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ExportImageAction extends AbstractExportImageAction {

    private JRadioButton buttonEntireImage;
    private SizeComponent sizeComponent;

    @Override
    public void actionPerformed(CommandEvent event) {
        exportImage(getVisatApp(), getSceneImageFileFilters(), event.getSelectableCommand());
    }

    @Override
    public void updateState(final CommandEvent event) {
        boolean enabled = getVisatApp().getSelectedProductSceneView() != null;
        event.getSelectableCommand().setEnabled(enabled);

    }

    @Override
    protected void configureFileChooser(BeamFileChooser fileChooser, ProductSceneView view, String imageBaseName) {
        fileChooser.setDialogTitle(getVisatApp().getAppName() + " - " + "Export Image"); /*I18N*/
        if (view.isRGB()) {
            fileChooser.setCurrentFilename(imageBaseName + "_RGB");
        } else {
            fileChooser.setCurrentFilename(imageBaseName + "_" + view.getRaster().getName());
        }
        final JPanel regionPanel = new JPanel(new GridLayout(2, 1));
        regionPanel.setBorder(BorderFactory.createTitledBorder("Region")); /*I18N*/
        buttonEntireImage = new JRadioButton("Entire image", true);
        final JRadioButton buttonClippingOnly = new JRadioButton("Clipping only", false); /*I18N*/
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(buttonEntireImage);
        buttonGroup.add(buttonClippingOnly);
        regionPanel.add(buttonEntireImage);
        regionPanel.add(buttonClippingOnly);
        sizeComponent = new SizeComponent(view);
        JComponent sizePanel = sizeComponent.createComponent();
        sizePanel.setBorder(BorderFactory.createTitledBorder("Size")); /*I18N*/
        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(regionPanel);
        accessory.add(sizePanel);
        fileChooser.setAccessory(accessory);
        
        buttonEntireImage.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                sizeComponent.updateDimensions();
            }});
    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        final boolean useAlpha = !"BMP".equals(imageFormat);
        final boolean entireImage = isEntireImageSelected();
        final LayerFilter layerFilter = new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer instanceof ImageLayer;
            }
        };
        return createImage(view, entireImage, useAlpha, layerFilter, sizeComponent.getDimension());
    }
    


    static RenderedImage createImage(ProductSceneView view, boolean entireImage, boolean useAlpha, LayerFilter layerFilter, Dimension imageDim) {
        Rectangle2D modelBounds;
        final ImageLayer imageLayer = view.getBaseImageLayer();
        if (entireImage) {
            modelBounds = imageLayer.getModelBounds();
        } else {
            final RenderedImage image = imageLayer.getImage();
            final Rectangle2D imageBounds = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
            final AffineTransform i2mTransform = imageLayer.getImageToModelTransform();
            final Rectangle2D modelImageArea = i2mTransform.createTransformedShape(imageBounds).getBounds2D();

            modelBounds = new Rectangle2D.Double();
            Rectangle2D.intersect(view.getVisibleModelBounds(), modelImageArea, modelBounds);
        }
        final int imageWidth;
        final int imageHeight;
        if (imageDim != null) {
            imageWidth = imageDim.width;
            imageHeight = imageDim.height;
        } else {
            Rectangle2D imageBounds = imageLayer.getModelToImageTransform().createTransformedShape(modelBounds).getBounds2D() ;
            int w = MathUtils.floorInt(imageBounds.getWidth());
            int h = MathUtils.floorInt(imageBounds.getHeight());
            Dimension truncateImageSize = truncateImageSize(w, h);
            imageWidth = truncateImageSize.width;
            imageHeight = truncateImageSize.height;
        }
        
        
        final int imageType = useAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        final BufferedImage bi = new BufferedImage(imageWidth, imageHeight, imageType);
        boolean isModelYAxisDown = view.getLayerCanvas().getViewport().isModelYAxisDown();
        Viewport snapshotVp = new DefaultViewport(isModelYAxisDown);
        final BufferedImageRendering imageRendering = new BufferedImageRendering(bi, snapshotVp);

        if (!useAlpha) {
            final Graphics2D graphics = imageRendering.getGraphics();
            graphics.setColor(view.getBackground());
            graphics.fillRect(0, 0, imageWidth, imageHeight);
        }

        snapshotVp.zoom(modelBounds);
        snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);

        view.getRootLayer().render(imageRendering, layerFilter);
        return bi;
    }

    @Override
    protected boolean isEntireImageSelected() {
        return buttonEntireImage.isSelected();
    }
    
    private class SizeComponent {
        
        private static final String PROPERTY_HEIGHT = "height";
        private static final String PROPERTY_WIDTH = "width";
        private static final String PROPERTY_PRESERVE_RATIO = "preserveAspectRatio";
        
        private final ValueContainer valueContainer;
        private final ProductSceneView view;
        private double ratio;
        
        private int width;
        private int height;
        private boolean preserveAspectRatio = true;
        
        public SizeComponent(ProductSceneView view) {
            this.view = view;
            valueContainer = createValueContainer();
            updateDimensions();
        }
        
        public void updateDimensions() {
            Rectangle2D modelBounds;
            final ImageLayer imageLayer = view.getBaseImageLayer();
            if (isEntireImageSelected()) {
                modelBounds = imageLayer.getModelBounds();
            } else {
                final RenderedImage image = imageLayer.getImage();
                final Rectangle2D imageBounds = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
                final AffineTransform i2mTransform = imageLayer.getImageToModelTransform();
                final Rectangle2D modelImageArea = i2mTransform.createTransformedShape(imageBounds).getBounds2D();

                modelBounds = new Rectangle2D.Double();
                Rectangle2D.intersect(view.getVisibleModelBounds(), modelImageArea, modelBounds);
            }

            Rectangle2D bounds = imageLayer.getModelToImageTransform().createTransformedShape(modelBounds).getBounds2D() ;
            ratio = bounds.getWidth() / bounds.getHeight();
            Dimension imageSize = truncateImageSize(MathUtils.floorInt(bounds.getWidth()), MathUtils.floorInt(bounds.getHeight()));
            update(PROPERTY_WIDTH, imageSize.width);
            update(PROPERTY_HEIGHT, imageSize.height);
        }

        public JComponent createComponent() {
            BindingContext bindingContext = new BindingContext(valueContainer);
            ValueEditorsPane valueEditorsPane = new ValueEditorsPane(bindingContext);
            return valueEditorsPane.createPanel();
        }
        
        public Dimension getDimension() {
            return new Dimension(width, height);
        }
        
        private ValueContainer createValueContainer() {
            final ValueContainer container = ValueContainer.createObjectBacked(this, new DescriptorFactory());
            PropertyChangeListener listener = new PropertyChangeListener() {
                
                private boolean isAdjusting = false;

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (preserveAspectRatio && !isAdjusting) {
                        isAdjusting = true;
                        if (evt.getPropertyName().equals(PROPERTY_WIDTH)) {
                            adjustHeight();
                        } else if (evt.getPropertyName().equals(PROPERTY_HEIGHT)) {
                            adjustWidth();
                        } else if (evt.getPropertyName().equals(PROPERTY_PRESERVE_RATIO)) {
                            if (width > height) {
                                adjustHeight();
                            } else {
                                adjustWidth();
                            }
                        }
                        isAdjusting = false;
                    }
                }
            };
            container.addPropertyChangeListener(PROPERTY_HEIGHT, listener);
            container.addPropertyChangeListener(PROPERTY_WIDTH, listener);
            return container;
        }
        
        private void adjustWidth() {
            update(PROPERTY_WIDTH, MathUtils.floorInt(height * ratio));
        }
        
        private void adjustHeight() {
            update(PROPERTY_HEIGHT, MathUtils.floorInt(width / ratio));
        }
        
        private void update(String propertyName, int intValue) {
            try {
                valueContainer.setValue(propertyName, intValue);
            } catch (ValidationException e) {
            }
        }

    }
    
    private class DescriptorFactory implements ClassFieldDescriptorFactory {

        @Override
        public ValueDescriptor createValueDescriptor(Field field) {
            String name = field.getName();
            if (name.equals(SizeComponent.PROPERTY_HEIGHT) ||
                    name.equals(SizeComponent.PROPERTY_WIDTH) ||
                    name.equals(SizeComponent.PROPERTY_PRESERVE_RATIO)) {
                return new ValueDescriptor(name, field.getType());
            } else {
                return null;
            }
        }
    }
}
