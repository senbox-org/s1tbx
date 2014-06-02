/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract action for toggling the display of overlays.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public abstract class AbstractShowOverlayAction extends ExecCommand {
    private AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public final void updateState(CommandEvent event) {
        final VisatApp visatApp = VisatApp.getApp();

        if (initialized.compareAndSet(false, true)) {
            initialize();
        }

        updateState(visatApp.getSelectedProductSceneView());
    }

    private void updateState(ProductSceneView view) {
        if (view != null) {
            updateEnableState(view);
            updateSelectState(view);
        } else {
            setEnabled(false);
            setSelected(false);
        }
    }

    /**
     * Called when the action should update its 'enable' state for the product
     * scene view selected in VISAT.
     * <p/>
     * This method can contain some code which analyzes the given scene view
     * and makes a decision whether the action should be enabled or disabled.
     *
     * @param view the product scene view selected in VISAT.
     */
    protected abstract void updateEnableState(ProductSceneView view);

    /**
     * Called when the action should update its 'select' state for the product
     * scene view selected in VISAT.
     *
     * @param view the product scene view selected in VISAT.
     */
    protected abstract void updateSelectState(ProductSceneView view);

    private void initialize() {
        VisatApp.getApp().addInternalFrameListener(new IFL());
    }

    private class IFL extends InternalFrameAdapter {
        private final Map<ProductSceneView, LCL> layerContentListenerMap;

        public IFL() {
            layerContentListenerMap = new HashMap<ProductSceneView, LCL>();
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e.getInternalFrame());

            if (view != null) {
                if (!layerContentListenerMap.containsKey(view)) {
                    final LCL layerContentListener = new LCL(view);

                    view.getRootLayer().addListener(layerContentListener);
                    layerContentListenerMap.put(view, layerContentListener);
                }
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            updateState(getProductSceneView(e.getInternalFrame()));
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e.getInternalFrame());

            if (view != null) {
                if (layerContentListenerMap.containsKey(view)) {
                    view.getRootLayer().removeListener(layerContentListenerMap.get(view));
                }
            }
        }

        private ProductSceneView getProductSceneView(JInternalFrame internalFrame) {
            final Container contentPane = internalFrame.getContentPane();

            if (contentPane instanceof ProductSceneView) {
                return (ProductSceneView) contentPane;
            }

            return null;
        }
    }

    private class LCL extends AbstractLayerListener {
        private final ProductSceneView view;

        public LCL(ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            if ("visible".equals(event.getPropertyName())) {
                updateSelectState(view);
            }
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            updateEnableState(view);
            updateSelectState(view);
        }
    }
}
