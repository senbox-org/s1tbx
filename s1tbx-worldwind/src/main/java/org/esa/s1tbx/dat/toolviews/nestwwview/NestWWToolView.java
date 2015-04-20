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
import gov.nasa.worldwindx.examples.util.DirectedPath;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.application.support.AbstractToolView;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.netbeans.api.annotations.common.NullAllowed;
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

/**
 * The window displaying the world map.
 *
 * @version $Revision: 1.22 $ $Date: 2011-11-02 23:04:54 $
 */
public class NestWWToolView extends AbstractToolView implements WWView {

    //private static final String loadDEMCommand = "loadDEM";
    //private static final ImageIcon loadDEMIcon = ResourceUtils.LoadIcon("org/esa/snap/icons/dem24.gif");

    private final Dimension canvasSize = new Dimension(800, 600);

    private AppPanel wwjPanel = null;
    private LayerPanel layerPanel = null;
    private ProductPanel productPanel = null;

    private JSlider opacitySlider = null;
    //private JLabel theSelectedObjectLabel = null;
    private ProductLayer productLayer = null;

    private final Dimension wmsPanelSize = new Dimension(400, 600);

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private int previousTabIndex = 0;

    private static final boolean includeStatusBar = true;
    private static final boolean includeLayerPanel = false;
    private static final boolean includeProductPanel = true;
    private static final boolean includeWMSPanel = false;

    private DirectedPath theLastSelectedDP = null;

    private boolean theOWILimitChanged = false;
    private boolean theRVLLimitChanged = false;

    private static final String[] servers = new String[]
            {
                    "http://neowms.sci.gsfc.nasa.gov/wms/wms",
                    //"http://mapserver.flightgear.org/cgi-bin/landcover",
                    "http://wms.jpl.nasa.gov/wms.cgi",
                    "http://worldwind46.arc.nasa.gov:8087/wms"
            };

    public NestWWToolView() {
    }

