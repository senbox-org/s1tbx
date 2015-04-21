/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.toolviews.nestwwview;

import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Earth.MSVirtualEarthLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.util.SystemUtils;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;

@TopComponent.Description(
        preferredID = "WorldWindTopComponent",
        //iconBase = "org/esa/snap/rcp/icons/xxx.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = true,
        position = 3
)
@ActionID(category = "Window", id = "org.esa.s1tbx.dat.toolviews.nestwwview.FlatEarthWWToolView")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows"),
        @ActionReference(path = "Toolbars/Views")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_WorldWindTopComponentName",
        preferredID = "WorldWindTopComponent"
)
@NbBundle.Messages({
        "CTL_WorldWindTopComponentName=WorldWind View",
        "CTL_WorldWindTopComponentDescription=WorldWind World Map",
})
/**
 * The window displaying the world map.
 */
public class FlatEarthWWToolView extends ToolTopComponent implements WWView {

    private final SnapApp datApp = SnapApp.getDefault();
    private final Dimension canvasSize = new Dimension(800, 600);
    private AppPanel wwjPanel = null;

    private ProductLayer productLayer = null;
    public Level2ProductLayer level2ProductLayer;
    private Position eyePosition = null;
    private ProductSceneView currentView;
    private ObservedViewportHandler observedViewportHandler;

    private static final boolean includeStatusBar = true;
    private final static String useflatWorld = System.getProperty(SystemUtils.getApplicationContextId() + ".use.flat.worldmap");
    private final static boolean flatWorld = false;//!(useflatWorld != null && useflatWorld.equals("false"));

    public FlatEarthWWToolView() {
        setDisplayName("WorldWind");
        initComponents();
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler((oldValue, newValue) -> setCurrentView(newValue));
    }

    private void initComponents() {
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
    }

    public JComponent createControl() {

        final Window windowPane = SwingUtilities.getWindowAncestor(this);
        if (windowPane != null)
            windowPane.setSize(300, 300);
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setSize(new Dimension(300, 300));

        // world wind canvas
        initialize(mainPane);

        observedViewportHandler = new ObservedViewportHandler();

        return mainPane;
    }

    WorldWindowGLCanvas getWwd() {
        if (wwjPanel == null)
            return null;
        return wwjPanel.getWwd();
    }

