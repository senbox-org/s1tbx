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
import java.awt.BorderLayout;
import java.awt.Container;
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

    private JPanel control;
    private Map<Container, LayerManagerForm> layerManagerMap;

    @Override
    protected JComponent createControl() {
        control = new JPanel(new BorderLayout());
        layerManagerMap = new HashMap<Container, LayerManagerForm>();

        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        setProductSceneView(sceneView);

        VisatApp.getApp().addInternalFrameListener(new LayerManagerIFL());

        return control;
    }

    private void setProductSceneView(final ProductSceneView sceneView) {
        if (control.getComponentCount() > 0) {
            control.remove(0);
        }
        if (sceneView != null) {
            final LayerManagerForm layerManagerForm;
            if (layerManagerMap.containsKey(sceneView)) {
                layerManagerForm = layerManagerMap.get(sceneView);
            } else {
                layerManagerForm = new LayerManagerForm(sceneView.getRootLayer(), new BandLayerProvider(this, sceneView.getProduct()));
                layerManagerMap.put(sceneView, layerManagerForm);
            }
            control.add(layerManagerForm.getControl(), BorderLayout.CENTER);
        }
        control.validate();
        control.repaint();
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