    @Override
    public JComponent createControl() {

        productLayer = new ProductLayer(true);
        // ADDED
        //productLayer.setMinActiveAltitude(3e6);
        //productLayer.setMaxActiveAltitude(4e6);

        final Window windowPane = getPaneWindow();
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

        // ADDED
        // testLayer will react to selectListener which is added later
        // anything added to product layer doesn't seem to react to select listener
        /*
        RenderableLayer testLayer = new RenderableLayer();


        Polyline pLine = new Polyline();
        pLine.setLineWidth(10);
        pLine.setFollowTerrain(true);

        java.util.List<Position> positions = new ArrayList<Position>();
        positions.add(new Position(Angle.fromDegreesLatitude(10.0),
                Angle.fromDegreesLongitude(10.0), 0.0));
        positions.add(new Position(Angle.fromDegreesLatitude(10.0),
                Angle.fromDegreesLongitude(20.0), 0.0));
        positions.add(new Position(Angle.fromDegreesLatitude(20.0),
                Angle.fromDegreesLongitude(20.0), 0.0));
        positions.add(new Position(Angle.fromDegreesLatitude(20.0),
                Angle.fromDegreesLongitude(10.0), 0.0));
        positions.add(new Position(Angle.fromDegreesLatitude(10.0),
                Angle.fromDegreesLongitude(10.0), 0.0));
        pLine.setPositions(positions);


        testLayer.addRenderable(pLine);

        AnnotationAttributes controlPointsAttributes = new AnnotationAttributes();
        // Define an 8x8 square centered on the screen point
        controlPointsAttributes.setFrameShape(AVKey.SHAPE_RECTANGLE);
        controlPointsAttributes.setLeader(AVKey.SHAPE_NONE);
        controlPointsAttributes.setAdjustWidthToText(AVKey.SIZE_FIXED);
        controlPointsAttributes.setSize(new Dimension(12, 12));
        controlPointsAttributes.setDrawOffset(new Point(0, -4));
        controlPointsAttributes.setInsets(new Insets(0, 0, 0, 0));
        controlPointsAttributes.setBorderWidth(0);
        controlPointsAttributes.setCornerRadius(0);
        controlPointsAttributes.setBackgroundColor(Color.BLUE);    // Normal color
        controlPointsAttributes.setTextColor(Color.GREEN);         // Highlighted color
        controlPointsAttributes.setHighlightScale(1.2);
        controlPointsAttributes.setDistanceMaxScale(1);            // No distance scaling
        controlPointsAttributes.setDistanceMinScale(1);
        controlPointsAttributes.setDistanceMinOpacity(1);

        Position pos = new Position(Angle.fromDegreesLatitude(10.0),
                Angle.fromDegreesLongitude(10.0), 0.0);
        GlobeAnnotation currControlPoint = new GlobeAnnotation("Test Point", pos, controlPointsAttributes);
        testLayer.addRenderable(currControlPoint);

        getWwd().getModel().getLayers().add(testLayer);
        */
        // ADDED: end

        insertTiledLayer(getWwd(), productLayer);

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
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER) && (event.getTopObject() instanceof DirectedPath)) {
                        System.out.println("click " + event.getTopObject());

                        System.out.println("DirectedPath:::");
                        DirectedPath dp = (DirectedPath) event.getTopObject();
                        //dp.getAttributes().setOutlineMaterial(Material.WHITE);
                        dp.setHighlighted(true);
                        //dp.setAttributes(productLayer.dpHighlightAttrs);
                        //theSelectedObjectLabel.setText("" + productLayer.theObjectInfoHash.get(dp));

                        productLayer.infoAnnotation.setText(productLayer.theObjectInfoHash.get(dp));
                        productLayer.infoAnnotation.getAttributes().setVisible(true);
                        theLastSelectedDP = dp;
                        //System.out.println("selectedProduct " + getSelectedProduct());
                        //final ExecCommand command = datApp.getCommandManager().getExecCommand("showPolarWaveView");
                        //command.execute(2);
                    }
                    else {

                        if (theLastSelectedDP != null) {
                            theLastSelectedDP.setHighlighted(false);

                        }
                        productLayer.infoAnnotation.getAttributes().setVisible(false);
                        //theSelectedObjectLabel.setText("");
                    }
                }
            });


        } catch (Throwable e) {
            System.out.println("Can't load openGL " + e.getMessage());
        }
    }

    private JPanel makeControlPanel() {
        // CHANGED
        //final JPanel controlPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        //final JPanel mainControlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        final JPanel controlPanel = new JPanel(new GridLayout(6, 1, 5, 5));
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

        controlPanel.add(opacityPanel);

        // ADDED

        JRadioButton owiBtn = new JRadioButton("OWI");
        owiBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("owi:");
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "owi", true);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "osw", false);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "rvl", false);


            }
        });


        JRadioButton oswBtn = new JRadioButton("OSW");
        oswBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("osw:");
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "owi", false);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "osw", true);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "rvl", false);

            }
        });


        JRadioButton rvlBtn = new JRadioButton("RVL");
        rvlBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("rvl:");
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "owi", false);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "osw", false);
                productLayer.setComponentVisible(productLayer.colorBarLegendProduct, "rvl", true);

            }
        });


        ButtonGroup group = new ButtonGroup();
        group.add(owiBtn);
        group.add(oswBtn);
        group.add(rvlBtn);
        owiBtn.setSelected(true);

        JPanel componentTypePanel = new JPanel(new GridLayout(1, 4, 5, 5));
        componentTypePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        componentTypePanel.add(new JLabel("Component:"));
        componentTypePanel.add(owiBtn);
        componentTypePanel.add(oswBtn);
        componentTypePanel.add(rvlBtn);
        controlPanel.add(componentTypePanel);


        JPanel maxPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        maxPanel.add(new JLabel("Max OWI Wind Speed:"));


        JSpinner maxSP = new JSpinner(new SpinnerNumberModel(10, 0, 10, 1));
        maxSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = (Integer) ((JSpinner) e.getSource()).getValue();

                theOWILimitChanged = true;
            }
        });
        maxPanel.add(maxSP);
        controlPanel.add(maxPanel);

        JPanel minPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        minPanel.add(new JLabel("Min OWI Wind Speed:"));

        JSpinner minSP = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        minSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                System.out.println("new value " + ((JSpinner) e.getSource()).getValue());

                theOWILimitChanged = true;
            }
        });
        minPanel.add(minSP);
        controlPanel.add(minPanel);

        JPanel maxRVLPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        maxRVLPanel.add(new JLabel("Max RVL Rad Vel.:"));


        JSpinner maxRVLSP = new JSpinner(new SpinnerNumberModel(6, 0, 10, 1));
        maxRVLSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = (Integer) ((JSpinner) e.getSource()).getValue();
                theRVLLimitChanged = true;
            }
        });
        maxRVLPanel.add(maxRVLSP);
        controlPanel.add(maxRVLPanel);

        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                if (theOWILimitChanged && productLayer.owiColorBarLegend != null) {

                    //double minValue = ((Integer) minSP.getValue()) * 1.0e4;
                    //double maxValue = ((Integer) maxSP.getValue()) * 1.0e4;
                    double minValue = ((Integer) minSP.getValue());
                    double maxValue = ((Integer) maxSP.getValue());

                    productLayer.removeRenderable(productLayer.owiColorBarLegend);
                    productLayer.createColorBarLegend(minValue, maxValue, productLayer.theCurrMinHue, productLayer.theCurrMaxHue, false, "OWI Wind Speed", "owi");
                    productLayer.addRenderable(productLayer.owiColorBarLegend);

                    productLayer.createColorGradient(minValue, maxValue, productLayer.theCurrMinHue, productLayer.theCurrMaxHue, false, productLayer.theProductRenderablesInfoHash.get(productLayer.colorBarLegendProduct), "owi");
                    getWwd().redrawNow();
                }
                if (theRVLLimitChanged && productLayer.rvlColorBarLegend != null) {
                    System.out.println("theRVLLimitChanged");

                    //double minValue = ((Integer) minSP.getValue()) * 1.0e4;
                    //double maxValue = ((Integer) maxSP.getValue()) * 1.0e4;

                    double maxValue = ((Integer) maxRVLSP.getValue());
                    double minValue = -1*maxValue;

                    productLayer.removeRenderable(productLayer.rvlColorBarLegend);
                    productLayer.createColorBarLegend(minValue, maxValue, productLayer.theCurrMinHue, productLayer.theCurrMaxHue, true, "RVL Rad. Vel.", "rvl");
                    productLayer.addRenderable(productLayer.rvlColorBarLegend);

                    productLayer.createColorGradient(minValue, maxValue, productLayer.theCurrMinHue, productLayer.theCurrMaxHue, true, productLayer.theProductRenderablesInfoHash.get(productLayer.colorBarLegendProduct), "rvl");
                    getWwd().redrawNow();
                }


                theOWILimitChanged = false;
                theRVLLimitChanged = false;
            }
        });
        controlPanel.add(updateButton);

        //mainControlPanel.add(controlPanel);
        //mainControlPanel.add(makeRVLPanel ());
        //controlPanel.add(makeRVLPanel ());

        //controlPanel.add(theSelectedObjectLabel);
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        return controlPanel;
    }

    // ADDED
    /*
    private JPanel makeRVLPanel () {
        //final JPanel rvlPanel = new JPanel(new GridLayout(1, 3, 5, 5));


        JPanel maxRVLPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        maxRVLPanel.add(new JLabel("Max RVL Rad Vel.:"));


        JSpinner maxRVLSP = new JSpinner(new SpinnerNumberModel(10, 0, 10, 1));
        maxRVLSP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newValue = (Integer) ((JSpinner) e.getSource()).getValue();
            }
        });
        maxRVLPanel.add(maxRVLSP);
        //rvlPanel.add(maxPanel);


        JButton newButton = new JButton("Update");
        newButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {

            }
        });
        maxPanel.add(newButton);

        //controlPanel.add(theSelectedObjectLabel);
        maxPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        return maxPanel;
    }
    */

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
                    productLayer.addProduct(prod, true, getWwd());
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
        if (productLayer != null)
            productLayer.removeProduct(product);
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