    private void initialize(final JPanel mainPane) {
        final WWView toolView = this;

        final SwingWorker worker = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                // Create the WorldWindow.
                try {
                    wwjPanel = new AppPanel(canvasSize, includeStatusBar);
                    wwjPanel.setPreferredSize(canvasSize);

                    // Put the pieces together.
                    mainPane.add(wwjPanel, BorderLayout.CENTER);

                    final LayerList layerList = getWwd().getModel().getLayers();

                    final MSVirtualEarthLayer virtualEarthLayerA = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_AERIAL);
                    virtualEarthLayerA.setName("MS Bing Aerial");
                    layerList.add(virtualEarthLayerA);

                    productLayer = new ProductLayer(false);
                    productLayer.setOpacity(1.0);
                    productLayer.setPickEnabled(false);
                    productLayer.setName("Opened Products");
                    layerList.add(productLayer);

                    level2ProductLayer = new Level2ProductLayer();

                    final Layer placeNameLayer = layerList.getLayerByName("Place Names");
                    placeNameLayer.setEnabled(true);

                    final SnapApp snapApp = SnapApp.getDefault();
                    snapApp.getProductManager().addListener(new WWProductManagerListener(toolView));
                    snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
                        @Override
                        public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                            setSelectedProduct(newValue.getProduct());
                        }
                    });
                    setProducts(snapApp.getProductManager().getProducts());
                    setSelectedProduct(snapApp.getSelectedProduct());

                    setProducts(datApp.getProductManager().getProducts());
                    setSelectedProduct(datApp.getSelectedProduct());
                } catch (Throwable e) {
                    System.out.println("Can't load openGL " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void gotoProduct(Product product) {
        if (product == null) return;

        final View theView = getWwd().getView();
        final Position origPos = theView.getEyePosition();
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding != null) {
            final GeoPos centre = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() / 2,
                    product.getSceneRasterHeight() / 2), null);
            centre.normalize();
            theView.setEyePosition(Position.fromDegrees(centre.getLat(), centre.getLon(), origPos.getElevation()));
        }
    }

    public void setCurrentView(final ProductSceneView newView) {
        if (currentView != newView) {
            final ProductSceneView oldView = currentView;
            currentView = newView;

            if (oldView != null) {
                final Viewport observedViewport = oldView.getLayerCanvas().getViewport();
                if (observedViewport != null)
                    observedViewport.removeListener(observedViewportHandler);
            }
            if (newView != null) {
                final Viewport observedViewport = newView.getLayerCanvas().getViewport();
                if (observedViewport != null)
                    observedViewport.addListener(observedViewportHandler);
            }
            //updateState();
        }
    }

    public void setSelectedProduct(final Product product) {
        if (product == getSelectedProduct() && eyePosition == getWwd().getView().getEyePosition())
            return;
        if (productLayer != null)
            productLayer.setSelectedProduct(product);

        if (isVisible()) {
            gotoProduct(product);
            getWwd().redrawNow();
            eyePosition = getWwd().getView().getEyePosition();
        }
    }

    public Product getSelectedProduct() {
        if (productLayer != null)
            return productLayer.getSelectedProduct();
        return null;
    }

    public void setProducts(Product[] products) {
        if (productLayer != null) {
            for (Product prod : products) {
                try {
                    //if ((prod.getName().indexOf("S1A_S1_OCN_") >= 0 || prod.getName().indexOf("003197_05B7") >= 0)) {
                    //    level2ProductLayer.addProduct(prod, getWwd());
                    //}
                    //else {
                        productLayer.addProduct(prod, getWwd());
                    //}
                } catch (Exception e) {
                    datApp.handleError("WorldWind unable to add product " + prod.getName(), e);
                }
            }
        }
        if (isVisible()) {
            getWwd().redrawNow();
        }
    }

    public void removeProduct(Product product) {
        if (getSelectedProduct() == product)
            setSelectedProduct(null);


        if (level2ProductLayer != null && (product.getName().indexOf("S1A_S1_OCN_") >= 0 || product.getName().indexOf("003197_05B7") >= 0)) {
            level2ProductLayer.removeProduct(product);
        }
        else if (productLayer != null)
            productLayer.removeProduct(product);

        if (isVisible()) {
            getWwd().redrawNow();
        }
    }

    public static class AppPanel extends JPanel {
        private WorldWindowGLCanvas wwd = null;
        private StatusBar statusBar = null;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar) {
            super(new BorderLayout());

            this.wwd = new WorldWindowGLCanvas();
            //this.wwd.setPreferredSize(canvasSize);
            //wwd.set
            // Create the default model as described in the current worldwind properties.            
            final Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);
            if (flatWorld) {
                m.setGlobe(new EarthFlat());
                this.wwd.setView(new FlatOrbitView());
            }

            final LayerList layerList = m.getLayers();
            for (Layer layer : layerList) {
                if (layer instanceof CompassLayer || layer instanceof WorldMapLayer ||
                        layer instanceof LandsatI3WMSLayer || layer instanceof SkyGradientLayer)
                    layerList.remove(layer);
            }

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));

            this.add(this.wwd, BorderLayout.CENTER);

            if (includeStatusBar) {
                this.statusBar = new MinimalStatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }
        }

        public final WorldWindowGLCanvas getWwd() {
            return wwd;
        }

        public final StatusBar getStatusBar() {
            return statusBar;
        }
    }

    private class WWIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            Product product = null;
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                setCurrentView(view);
                product = view.getProduct();
            }
            setSelectedProduct(product);
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
        }
    }

    private class ObservedViewportHandler implements ViewportListener {

        @Override
        public void handleViewportChanged(Viewport observedViewport, boolean orientationChanged) {
            /*if (!adjustingObservedViewport) {
                if (orientationChanged) {
                    thumbnailCanvas.getViewport().setOrientation(observedViewport.getOrientation());
                    thumbnailCanvas.zoomAll();
                }
                updateMoveSliderRect();
            }  */
        }
    }
}
