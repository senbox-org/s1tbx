/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.ocean.toolviews.polarview;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.*;


@TopComponent.Description(
        preferredID = "OceanSwellTopComponent",
        iconBase = "org/esa/s1tbx/ocean/icons/ocean-swell22.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 60
)
@ActionID(category = "Window", id = "org.esa.s1tbx.ocean.toolviews.polarview.OceanSwellTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows/Radar"),
        @ActionReference(path = "Toolbars/Radar Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_OceanSwellTopComponentName",
        preferredID = "OceanSwellTopComponent"
)
@NbBundle.Messages({
        "CTL_OceanSwellTopComponentName=Ocean Swell",
        "CTL_OceanSwellTopComponentDescription=Level2 Ocean Swell Polar View"
})
/**
 * The window displaying the Level-2 OSW polar plot
 *
 */
public class OceanSwellTopComponent extends ToolTopComponent {

    private PolarView polarView;

    public OceanSwellTopComponent() {
        setDisplayName("Ocean Swell");
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
    }

    public JComponent createControl() {

        polarView = new PolarView();

        // update world map window with the information of the currently activated  product scene view.
        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new OSWProductManagerListener(polarView));
        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if (newValue != null) {
                    polarView.setProduct(newValue.getProduct());
                }
            }
        });
        polarView.setProduct(snapApp.getSelectedProduct(AUTO));

        return polarView;
    }

    void setSelectedRecord(final Product product, final int recNum) {
        polarView.setProduct(product);
        polarView.showPlot(recNum);
    }

    public class OSWProductManagerListener implements ProductManager.Listener {

        private final PolarView polarView;

        public OSWProductManagerListener(final PolarView polarView) {
            this.polarView = polarView;
        }

        @Override
        public void productAdded(ProductManager.Event event) {
            polarView.setProduct(event.getProduct());
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            polarView.removeProduct(event.getProduct());
        }
    }

    public static void setOSWRecord(final Product product, final int recNum) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final OceanSwellTopComponent window = (OceanSwellTopComponent)
                        WindowManager.getDefault().findTopComponent("OceanSwellTopComponent");
                if(window != null) {
                    window.open();
                    window.requestActive();

                    window.setSelectedRecord(product, recNum);
                }
            }
        });
    }
}
