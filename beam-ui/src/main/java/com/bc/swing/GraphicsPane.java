/*
 * $Id: GraphicsPane.java,v 1.2 2007/04/23 13:41:24 marcop Exp $
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
package com.bc.swing;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

import com.bc.layer.DefaultLayerModel;
import com.bc.layer.LayerModel;
import com.bc.layer.LayerModelChangeAdapter;
import com.bc.view.DefaultViewModel;
import com.bc.view.ScrollableView;
import com.bc.view.ViewModel;
import com.bc.view.ViewModelChangeListener;

/**
 * A swing component which combines a {@link ViewModel} and a {@link LayerModel} in order to provide a scrollable
 * graphics pane.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class GraphicsPane extends JComponent implements ScrollableView {

    private static final long serialVersionUID = -862357460530470103L;

    public static final Stroke PLAIN_STROKE_0 = new BasicStroke(0.0f);
    public static final Stroke PLAIN_STROKE_05 = new BasicStroke(0.5f);
    public static final Stroke PLAIN_STROKE_1 = new BasicStroke(1.0f);

    private LayerModel layerModel;
    private LayerModelHandler layerModelHandler;

    private ViewModel viewModel;
    private ViewModelHandler viewModelHandler;

    public GraphicsPane() {
        this(new DefaultLayerModel(), new DefaultViewModel());
    }

    public GraphicsPane(LayerModel layerModel) {
        this(layerModel, new DefaultViewModel());
    }

    public GraphicsPane(LayerModel layerModel, ViewModel viewModel) {
        setOpaque(false);
        this.viewModelHandler = new ViewModelHandler();
        this.viewModel = viewModel;
        if (this.viewModel != null) {
            this.viewModel.addViewModelChangeListener(this.viewModelHandler);
        }
        this.layerModelHandler = new LayerModelHandler();
        this.layerModel = layerModel;
        if (this.layerModel != null) {
            this.layerModel.addLayerModelChangeListener(this.layerModelHandler);
        }
    }

    public void dispose() {
        if (viewModel != null) {
            viewModel.removeViewModelChangeListener(viewModelHandler);
            viewModel = null;
        }
        if (layerModel != null) {
            layerModel.removeLayerModelChangeListener(layerModelHandler);
            layerModel = null;
        }
    }

    public ViewPane createViewPane() {
        final ViewPane viewPane = new ViewPane(this);
        URL resource = GraphicsPane.class.getResource("/com/bc/swing/icons/zoom_all.gif");
        final ImageIcon icon = new ImageIcon(resource);
        final JButton corner = new JButton(icon);
        corner.setToolTipText("Zoom all."); /*I18N*/
        corner.setBorder(null);
        corner.setSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        corner.setPreferredSize(corner.getSize());
        corner.setRequestFocusEnabled(false);
        corner.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoomAll();
            }
        });
        viewPane.setCornerComponent(corner);
        return viewPane;
    }

    /**
     * Gets the view model.
     *
     * @return the view model, never null
     */
    public ViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the view model.
     *
     * @param viewModel the view model, never null
     */
    public void setViewModel(ViewModel viewModel) {
        ViewModel viewModelOld = this.viewModel;
        if (viewModelOld != viewModel) {
            if (viewModelOld != null) {
                viewModelOld.removeViewModelChangeListener(viewModelHandler);
            }
            this.viewModel = viewModel;
            if (this.viewModel != null) {
                this.viewModel.addViewModelChangeListener(viewModelHandler);
            }
            firePropertyChange("viewModel", viewModelOld, this.viewModel);
        }
    }

    public LayerModel getLayerModel() {
        return layerModel;
    }

    public void setLayerModel(LayerModel layerModel) {
        LayerModel layerModelOld = this.layerModel;
        if (layerModelOld != layerModel) {
            if (layerModelOld != null) {
                layerModelOld.removeLayerModelChangeListener(layerModelHandler);
            }
            this.layerModel = layerModel;
            if (this.layerModel != null) {
                this.layerModel.addLayerModelChangeListener(layerModelHandler);
            }
            firePropertyChange("layerModel", layerModelOld, layerModel);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Utilities

    public void setModelAreaFromLayerModel() {
        final Rectangle2D visibleBoundingBox = getLayerModel().getVisibleBoundingBox(null);
        getViewModel().setModelArea(visibleBoundingBox);
    }

    public void zoom(double viewScale) {
        final double modelOffsetXOld = viewModel.getModelOffsetX();
        final double modelOffsetYOld = viewModel.getModelOffsetY();
        final double viewScaleOld = viewModel.getViewScale();
        final double viewportWidth = getWidth();
        final double viewportHeight = getHeight();
        final double centerX = modelOffsetXOld + 0.5 * viewportWidth / viewScaleOld;
        final double centerY = modelOffsetYOld + 0.5 * viewportHeight / viewScaleOld;
        zoom(centerX, centerY, viewScale);
    }

    public void zoom(Rectangle2D zoomRect) {
        final double viewportWidth = getWidth();
        final double viewportHeight = getHeight();
        zoom(zoomRect.getCenterX(),
             zoomRect.getCenterY(),
             Math.min(viewportWidth / zoomRect.getWidth(),
                      viewportHeight / zoomRect.getHeight()));
    }

    public void zoom(double centerX, double centerY, double viewScale) {
        viewScale = cropViewScale(viewScale);
        final double viewportWidth = getWidth();
        final double viewportHeight = getHeight();
        final double modelOffsetX = centerX - 0.5 * viewportWidth / viewScale;
        final double modelOffsetY = centerY - 0.5 * viewportHeight / viewScale;
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY, viewScale);
    }

    public void zoomAll() {
        final double viewportWidth = getWidth();
        final double viewportHeight = getHeight();
        final Rectangle2D modelArea = viewModel.getModelArea();
        final double viewScale = cropViewScale(Math.min(viewportWidth / modelArea.getWidth(),
                                                        viewportHeight / modelArea.getHeight()));
        final double viewportWidth1 = getWidth();
        final double viewportHeight1 = getHeight();
        final double modelOffsetX = modelArea.getX() + 0.5 * modelArea.getWidth() - 0.5 * viewportWidth1 / viewScale;
        final double modelOffsetY = modelArea.getY() + 0.5 * modelArea.getHeight() - 0.5 * viewportHeight1 / viewScale;
        getViewModel().setModelOffset(modelOffsetX, modelOffsetY, viewScale);
    }

    public double viewToModelX(double viewX) {
        return viewModel.getModelOffsetX() + viewToModelLength(viewX);
    }

    public double viewToModelY(double viewY) {
        return viewModel.getModelOffsetY() + viewToModelLength(viewY);
    }

    public double viewToModelLength(double viewLength) {
        return viewLength / viewModel.getViewScale();
    }

    public double modelToViewX(double modelX) {
        return modelToViewLength(modelX - viewModel.getModelOffsetX());
    }

    public double modelToViewY(double modelY) {
        return modelToViewLength(modelY - viewModel.getModelOffsetY());
    }

    public double modelToViewLength(double modelLength) {
        return modelLength * viewModel.getViewScale();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // JComponent Overrides

    /**
     * If you override this in a subclass you should not make permanent changes to the passed in <code>Graphics</code>.
     * For example, you should not alter the clip <code>Rectangle</code> or modify the transform. If you need to do
     * these operations you may find it easier to create a new <code>Graphics</code> from the passed in
     * <code>Graphics</code> and manipulate it. Further, if you do not invoker super's implementation you must honor the
     * opaque property, that is if this component is opaque, you must completely fill in the background in a non-opaque
     * color. If you do not honor the opaque property you will likely see visual artifacts.
     *
     * @param g the <code>Graphics</code> object to protect
     *
     * @see #paint
     * @see javax.swing.plaf.ComponentUI
     */
    @Override
    protected void paintComponent(Graphics g) {
        // honor the opaque property
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (g instanceof Graphics2D) {
            drawLayers((Graphics2D) g);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Drawing

    public void drawLayers(Graphics2D g2d) {
        // create a new Graphics because we shall not alter the Graphics passed in
        final Graphics2D g2dClone = (Graphics2D) g2d.create();
        transformGraphics(g2dClone, true);
        getLayerModel().draw(g2dClone, viewModel);
        g2dClone.dispose();
    }

    public void transformGraphics(final Graphics2D g2d, boolean forward) {
        if (forward) {
            // forward transform
            g2d.scale(viewModel.getViewScale(), viewModel.getViewScale());
            g2d.translate(-viewModel.getModelOffsetX(), -viewModel.getModelOffsetY());
        } else {
            // inverse transform
            g2d.translate(viewModel.getModelOffsetX(), viewModel.getModelOffsetY());
            g2d.scale(1.0 / viewModel.getViewScale(), 1.0 / viewModel.getViewScale());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Helpers

    private double cropViewScale(double viewScale) {
        return DefaultViewModel.cropViewScale(viewScale, viewModel.getViewScaleMax());
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Inner Classes

    private class LayerModelHandler extends LayerModelChangeAdapter {

        @Override
        public void handleLayerModelChanged(LayerModel layerModel) {
            repaint();
        }
    }

    private class ViewModelHandler implements ViewModelChangeListener {

        public void handleViewModelChanged(ViewModel viewModel) {
            repaint();
        }
    }
}
