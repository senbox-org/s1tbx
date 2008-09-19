/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Layer manager tool view.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class LayerManagerToolView extends AbstractToolView {

    private JPanel panel;
    private Map<Container, LayerManager> layerManagerMap;

    @Override
    protected JComponent createControl() {
        panel = new JPanel(new BorderLayout());
        layerManagerMap = new HashMap<Container, LayerManager>();

        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        setProductSceneView(sceneView);

        VisatApp.getApp().addInternalFrameListener(new LayerManagerIFL());

        return panel;
    }

    private void setProductSceneView(final ProductSceneView sceneView) {
        if (panel.getComponentCount() > 0) {
            panel.remove(0);
        }
        if (sceneView != null) {
            final LayerManager layerManager;
            if (layerManagerMap.containsKey(sceneView)) {
                layerManager = layerManagerMap.get(sceneView);
            } else {
                  layerManager = new LayerManager(sceneView.getRootLayer());
                layerManagerMap.put(sceneView, layerManager);
            }
            panel.add(layerManager.getControl(), BorderLayout.CENTER);
        }
        panel.validate();
        panel.repaint();
    }

    private class LayerManagerIFL extends InternalFrameAdapter {
        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container container = e.getInternalFrame().getContentPane();

            if (container instanceof ProductSceneView) {
                setProductSceneView((ProductSceneView) container);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container container = e.getInternalFrame().getContentPane();

            if (container instanceof ProductSceneView) {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();

            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView selectedSceneView = VisatApp.getApp().getSelectedProductSceneView();
                setProductSceneView(selectedSceneView);
                layerManagerMap.remove(contentPane);
            }
        }
    }
}
