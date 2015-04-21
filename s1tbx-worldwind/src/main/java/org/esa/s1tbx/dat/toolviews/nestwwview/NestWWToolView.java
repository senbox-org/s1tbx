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

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Earth.MSVirtualEarthLayer;
import gov.nasa.worldwind.layers.Earth.OSMMapnikLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.WMSBasicElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.wms.CapabilitiesRequest;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import gov.nasa.worldwindx.examples.WMSLayersPanel;
<<<<<<< HEAD:s1tbx-worldwind/src/main/java/org/esa/nest/dat/toolviews/nestwwview/NestWWToolView.java
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
=======
import gov.nasa.worldwindx.examples.util.DirectedPath;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.application.support.AbstractToolView;
import org.esa.snap.framework.ui.product.ProductSceneView;
>>>>>>> 4ae7dc0a4abc883fe388c9770991b1a520aa43e3:s1tbx-worldwind/src/main/java/org/esa/s1tbx/dat/toolviews/nestwwview/NestWWToolView.java
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// CHANGED
//import gov.nasa.worldwind.layers.Earth.OpenStreetMapLayer;
// ADDED

@TopComponent.Description(
        preferredID = "WorldWind3DTopComponent",
        //iconBase = "org/esa/snap/rcp/icons/xxx.gif",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = true,
        position = 3
)
@ActionID(category = "Window", id = "org.esa.nest.dat.toolviews.nestwwview.NestWWToolView")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows"),
        @ActionReference(path = "Toolbars/Views")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_WorldWind3DTopComponentName",
        preferredID = "WorldWind3DTopComponent"
)
@NbBundle.Messages({
        "CTL_WorldWind3DTopComponentName=WorldWind 3D View",
        "CTL_WorldWind3DTopComponentDescription=WorldWind 3D World Map",
})

/**
 * The window displaying the world map.
 *
 * @version $Revision: 1.22 $ $Date: 2011-11-02 23:04:54 $
 */
public class NestWWToolView extends ToolTopComponent implements WWView {

    //private static final String loadDEMCommand = "loadDEM";
    //private static final ImageIcon loadDEMIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/dem24.gif");

    private final Dimension canvasSize = new Dimension(800, 600);

    private AppPanel wwjPanel = null;
    private LayerPanel layerPanel = null;
    private ProductPanel productPanel = null;

    //private JSlider opacitySlider = null;

    private ProductLayer productLayer = null;
    public Level2ProductLayer level2ProductLayer;
    
    private final Dimension wmsPanelSize = new Dimension(400, 600);

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private int previousTabIndex = 0;

    private static final boolean includeStatusBar = true;
    private static final boolean includeLayerPanel = false;
    private static final boolean includeProductPanel = true;
    private static final boolean includeWMSPanel = false;



    private static final String[] servers = new String[]
            {
                    "http://neowms.sci.gsfc.nasa.gov/wms/wms",
                    //"http://mapserver.flightgear.org/cgi-bin/landcover",
                    "http://wms.jpl.nasa.gov/wms.cgi",
                    "http://worldwind46.arc.nasa.gov:8087/wms"
            };

    public NestWWToolView() {
        setDisplayName("WorldWind");
        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
        //SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler((oldValue, newValue) -> setCurrentView(newValue));
    }

    public JComponent createControl() {

        productLayer = new ProductLayer(true);
        level2ProductLayer = new Level2ProductLayer();
        // ADDED
        //productLayer.setMinActiveAltitude(3e6);
        //productLayer.setMaxActiveAltitude(4e6);

        final Window windowPane = SwingUtilities.getWindowAncestor(this);
        if (windowPane != null)
            windowPane.setSize(800, 400);
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setSize(new Dimension(300, 300));


        /*     JToolBar toolbar = new JToolBar();

          JButton loadDEMButton = new JButton();
          loadDEMButton.setName(getClass().getName() + loadDEMCommand);

          loadDEMButton = (JButton) ToolButtonFactory.createButton(loadDEMIcon, false);
          loadDEMButton.setBackground(mainPane.getBackground());
          loadDEMButton.setActionCommand(loadDEMCommand);
          loadDEMButton.setVisible(true);

          loadDEMButton.addActionListener(new ActionListener() {

              public void actionPerformed(final ActionEvent e) {
                  LoadDEM();
              }
          });
          toolbar.add(loadDEMButton);

          mainPane.add(toolbar, BorderLayout.NORTH); */

        // world wind canvas
        initialize(mainPane);
        if (wwjPanel == null) return mainPane;


        final MSVirtualEarthLayer virtualEarthLayerA = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_AERIAL);
        virtualEarthLayerA.setName("MS Bing Aerial");
        insertTiledLayer(getWwd(), virtualEarthLayerA);

