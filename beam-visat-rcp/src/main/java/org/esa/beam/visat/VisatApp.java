/*
 * $Id: VisatApp.java,v 1.107 2007/04/23 13:51:01 marcoz Exp $
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
package org.esa.beam.visat;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.layer.LayerModel;
import com.bc.swing.desktop.TabbedDesktopPane;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.action.DockableBarContext;
import com.jidesoft.action.event.DockableBarAdapter;
import com.jidesoft.action.event.DockableBarEvent;
import com.jidesoft.status.LabelStatusBarItem;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.status.ResizeStatusBarItem;
import com.jidesoft.status.TimeStatusBarItem;
import com.jidesoft.swing.JideBoxLayout;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamException;
import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.ApplicationWindow;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.product.*;
import org.esa.beam.framework.ui.tool.DrawingEditor;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.*;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.visat.actions.ToolAction;

import javax.media.jai.JAI;
import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The <code>VisatApp</code> class represents the VISAT application.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class VisatApp extends BasicApp {

    /**
     * Application Name
     *
     * @deprecated since BEAM 4.2, not used anymore
     */
    @Deprecated
    public static String APP_NAME;
    /**
     * Application symbolic name
     *
     * @deprecated since BEAM 4.2, not used anymore
     */
    @Deprecated
    public static String APP_SYMBOLIC_NAME;
    /**
     * Application Version
     *
     * @deprecated since BEAM 4.2, not used anymore
     */
    @Deprecated
    public static String APP_VERSION;
    /**
     * Application Copyright Information
     *
     * @deprecated since BEAM 4.2, not used anymore
     */
    @Deprecated
    public static String APP_COPYRIGHTINFO;
    /**
     * The name of the system logger for VISAT
     *
     * @deprecated since BEAM 4.2, not used anymore
     */
    @Deprecated
    public static String APP_LOGGER_NAME;

    /**
     * VISAT's plug-in directory
     */
    public static final String APP_DEFAULT_PLUGIN_DIR = SystemUtils.EXTENSION_DIR_NAME;
    /**
     * Preferences key for big-product size
     */
    public static final String PROPERTY_KEY_BIG_PRODUCT_SIZE = "big.product.size";
    /**
     * Preferences key for save product headers (MPH, SPH) or not
     */
    public static final String PROPERTY_KEY_SAVE_PRODUCT_HEADERS = "save.product.headers";
    /**
     * Preferences key for save product history or not
     */
    public static final String PROPERTY_KEY_SAVE_PRODUCT_HISTORY = "save.product.history";
    /**
     * Preferences key for save product annotations (ADS) or not
     */
    public static final String PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS = "save.product.annotations";
    /**
     * Preferences key for geo-location epsilon
     */
    public static final String PROPERTY_KEY_GEOLOCATION_EPS = "geolocation.eps";
    /**
     * Preferences key for geo-location epsilon
     */
    public static final double PROPERTY_DEFAULT_GEOLOCATION_EPS = 1.0e-4;
    /**
     * Preferences key for incremental mode at save
     */
    public static final String PROPERTY_KEY_SAVE_INCREMENTAL = "save.incremental";
    /**
     * Preferences key for low memory size
     */
    public static final String PROPERTY_KEY_LOW_MEMORY_LIMIT = "low.memory.limit";
    /**
     * Preferences key for the memory capacity of the JAI tile cache in megabytes
     */
    public static final String PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY = "jai.tileCache.memoryCapacity";
    /**
     * Preferences key for limit for automatic data load
     */
    public static final String PROPERTY_KEY_AUTO_LOAD_DATA_LIMIT = "visat.autoload.limit";
    /**
     * Default value for limit for automatic data load
     */
    public static final int PROPERTY_DEFAULT_AUTO_LOAD_DATA_LIMIT = 512;
    /**
     * Preferences key for automatic data unload
     */
    public static final String PROPERTY_KEY_AUTO_UNLOAD_DATA = "visat.autounload.enabled";
    /**
     * Preferences key for automatically showing new bands
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_NEW_BANDS = "visat.autoshowbands.enabled";
    /**
     * Preferences key for automatically showing magnifier
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_MAGNIFIER = "visat.autoshowmagnifier.enabled";
    /**
     * Preferences key for automatically showing navigation
     */
    public static final String PROPERTY_KEY_AUTO_SHOW_NAVIGATION = "visat.autoshownavigation.enabled";
    /**
     * Preferences key for on-line version check
     */
    public static final String PROPERTY_KEY_VERSION_CHECK_ENABLED = "visat.versionCheck" + SuppressibleOptionPane.KEY_PREFIX_ENABLED;
    /**
     * Preferences key for on-line version question
     */
    public static final String PROPERTY_KEY_VERSION_CHECK_DONT_ASK = "visat.versionCheck" + SuppressibleOptionPane.KEY_PREFIX_DONT_SHOW;
    /**
     * Preferences key for pixel offset-X for display pixel positions
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X = "pixel.offset.display.x";
    /**
     * Preferences key for pixel offset-Y for display pixel positions
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y = "pixel.offset.display.y";
    /**
     * Default value for pixel offset's for display pixel positions
     */
    public static final float PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY = 0.5f;
    /**
     * Preferences key for pixel offset-Y for display pixel positions
     */
    public static final String PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = "pixel.offset.display.show.decimals";
    /**
     * Default value for pixel offset's for display pixel positions
     */
    public static final boolean PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS = false;

    /**
     * Location key "browserPane" for plugin comonents
     */
    public static final String LOCATION_BROWSERPANE = "browserPane";

    /**
     * default value for preference save product annotations (ADS) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS = false;
    /**
     * default value for preference incremental mode at save
     */
    public static final boolean DEFAULT_VALUE_SAVE_INCREMENTAL = true;
    /**
     * default value for preference save product headers (MPH, SPH) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_HEADERS = true;
    /**
     * default value for preference save product history (History) or not
     */
    public static final boolean DEFAULT_VALUE_SAVE_PRODUCT_HISTORY = true;

    public static final String ALL_FILES_IDENTIFIER = "ALL_FILES";

    /**
     * The one and only visat instance
     */
    private static VisatApp instance;

    /**
     * VISAT's plug-in manager
     */
    private VisatPlugInManager plugInManager;
    /**
     * All internal frame listeners.
     */
    private List<InternalFrameListener> internalFrameListeners;
    /**
     * All registered property map listeners.
     */
    private List<PropertyMapChangeListener> propertyMapChangeListeners;
    /**
     * VISAT's product manager
     */
    private ProductManager productManager;

    /**
     * VISAT's scrollable desktop pane
     */
    private TabbedDesktopPane desktopPane;

    /**
     * The currently selected node within a data product. Can be <code>null</code>
     */
    private ProductNode selectedNode;
    /**
     * VISAT's preferences dialog
     */
    private VisatPreferencesDialog preferencesDialog;
    /**
     * VISAT's product node listener
     */
    private ProductNodeListener productNodeListener;
    private ParamExceptionHandler preferencesErrorHandler;

    private boolean visatExitConfirmed = false;

    // todo use instead
    private VisatApplicationPage applicationPage;
    private VisatApplicationWindow window;

    private ProductsToolView productsToolView;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructs the VISAT application instance. The constructor does not start the application nor does it perform any GUI
     * work.
     *
     * @param applicationDescriptor The application descriptor.
     */
    public VisatApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);
        if (instance != null) {
            throw new IllegalStateException("Only one instance of " + VisatApp.class + " allowed per VM.");
        }
        APP_NAME = applicationDescriptor.getDisplayName();
        APP_SYMBOLIC_NAME = applicationDescriptor.getSymbolicName();
        APP_VERSION = applicationDescriptor.getVersion();
        APP_COPYRIGHTINFO = applicationDescriptor.getCopyright();
        APP_LOGGER_NAME = applicationDescriptor.getSymbolicName();
        instance = this;
    }

    @Override
    protected void initClient(ProgressMonitor pm) {

        try {
            pm.beginTask(String.format("Initialising %s components", getAppName()), 3);

            internalFrameListeners = new ArrayList<InternalFrameListener>(10);
            propertyMapChangeListeners = new ArrayList<PropertyMapChangeListener>(4);
            productNodeListener = createProductNodeListener();
            productManager = new ProductManager();
            productManager.addListener(new ProductManager.Listener() {
                public void productAdded(ProductManager.Event event) {
                    event.getProduct().addProductNodeListener(productNodeListener);
                }

                public void productRemoved(ProductManager.Event event) {
                    event.getProduct().removeProductNodeListener(productNodeListener);
                }
            });

            getMainFrame().getDockingManager().setHideFloatingFramesOnSwitchOutOfApplication(true);
            getMainFrame().getDockingManager().setHideFloatingFramesWhenDeactivate(false);

            desktopPane = new TabbedDesktopPane();

            window = new VisatApplicationWindow(this);
            applicationPage = new VisatApplicationPage(desktopPane, getMainFrame().getDockingManager());
            applicationPage.setWindow(window);
            window.setPage(applicationPage);

            pm.setTaskName("Loading commands");
            Command[] commands = VisatActivator.getInstance().getCommands();
            for (Command command : commands) {
                addCommand(command, getCommandManager());
            }
            pm.worked(1);

            pm.setTaskName("Loading tool windows");
            loadToolViews();
            pm.worked(1);

            pm.setTaskName("Starting plugins");
            loadPlugins();
            plugInManager.startPlugins();
            registerShowToolViewCommands();
            pm.worked(1);

        } finally {
            pm.done();
        }
    }

    @Override
    protected void initClientUI(ProgressMonitor pm) {
        try {
            pm.beginTask(String.format("Initialising %s UI components", getAppName()), 4);

            CommandBar layersToolBar = createLayersToolBar();
            layersToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            layersToolBar.getContext().setInitIndex(1);
            getMainFrame().getDockableBarManager().addDockableBar(layersToolBar);
            pm.worked(1);

            CommandBar analysisToolBar = createAnalysisToolBar();
            analysisToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            analysisToolBar.getContext().setInitIndex(1);
            getMainFrame().getDockableBarManager().addDockableBar(analysisToolBar);
            pm.worked(1);

            CommandBar toolsToolBar = createToolsToolBar();
            toolsToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_EAST);
            toolsToolBar.getContext().setInitIndex(0);
            getMainFrame().getDockableBarManager().addDockableBar(toolsToolBar);
            pm.worked(1);

            CommandBar viewsToolBar = createViewsToolBar();
            viewsToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            viewsToolBar.getContext().setInitIndex(1);
            getMainFrame().getDockableBarManager().addDockableBar(viewsToolBar);
            pm.worked(1);

            // JAIJAIJAI
            if (Boolean.getBoolean("beam.imageTiling.enabled")) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        instance.showInfoDialog("You are using " + getAppName() + " in JAI tiled image mode.\n" +
                                "THIS MODE IS STILL UNDER DEVELOPMENT!",
                                                "beam.imageTiling.warn");
                    }
                });
            }

        } finally {
            pm.done();
        }
    }

    private static void addCommand(Command command, CommandManager commandManager) {
        String parentId = command.getParent();
        if (parentId != null && commandManager.getCommandGroup(parentId) == null) {
            Command com = getCommand(VisatActivator.getInstance().getCommands(), parentId);
            if (com != null) {
                // enter recursion
                // needed to solve depencies to command groups
                addCommand(com, commandManager);
            }
        }
        if (commandManager.getCommand(command.getCommandID()) == null) { // my be already added in the recursion
            commandManager.addCommand(command);
        }
    }

    private static Command getCommand(Command[] commands, String commandId) {
        for (Command command : commands) {
            if (command.getCommandID().equals(commandId)) {
                return command;
            }
        }
        return null;
    }


    public final ApplicationWindow getWindow() {
        return window;
    }

    public final ApplicationPage getPage() {
        return window.getPage();
    }

    private void loadToolViews() {
        ToolViewDescriptor[] toolViewDescriptors = VisatActivator.getInstance().getToolViewDescriptors();
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            applicationPage.addToolView(toolViewDescriptor);
        }
        productsToolView = (ProductsToolView) applicationPage.getToolView(ProductsToolView.ID);
        Assert.state(productsToolView != null, "productsToolView != null");
    }

    /**
     * Resets the singleton application instance so that {@link #getApp()} will return <code>null</code> after this method has been called.
     */
    @Override
    protected void handleImminentExit() {
        if (plugInManager != null) {
            plugInManager.stopPlugins();
        }
        singleThreadExecutor.shutdown();
        super.handleImminentExit();
    }

    private void registerShowToolViewCommands() {

        ToolViewDescriptor[] toolViewDescriptors = VisatActivator.getInstance().getToolViewDescriptors();
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            // triggers also command registration in command manager
            toolViewDescriptor.createShowViewCommand(window);
        }
    }

    private void loadPlugins() {
        plugInManager = new VisatPlugInManager(VisatActivator.getInstance().getPlugins());
    }

    /**
     * Returns the one and only VISAT application instance (singleton).
     *
     * @return the VISAT application. If it has not been started so far, <code>null</code> is returned.
     */
    public static VisatApp getApp() {
        return instance;
    }

    /**
     * @return VISAT's preferences dialog
     */
    public VisatPreferencesDialog getPreferencesDialog() {
        return preferencesDialog;
    }

    /**
     * @return the scrollable desktop pane used by VISAT
     */
    public TabbedDesktopPane getDesktopPane() {
        return desktopPane;
    }


    /**
     * Adds an internal frame listener to VISAT. Internal frame listeners are notified each time an internal frame
     * within VISAT's desktop pane is activated, deactivated,opened or closed.
     *
     * @param listener the listener to be added
     */
    public void addInternalFrameListener(final InternalFrameListener listener) {
        internalFrameListeners.add(listener);
        JInternalFrame[] internalFrames = getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            internalFrame.addInternalFrameListener(listener);
        }
    }

    /**
     * Removes an internal frame listener from VISAT. Internal frame listeners are notified each time an internal frame
     * within VISAT's desktop pane is activated, deactivated,opened or closed.
     *
     * @param listener the listener to be removed
     */
    public void removeInternalFrameListener(final InternalFrameListener listener) {
        internalFrameListeners.remove(listener);
        JInternalFrame[] internalFrames = getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            internalFrame.removeInternalFrameListener(listener);
        }
    }

    /**
     * Adds a product tree listener to VISAT. Product tree listeners are notified each time a product node is selected
     * or double-clicked within VISAT's product tree browser. Product nodes comprise a product itself, its bands,
     * tie-point grids or metadata elements.
     *
     * @param listener the listener to be added
     */
    public void addProductTreeListener(final ProductTreeListener listener) {
        if (productsToolView == null) {
            throw new IllegalStateException("productsToolView == null");
        }
        productsToolView.getProductTree().addProductTreeListener(listener);
    }

    /**
     * Removes a product tree listener from VISAT. Product tree listeners are notified each time a product node is
     * selected or double-clicked within VISAT's product tree browser. Product nodes comprise a product itself, its
     * bands, tie-point grids or metadata elements.
     *
     * @param listener the listener to be removed
     */
    public void removeProductTreeListener(final ProductTreeListener listener) {
        if (productsToolView == null) {
            throw new IllegalStateException("_productstoolView == null");
        }
        productsToolView.getProductTree().removeProductTreeListener(listener);
    }

    // todo - use getPreferences().addPropertyChangeListener()

    /**
     * Adds a property map change listener to VISAT. Property map change listeners are notified each time the VISAT
     * prferences have been loaded or modified.
     *
     * @param listener the listener to be added
     */
    public void addPropertyMapChangeListener(final PropertyMapChangeListener listener) {
        propertyMapChangeListeners.add(listener);
    }

    // todo - use getPreferences().removePropertyChangeListener()

    /**
     * Removes a property map change listener from VISAT. Property map change listeners are notified each time the VISAT
     * prferences have been loaded or modified.
     *
     * @param listener the listener to be removed
     */
    public void removePropertyMapChangeListener(final PropertyMapChangeListener listener) {
        propertyMapChangeListeners.remove(listener);
    }

    /**
     * Adds the given product to VISAT's internal open product list.
     *
     * @param product the product to be added
     */
    public void addProduct(final Product product) {
        getProductManager().addProduct(product);
    }

    /**
     * Removes the given product from VISAT's internal open product list.
     *
     * @param product the product to be removed
     */
    public void removeProduct(final Product product) {
        getProductManager().removeProduct(product);
    }

    /**
     * Removes and disposes the given product and performs a garbage collection. After calling this method, the product
     * object cannot be used anymore.
     *
     * @param product the product to be disposed
     * @see org.esa.beam.framework.datamodel.Product#dispose
     */
    public void disposeProduct(final Product product) {
        removeProduct(product);
        product.dispose();
    }

    /**
     * Returns the product manager which holds the list of currently open products.
     */
    public ProductManager getProductManager() {
        return productManager;
    }

    /**
     * Returns VISAT's product tree browser.
     */
    public ProductTree getProductTree() {
        return productsToolView.getProductTree();
    }

    /**
     * Returns the currently selected product.
     *
     * @return the selected product, which can be <code>null</code>
     */
    public Product getSelectedProduct() {
        if (selectedNode instanceof Product) {
            return (Product) selectedNode;
        } else if (selectedNode != null) {
            return selectedNode.getProduct();
        } else if (getSelectedProductSceneView() != null) {
            return getSelectedProductSceneView().getProduct();
        } else if (getSelectedProductMetadataView() != null) {
            return getSelectedProductMetadataView().getProduct();
        } else if (getProductManager().getNumProducts() == 1) {
            return getProductManager().getProductAt(0);
        } else {
            return null;
        }
    }

    /**
     * Sets the selected product node to the node displyed in the given internal frame.
     *
     * @param frame the internal frame which assigns a product node
     */
    public void setSelectedProductNode(final JInternalFrame frame) {
        setSelectedProductNode(getProductNode(frame));
    }

    /**
     * Returns the product node cuurrently displayed in the given internal frame.
     *
     * @return the displayed product nod or <code>null</code> if the product node cannot be identified.
     */
    public static ProductNode getProductNode(final JInternalFrame frame) {
        if (frame == null) {
            return null;
        }
        final Container contentPane = frame.getContentPane();
        if (contentPane instanceof ProductNodeView) {
            return ((ProductNodeView) contentPane).getVisibleProductNode();
        }
        return null;
    }

    /**
     * Returns the currently selected node within a product.
     *
     * @return the selected node, which can be <code>null</code>
     */
    public ProductNode getSelectedProductNode() {
        return selectedNode;
    }

    /**
     * Sets the currently selected node.
     * <p/>
     * <p>The method does nothing if the given selected node is already the selected one The method calls
     * <code>updateState()</code> if the selected node changes.
     *
     * @param selectedNode the product node, can be <code>null</code>
     */
    public void setSelectedProductNode(final ProductNode selectedNode) {
        if (this.selectedNode == selectedNode) {
            return;
        }
        // @todo 2 nf/nf - make sure, node CAN be selected
        this.selectedNode = selectedNode;

        Debug.trace("VisatApp: selected node changed: " + this.selectedNode);
        updateCurrentDocTitle();
        getProductTree().select(selectedNode);
        updateState();
    }

    /**
     * Returns the selected drawing editor.
     *
     * @return the selected drawing editor, or <code>null</code> if no drawing editor is selected
     */
    public DrawingEditor getSelectedDrawingEditor() {
        final Component contentPane = getContentPaneOfSelectedInternalFrame();
        if (contentPane instanceof DrawingEditor) {
            return (DrawingEditor) contentPane;
        } else {
            return null;
        }
    }

    /**
     * Returns the selected product node view.
     *
     * @return the selected product node view, or <code>null</code> if no product scene view is selected
     */
    public ProductNodeView getSelectedProductNodeView() {
        final Component contentPane = getContentPaneOfSelectedInternalFrame();
        if (contentPane instanceof ProductNodeView) {
            return (ProductNodeView) contentPane;
        } else {
            return null;
        }
    }

    /**
     * Returns the selected product scene view.
     *
     * @return the selected product scene view, or <code>null</code> if no product scene view is selected
     */
    public ProductSceneView getSelectedProductSceneView() {
        final Component contentPane = getContentPaneOfSelectedInternalFrame();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        } else {
            return null;
        }
    }

    /**
     * Returns the selected product metadata view.
     *
     * @return the selected product metadata view, or <code>null</code> if no product metadata view is selected
     */
    public ProductMetadataView getSelectedProductMetadataView() {
        final Component contentPane = getContentPaneOfSelectedInternalFrame();
        if (contentPane instanceof ProductMetadataView) {
            return (ProductMetadataView) contentPane;
        } else {
            return null;
        }
    }

    /**
     * Returns all open (internal) frames VISAT currently has.
     *
     * @return the internal frames, never <code>null</code>.
     */
    public synchronized JInternalFrame[] getAllInternalFrames() {
        return desktopPane != null ? desktopPane.getAllFrames() : new JInternalFrame[0];
    }

    /**
     * Finds the (internal) frames for the given raster data node.
     * <p/>
     * <p>The content panes of the returned frames are always  instances of <code>ProductSceneView</code>.
     *
     * @param raster   the raster for which to perform the lookup
     * @param numBands the number of bands in the view, pass -1 for all view types, 1 for single band type and 3 for RGB
     *                 views
     * @return the internal frames, never <code>null</code>.
     */
    public JInternalFrame[] findInternalFrames(final RasterDataNode raster, final int numBands) {
        final JInternalFrame[] frames = getAllInternalFrames();
        final ArrayList<JInternalFrame> frameList = new ArrayList<JInternalFrame>(10);
        for (final JInternalFrame frame : frames) {
            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                if ((numBands == -1 || view.getNumRasters() == numBands) &&
                        view.getRaster() == raster) {
                    frameList.add(frame);
                }
            }
        }
        return frameList.toArray(new JInternalFrame[frameList.size()]);
    }

    /**
     * Finds the any internal frame for the given raster data node.
     * <p/>
     * <p>The content pane of the returned frame is always an instance of <code>ProductSceneView</code>.
     *
     * @param raster the raster for which to perform the lookup
     * @return the internal frame or <code>null</code> if no frame was found
     */
    public JInternalFrame findInternalFrame(final RasterDataNode raster) {
        final JInternalFrame[] frames = getAllInternalFrames();
        for (final JInternalFrame frame : frames) {
            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                final int numRasters = view.getNumRasters();
                for (int j = 0; j < numRasters; j++) {
                    if (view.getRaster(j) == raster) {
                        return frame;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the (internal) frames for the given raster data node.
     * <p/>
     * <p>The content pane of the returned frame is always an instance of <code>ProductSceneView</code>.
     *
     * @param raster the raster for which to perform the lookup
     * @return the internal frames, never <code>null</code>.
     */
    public JInternalFrame[] findInternalFrames(final RasterDataNode raster) {
        final JInternalFrame[] frames = getAllInternalFrames();
        final ArrayList<JInternalFrame> frameList = new ArrayList<JInternalFrame>(10);
        for (final JInternalFrame frame : frames) {
            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                final int numRasters = view.getNumRasters();
                for (int j = 0; j < numRasters; j++) {
                    if (view.getRaster(j) == raster) {
                        frameList.add(frame);
                    }
                }
            }
        }
        return frameList.toArray(new JInternalFrame[frameList.size()]);
    }

    /**
     * Finds the (internal) frame for the given meta data element.
     * <p/>
     * <p>The content pane of the returned frame is always an instance of <code>ProductMetadataView</code>.
     *
     * @param metadataElement the metadata element for which to perform the lookup
     * @return the internal frame or <code>null</code> if no frame was found
     */
    public JInternalFrame findInternalFrame(final MetadataElement metadataElement) {
        final JInternalFrame[] frames = getAllInternalFrames();
        if (frames == null) {
            return null;
        }
        for (final JInternalFrame frame : frames) {
            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductMetadataView) {
                final ProductMetadataView view = (ProductMetadataView) contentPane;
                if (view.getMetadataElement() == metadataElement) {
                    return frame;
                }
            }
        }
        return null;
    }

    /**
     * Finds the (internal) frame for the given product node.
     * <p/>
     * <p>The content pane of the returned frame is always an instance of <code>ProductMetadataView</code>.
     *
     * @param productNode the product node for which to perform the lookup
     * @return the internal frame or <code>null</code> if no frame was found
     */
    public JInternalFrame findInternalFrame(final ProductNode productNode) {
        final JInternalFrame[] frames = getAllInternalFrames();
        if (frames == null) {
            return null;
        }
        for (final JInternalFrame frame : frames) {
            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductNodeView) {
                final ProductNodeView view = (ProductNodeView) contentPane;
                if (view.getVisibleProductNode() == productNode) {
                    return frame;
                }
            }
        }
        return null;
    }

    /**
     * Finds the product associated with the given file.
     *
     * @param file the file
     * @return the product associated with the given file. or <code>null</code> if no such exists.
     */
    public Product getOpenProduct(final File file) {
        final ProductManager productManager = getProductManager();
        for (int i = 0; i < productManager.getNumProducts(); i++) {
            final Product product = productManager.getProductAt(i);
            final File productFile = product.getFileLocation();
            if (file.equals(productFile)) {
                return product;
            }
        }
        return null;
    }

    /**
     * Closes all (internal) frames associated with the given product.
     */
    public synchronized void closeAllAssociatedFrames(final Product product) {

        boolean frameFound;
        do {
            frameFound = false;
            final JInternalFrame[] frames = desktopPane.getAllFrames();
            if (frames == null) {
                break;
            }
            for (final JInternalFrame frame : frames) {
                final Container cont = frame.getContentPane();
                Product frameProduct = null;
                if (cont instanceof ProductSceneView) {
                    final ProductSceneView imageView = (ProductSceneView) cont;
                    frameProduct = imageView.getProduct();
                } else if (cont instanceof ProductMetadataView) {
                    final ProductMetadataView metadataView = (ProductMetadataView) cont;
                    frameProduct = metadataView.getMetadataElement().getProduct();
                }
                if (frameProduct != null && frameProduct == product) {
                    desktopPane.closeFrame(frame);
                    frameFound = true;
                    break;
                }
            }
        } while (frameFound);
    }

    /**
     * Unloads all bands of the given product.
     */
    public synchronized void unloadAllAssociatedBands(final Product product) {
        product.acceptVisitor(new BandUnloader());
    }

    /**
     * Returns true if the given raster data node is used in any product scene view.
     *
     * @param raster
     * @return true if raster is used
     */
    public boolean hasRasterProductSceneView(final RasterDataNode raster) {
        final JInternalFrame[] internalFrames = getAllInternalFrames();
        if (internalFrames != null) {
            for (final JInternalFrame frame : internalFrames) {
                final Container contentPane = frame.getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView productSceneView = (ProductSceneView) contentPane;
                    final int numRasters = productSceneView.getNumRasters();
                    for (int j = 0; j < numRasters; j++) {
                        final RasterDataNode rasterAt = productSceneView.getRaster(j);
                        if (rasterAt == raster) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    public void updateAssociatedViews(final RasterDataNode[] rasters, final ViewUpdateMethod updateMethod) {
        final JInternalFrame[] internalFrames = getAllInternalFrames();
        for (final JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                boolean updateView = false;
                for (int j = 0; j < rasters.length && !updateView; j++) {
                    final RasterDataNode raster = rasters[j];
                    for (int k = 0; k < view.getNumRasters() && !updateView; k++) {
                        if (view.getRaster(k) == raster) {
                            updateView = true;
                        }
                    }
                }
                if (updateView) {
                    final Runnable doRun = new Runnable() {
                        public void run() {
                            updateMethod.updateView(view);
                        }
                    };
                    SwingUtilities.invokeLater(doRun);
                }
            }
        }
    }

    public void updateImages(final RasterDataNode[] rasters) {
        updateAssociatedViews(rasters, new ViewUpdateMethod() {
            public void updateView(final ProductSceneView view) {
                updateImage(view);
            }
        });
    }

    public void updateImage(final ProductSceneView view) {
        try {
            view.updateImage(ProgressMonitor.NULL);
        } catch (IOException e) {
            showErrorDialog("An I/O error occured during image update:\n" + e.getMessage()); /*I18N*/
            Debug.trace(e);
        }
    }

    public void updateROIImages(final RasterDataNode[] rasters, final boolean recreateROIImage) {
        updateAssociatedViews(rasters, new ViewUpdateMethod() {
            public void updateView(final ProductSceneView view) {
                updateROIImage(view, recreateROIImage);
            }
        });
    }

    public void updateROIImage(final ProductSceneView view, final boolean recreateROIImage) {
        final SwingWorker swingWorker = new SwingWorker() {
            ProgressMonitor pm = new DialogProgressMonitor(getMainFrame(), "Computing ROI Image",
                                                           Dialog.ModalityType.APPLICATION_MODAL);

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    view.updateROIImage(recreateROIImage, pm);
                } catch (IOException e) {
                    Debug.trace(e);
                    showErrorDialog("Failed to create ROI image.\nAn I/O error occured:\n" + e.getMessage());
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            protected void done() {
                updateState();
            }

        };
        swingWorker.execute();
    }

    @Override
    public synchronized void shutDown() {
        final ArrayList<Product> modifiedOrNew = new ArrayList<Product>(5);
        final Product[] products = getProductManager().getProducts();
        for (final Product product : products) {
            final ProductReader reader = product.getProductReader();
            if (reader != null) {
                final Object input = reader.getInput();
                if (input instanceof Product) {
                    modifiedOrNew.add(product);
                }
            }
            if (!modifiedOrNew.contains(product)
                    && product.isModified()) {
                modifiedOrNew.add(product);
            }
        }
        if (modifiedOrNew.size() == 0) {
            super.shutDown();
        } else {
            final Product[] modifiedProducts = modifiedOrNew.toArray(new Product[modifiedOrNew.size()]);
            final StringBuilder message = new StringBuilder();
            if (modifiedProducts.length == 1) {
                message.append("The following product has been modified:"); /*I18N*/
                message.append("\n    ").append(modifiedProducts[0].getDisplayName());
                message.append(String.format("\n\nDo you want to save this product before exiting %s?",
                                             getAppName())); /*I18N*/
            } else {
                message.append("The following products have been modified:"); /*I18N*/
                for (Product modifiedProduct : modifiedProducts) {
                    message.append("\n    ").append(modifiedProduct.getDisplayName());
                }
                message.append(String.format("\n\nDo you want to save these products before exiting %s?",
                                             getAppName())); /*I18N*/
            }
            final int result = showQuestionDialog("Products Modified", message.toString(), true, null);
            if (result == JOptionPane.YES_OPTION) {
                setVisatExitConfirmed(true);
                //Save Products in reverse order is neccessary because derived products must be saved first
                for (int i = modifiedProducts.length - 1; i >= 0; i--) {
                    final Product modifiedProduct = modifiedProducts[i];
                    saveProduct(modifiedProduct);
                }
                super.shutDown();
            } else if (result == JOptionPane.NO_OPTION) {
                super.shutDown();
            }
        }
    }


    public synchronized Product newProduct() {
        return newProductImpl();
    }


    private void openProductImpl(final File file) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new OpenProductRunnable(file));
    }


    public synchronized void openProduct(final File file) {
        openProductImpl(file);
    }

    /**
     * Closes the currently selected product.
     */
    public synchronized void closeSelectedProduct() {
        final Product product = getSelectedProductChecked();
        if (product == null) {
            return;
        }
        closeProduct(product);
    }

    /**
     * Closes the given product.
     *
     * @param product the product to be closed
     */
    public synchronized void closeProduct(final Product product) {
        closeProductImpl(product, true);
    }

    /**
     * Closes the all open products.
     */
    public synchronized void closeAllProducts() {
        final Product[] products = getProductManager().getProducts();
        for (int i = products.length - 1; i >= 0; i--) {
            final Product product = products[i];
            closeProduct(product);
        }
    }

    /**
     * Prompts the user to enter a new file name and saves the currently selected product under this new file name.
     */
    public synchronized void saveSelectedProductAs() {
        final Product product = getSelectedProductChecked();
        if (product != null) {
            saveProductAs(product);
        }
    }

    /**
     * Prompts the user to enter a new file name and saves the given product under this new file name.
     *
     * @param product the product to be saved
     */
    public synchronized void saveProductAs(final Product product) {
        saveProductAsImpl(product);
    }

    /**
     * Saves the currently selected product using its current file path. If it does not have a file name the method call
     * is equivalent to a call to the <code>{@link #saveSelectedProductAs}</code> method.
     */
    public synchronized void saveSelectedProduct() {
        final Product product = getSelectedProductChecked();
        if (product != null) {
            saveProduct(product);
        }
    }

    /**
     * Saves the given product using its current file path. If it does not have a file name the method call is
     * equivalent to a call to the <code>{@link #saveSelectedProductAs}</code> method.
     *
     * @param product the product to be saved
     */
    public synchronized void saveProduct(final Product product) {
        if (!(product.getProductReader() instanceof DimapProductReader) || product.getFileLocation() == null) {
            saveProductAs(product);
            return;
        }
        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                boolean success = false;
                try {
                    boolean incremental = DEFAULT_VALUE_SAVE_INCREMENTAL;
                    final PropertyMap preferences = getPreferences();
                    if (preferences != null) {
                        incremental = preferences.getPropertyBool(PROPERTY_KEY_SAVE_INCREMENTAL, incremental);
                    }
                    success = saveProductImpl(product, incremental);
                } finally {
                    unregisterJob(this);
                    if (success) {
                        product.setModified(false);
                    }
                }
                return null;
            }
        };
        registerJob(worker);
        worker.execute();
    }

    public synchronized boolean writeProduct(final Product product, final File file, final String formatName) {
        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                if (!writeProductImpl(product, file, formatName, false)) {
                    // @todo 1 nf/nf - end thread and return false
                }
                return null;
            }
        };
        worker.execute();
        // CAUTION: Here, worker is possibly still working in construct()!!!
        // @todo 1 nf/nf - check this is NOT always true!
        return true;
    }

    /**
     * Creates a new product scene view and opens an internal frame for it.
     */
    public void openProductSceneView(final RasterDataNode raster, final String helpId) {
        setStatusBarMessage("Creating image view...");
        UIUtils.setRootFrameWaitCursor(getMainFrame());

        final SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(getMainFrame(),
                                                                                             getAppName() + " - Creating image for '" + raster.getName() + "'") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                try {
                    return createProductSceneImage(raster, pm);
                } finally {
                    if (pm.isCanceled()) {
                        raster.unloadRasterData();
                    }
                }
            }

            @Override
            public void done() {
                UIUtils.setRootFrameDefaultCursor(getMainFrame());
                clearStatusBarMessage();

                final ProductSceneImage productSceneImage;
                try {
                    productSceneImage = get();
                } catch (OutOfMemoryError e) {
                    showOutOfMemoryErrorDialog("The image view could not be created.");
                    return;
                } catch (Exception e) {
                    handleUnknownException(e);
                    return;
                }

                ProductSceneView productSceneView = new ProductSceneView(productSceneImage);
                productSceneView.setCommandUIFactory(getCommandUIFactory());
                productSceneView.setROIOverlayEnabled(true);
                productSceneView.setGraticuleOverlayEnabled(false);
                productSceneView.setPinOverlayEnabled(true);
                productSceneView.setLayerProperties(getPreferences());
                productSceneView.addImageUpdateListener(new ProductSceneView.ImageUpdateListener() {
                    public void handleImageUpdated(final ProductSceneView view) {
                        updateState();
                    }
                });
                final String title = createInternalFrameTitle(raster);
                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
                final JInternalFrame internalFrame = createInternalFrame(title, icon, productSceneView, helpId);
                final Product product = raster.getProduct();
                product.addProductNodeListener(new ProductNodeListenerAdapter() {
                    @Override
                    public void nodeChanged(final ProductNodeEvent event) {
                        if (event.getSourceNode() == raster &&
                                event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                            internalFrame.setTitle(createInternalFrameTitle(raster));
                        }
                    }
                });

// @todo 1 nf/nf - extract layer properties dialog from VISAT preferences
// note: the following line has been out-commented because a preferences change is reflected
// in all open product scene views. The actual solution is to extract a layer properties dialog
// from the preferences which lets a user edit the properties of the current product scene view.
//            addPropertyMapChangeListener(productSceneView);
                updateState();
            }
        };
        singleThreadExecutor.submit(worker);
    }

    /**
     * Asks the user to select or define a RGB profile from which the product scene view will be
     * created and opend as internal frame.
     */
    public void openProductSceneViewRGB(final Product product, final String helpId) {
        final RGBImageProfilePane profilePane = new RGBImageProfilePane(getPreferences(), product);
        final boolean ok = profilePane.showDialog(getMainFrame(), getAppName() + " - Select RGB-Image Channels",
                                                  helpId);
        if (!ok) {
            return;
        }
        final String[] rgbaExpressions = profilePane.getRgbaExpressions();
        if (profilePane.getStoreProfileInProduct()) {
            RGBImageProfile.storeRgbaExpressions(product, rgbaExpressions);
        }

        final RGBImageProfile selectedProfile = profilePane.getSelectedProfile();
        final String name = selectedProfile != null ? selectedProfile.getName().replace("_", " ") : "";

        openProductSceneViewRGB(name, product, rgbaExpressions, helpId);
    }

    /**
     * Creates product scene view using the given RGBA expressions.
     */
    public void openProductSceneViewRGB(final String name, final Product product, final String[] rgbaExpressions,
                                        final String helpId) {
        final SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                getMainFrame(),
                getAppName() + " - Creating image for '" + name + "'") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                return createProductSceneImageRGB(name, product, rgbaExpressions, pm);
            }

            @Override
            protected void done() {
                getMainFrame().setCursor(Cursor.getDefaultCursor());

                final ProductSceneImage productSceneImage;
                try {
                    productSceneImage = get();
                } catch (OutOfMemoryError e) {
                    showOutOfMemoryErrorDialog("The RGB image view could not be created."); /*I18N*/
                    return;
                } catch (Exception e) {
                    handleUnknownException(e);
                    return;
                }
                clearStatusBarMessage();

                ProductSceneView productSceneView = new ProductSceneView(productSceneImage);
                productSceneView.setCommandUIFactory(getCommandUIFactory());
                productSceneView.setNoDataOverlayEnabled(false);
                productSceneView.setROIOverlayEnabled(false);
                productSceneView.setGraticuleOverlayEnabled(false);
                productSceneView.setPinOverlayEnabled(false);
                productSceneView.setLayerProperties(getPreferences());
                final String title = createInternalFrameTitleRGB(product, productSceneImage.getName());
                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
                createInternalFrame(title, icon, productSceneView, helpId);
                addPropertyMapChangeListener(productSceneView);
                updateState();
            }
        };
        setStatusBarMessage("Creating RGB image view...");  /*I18N*/
        singleThreadExecutor.submit(worker);
    }

    /**
     * Shows VISAT's about box.
     */
    public void showAboutBox() {
        final ModalDialog box = createAboutBox();
        box.show();
    }

    protected ModalDialog createAboutBox() {
        return new VisatAboutBox();
    }


    /**
     * Updates the main frame's document title.
     */
    public void updateCurrentDocTitle() {

        final ProductNode productNode = getSelectedProductNode();
        final JInternalFrame selectedInternalFrame = getSelectedInternalFrame();

        final StringBuffer docTitle = new StringBuffer();
        if (selectedInternalFrame != null) {
            docTitle.append(selectedInternalFrame.getTitle());
        } else if (productNode != null) {
            docTitle.append(productNode.getName());
        }

        if (productNode != null) {
            final Product product = productNode.getProduct();
            if (product != null) {
                if (docTitle.length() > 0) {
                    docTitle.append(" - ");
                }
                docTitle.append("[");
                final File fileLocation = product.getFileLocation();
                if (fileLocation != null) {
                    docTitle.append(fileLocation.getPath());
                } else {
                    docTitle.append("Not yet saved");
                }
                docTitle.append("]");
            }
        }

        setCurrentDocTitle(docTitle.toString());
    }

    private Product newProductImpl() {
        if (getProductManager().getNumProducts() == 0) {
            return null;
        }
        final ProductNodeList<Product> products = new ProductNodeList<Product>();
        products.copyInto(getProductManager().getProducts());
        final Product selectedProduct = getSelectedProduct();
        if (selectedProduct == null) {
            return null;
        }
        final int selectedSourceIndex = products.indexOf(selectedProduct);
        final NewProductDialog dialog = new NewProductDialog(getMainFrame(), products, selectedSourceIndex, false);
        if (dialog.show() != NewProductDialog.ID_OK) {
            return null;
        }
        final Product product = dialog.getResultProduct();
        if (product != null) {
            addProduct(product);
            updateState();
        }
        return product;
    }

    private boolean closeProductImpl(final Product product, final boolean modificationLostWarning) {
        final List<String> derivedProductNameList = new LinkedList<String>();

        for (final Product p : getProductManager().getProducts()) {
            final Set<Product> sourceProductSet = new HashSet<Product>(2);
            collectSourceProducts(p, sourceProductSet);

            if (sourceProductSet.contains(product)) {
                derivedProductNameList.add(p.getDisplayName());
            }
        }

        if (derivedProductNameList.size() > 0) {
            final StringBuilder message = new StringBuilder();
            message.append("Some (new) products are derived from the product you want to close now.\n");
            message.append(
                    "You cannot close this product until you have closed (or saved) the following product(s):\n");
            for (String name : derivedProductNameList) {
                message.append("  ").append(name).append("\n");
            }
            showInfoDialog("Cannot close", message.toString(), null);
            return false;
        }

        if (modificationLostWarning) {
            StringBuilder message = null;
            if (product.getFileLocation() == null) {
                message = new StringBuilder("The product\n" +
                        "  " + product.getDisplayName() + "\n" +
                        "you want to close has not been saved yet.\n");
            } else if (product.isModified()) {
                message = new StringBuilder("The product\n" +
                        "  " + product.getDisplayName() + "\n" +
                        "has been modified.\n");
            }
            if (message != null) {
                message.append("After closing this product all modifications will be lost.\n" +
                        "\n" +
                        "Do you really want to close this product now?");
                final int pressedButton = showQuestionDialog("Product Modified", message.toString(), null);
                if (pressedButton != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }

        try {
            UIUtils.setRootFrameWaitCursor(getMainFrame());
            setStatusBarMessage("Closing product '" + product.getDisplayName() + "'...");
            closeAllAssociatedFrames(product);
            unloadAllAssociatedBands(product);
        } finally {
            removeProduct(product);
            updateState();
            disposeProduct(product);
            clearStatusBarMessage();
            UIUtils.setRootFrameDefaultCursor(getMainFrame());
        }
        return true;
    }

    private static void collectSourceProducts(Product product, Set<Product> sourceProductSet) {
        final ProductReader reader = product.getProductReader();
        if (reader != null) {
            final Object input = reader.getInput();
            if (input instanceof Product) {
                sourceProductSet.add((Product) input);
                collectSourceProducts((Product) input, sourceProductSet);
            } else {
                if (input instanceof Product[]) {
                    for (final Product sourceProduct : (Product[]) input) {
                        sourceProductSet.add(sourceProduct);
                        collectSourceProducts(sourceProduct, sourceProductSet);
                    }
                }
            }
        }
    }

    private synchronized boolean saveProductImpl(final Product product, final boolean incremental) {
        final File file = product.getFileLocation();
        if (file.isFile() && !file.canWrite()) {
            showWarningDialog("The product\n" +
                    "'" + file.getPath() + "'\n" +
                    "exists and cannot be overwritten, because it is read only.\n" +
                    "Please choose another file or remove the write protection."); /*I18N*/
            return false;
        }

        boolean saveProductHeaders = DEFAULT_VALUE_SAVE_PRODUCT_HEADERS;
        boolean saveProductHistory = DEFAULT_VALUE_SAVE_PRODUCT_HISTORY;
        boolean saveADS = DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS;
        final PropertyMap preferences = getPreferences();
        if (preferences != null) {
            saveProductHeaders = preferences.getPropertyBool(PROPERTY_KEY_SAVE_PRODUCT_HEADERS, saveProductHeaders);
            saveProductHistory = preferences.getPropertyBool(PROPERTY_KEY_SAVE_PRODUCT_HISTORY, saveProductHistory);
            saveADS = getPreferences().getPropertyBool(PROPERTY_KEY_SAVE_PRODUCT_ANNOTATIONS, saveADS);
        }
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final ProductNodeList<MetadataElement> metadataElementBackup = new ProductNodeList<MetadataElement>();
        if (!saveProductHeaders) {
            String[] headerNames = new String[] {
                    "MPH", "SPH",
                    "Earth_Explorer_Header", "Fixed_Header", "Variable_Header", "Specific_Product_Header",
                    "Global_Attributes", "GlobalAttributes",
            };
            for (String headerName : headerNames) {
                MetadataElement element = metadataRoot.getElement(headerName);
                metadataElementBackup.add(element);
                metadataRoot.removeElement(element);
            }
        }
        if (!saveProductHistory) {
            final MetadataElement element = metadataRoot.getElement("History");
            metadataElementBackup.add(element);
            metadataRoot.removeElement(element);
        }
        if (!saveADS) {
            final String[] names = metadataRoot.getElementNames();
            for (final String name : names) {
                if (name.endsWith("ADS") || name.endsWith("Ads") || name.endsWith("ads")) {
                    final MetadataElement element = metadataRoot.getElement(name);
                    metadataElementBackup.add(element);
                    metadataRoot.removeElement(element);
                }
            }
        }

        final boolean saveOk = writeProductImpl(product, product.getFileLocation(),
                                                DimapProductConstants.DIMAP_FORMAT_NAME,
                                                incremental);
        if (saveOk) {
            product.setModified(false);
        } else {
            if (metadataRoot != null) {
                final MetadataElement[] elementsArray = new MetadataElement[metadataElementBackup.size()];
                metadataElementBackup.toArray(elementsArray);
                for (final MetadataElement metadataElement : elementsArray) {
                    metadataRoot.addElement(metadataElement);
                }
            }
        }
        return saveOk;
    }

    private boolean writeProductImpl(final Product product, final File file, final String formatName,
                                     final boolean incremental) {
        Debug.assertNotNull(product);

        boolean status = false;
        setStatusBarMessage("Writing product '" + product.getDisplayName() + "' to " + file + "...");
        ProgressMonitor pm = new DialogProgressMonitor(getMainFrame(), "Writing " + formatName + " format",
                                                       Dialog.ModalityType.APPLICATION_MODAL) {
            @Override
            public void setCanceled(boolean canceled) {
                if (canceled) {
                    int result = JOptionPane.showConfirmDialog(getMainFrame(),
                                                               "Cancel saving may lead to an unreadable product.\n\n"
                                                                       + "Do you really want to cancel the save process?",
                                                               "Cancel Process", JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        super.setCanceled(false);
                    }
                }
                super.setCanceled(canceled);
            }
        };
        try {
            ProductIO.writeProduct(product,
                                   file,
                                   formatName,
                                   incremental,
                                   pm);
            updateState();
            status = !pm.isCanceled();
        } catch (Exception e) {
            handleUnknownException(e);
        }

        UIUtils.setRootFrameDefaultCursor(getMainFrame());
        clearStatusBarMessage();
        return status;
    }

    private ProductSceneImage createProductSceneImage(final RasterDataNode raster,
                                                      ProgressMonitor pm) throws Exception {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        final boolean mustLoadData;
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            mustLoadData = false;
        } else {
            final long dataAutoLoadMemLimit = getDataAutoLoadLimit();
            mustLoadData = raster.getRasterDataSizeInBytes() < dataAutoLoadMemLimit;
        }

        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating image...", mustLoadData ? 2 : 1);
            if (mustLoadData) {
                loadProductRasterDataImpl(raster, SubProgressMonitor.create(pm, 1));
                if (!raster.hasRasterData()) {
                    return null;
                }
            }
            final JInternalFrame[] frames = findInternalFrames(raster, 1);
            if (frames.length > 0) {
                final ProductSceneView view = (ProductSceneView) frames[0].getContentPane();
                final LayerModel layerModel = view.getImageDisplay().getLayerModel();
                final ImageInfo imageInfo = view.getScene().getImageInfo();
                productSceneImage = ProductSceneImage.create(raster, imageInfo, layerModel,
                                                             SubProgressMonitor.create(pm, 1));
            } else {
                productSceneImage = ProductSceneImage.create(raster, SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }

        return productSceneImage;
    }

    private String createInternalFrameTitle(final RasterDataNode raster) {
        return UIUtils.getUniqueFrameTitle(getAllInternalFrames(), raster.getDisplayName());
    }

    private long getDataAutoLoadLimit() {
        final long megabyte = 1024 * 1024;
        return megabyte * getPreferences().getPropertyInt(PROPERTY_KEY_AUTO_LOAD_DATA_LIMIT,
                                                          PROPERTY_DEFAULT_AUTO_LOAD_DATA_LIMIT);
    }

    private static class RGBBand {

        private Band band;
        private boolean dataLoaded;
    }

    private ProductSceneImage createProductSceneImageRGB(String name, final Product product, String[] rgbaExpressions,
                                                         ProgressMonitor pm) throws Exception {
        getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RGBBand[] rgbBands = null;
        boolean errorOccured = false;
        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating RGB image...", 2);
            rgbBands = allocateRgbBands(product, rgbaExpressions, getDataAutoLoadLimit(),
                                        SubProgressMonitor.create(pm, 1));
            productSceneImage = ProductSceneImage.create(rgbBands[0].band, rgbBands[1].band, rgbBands[2].band,
                                                         SubProgressMonitor.create(pm, 1));
            productSceneImage.setName(name);
        } catch (Exception e) {
            errorOccured = true;
            throw e;
        } finally {
            pm.done();
            if (rgbBands != null) {
                releaseRgbBands(rgbBands, errorOccured);
            }
        }
        return productSceneImage;
    }

    private static RGBBand[] allocateRgbBands(final Product product,
                                              final String[] rgbaExpressions,
                                              final long dataAutoLoadMemLimit,
                                              final ProgressMonitor pm) throws IOException {
        final RGBBand[] rgbBands = new RGBBand[3]; // todo - set to [4] as soon as we support alpha
        long storageMem = 0;
        for (int i = 0; i < rgbBands.length; i++) {
            final RGBBand rgbBand = new RGBBand();
            String expression = rgbaExpressions[i].isEmpty() ? "0" : rgbaExpressions[i];
            rgbBand.band = product.getBand(expression);
            if (rgbBand.band == null) {
                rgbBand.band = new ProductSceneView.RGBChannel(product,
                                                               RGBImageProfile.RGB_BAND_NAMES[i],
                                                               expression);
            }
            rgbBands[i] = rgbBand;
            storageMem += rgbBand.band.getRawStorageSize();
        }
        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            // don't need to load any data!
        } else {
            if (storageMem < dataAutoLoadMemLimit) {
                pm.beginTask("Loading RGB channels...", rgbBands.length);
                String msgPattern = "Loading RGB channel ''{0}''...";
                for (final RGBBand rgbBand : rgbBands) {
                    if (!rgbBand.band.hasRasterData()) {
                        pm.setSubTaskName(MessageFormat.format(msgPattern, rgbBand.band.getName()));
                        rgbBand.band.loadRasterData(SubProgressMonitor.create(pm, 1));
                        rgbBand.dataLoaded = true;
                    } else {
                        pm.worked(1);
                    }
                    if (pm.isCanceled()) {
                        throw new IOException("Image creation canceled by user.");
                    }
                }
            }
        }
        return rgbBands;
    }

    private static void releaseRgbBands(RGBBand[] rgbBands, boolean errorOccured) {
        for (int i = 0; i < rgbBands.length; i++) {
            final RGBBand rgbBand = rgbBands[i];
            if (rgbBand != null && rgbBand.band != null) {
                if (rgbBand.band instanceof ProductSceneView.RGBChannel) {
                    if (rgbBand.dataLoaded) {
                        rgbBand.band.unloadRasterData();
                    }
                    if (errorOccured) {
                        rgbBand.band.dispose();
                    }
                }
                rgbBand.band = null;
            }
            rgbBands[i] = null;
        }
    }

    private String createInternalFrameTitleRGB(final Product product, String name) {
        return UIUtils.getUniqueFrameTitle(getAllInternalFrames(),
                                           product.getProductRefString() + " " + name + " RGB");
    }

    private ProductMetadataView createProductMetadataViewImpl(final MetadataElement element) {
        ProductMetadataView metadataView = null;

        setStatusBarMessage("Creating metadata view...");
        getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            metadataView = new ProductMetadataView(element);
            metadataView.setCommandUIFactory(getCommandUIFactory());
            final Icon icon = UIUtils.loadImageIcon("icons/RsMetaData16.gif");
            final JInternalFrame internalFrame = createInternalFrame(element.getDisplayName(),
                                                                     icon,
                                                                     metadataView, null);
            final Product product = metadataView.getProduct();
            product.addProductNodeListener(new ProductNodeListenerAdapter() {
                @Override
                public void nodeChanged(final ProductNodeEvent event) {
                    if (event.getSourceNode() == element &&
                            event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                        internalFrame.setTitle(element.getDisplayName());
                    }
                }
            });
            updateState();
        } catch (Exception e) {
            handleUnknownException(e);
        }

        getMainFrame().setCursor(Cursor.getDefaultCursor());
        clearStatusBarMessage();

        return metadataView;
    }

    /**
     * Notify all listeners that have registered interest for notification on VISAT's preferences changes..
     */
    private void firePreferencesChanged() {
        firePropertyMapChanged(getPreferences());
    }

    /**
     * Notify all listeners that have registered interest for notification on property map changes.
     */
    private void firePropertyMapChanged(final PropertyMap propertyMap) {
        for (PropertyMapChangeListener l : propertyMapChangeListeners) {
            l.propertyMapChanged(propertyMap);
        }
    }

    private void saveProductAsImpl(final Product product) {
        final ProductReader reader = product.getProductReader();
        if (reader != null && !(reader instanceof DimapProductReader)) {
            final int answer = showQuestionDialog("Save Product As",
                                                  "In order to save the product\n" +
                                                          "   " + product.getDisplayName() + "\n" +
                                                          "it has to be converted to the BEAM-DIMAP format.\n" +
                                                          "The current product and all of its views will be closed.\n" +
                                                          "Depending on the product size the conversion also may take a while.\n\n" +
                                                          "Do you really want to convert the product now?\n",
                                                  "productConversionRequired"); /*I18N*/
            if (answer != 0) { // Zero means YES
                return;
            }
        }
        final FileFilter dimapFileFilter = DimapProductHelpers.createDimapFileFilter();
        File file;
        while (true) {
            final String title = "Save Product As"; /*I18N*/
            final String extension = DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION;
            String fileName;
            if (product.getFileLocation() != null) {
                fileName = product.getFileLocation().getName();
            } else {
                fileName = product.getName();
                if (!fileName.endsWith(extension)) {
                    fileName += extension;
                }
            }
            file = showFileSaveDialog(title, false, dimapFileFilter, extension, fileName); /*I18N*/
            if (file == null) {
                break;
            }

            file = FileUtils.ensureExtension(file, extension);
            file = file.getAbsoluteFile();

            if (file.exists()) {
                final int answer = showQuestionDialog("Product Exists",
                                                      "The selected directory already contains a data product with the name\n" +
                                                              "'" + file.getName() + "'.\n\n" +
                                                              "Do you really want to overwrite this product?\n", null); /*I18N*/
                if (answer == 0) { // Zero means YES
                    break;
                } else {
                    continue;
                }
            }

            break;
        }

        if (file == null) {
            return;
        }

        final String oldProductName = product.getName();
        final File oldFile = product.getFileLocation();
        final File newFile = FileUtils.ensureExtension(file, DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);

// For DIMAP products, check if file path has really changed
// if not, just save product
        if (reader instanceof DimapProductReader && newFile.equals(oldFile)) {
            saveProduct(product);
            return;
        }

        product.setFileLocation(newFile);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                final boolean incremental = false;
                final boolean successfullySaved = saveProductImpl(product, incremental);
                final boolean successfullyClosed;
                if (successfullySaved) {
                    successfullyClosed = closeProductImpl(product, false);
                    if (!isVisatExitConfirmed()) {
                        openProduct(newFile);
                    }
                } else {
                    successfullyClosed = false;
                }
                if (!successfullySaved || !successfullyClosed) {
                    product.setFileLocation(oldFile);
                    product.setName(oldProductName);
                }
                return null;
            }

            @Override
            public void done() {
                unregisterJob(this);
            }
        };
        registerJob(worker);
        worker.execute();
    }

    private boolean loadProductRasterDataImpl(final RasterDataNode raster, ProgressMonitor pm) {
        if (raster.hasRasterData()) {
            return true;
        }

        setStatusBarMessage("Loading raster data...");
// Don't show wait cursor here - progress bar should pop-up soon...

        boolean state = false;
        try {
            raster.loadRasterData(pm);
            updateState();
            state = true;
        } catch (Exception e) {
            handleUnknownException(e);
        }

        clearStatusBarMessage();
        return state;
    }

    @Override
    protected void applyPreferences() {
        super.applyPreferences();
        updateReopenMenu();
        configureJaiTileCache();
    }

    private void updateReopenMenu() {
        final JMenu menu = findMenu("reopen");
        if (menu == null) {
            return;
        }
        menu.removeAll();
//        final FileHistory history = new FormatedFileHistory(getFileHistory(), new DimapProductReaderPlugIn());
        final FileHistory history = getFileHistory();
        history.initBy(getPreferences());
        final String[] entries = history.getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                final String filePath = entries[i];
                final JMenuItem item = new JMenuItem((i + 1) + ": " + filePath);
                item.setMnemonic('1' + i);
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        openProduct(new File(filePath));
                    }
                });
                menu.add(item);
            }
        }
    }

    private void configureJaiTileCache() {
        final int tileCacheCapacity = getPreferences().getPropertyInt(PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY, 512);
        JAIUtils.setDefaultTileCacheCapacity(tileCacheCapacity);
    }

    @Override
    protected void historyPush(final File file) {
        super.historyPush(file);
        updateReopenMenu();
    }


    private Component getContentPaneOfSelectedInternalFrame() {
        final JInternalFrame selectedFrame = getSelectedInternalFrame();
        if (selectedFrame != null) {
            return selectedFrame.getContentPane();
        } else {
            return null;
        }
    }

    public JInternalFrame getSelectedInternalFrame() {
        JInternalFrame selectedFrame = null;
        if (desktopPane != null) {
            selectedFrame = desktopPane.getSelectedFrame();
            if (selectedFrame == null) {
                final JInternalFrame[] internalFrames = desktopPane.getAllFrames();
                if (internalFrames.length > 0) {
                    selectedFrame = internalFrames[internalFrames.length - 1];
                }
            }
        }
        return selectedFrame;
    }

    public void showPreferencesDialog(final String helpId) {
        if (preferencesDialog == null) {
            getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            preferencesDialog = new VisatPreferencesDialog(this, helpId);
            getMainFrame().setCursor(Cursor.getDefaultCursor());
        }
        if (preferencesErrorHandler == null) {
            preferencesErrorHandler = new ParamExceptionHandler() {
                public boolean handleParamException(final ParamException e) {
                    final Parameter parameter = e.getParameter();
                    final Object defaultValue = parameter.getProperties().getDefaultValue();
                    showErrorDialog("Error in Preferences",
                                    String.format("A problem has been detected in the preferences settings of %s:\n\n"
                                            + "Value for parameter '%s' is invalid.\n"
                                            + "Its default value '%s' will be used instead.",
                                                  getAppName(), parameter.getName(), defaultValue));
                    try {
                        parameter.setDefaultValue();
                    } catch (IllegalArgumentException e1) {
                        Debug.trace(e1);
                    }
                    return true;
                }
            };
        }
        preferencesDialog.setConfigParamValues(getPreferences(), preferencesErrorHandler);
        if (preferencesDialog.show() == ModalDialog.ID_OK) {
            final PreferencesChangeChecker checker = new PreferencesChangeChecker();
            getPreferences().addPropertyChangeListener(checker);
            preferencesDialog.getConfigParamValues(getPreferences());
            getPreferences().removePropertyChangeListener(checker);
            if (checker.arePropertiesChanged()) {
                configureJaiTileCache();
                applyLookAndFeelPreferences();
// @todo 1 nf/nf - extract layer properties dialog from VISAT preferences
// note: the following line is necessary in order to transfer layer proerties from
// preferences to current product scene view. Only the current view is affected by
// the preferences change.
                applyProductSceneViewlPreferences();
                firePreferencesChanged();
            }
        }
    }

    private void applyProductSceneViewlPreferences() {
        final ProductSceneView selectedProductSceneView = getSelectedProductSceneView();
        if (selectedProductSceneView != null) {
            selectedProductSceneView.setLayerProperties(getPreferences());
        }
    }

    /**
     * Called after the look & feel has changed. The method simply calls <code>SwingUtilities.updateComponentTreeUI(getMainFrame())</code>
     * in order to reflect changes of the look-and-feel.
     * <p/>
     * <p>You might want to override this method in order to call <code>SwingUtilities.updateComponentTreeUI()</code> on
     * other top-level containers beside the main frame.
     */
    @Override
    protected void updateComponentTreeUI() {
        super.updateComponentTreeUI();
        if (preferencesDialog != null) {
            SwingUtilities.updateComponentTreeUI(preferencesDialog.getJDialog());
        }
        plugInManager.updatePluginsComponentTreeUI();
        getCommandManager().updateComponentTreeUI();
    }

    private Product getSelectedProductChecked() {
        final Product product = getSelectedProduct();
        if (product == null) {
            showInfoDialog("No data product selected.", null); /*I18N*/
        }
        return product;
    }

    private void addRegisteredInternalFrameListeners(final JInternalFrame frame) {
        for (InternalFrameListener l : internalFrameListeners) {
            frame.addInternalFrameListener(l);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // UI Creation

    /**
     * Overrides the base class version in order to create and configure the VISAT's main pane.
     */
    @Override
    protected JComponent createMainPane() {

        final JMenuBar menuBar = getMainFrame().getJMenuBar();
        JMenu windowMenu = null;
        for (int i = 0; i < menuBar.getComponentCount(); i++) {
            final Component component = menuBar.getComponent(i);
            if (component instanceof JMenu && "window".equals(component.getName())) {
                windowMenu = (JMenu) menuBar.getComponent(i);
            }
        }
        desktopPane.setWindowMenu(windowMenu);

        return desktopPane;
    }

    /**
     * Overrides the base class version in order to create a tool bar for VISAT.
     */
    @Override
    protected CommandBar createMainToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar("mainToolBar");
        toolBar.setTitle("Standard");
        toolBar.addDockableBarListener(new ToolBarListener());

        addCommandsToToolBar(toolBar, new String[]{
                "new",
                "open",
                "save",
                null,
                "preferences",
                "properties",
                null,
                "showUpdateDialog",
                "helpTopics",
        });

        return toolBar;
    }

    private CommandBar createLayersToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar("layersToolBar");
        toolBar.setTitle("Layers");
        toolBar.addDockableBarListener(new ToolBarListener());

        addCommandsToToolBar(toolBar, new String[]{
                "showNoDataOverlay",
                "showROIOverlay",
                "showShapeOverlay",
                "showGraticuleOverlay",
                PinDescriptor.INSTANCE.getShowLayerCommandId(),
                GcpDescriptor.INSTANCE.getShowLayerCommandId(),
        });

        return toolBar;
    }

    private CommandBar createAnalysisToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar("analysisToolBar");
        toolBar.setTitle("Analysis");
        toolBar.addDockableBarListener(new ToolBarListener());

        addCommandsToToolBar(toolBar, new String[]{
                "openInformationDialog",
                "openGeoCodingInfoDialog",
                "openStatisticsDialog",
                "openHistogramDialog",
                "openScatterPlotDialog",
        });

        return toolBar;
    }

    private CommandBar createToolsToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar("toolsToolBar");
        toolBar.setTitle("Tools");
        toolBar.addDockableBarListener(new ToolBarListener());

        addCommandsToToolBar(toolBar, new String[]{
                // These IDs are defined in the module.xml
                "selectTool",
                "rangeFinder",
                "zoomTool",
                "pannerTool",
                "pinTool",
                "gcpTool",
                "drawLineTool",
                "drawRectangleTool",
                "drawEllipseTool",
                "drawPolylineTool",
                "drawPolygonTool",
                "deleteShape",
//                "magicStickTool",
                null,
                "convertShapeToROI",
                "convertROIToShape",
        });

        return toolBar;
    }


    private CommandBar createViewsToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar("viewsToolBar");
        toolBar.setTitle("Views");
        toolBar.addDockableBarListener(new ToolBarListener());

        ToolViewDescriptor[] toolViewDescriptors = VisatActivator.getInstance().getToolViewDescriptors();
        List<String> viewCommandIdList = new ArrayList<String>(5);
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            if (!"org.esa.beam.visat.toolviews.stat.StatisticsToolView".equals(toolViewDescriptor.getId())) {
                viewCommandIdList.add(toolViewDescriptor.getId() + ".showCmd");
            }
        }

        addCommandsToToolBar(toolBar, viewCommandIdList.toArray(new String[viewCommandIdList.size()]));

        return toolBar;

    }

    private void addCommandsToToolBar(CommandBar toolBar, String[] commandIDs) {
        for (final String commandID : commandIDs) {
            if (commandID == null) {
                toolBar.add(ToolButtonFactory.createToolBarSeparator());
            } else {
                final Command command = getCommandManager().getCommand(commandID);
                Debug.assertTrue(command != null, "commandID=" + commandID);
                assert command != null;
                final AbstractButton toolBarButton = command.createToolBarButton();
                toolBarButton.addMouseListener(getMouseOverActionHandler());
                toolBar.add(toolBarButton);
            }
            toolBar.add(Box.createHorizontalStrut(1));
        }
    }

    /**
     * Creates a standard status bar for this application.
     */
    @Override
    protected com.jidesoft.status.StatusBar createStatusBar() {
        final com.jidesoft.status.StatusBar statusBar = new com.jidesoft.status.StatusBar();

        final LabelStatusBarItem message = new LabelStatusBarItem(MESSAGE_STATUS_BAR_ITEM_KEY);
        message.setText("Ready.");
        message.setPreferredWidth(600);
        message.setAlignment(JLabel.LEFT);
        message.setToolTip("Displays status messages.");
        statusBar.add(message, JideBoxLayout.FLEXIBLE);

        final LabelStatusBarItem position = new LabelStatusBarItem(POSITION_STATUS_BAR_ITEM_KEY);
        position.setText("");
        position.setPreferredWidth(80);
        position.setAlignment(JLabel.CENTER);
        position.setToolTip("Displays pixel position");
        statusBar.add(position, JideBoxLayout.FLEXIBLE);

        final TimeStatusBarItem time = new TimeStatusBarItem();
        time.setPreferredWidth(80);
        time.setUpdateInterval(1000);
        time.setAlignment(JLabel.CENTER);
        statusBar.add(time, JideBoxLayout.FLEXIBLE);

        final MemoryStatusBarItem gc = new MemoryStatusBarItem();
        gc.setPreferredWidth(100);
        gc.setUpdateInterval(1000);
        gc.setGcIcon(UIUtils.loadImageIcon("icons/GC18.gif"));
        statusBar.add(gc, JideBoxLayout.FLEXIBLE);

        final ResizeStatusBarItem resize = new ResizeStatusBarItem();
        statusBar.add(resize, JideBoxLayout.FIX);

        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled")) {
            hookJaiTileCacheFlush(gc);
        }

        return statusBar;
    }

    private static void hookJaiTileCacheFlush(MemoryStatusBarItem gc) {
        AbstractButton button = findButtonForIcon(gc, gc.getGcIcon());
        if (button != null) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JAI.getDefaultInstance().getTileCache().flush();
                    Debug.trace("JAI tile cache flushed!");
                }
            });
        }
    }

    private static AbstractButton findButtonForIcon(Container container, Icon icon) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                if (button.getIcon() == icon) {
                    return button;
                }
            }
        }
        for (Component component : components) {
            if (component instanceof Container) {
                AbstractButton button = findButtonForIcon((Container) component, icon);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    /**
     * Creates a tool button for the tool-command with the given command ID.
     * <p/>
     * <p>A command with the given ID must already been registered using any of the <code>createToolCommand</code>
     * methods of VisatApp, or directly using the <code>createToolCommand</code> method of VISAT's {@link
     * CommandManager}. Otherwise an {@link IllegalArgumentException} will be thrown.
     * <p/>
     * <p>The new button is which is automatically added to VISAT's tool button group to ensure that a only single tool
     * is selected.
     *
     * @param commandID the command ID
     * @return a tool button which is automatically added to VISAT's tool button group.
     * @see #getCommandManager
     */
    public AbstractButton createToolButton(final String commandID) {
        final Command command = getCommandManager().getCommand(commandID);
        Guardian.assertNotNull("command", command);
        return command.createToolBarButton();
    }

    /**
     * Overrides the base class version in order to creates the menu bar for VISAT.
     */
    @Override
    protected CommandBar createMainMenuBar() {
        final CommandMenuBar menuBar = new CommandMenuBar("Main Menu");
        menuBar.setHidable(false);
        menuBar.setStretch(true);

        menuBar.add(createJMenu("file", "File", 'F')); /*I18N*/
        menuBar.add(createJMenu("edit", "Edit", 'E')); /*I18N*/
        menuBar.add(createJMenu("view", "View", 'V'));  /*I18N*/
        menuBar.add(createJMenu("data", "Analysis", 'A')); /*I18N*/
        menuBar.add(createJMenu("tools", "Tools", 'T')); /*I18N*/
        menuBar.add(createJMenu("window", "Window", 'W')); /*I18N*/
        menuBar.add(createJMenu("help", "Help", 'H')); /*I18N*/

        return menuBar;
    }

    /**
     * Creates a new product metadata view and opens an internal frame for it.
     */
    public synchronized ProductMetadataView createProductMetadataView(final MetadataElement element) {
        return createProductMetadataViewImpl(element);
    }

    /**
     * Creates an internal frame and adds it to VISAT's desktop.
     *
     * @param title   a frame title
     * @param icon    a frame icon, can be null
     * @param content the frame's content pane
     * @return the newly created frame
     */
    public synchronized JInternalFrame createInternalFrame(final String title, final Icon icon,
                                                           final JComponent content, final String helpId) {
        Debug.assertNotNull(desktopPane);

        final JInternalFrame frame = new JInternalFrame(title, true, true, true, true) {
            @Override
            public void dispose() {
                super.dispose();
                // Note that super.dispose() does not remove registered InternalFrameListener! Why?
                InternalFrameListener[] listeners = getListeners(InternalFrameListener.class);
                for (InternalFrameListener l : listeners) {
                    removeInternalFrameListener(l);
                }
            }
        };

        if (helpId != null) {
            HelpSys.enableHelpKey(frame, helpId);
        }

        frame.addInternalFrameListener(new VisatIFL());
        addRegisteredInternalFrameListeners(frame);

        if (icon != null) {
            frame.setFrameIcon(icon);
        }
        if (content != null) {
            frame.setContentPane(content);
        }
        frame.setVisible(true);
        frame.setLocation(0, 0);
        if (content != null && content.getPreferredSize() != null) {
            frame.pack();
        } else {
            frame.setSize(640, 480);
        }

// Resize frame so that it completely fits into desktopPane
        final Rectangle desktopBounds = new Rectangle(desktopPane.getDesktopPane().getSize());
        if (!desktopBounds.isEmpty()) {
            frame.setBounds(frame.getBounds().intersection(desktopBounds));
        }

        desktopPane.addFrame(frame);

// force frame to be activated so that the frame listeners are informed
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException ignored) {
            // ok
        }
        updateState();
        return frame;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(Product.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if (getSelectedProductNode() == sourceNode) {
                        updateCurrentDocTitle();
                    }
                }

            }
        };
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Nested classes & Interfaces

    /**
     * A method used to update a <code>ProductSceneView</code>.
     */
    public static interface ViewUpdateMethod {

        void updateView(ProductSceneView view);
    }


    /**
     * This is VISAT's internal frame listener.
     */
    private class VisatIFL implements InternalFrameListener {

        /**
         * Invoked when an internal frame is activated.
         *
         * @see javax.swing.JInternalFrame#setSelected
         */
        public void internalFrameActivated(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame activated: " + e);
            setSelectedProductNode(e.getInternalFrame());
            final Component contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof DrawingEditor) {
                final DrawingEditor drawingEditor = (DrawingEditor) contentPane;
                drawingEditor.setTool(ToolAction.getActiveTool());
            }
            updateCurrentDocTitle();
            updateState();
        }

        /**
         * Invoked when an internal frame is de-activated.
         *
         * @see javax.swing.JInternalFrame#setSelected
         */
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame deactivated: " + e);
            final Component contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof DrawingEditor) {
                final DrawingEditor drawingEditor = (DrawingEditor) contentPane;
                drawingEditor.setTool(null);
            }
            updateState();
        }

        /**
         * Invoked when a internal frame has been opened.
         *
         * @see javax.swing.JInternalFrame#show
         */
        public void internalFrameOpened(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame opened: " + e);
            setSelectedProductNode(e.getInternalFrame());
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof DrawingEditor) {
                final DrawingEditor drawingEditor = (DrawingEditor) contentPane;
                drawingEditor.setTool(ToolAction.getActiveTool());
            }
        }

        /**
         * Invoked when an internal frame is in the process of being closed. The close operation can be overridden at
         * this point.
         *
         * @see javax.swing.JInternalFrame#setDefaultCloseOperation
         */
        public void internalFrameClosing(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame closing: " + e);
            updateState();
        }

        /**
         * Invoked when an internal frame has been closed.
         *
         * @see javax.swing.JInternalFrame#setClosed
         */
        public void internalFrameClosed(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame closed: " + e);
            final String title = e.getInternalFrame().getTitle();
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof DrawingEditor) {
                final DrawingEditor drawingEditor = (DrawingEditor) contentPane;
                drawingEditor.setTool(null);
            }
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView productSceneView = (ProductSceneView) contentPane;
                removePropertyMapChangeListener(productSceneView);

                final int numRasters = productSceneView.getNumRasters();

                JInternalFrame otherFrame = null;
                for (int i = 0; i < numRasters; i++) {
                    final RasterDataNode raster = productSceneView.getRaster(i);
                    otherFrame = findInternalFrame(raster);
                    // If this band is not referenced by other frames anymore:
                    if (otherFrame == null) {
                        final boolean autoUnload = getPreferences().getPropertyBool(PROPERTY_KEY_AUTO_UNLOAD_DATA,
                                                                                    true);
                        // If auto-unload is on, unload data
                        if (autoUnload && !raster.isSynthetic() && !raster.isReadOnly()) {
                            Debug.trace("Unloading raster data of '" + raster.getName() + "'...");
                            raster.unloadRasterData();
                            Debug.trace("Raster data unloaded.");
                        }
                    }
                }

                // Save view-associtated objects before we dispose the view
                final LayerModel layerModel = productSceneView.getImageDisplay().getLayerModel();
                final JInternalFrame[] otherFrames = findInternalFrames(productSceneView.getRaster(), 1);

                Debug.trace("Disposing view '" + title + "'...");
                productSceneView.dispose();
                Debug.trace("View disposed.");

                if (otherFrames.length == 0) {
                    Debug.trace("Disposing layer model of view '" + title + "'...");
                    layerModel.dispose();
                    Debug.trace("Layer model disposed.");
                }

                if (otherFrame != null) {
                    try {
                        otherFrame.setSelected(true);
                    } catch (PropertyVetoException ignored) {
                        // ok
                    }
                }
            }

            updateState();
        }

        /**
         * Invoked when an internal frame is iconified.
         *
         * @see javax.swing.JInternalFrame#setIcon
         */
        public void internalFrameIconified(final InternalFrameEvent e) {
            updateState();
        }

        /**
         * Invoked when an internal frame is de-iconified.
         *
         * @see javax.swing.JInternalFrame#setIcon
         */
        public void internalFrameDeiconified(final InternalFrameEvent e) {
            updateState();
        }
    }

    /**
     * This band unloader is used to unload all bands of a product.
     */
    private class BandUnloader extends ProductVisitorAdapter {

        @Override
        public void visit(final Band band) {
            if (band.getRasterData() != null && !band.isReadOnly()) {
                getLogger().info("Unloading raster data of '" + band.getName() + "'...");
                band.unloadRasterData();
                getLogger().info("Raster data unloaded.");
            }
        }
    }

    /**
     * This listener is used to identify whether or not a property map has changed.
     */
    private static class PreferencesChangeChecker implements PropertyChangeListener {

        private boolean propertiesChanged;

        PreferencesChangeChecker() {
        }

        public boolean arePropertiesChanged() {
            return propertiesChanged;
        }

        public void propertyChange(final PropertyChangeEvent evt) {
            propertiesChanged = true;
        }
    }

    private boolean isVisatExitConfirmed() {
        return visatExitConfirmed;
    }

    public void setVisatExitConfirmed(final boolean visatExitConfirmed) {
        this.visatExitConfirmed = visatExitConfirmed;
    }

    private class OpenProductRunnable implements Runnable {

        private final File file;

        public OpenProductRunnable(File file) {
            this.file = file;
        }

        public void run() {
            File[] selectedFiles;
            FileFilter selectedFileFilter = null;

            if (file == null || !file.exists()) {
                JFileChooser fileChooser = showOpenFileDialog();
                if (fileChooser == null) {
                    return;
                }
                selectedFiles = fileChooser.getSelectedFiles();
                selectedFileFilter = fileChooser.getFileFilter();
            } else {
                selectedFiles = new File[]{file};
            }

            Cursor oldCursor = getMainFrame().getCursor();
            UIUtils.setRootFrameWaitCursor(getMainFrame());

            StringBuffer msgBuffer = new StringBuffer();
            for (File selectedFile : selectedFiles) {
                if (getOpenProduct(selectedFile) != null) {
                    msgBuffer.append(String.format("Product is already open: %s\n", selectedFile));
                    continue;
                }

                ProductReader reader = getReader(selectedFile, selectedFileFilter);
                if (reader == null) {
                    msgBuffer.append(String.format("No appropriate reader found: %s\n", selectedFile));  /*I18N*/
                    continue;
                }

                setStatusBarMessage(String.format("Opening product %s...", selectedFile)); /*I18N*/
                Product product = loadProduct(reader, selectedFile);
                if (product == null) {
                    msgBuffer.append(String.format("Not able to read file: %s\n", selectedFile));  /*I18N*/
                    continue;
                }
                addProduct(product);
                historyPush(selectedFile);
            }

            if (msgBuffer.length() > 0) {
                showWarningDialog(msgBuffer.toString());
            }
            updateState();
            UIUtils.setRootFrameCursor(getMainFrame(), oldCursor);
        }

        private JFileChooser showOpenFileDialog() {
            String lastDir = getPreferences().getPropertyString(PROPERTY_KEY_APP_LAST_OPEN_DIR,
                                                                SystemUtils.getUserHomeDir().getPath());
            String lastFormat = getPreferences().getPropertyString(PROPERTY_KEY_APP_LAST_OPEN_FORMAT,
                                                                   ALL_FILES_IDENTIFIER);
            BeamFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setCurrentDirectory(new File(lastDir));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setDialogTitle(getAppName() + " - " + "Open Data Product(s)"); /*I18N*/
            fileChooser.setMultiSelectionEnabled(true);

            FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
            Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (allReaderPlugIns.hasNext()) {
                final ProductIOPlugIn plugIn = (ProductIOPlugIn) allReaderPlugIns.next();
                BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
                fileChooser.addChoosableFileFilter(productFileFilter);
                if (!ALL_FILES_IDENTIFIER.equals(lastFormat) &&
                        productFileFilter.getFormatName().equals(lastFormat)) {
                    actualFileFilter = productFileFilter;
                }
            }
            fileChooser.setFileFilter(actualFileFilter);

            int result = fileChooser.showDialog(getMainFrame(), "Open Product");    /*I18N*/
            if (result != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            String currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (currentDir != null) {
                getPreferences().setPropertyString(PROPERTY_KEY_APP_LAST_OPEN_DIR, currentDir);
            }

            if (fileChooser.getFileFilter() instanceof BeamFileFilter) {
                String currentFormat = ((BeamFileFilter) fileChooser.getFileFilter()).getFormatName();
                if (currentFormat != null) {
                    getPreferences().setPropertyString(PROPERTY_KEY_APP_LAST_OPEN_FORMAT, currentFormat);
                }
            } else {
                getPreferences().setPropertyString(PROPERTY_KEY_APP_LAST_OPEN_FORMAT, ALL_FILES_IDENTIFIER);
            }
            return fileChooser;

        }

        private Product loadProduct(ProductReader reader, File selectedFile) {
            Product product = null;
            try {
                product = reader.readProductNodes(selectedFile, null);
            } catch (Exception e) {
                handleUnknownException(e);
            } finally {
                clearStatusBarMessage();
            }
            return product;
        }


        private ProductReader getReader(File selectedFile, FileFilter selectedFileFilter) {
            if (selectedFileFilter instanceof BeamFileFilter) {
                return ProductIO.getProductReader(((BeamFileFilter) selectedFileFilter).getFormatName());
            } else {
                return ProductIO.getProductReaderForFile(selectedFile);
            }
        }

    }

    private class ToolBarListener extends DockableBarAdapter {

        @Override
        public void dockableBarHidden(DockableBarEvent dockableBarEvent) {
            updateState();
        }
    }
}