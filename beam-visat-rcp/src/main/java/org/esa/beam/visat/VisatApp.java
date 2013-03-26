/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.actions.CopyAction;
import com.bc.ceres.swing.actions.CutAction;
import com.bc.ceres.swing.actions.DeleteAction;
import com.bc.ceres.swing.actions.PasteAction;
import com.bc.ceres.swing.actions.SelectAllAction;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.ceres.swing.selection.SelectionManager;
import com.bc.ceres.swing.selection.support.DefaultSelectionManager;
import com.bc.swing.desktop.TabbedDesktopPane;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.action.DockableBarContext;
import com.jidesoft.status.LabelStatusBarItem;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.status.ResizeStatusBarItem;
import com.jidesoft.status.TimeStatusBarItem;
import com.jidesoft.swing.JideBoxLayout;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptorRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.ProductVisitorAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamException;
import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.FileHistory;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.NewProductDialog;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ToolCommand;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.jai.BandOpImage;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.PropertyMapChangeListener;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.visat.actions.ShowImageViewAction;
import org.esa.beam.visat.actions.ShowImageViewRGBAction;
import org.esa.beam.visat.actions.ShowToolBarAction;
import org.esa.beam.visat.toolviews.diag.TileCacheDiagnosisToolView;
import org.esa.beam.visat.toolviews.stat.DensityPlotToolView;
import org.esa.beam.visat.toolviews.stat.GeoCodingToolView;
import org.esa.beam.visat.toolviews.stat.HistogramPlotToolView;
import org.esa.beam.visat.toolviews.stat.InformationToolView;
import org.esa.beam.visat.toolviews.stat.ProfilePlotToolView;
import org.esa.beam.visat.toolviews.stat.ScatterPlotToolView;
import org.esa.beam.visat.toolviews.stat.StatisticsToolView;