        final MSVirtualEarthLayer virtualEarthLayerR = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_ROADS);
        virtualEarthLayerR.setName("MS Bing Roads");
        virtualEarthLayerR.setEnabled(false);
        insertTiledLayer(getWwd(), virtualEarthLayerR);

        final MSVirtualEarthLayer virtualEarthLayerH = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_HYBRID);
        virtualEarthLayerH.setName("MS Bing Hybrid");
        virtualEarthLayerH.setEnabled(false);
        insertTiledLayer(getWwd(), virtualEarthLayerH);
        // CHANGED
        //final OpenStreetMapLayer streetLayer = new OpenStreetMapLayer();
        final OSMMapnikLayer streetLayer = new OSMMapnikLayer();
        streetLayer.setOpacity(0.7);
        streetLayer.setEnabled(false);
        streetLayer.setName("Open Street Map");
        insertTiledLayer(getWwd(), streetLayer);

        productLayer.setOpacity(0.8);
        // CHANGED: otherwise the objects in the product layer won't react to the select listener
        //productLayer.setPickEnabled(false);
        productLayer.setName("Opened Products");

        level2ProductLayer.setOpacity(0.8);
        // CHANGED: otherwise the objects in the product layer won't react to the select listener
        //productLayer.setPickEnabled(false);
        level2ProductLayer.setName("Level 2 Products");

        insertTiledLayer(getWwd(), productLayer);
        insertTiledLayer(getWwd(), level2ProductLayer);

        // Add an internal frame listener to VISAT so that we can update our
        // world map window with the information of the currently activated  product scene view.
        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new WWProductManagerListener(this));
        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                setSelectedProduct(newValue.getProduct());
            }
        });
        setProducts(snapApp.getProductManager().getProducts());
        setSelectedProduct(snapApp.getSelectedProduct());

        return mainPane;
    }

    WorldWindowGLCanvas getWwd() {
        if (wwjPanel == null)
            return null;
        return wwjPanel.getWwd();
    }

    private static void insertTiledLayer(WorldWindow wwd, Layer layer) {
        int position = 0;
        final LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers) {
            if (l instanceof PlaceNameLayer) {
                position = layers.indexOf(l);
                break;
            }
        }
        layers.add(position, layer);
    }

    private void initialize(JPanel mainPane) {
        // ADDED
        System.out.println("INITIALIZE IN NestWWToolView CALLED" + " includeLayerPanel " + includeLayerPanel + " includeProductPanel " + includeProductPanel);

        // Create the WorldWindow.
        try {
            wwjPanel = new AppPanel(canvasSize, includeStatusBar);
            wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            mainPane.add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel) {
                layerPanel = new LayerPanel(wwjPanel.getWwd(), null);
                mainPane.add(layerPanel, BorderLayout.WEST);

                layerPanel.add(makeControlPanel(), BorderLayout.SOUTH);
                layerPanel.update(getWwd());
            }
            if (includeProductPanel) {
                productPanel = new ProductPanel(wwjPanel.getWwd(), productLayer);
                mainPane.add(productPanel, BorderLayout.WEST);

                productPanel.add(makeControlPanel(), BorderLayout.SOUTH);

                productPanel.update(getWwd());
            }
            if (includeWMSPanel) {
                tabbedPane.add(new JPanel());
                tabbedPane.setTitleAt(0, "+");
                tabbedPane.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent changeEvent) {
                        if (tabbedPane.getSelectedIndex() != 0) {
                            previousTabIndex = tabbedPane.getSelectedIndex();
                            return;
                        }

                        final String server = JOptionPane.showInputDialog("Enter WMS server URL");
                        if (server == null || server.length() < 1) {
                            tabbedPane.setSelectedIndex(previousTabIndex);
                            return;
                        }

                        // Respond by adding a new WMSLayerPanel to the tabbed pane.
                        if (addTab(tabbedPane.getTabCount(), server.trim()) != null)
                            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                    }
                });

                // Create a tab for each server and add it to the tabbed panel.
                for (int i = 0; i < servers.length; i++) {
                    this.addTab(i + 1, servers[i]); // i+1 to place all server tabs to the right of the Add Server tab
                }

                // Display the first server pane by default.
                this.tabbedPane.setSelectedIndex(this.tabbedPane.getTabCount() > 0 ? 1 : 0);
                this.previousTabIndex = this.tabbedPane.getSelectedIndex();

                mainPane.add(tabbedPane, BorderLayout.EAST);
            }

            // ADDED
            this.wwjPanel.getWwd().addSelectListener(new SelectListener()
            {

                public void selected(SelectEvent event)
                {
                    //System.out.println(event.getTopObject());
                    //System.out.println(event.getEventAction());
                    level2ProductLayer.updateInfoAnnotation(event);

                }
            });
            //level2ProductLayer.createColorBarLegend(0, 10, false, "OWI Wind Speed", "owi");
            //level2ProductLayer.createColorBarLegend(0, 10, true, "RVL Rad. Vel.", "rvl");


        } catch (Throwable e) {
            System.out.println("Can't load openGL " + e.getMessage());
        }
    }

    private JPanel makeControlPanel() {
        // CHANGED
        //final JPanel controlPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        //final JPanel mainControlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        final JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // THIS CODE WAS MOVED TO ProductLayer.getControlPanel
        /*
        opacitySlider = new JSlider();
        opacitySlider.setMaximum(100);
        opacitySlider.setValue((int) (productLayer.getOpacity() * 100));
        opacitySlider.setEnabled(true);
        opacitySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int value = opacitySlider.getValue();
                productLayer.setOpacity(value / 100d);
                getWwd().repaint();
            }
        });

        //theSelectedObjectLabel = new JLabel("Selected: ");

        final JPanel opacityPanel = new JPanel(new BorderLayout(5, 5));
        opacityPanel.add(new JLabel("Opacity"), BorderLayout.WEST);
        opacityPanel.add(this.opacitySlider, BorderLayout.CENTER);
        */

        JPanel opacityPanel = productLayer.getControlPanel(getWwd());
        controlPanel.add(opacityPanel);

        // ADDED
        JPanel controlLevel2Panel = level2ProductLayer.getControlPanel(getWwd());

        controlPanel.add(controlLevel2Panel);
        //mainControlPanel.add(controlPanel);
        //mainControlPanel.add(makeRVLPanel ());
        //controlPanel.add(makeRVLPanel ());

        //controlPanel.add(theSelectedObjectLabel);
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        return controlPanel;
    }


    private void LoadDEM() {

        //_eventListener.LoadDEM();
    }

    public void setSelectedProduct(Product product) {
        // ADDED
        System.out.println("SET SELECTED PRODUCT " + product);
        if (productLayer != null)
            productLayer.setSelectedProduct(product);
        if (productPanel != null)
            productPanel.update(getWwd());
        if (isVisible()) {
            getWwd().redrawNow();
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
                    if (level2ProductLayer != null && (prod.getName().indexOf("S1A_S1_OCN_") >= 0 || prod.getName().indexOf("003197_05B7") >= 0)) {
                        level2ProductLayer.addProduct(prod, getWwd());
                    }
                    else {
                        productLayer.addProduct(prod, getWwd());
                    }
                } catch (Exception e) {
                    SnapApp.getDefault().handleError("WorldWind unable to add product " + prod.getName(), e);
                }
            }
        }
        if (productPanel != null)
            productPanel.update(getWwd());
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
        else if (productLayer != null) {
            productLayer.removeProduct(product);
        }
        if (productPanel != null)
            productPanel.update(getWwd());

        if (isVisible()) {
            getWwd().redrawNow();
        }
    }

    private WMSLayersPanel addTab(int position, String server) {
        // Add a server to the tabbed dialog.
        try {
            final WMSLayersPanel layersPanel = new WMSLayersPanel(wwjPanel.getWwd(), server, wmsPanelSize);
            this.tabbedPane.add(layersPanel, BorderLayout.CENTER);
            final String title = layersPanel.getServerDisplayString();
            this.tabbedPane.setTitleAt(position, title != null && title.length() > 0 ? title : server);

            // Add a listener to notice wms layer selections and tell the layer panel to reflect the new state.
            layersPanel.addPropertyChangeListener("LayersPanelUpdated", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    layerPanel.update(wwjPanel.getWwd());
                }
            });

            return layersPanel;
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(null, "Server URL is invalid", "Invalid Server URL",
                    JOptionPane.ERROR_MESSAGE);
            tabbedPane.setSelectedIndex(previousTabIndex);
            return null;
        }
    }

    private static ElevationModel makeElevationModel() throws URISyntaxException, ParserConfigurationException,
            IOException, SAXException {
        final URI serverURI = new URI("http://www.nasa.network.com/elev");

        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        if (Configuration.getJavaVersion() >= 1.6) {
            try {
                docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException e) {   // Note it and continue on. Some Java5 parsers don't support the feature.
                String message = Logging.getMessage("XML.NonvalidatingNotSupported");
                Logging.logger().finest(message);
            }
        }
        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

        // Request the capabilities document from the server.
        final CapabilitiesRequest req = new CapabilitiesRequest(serverURI);
        final Document doc = docBuilder.parse(req.toString());

        // Parse the DOM as a capabilities document.
        // CHANGED
        //final Capabilities caps = Capabilities.parse(doc);
        final WMSCapabilities caps = new WMSCapabilities (doc);

        final double HEIGHT_OF_MT_EVEREST = 8850d; // meters
        final double DEPTH_OF_MARIANAS_TRENCH = -11000d; // meters

        // Set up and instantiate the elevation model
        final AVList params = new AVListImpl();
        params.setValue(AVKey.LAYER_NAMES, "|srtm3");
        params.setValue(AVKey.TILE_WIDTH, 150);
        params.setValue(AVKey.TILE_HEIGHT, 150);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(20, 20));
        params.setValue(AVKey.NUM_LEVELS, 8);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.ELEVATION_MIN, DEPTH_OF_MARIANAS_TRENCH);
        params.setValue(AVKey.ELEVATION_MAX, HEIGHT_OF_MT_EVEREST);

        final CompoundElevationModel cem = new CompoundElevationModel();
        cem.addElevationModel(new WMSBasicElevationModel(caps, params));

        return cem;
    }

    public static class AppPanel extends JPanel {
        private final WorldWindowGLCanvas wwd;
        private StatusBar statusBar = null;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar) {
            super(new BorderLayout());

            this.wwd = new WorldWindowGLCanvas();
            this.wwd.setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            final Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);
            m.setGlobe(new Earth());
            this.wwd.setView(new BasicOrbitView());

            // Setup a select listener for the worldmap click-and-go feature
            // ADDED: the following line was commented out
            // NOTE: it doesn't seem to make difference, ProdectLayer doesn't seem to be notified about select events
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

            this.add(this.wwd, BorderLayout.CENTER);
            if (includeStatusBar) {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }

            m.getLayers().add(new LayerPanelLayer(getWwd()));

            try {
                final ElevationModel em = makeElevationModel();
                m.getGlobe().setElevationModel(em);
            } catch (Exception e) {
                //
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
                product = ((ProductSceneView) contentPane).getProduct();
            }
            setSelectedProduct(product);
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
        }
    }
}