import javax.media.jai.JAI;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * The <code>VisatApp</code> class represents the VISAT application.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class VisatApp extends BasicApp implements AppContext {

    /**
     * VISAT's plug-in directory
     *
     * @deprecated since BEAM 4.11, plugin directory is not used anymore
     */
    @Deprecated
    public static final String APP_DEFAULT_PLUGIN_DIR = SystemUtils.EXTENSION_DIR_NAME;
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
     * Preferences key for display style of geo-locations
     */
    public static final String PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL = "geolocation.display.decimal";

    /**
     * Default value for display style of geo-locations.
     */
    public static final boolean PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL = false;

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

    public static final String MAIN_TOOL_BAR_ID = "mainToolBar";
    public static final String VIEWS_TOOL_BAR_ID = "viewsToolBar";
    public static final String INTERACTIONS_TOOL_BAR_ID = "toolsToolBar";
    public static final String ANALYSIS_TOOL_BAR_ID = "analysisToolBar";
    public static final String LAYERS_TOOL_BAR_ID = "layersToolBar";

    /**
     * The one and only visat instance
     */
    private static VisatApp instance;
    private static final String SHOW_TOOLVIEW_CMD_POSTFIX = ".showCmd";

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

    private VisatApplicationPage applicationPage;

    private ProductsToolView productsToolView;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private File sessionFile;

    private Interactor activeInteractor = NullInteractor.INSTANCE;
    private Interactor selectionInteractor;

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
                @Override
                public void productAdded(ProductManager.Event event) {
                    event.getProduct().addProductNodeListener(productNodeListener);
                }

                @Override
                public void productRemoved(ProductManager.Event event) {
                    event.getProduct().removeProductNodeListener(productNodeListener);
                }
            });

            getMainFrame().getDockingManager().setHideFloatingFramesOnSwitchOutOfApplication(true);
            getMainFrame().getDockingManager().setHideFloatingFramesWhenDeactivate(false);

            desktopPane = new TabbedDesktopPane();

            applicationPage = new VisatApplicationPage(getMainFrame(),
                                                       getCommandManager(),
                                                       new DefaultSelectionManager(this),
                                                       getMainFrame().getDockingManager(),
                                                       desktopPane);

            pm.setTaskName("Loading commands");
            loadCommands();
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

    private void loadCommands() {
        Command[] commands = VisatActivator.getInstance().getCommands();
        for (Command command : commands) {
            addCommand(command, getCommandManager());
            if ("selectTool".equals(command.getCommandID())) {
                ToolCommand toolCommand = (ToolCommand) command;
                selectionInteractor = toolCommand.getInteractor();
                setActiveInteractor(selectionInteractor);
                toolCommand.setSelected(true);
            }
        }
    }


    public Interactor getActiveInteractor() {
        return activeInteractor;
    }

    public void setActiveInteractor(Interactor interactor) {
        activeInteractor.deactivate();
        activeInteractor = interactor;
        activeInteractor.activate();
        setInteractor(getSelectedProductSceneView(), activeInteractor);
    }

    private void setInteractor(Component contentPane, Interactor interactor) {
        if (contentPane instanceof FigureEditorAware) {
            final FigureEditor figureEditor = ((FigureEditorAware) contentPane).getFigureEditor();
            figureEditor.setInteractor(interactor);
        }
    }

    @Override
    public String getApplicationName() {
        return getApplicationDescriptor().getDisplayName();
    }

    @Override
    public Window getApplicationWindow() {
        return getMainFrame();
    }

    @Override
    public void handleError(String message, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        showErrorDialog(getAppName(), message);
        getLogger().log(Level.SEVERE, message, t);
    }

    @Override
    protected void initClientUI(ProgressMonitor pm) {
        try {
            pm.beginTask(String.format("Initialising %s UI components", getAppName()), 5);

            CommandBar layersToolBar = createLayersToolBar();
            layersToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            layersToolBar.getContext().setInitIndex(2);
            getMainFrame().getDockableBarManager().addDockableBar(layersToolBar);
            pm.worked(1);

            CommandBar analysisToolBar = createAnalysisToolBar();
            analysisToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
            analysisToolBar.getContext().setInitIndex(2);
            getMainFrame().getDockableBarManager().addDockableBar(analysisToolBar);
            pm.worked(1);

            CommandBar toolsToolBar = createInteractionsToolBar();
            toolsToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_EAST);
            toolsToolBar.getContext().setInitIndex(1);
            getMainFrame().getDockableBarManager().addDockableBar(toolsToolBar);
            pm.worked(1);

            CommandBar[] viewToolBars = createViewsToolBars();
            for (CommandBar viewToolBar : viewToolBars) {
                if (VIEWS_TOOL_BAR_ID.equals(viewToolBar.getName())) {
                    viewToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
                    viewToolBar.getContext().setInitIndex(2);
                } else {
                    viewToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_EAST);
                    viewToolBar.getContext().setInitIndex(1);
                }
                getMainFrame().getDockableBarManager().addDockableBar(viewToolBar);
            }
            pm.worked(1);

            registerForMacOSXEvents();
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs

    private void registerForMacOSXEvents() {
        if (SystemUtils.isRunningOnMacOS()) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("shutDown", (Class[]) null));
                JMenu fileMenu = findMainMenu("file");
                JMenuItem menuItem = findMenuItem("exit", fileMenu);
                fileMenu.remove(menuItem);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error while loading the OSXAdapter", e);
            }
        }
    }

    private JMenuItem findMenuItem(final String command, final JMenu menu) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem != null && command.equals(menuItem.getActionCommand())) {
                return menuItem;
            }
        }
        return null;
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

    @Override
    public VisatApplicationPage getApplicationPage() {
        return applicationPage;
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
        getExecutorService().shutdown();
        super.handleImminentExit();
    }

    private void registerShowToolViewCommands() {

        ToolViewDescriptor[] toolViewDescriptors = VisatActivator.getInstance().getToolViewDescriptors();
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            // triggers also command registration in command manager
            toolViewDescriptor.createShowViewCommand(applicationPage);
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
     * @return The file of the current session.
     *
     * @since BEAM 4.6
     */
    public File getSessionFile() {
        return sessionFile;
    }

    /**
     * Sets the file of the current session.
     *
     * @param sessionFile The file of the current session.
     *
     * @since BEAM 4.6
     */
    public void setSessionFile(File sessionFile) {
        this.sessionFile = sessionFile;
        updateMainFrameTitle();
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
        if (product.getFileLocation() != null) {
            historyPush(product.getFileLocation());
        }
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
     *
     * @see org.esa.beam.framework.datamodel.Product#dispose
     */
    public void disposeProduct(final Product product) {
        removeProduct(product);
        product.dispose();
    }

    /**
     * Returns the product manager which holds the list of currently open products.
     */
    @Override
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
    @Override
    public Product getSelectedProduct() {
        if (selectedNode instanceof Product) {
            return (Product) selectedNode;
        } else if (selectedNode != null) {
            return selectedNode.getProduct();
        } else if (getSelectedProductSceneView() != null) {
            return getSelectedProductSceneView().getProduct();
        } else if (getSelectedProductMetadataView() != null) {
            return getSelectedProductMetadataView().getProduct();
        } else if (getProductManager().getProductCount() == 1) {
            return getProductManager().getProduct(0);
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
        updateMainFrameTitle();
        getProductTree().select(selectedNode);
        updateState();
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
    @Override
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
     *
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
     *
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
     *
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
     *
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
     *
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
                final ProductNode visibleProductNode = view.getVisibleProductNode();
                if (visibleProductNode != null && visibleProductNode == productNode) {
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
     *
     * @return the product associated with the given file. or <code>null</code> if no such exists.
     */
    public Product getOpenProduct(final File file) {
        final ProductManager productManager = getProductManager();
        for (int i = 0; i < productManager.getProductCount(); i++) {
            final Product product = productManager.getProduct(i);
            final File productFile = product.getFileLocation();
            if (file.equals(productFile)) {
                return product;
            }
        }
        return null;
    }

    /**
     * Closes all (internal) frames associated with the given product.
     *
     * @param product The product to close the internal frames for.
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
                if (cont instanceof ProductNodeView) {
                    final ProductNodeView nodeView = (ProductNodeView) cont;
                    ProductNode visibleProductNode = nodeView.getVisibleProductNode();
                    if (visibleProductNode != null) {
                        frameProduct = visibleProductNode.getProduct();
                    }
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
     *
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
                        @Override
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
            @Override
            public void updateView(final ProductSceneView view) {
                updateImage(view);
            }
        });
    }

    public void updateImage(final ProductSceneView view) {
        view.updateImage();
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

    public synchronized void openProduct(final File file) {
        openProductImpl(file);
    }

    private void openProductImpl(final File file) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new OpenProductRunnable(file));
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
                    boolean incremental = getPreferences().getPropertyBool(PROPERTY_KEY_SAVE_INCREMENTAL,
                                                                           DEFAULT_VALUE_SAVE_INCREMENTAL);
                    success = saveProductImpl(product, incremental);
                } finally {
                    if (success) {
                        product.setModified(false);
                    }
                }
                return null;
            }
        };
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
    public void openProductSceneView(final RasterDataNode raster) {
        final ShowImageViewAction command = (ShowImageViewAction) getCommandManager().getCommand(
                ShowImageViewAction.ID);
        command.openProductSceneView(raster);
    }

    /**
     * Asks the user to select or define a RGB profile from which the product scene view will be
     * created and opend as internal frame.
     */
    public void openProductSceneViewRGB(final Product product, final String helpId) {
        final ShowImageViewRGBAction command = (ShowImageViewRGBAction) getCommandManager().getCommand(
                ShowImageViewRGBAction.ID);
        command.openProductSceneViewRGB(product, helpId);
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
    public void updateMainFrameTitle() {
        final StringBuilder docTitle = new StringBuilder();

        final ProductNode productNode = getSelectedProductNode();
        if (productNode != null) {
            docTitle.append(productNode.getDisplayName());

            docTitle.append(" - [");
            final Product product = productNode.getProduct();
            File productFile = product.getFileLocation();
            if (productFile != null) {
                if (product.isModified()) {
                    docTitle.append("*");
                }
                docTitle.append(FileUtils.getDisplayText(productFile, 100));
            } else {
                docTitle.append("Product not saved");
            }
            docTitle.append("]");

            docTitle.append(" - [");
            final File sessionFile = getSessionFile();
            if (sessionFile != null) {
                docTitle.append(FileUtils.getDisplayText(sessionFile, 50));
            } else {
                docTitle.append("Session not saved");
            }
            docTitle.append("]");
        }

        setCurrentDocTitle(docTitle.toString());
    }

    private Product newProductImpl() {
        if (getProductManager().getProductCount() == 0) {
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
        } else if (dialog.getException() != null) {
            showErrorDialog("The product subset could not be created:\n" + dialog.getException().getMessage());
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
            String[] headerNames = new String[]{
                    "MPH", "SPH",
                    "Earth_Explorer_Header", "Fixed_Header", "Variable_Header", "Specific_Product_Header",
                    "Global_Attributes", "GlobalAttributes", "Variable_Attributes"
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

        final boolean saveOk = writeProductImpl(product, file,
                                                DimapProductConstants.DIMAP_FORMAT_NAME,
                                                incremental);
        if (saveOk) {
            product.setModified(false);
            historyPush(file);
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
            GPF.writeProduct(product,
                             file,
                             formatName,
                             incremental,
                             pm);
            updateState();
            status = !pm.isCanceled();
        } catch (Exception e) {
            handleUnknownException(e);
        } finally {
            UIUtils.setRootFrameDefaultCursor(getMainFrame());
            clearStatusBarMessage();
        }
        return status;
    }

    public ExecutorService getExecutorService() {
        return singleThreadExecutor;
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
                                                  "Depending on the product size the conversion also may take a while.\n\n" +
                                                  "Do you really want to convert the product now?\n",
                                                  "productConversionRequired"); /*I18N*/
            if (answer != 0) { // Zero means YES
                return;
            }
        }


        String fileName;
        if (product.getFileLocation() != null) {
            fileName = product.getFileLocation().getName();
        } else {
            fileName = product.getName();
        }
        final File newFile = showFileSaveDialog("Save Product As",
                                                false,
                                                DimapProductHelpers.createDimapFileFilter(),
                                                DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION,
                                                fileName); /*I18N*/
        if (newFile == null) {
            return;
        }

        final String oldProductName = product.getName();
        final File oldFile = product.getFileLocation();

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
                if (successfullySaved) {
                    if (!isVisatExitConfirmed()) {
                        reopenProduct(product, newFile);
                    }
                }
                if (!successfullySaved) {
                    product.setFileLocation(oldFile);
                    product.setName(oldProductName);
                }
                return null;
            }

            @Override
            public void done() {
            }
        };
        worker.execute();
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
        final FileHistory history = getFileHistory();
        final String[] entries = history.getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                final String filePath = entries[i];
                final JMenuItem item = new JMenuItem(String.format("%d: %s", (i + 1), filePath));
                item.setMnemonic('1' + i);
                item.addActionListener(new ActionListener() {
                    @Override
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
                @Override
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
                applyProductSceneViewPreferences();
                firePreferencesChanged();
            }
        }
    }

    private void applyProductSceneViewPreferences() {
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

    @Override
    public void clearStatusBarMessage() {
        final ProductSceneView sceneView = getSelectedProductSceneView();
        if (sceneView == null) {
            super.clearStatusBarMessage();
            return;
        }
        final RasterDataNode raster = sceneView.getRaster();
        final String csName;
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
            csName = geoCoding.getMapCRS().getName().toString();
        } else {
            csName = "Satellite coordinates";
        }
        super.setStatusBarMessage(raster.getDisplayName() + " - " + csName);
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
        final CommandBar toolBar = createToolBar(MAIN_TOOL_BAR_ID, "Standard");
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
        final CommandBar toolBar = createToolBar(LAYERS_TOOL_BAR_ID, "Layers");
        ArrayList<String> commandIdList = new ArrayList<String>(Arrays.asList(
                "showNoDataOverlay",
                "showShapeOverlay",
                "showGraticuleOverlay",
                "showWorldMapOverlay"));
        Set<PlacemarkDescriptor> placemarkDescriptors = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptors();
        for (PlacemarkDescriptor placemarkDescriptor : placemarkDescriptors) {
            if (placemarkDescriptor.getShowLayerCommandId() != null) {
                commandIdList.add(placemarkDescriptor.getShowLayerCommandId());
            }
        }
        addCommandsToToolBar(toolBar, commandIdList.toArray(new String[0]));
        return toolBar;
    }

    private CommandBar createAnalysisToolBar() {
        final CommandBar toolBar = createToolBar(ANALYSIS_TOOL_BAR_ID, "Analysis");
        addCommandsToToolBar(toolBar, new String[]{
                InformationToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                GeoCodingToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                StatisticsToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                HistogramPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                DensityPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                ScatterPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                ProfilePlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
        });
        return toolBar;
    }

    private CommandBar createInteractionsToolBar() {
        final CommandBar toolBar = createToolBar(INTERACTIONS_TOOL_BAR_ID, "Interactions");
        addCommandsToToolBar(toolBar, new String[]{
                // These IDs are defined in the module.xml
                "selectTool",
                "rangeFinder",
                "zoomTool",
                "pannerTool",
                "pinTool",
                "gcpTool",
                "drawLineTool",
                "drawPolylineTool",
                "drawRectangleTool",
                "drawEllipseTool",
                "drawPolygonTool",
                "createVectorDataNode",
                // Magic Wand removed for 4.10 release
                "true".equalsIgnoreCase(
                        System.getProperty("beam.magicWandTool.enabled", "false")) ? "magicWandTool" : null,
        });
        return toolBar;
    }


    private CommandBar[] createViewsToolBars() {

        final HashSet<String> excludedIds = new HashSet<String>(8);
        // todo - remove bad forward dependencies to tool views (nf - 30.10.2008)
        excludedIds.add(TileCacheDiagnosisToolView.ID);
        excludedIds.add(InformationToolView.ID);
        excludedIds.add(GeoCodingToolView.ID);
        excludedIds.add(StatisticsToolView.ID);
        excludedIds.add(HistogramPlotToolView.ID);
        excludedIds.add(ScatterPlotToolView.ID);
        excludedIds.add(DensityPlotToolView.ID);
        excludedIds.add(ProfilePlotToolView.ID);
        excludedIds.add("org.esa.beam.scripting.visat.ScriptConsoleToolView");
        excludedIds.add("org.esa.beam.visat.toolviews.layermanager.LayerEditorToolView");

        ToolViewDescriptor[] toolViewDescriptors = VisatActivator.getInstance().getToolViewDescriptors();

        Map<String, List<String>> toolBar2commandIds = new HashMap<String, List<String>>();
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            if (!excludedIds.contains(toolViewDescriptor.getId())) {
                final String commandId = toolViewDescriptor.getId() + SHOW_TOOLVIEW_CMD_POSTFIX;

                String toolBarId = toolViewDescriptor.getToolBarId();
                if (toolBarId == null || toolBarId.isEmpty()) {
                    toolBarId = VIEWS_TOOL_BAR_ID;
                }

                List<String> commandIds = toolBar2commandIds.get(toolBarId);
                if (commandIds == null) {
                    commandIds = new ArrayList<String>(5);
                    toolBar2commandIds.put(toolBarId, commandIds);
                }
                commandIds.add(commandId);
            }
        }

        List<CommandBar> viewToolBars = new ArrayList<CommandBar>(5);
        viewToolBars.add(createToolBar(VIEWS_TOOL_BAR_ID, "Views"));
        for (String toolBarId : toolBar2commandIds.keySet()) {
            CommandBar toolBar = getToolBar(toolBarId);
            if (toolBar == null) {
                // todo - use ToolBarDescriptor to define tool bar properties, e.g. title, dockSite, ...  (nf - 20090119)
                toolBar = createToolBar(toolBarId, toolBarId.replace('.', ' ').replace('_', ' '));
                viewToolBars.add(toolBar);

                // 	Retrospectively add "tool bar toggle" menu item
                ShowToolBarAction action = new ShowToolBarAction(toolBarId + ".showToolBar");
                action.setText(toolBarId);
                action.setContexts(new String[]{toolBarId});
                action.setToggle(true);
                action.setSelected(true);
                getCommandManager().addCommand(action);
                JMenu toolBarsMenu = findMenu("toolBars");
                toolBarsMenu.add(action.createMenuItem());
            }
            List<String> commandIds = toolBar2commandIds.get(toolBarId);
            addCommandsToToolBar(toolBar, commandIds.toArray(new String[commandIds.size()]));
        }

        return viewToolBars.toArray(new CommandBar[viewToolBars.size()]);
    }

    private void addCommandsToToolBar(CommandBar toolBar, String[] commandIDs) {
        for (final String commandID : commandIDs) {
            if (commandID == null) {
                toolBar.add(ToolButtonFactory.createToolBarSeparator());
            } else {
                final Command command = getCommandManager().getCommand(commandID);
                if (command != null) {
                    final AbstractButton toolBarButton = command.createToolBarButton();
                    toolBarButton.addMouseListener(getMouseOverActionHandler());
                    toolBar.add(toolBarButton);
                } else {
                    getLogger().warning(String.format("Toolbar '%s': No command found for ID = '%s'", toolBar.getName(),
                                                      commandID));
                }
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
        hookJaiTileCacheFlush(gc);

        return statusBar;
    }

    private static void hookJaiTileCacheFlush(MemoryStatusBarItem gc) {
        AbstractButton button = findButtonForIcon(gc, gc.getGcIcon());
        if (button != null) {
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JAI.getDefaultInstance().getTileCache().flush();
                    System.gc();
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
     *
     * @return a tool button which is automatically added to VISAT's tool button group.
     *
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

        menuBar.add(createJMenu("file", "File", 'F'));
        menuBar.add(createJMenu("edit", "Edit", 'E'));
        menuBar.add(createJMenu("view", "View", 'V'));
        menuBar.add(createJMenu("data", "Analysis", 'A',
                                InformationToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                GeoCodingToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                StatisticsToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                HistogramPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                ScatterPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                DensityPlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX,
                                ProfilePlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX
        ));
        menuBar.add(createJMenu("tools", "Tools", 'T'));
        menuBar.add(createJMenu("window", "Window", 'W'));
        menuBar.add(createJMenu("help", "Help", 'H'));

        return menuBar;
    }

// TODO: Not included in BEAM 4.7
// enable source code when undo/redo shall be supported
//    private UndoAction undoAction;
//    private RedoAction redoAction;
//    private UndoContext undoContext;

    @Override
    protected void insertCommandMenuItems() {
        super.insertCommandMenuItems();

        JMenu menu = findMenu("edit");

/*      // TODO: Not included in BEAM 4.7
        // enable source code when undo/redo shall be supported
        undoContext = new DefaultUndoContext(this);
        undoAction = new UndoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                redoAction.updateState();
            }
        };
        redoAction = new RedoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                undoAction.updateState();
            }
        };
*/

        SelectionManager selectionManager = getApplicationPage().getSelectionManager();
        Action cutAction = new CutAction(selectionManager);
        Action copyAction = new CopyAction(selectionManager);
        Action pasteAction = new PasteAction(selectionManager);
        Action selectAllAction = new SelectAllAction(selectionManager);
        Action deleteAction = new DeleteAction(selectionManager);

// TODO: Not included in BEAM 4.7
// enable source code when undo/redo shall be supported
//        menu.insert(undoAction, 0);
//        menu.insert(redoAction, 1);
        menu.insertSeparator(0);
        menu.insert(cutAction, 1);
        menu.insert(copyAction, 2);
        menu.insert(pasteAction, 3);
        menu.insertSeparator(4);
        menu.insert(selectAllAction, 5);
        menu.insertSeparator(6);
        menu.insert(deleteAction, 7);
        menu.insertSeparator(8);
    }


    /**
     * Creates an internal frame and adds it to VISAT's desktop.
     *
     * @param title   a frame title
     * @param icon    a frame icon, can be null
     * @param content the frame's content pane
     * @param helpId  the id for help system
     *
     * @return the newly created frame
     *
     * @deprecated Since BEAM 4.10, use {@link #createInternalFrame(String, javax.swing.Icon, javax.swing.JComponent, String, boolean)} instead
     */
    @Deprecated
    public synchronized JInternalFrame createInternalFrame(final String title, final Icon icon,
                                                           final JComponent content, final String helpId) {
        return createInternalFrame(title, icon, content, helpId, true);
    }

    /**
     * Creates an internal frame and adds it to VISAT's desktop.
     *
     * @param title         a frame title
     * @param icon          a frame icon, can be null
     * @param content       the frame's content pane
     * @param helpId        the id for help system
     * @param maximizeFrame flag indicating whether the frame is to be maximized
     *
     * @return the newly created frame
     */
    public synchronized JInternalFrame createInternalFrame(final String title, final Icon icon,
                                                           final JComponent content, final String helpId,
                                                           final boolean maximizeFrame) {
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
            ensureMinWidthAndHight(frame, 640, 480);
        } else {
            frame.setSize(640, 480);
        }

        try {
            desktopPane.addFrame(frame);
        } catch (RuntimeException e) {
            frame.dispose();
            throw e;
        }
        if (maximizeFrame) {
            try {
                // try to resize frame so that it completely fits into desktopPane
                frame.setMaximum(true);
            } catch (PropertyVetoException e) {
                // ok
            }
        }

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
                ProductNode productNode = event.getSourceNode();
                if (productNode != null) {
                    if (productNode.getProduct() == getSelectedProduct()) {
                        updateMainFrameTitle();
                    }
                }
            }
        };
    }

    private void ensureMinWidthAndHight(JInternalFrame frame, int minWidth, int minHeight) {
        final Dimension frameSize = frame.getSize();
        final int correctedWidth = Math.max(frameSize.width, minWidth);
        final int correctedHeight = Math.max(frameSize.height, minHeight);
        frame.setSize(new Dimension(correctedWidth, correctedHeight));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Nested classes & Interfaces

    /**
     * A method used to update a <code>ProductSceneView</code>.
     */
    public interface ViewUpdateMethod {

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
        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame activated: " + e);
            setSelectedProductNode(e.getInternalFrame());
            final Component contentPane = e.getInternalFrame().getContentPane();
            setInteractor(contentPane, activeInteractor);
            getApplicationPage().setActiveComponent();
            updateMainFrameTitle();
            updateState();
            clearStatusBarMessage();
        }

        /**
         * Invoked when an internal frame is de-activated.
         *
         * @see javax.swing.JInternalFrame#setSelected
         */
        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame deactivated: " + e);
            final Component contentPane = e.getInternalFrame().getContentPane();
            setInteractor(contentPane, NullInteractor.INSTANCE);
            getApplicationPage().setActiveComponent();
            updateState();
        }

        /**
         * Invoked when an internal frame has been opened.
         *
         * @see javax.swing.JInternalFrame#show
         */
        @Override
        public void internalFrameOpened(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame opened: " + e);
            setSelectedProductNode(e.getInternalFrame());
            final Container contentPane = e.getInternalFrame().getContentPane();
            setInteractor(contentPane, activeInteractor);
        }

        /**
         * Invoked when an internal frame is in the process of being closed. The close operation can be overridden at
         * this point.
         *
         * @see javax.swing.JInternalFrame#setDefaultCloseOperation
         */
        @Override
        public void internalFrameClosing(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame closing: " + e);
            updateState();
        }

        /**
         * Invoked when an internal frame has been closed.
         *
         * @see javax.swing.JInternalFrame#setClosed
         */
        @Override
        public void internalFrameClosed(final InternalFrameEvent e) {
            Debug.trace("VisatApp: internal frame closed: " + e);
            final String title = e.getInternalFrame().getTitle();
            final Container contentPane = e.getInternalFrame().getContentPane();
            setInteractor(contentPane, NullInteractor.INSTANCE);

            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView productSceneView = (ProductSceneView) contentPane;
                removePropertyMapChangeListener(productSceneView);

                final int numRasters = productSceneView.getNumRasters();

                JInternalFrame otherFrame = null;
                for (int i = 0; i < numRasters; i++) {
                    final RasterDataNode raster = productSceneView.getRaster(i);
                    otherFrame = findInternalFrame(raster);
                }

                // Save view-associtated objects before we dispose the view
                final JInternalFrame[] otherFrames = findInternalFrames(productSceneView.getRaster(), 1);

                if (otherFrames.length == 0) {
                    Debug.trace("Disposing layer model of view '" + title + "'...");
                    productSceneView.disposeLayers();
                    Debug.trace("Layer model disposed.");
                }

                Debug.trace("Disposing view '" + title + "'...");
                productSceneView.dispose();
                Debug.trace("View disposed.");

                if (otherFrame != null) {
                    try {
                        otherFrame.setSelected(true);
                    } catch (PropertyVetoException ignored) {
                        // ok
                    }
                }
            }

            updateState();
            clearStatusBarMessage();
        }

        /**
         * Invoked when an internal frame is iconified.
         *
         * @see javax.swing.JInternalFrame#setIcon
         */
        @Override
        public void internalFrameIconified(final InternalFrameEvent e) {
            updateState();
        }

        /**
         * Invoked when an internal frame is de-iconified.
         *
         * @see javax.swing.JInternalFrame#setIcon
         */
        @Override
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

        @Override
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

    private void reopenProduct(Product product, File newFile) throws IOException {
        DimapProductReader productReader = (DimapProductReader) ProductIO.getProductReader(
                DimapProductConstants.DIMAP_FORMAT_NAME);
        productReader.bindProduct(newFile, product);
        product.setProductReader(productReader);
        Band[] bands = product.getBands();
        for (Band band : bands) {
            if (band.isSourceImageSet() && band.getSourceImage().getImage(0) instanceof BandOpImage) {
                band.setSourceImage(null);
            }
        }
    }

    private class OpenProductRunnable implements Runnable {

        private final File file;

        private OpenProductRunnable(File file) {
            this.file = file;
        }

        @Override
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

            try {
                StringBuilder msgBuffer = new StringBuilder();
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
                }

                if (msgBuffer.length() > 0) {
                    showWarningDialog(msgBuffer.toString());
                }
                updateState();
            } finally {
                UIUtils.setRootFrameCursor(getMainFrame(), oldCursor);
            }
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
            Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            List<BeamFileFilter> sortedFileFilters = BeamFileFilter.getSortedFileFilters(allReaderPlugIns);
            for (BeamFileFilter productFileFilter : sortedFileFilters) {
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
                return ProductIO.getProductReaderForInput(selectedFile);
            }
        }

    }

}
