/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat;

import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.action.DockableBarContext;
import com.jidesoft.status.LabelStatusBarItem;
import org.esa.beam.framework.dataio.ProductCache;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.diag.TileCacheDiagnosisToolView;
import org.esa.beam.visat.toolviews.stat.*;
import org.esa.nest.dat.actions.LoadTabbedLayoutAction;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;
import org.esa.nest.dat.views.polarview.PolarView;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.db.ProductDB;
import org.esa.nest.util.MemUtils;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatApp extends VisatApp {

    public static final String PROCESSORS_TOOL_BAR_ID = "processorsToolBar";
    public static final String LABELS_TOOL_BAR_ID = "labelsToolBar";

    public DatApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);

        DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS = true;

        // enable anti-aliased text:

        System.setProperty("sun.java2d.opengl","true");
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
    }

    public static DatApp getApp() {
        return (DatApp) VisatApp.getApp();
    }

    @Override
    protected String getMainFrameTitle() {
        final String ver = System.getProperty(ResourceUtils.getContextID()+".version");
        return getAppName() + ' '+ver;
    }

    // You can now override numerous createXXX() methods
    // to customize the application GUI

    @Override
    protected ModalDialog createAboutBox() {
        return new DatAboutBox();
    }

    @Override
    protected void initClientUI(ProgressMonitor pm) {
        super.initClientUI(pm);

        final CommandBar processorToolBar = createProcessorToolBar();
        processorToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
        processorToolBar.getContext().setInitIndex(3);
        getMainFrame().getDockableBarManager().addDockableBar(processorToolBar);

        final CommandBar labelToolBar = createLabelToolBar();
        labelToolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_EAST);
        labelToolBar.getContext().setInitIndex(4);
        getMainFrame().getDockableBarManager().addDockableBar(labelToolBar);

        getMainFrame().setIconImage(ResourceUtils.nestIcon.getImage());

        updateGraphMenu();
    }

    @Override
    protected void configureJaiTileCache() {
        MemUtils.createTileCache();
        super.configureJaiTileCache();
    }

    @Override
    protected void postInit() {
        try {
            final String getStarted = VisatApp.getApp().getPreferences().getPropertyString("visat.showGettingStarted", "true");
            if(getStarted == null || getStarted.equals("true")) {
                LoadTabbedLayoutAction.loadTabbedLayout();

                HelpSys.showTheme("top");
                VisatApp.getApp().getPreferences().setPropertyString("visat.showGettingStarted", "false");
            }

            //disable JAI media library
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            disableUnwantedOperators();

            validateAuxDataFolder();

            UIManager.put("List.lockToPositionOnScroll", Boolean.FALSE);

            backgroundInitTasks();
        } catch(Throwable t) {
            VisatApp.getApp().showErrorDialog("PostInit failed. "+t.toString());
        }
    }
    
    protected void disableUnwantedOperators() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();

        //final OperatorSpi pcaOp = registry.getOperatorSpi("org.esa.nest.gpf.PCAOp$Spi");
        //if(pcaOp != null)
        //    registry.removeOperatorSpi(pcaOp);
    }

    private static void validateAuxDataFolder() throws IOException {
        File NestData = new File("~\\NestData");
        if(Settings.isWindowsOS()) {
            NestData = new File("c:\\NestData");
        }
        File auxDataFolder = Settings.getAuxDataFolder();
        if(!auxDataFolder.exists()) {
            if(NestData.exists()) {
                NestData.renameTo(auxDataFolder);
            }
        }
    }

    private static void backgroundInitTasks() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new BackgroundInitRunnable());
    }

    private static class BackgroundInitRunnable implements Runnable {

        private BackgroundInitRunnable() {
        }

        @Override
        public void run() {
            try {
                ProductDB.instance();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes the all open products.
     */
    @Override
    public synchronized void closeAllProducts() {
        final Product[] products = getProductManager().getProducts();
        for (int i = products.length - 1; i >= 0; i--) {
            final Product product = products[i];
            closeProduct(product);
        }
        
        ProductCache.instance().clearCache();
        MemUtils.freeAllMemory();
    }

    /**
     * Removes and disposes the given product and performs a garbage collection. After calling this method, the product
     * object cannot be used anymore.
     *
     * @param product the product to be disposed
     * @see org.esa.beam.framework.datamodel.Product#dispose
     */
    @Override
    public void disposeProduct(final Product product) {
        removeProduct(product);
        ProductCache.instance().removeProduct(product.getFileLocation());
        product.dispose();
    }

    @Override
    public synchronized void shutDown() {
        cleanTempFolder();

        super.shutDown();
    }

    private static void cleanTempFolder() {
        final File tempFolder = ResourceUtils.getApplicationUserTempDataDir();

        File[] fileList = tempFolder.listFiles();
        if(fileList == null) return;

        for(File file : fileList) {
            if(file.getName().startsWith("tmp_")) {
                ResourceUtils.deleteFile(file);
            }
        }

        long freeSpace = tempFolder.getFreeSpace() / 1024 / 1024 / 1024;
        int cutoff = 20;
        if(freeSpace > 30)
            cutoff = 60;

        fileList = tempFolder.listFiles();
        if(fileList != null && fileList.length > cutoff) {
            final long[] dates = new long[fileList.length];
            int i = 0;
            for(File file : fileList) {
                dates[i++] = file.lastModified();
            }
            Arrays.sort(dates);
            final long cutoffDate = dates[dates.length - cutoff];

            for(File file : fileList) {
                if(file.lastModified() < cutoffDate) {
                    file.delete();
                }
            }
        }
    }

    @Override
    protected String getCSName(final RasterDataNode raster) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(raster.getProduct());
        final String mapProjStr = absRoot.getAttributeString(AbstractMetadata.map_projection, "").trim();
        if(!mapProjStr.isEmpty()) {
            return mapProjStr;
        }

        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
            return geoCoding.getMapCRS().getName().toString();
        } else {
            return "Satellite coordinates";
        }
    }

    /**
     * Creates a standard status bar for this application.
     */
    @Override
    protected com.jidesoft.status.StatusBar createStatusBar() {
        final com.jidesoft.status.StatusBar statusBar = super.createStatusBar();

        final LabelStatusBarItem valueItem = new LabelStatusBarItem("STATUS_BAR_VALUE_ITEM");
        valueItem.setText("");
        valueItem.setPreferredWidth(50);
        valueItem.setAlignment(JLabel.CENTER);
        valueItem.setToolTipText("Displays pixel value");
        statusBar.add(valueItem, 3);

        final LabelStatusBarItem dimensions = new LabelStatusBarItem("STATUS_BAR_DIMENSIONS_ITEM");
        dimensions.setText("");
        dimensions.setPreferredWidth(70);
        dimensions.setAlignment(JLabel.CENTER);
        dimensions.setToolTipText("Displays image dimensions");
        statusBar.add(dimensions, 4);

        return statusBar;
    }

    @Override
    protected HashSet<String> getExcludedToolbars() {
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
        excludedIds.add("org.esa.beam.visat.toolviews.placemark.pin.PinManagerToolView");
        excludedIds.add("org.esa.beam.visat.toolviews.placemark.gcp.GcpManagerToolView");
        excludedIds.add("org.esa.nest.dat.toolviews.worldmap.NestWorldMapToolView");

        return excludedIds;
    }

    @Override
    protected void addDefaultToolViewCommands(final List<String> commandIds) {
        // add default views grouped
        commandIds.add("org.esa.nest.dat.toolviews.Projects.ProjectsToolView.showCmd");
        commandIds.add("org.esa.beam.visat.ProductsToolView.showCmd");
        commandIds.add("org.esa.beam.visat.toolviews.pixelinfo.PixelInfoToolView.showCmd");
        commandIds.add(null);
        commandIds.add("org.esa.beam.visat.toolviews.nav.NavigationToolView.showCmd");
        commandIds.add("org.esa.beam.visat.toolviews.imageinfo.ColorManipulationToolView.showCmd");
        commandIds.add("org.esa.beam.visat.toolviews.layermanager.LayerManagerToolView.showCmd");
        commandIds.add(null);
    }

    /**
     * Overrides the base class version in order to create a tool bar for VISAT.
     */
    @Override
    protected CommandBar createMainToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = createToolBar(MAIN_TOOL_BAR_ID, "Standard");

        addCommandsToToolBar(toolBar, new String[]{
                "newProject",
                "loadProject",
                null,
                "openRaster",
                //"openVector",
                "save",
        });

        return toolBar;
    }

    /**
     * Overrides the base class version in order to creates the menu bar for VISAT.
     */
    @Override
    protected CommandBar createMainMenuBar() {
        final CommandMenuBar menuBar = new CommandMenuBar("Main Menu");
        menuBar.setHidable(false);
        menuBar.setStretch(true);

        boolean incMultispectralTools = false;
        boolean incImageProcessing = false;
        boolean incWizards = false;
        final CommandManager cmdMan = getCommandManager();
        for (int i = 0; i < cmdMan.getNumCommands(); i++) {
            final String parent = cmdMan.getCommandAt(i).getParent();
            if(parent == null)
                continue;

            if(parent.equals("multispectraltools"))
                incMultispectralTools = true;
            else if(parent.equals("Image Processing"))
                incImageProcessing = true;
            else if(parent.equals("Wizards"))
                incWizards = true;
        }

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
                ProfilePlotToolView.ID + SHOW_TOOLVIEW_CMD_POSTFIX));
        menuBar.add(createJMenu("tools", "Utilities", 'U'));
        menuBar.add(createJMenu("sartools", "SAR Tools", 'S'));
        menuBar.add(createJMenu("geometry", "Geometry", 'G'));
        menuBar.add(createJMenu("insar", "InSAR", 'I'));
        menuBar.add(createJMenu("oceanTools", "Ocean Tools", 'O'));
        menuBar.add(createJMenu("polarimetrictools", "Polarimetric", 'P'));
        if(incMultispectralTools)
            menuBar.add(createJMenu("multispectraltools", "Multispectral Tools", 'M'));
        if(incImageProcessing)
            menuBar.add(createJMenu("Image Processing", "Image Processing", 'C'));
        menuBar.add(createJMenu("graphs", "Graphs", 'R'));
        if(incWizards)
            menuBar.add(createJMenu("Wizards", "Wizards", 'Z'));
        menuBar.add(createJMenu("window", "Window", 'W'));
        menuBar.add(createJMenu("help", "Help", 'H'));

        return menuBar;
    }

    private void updateGraphMenu() {
        final JMenu menu = findMenu("graphs");
        if (menu == null) {
            return;
        }

        final File graphPath = ResourceUtils.getGraphFolder("");
        if(!graphPath.exists()) return;

        createGraphMenu(menu, graphPath);
    }

    private static void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if(filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if(file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if(name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        final GraphBuilderDialog dialog = new GraphBuilderDialog(VisatApp.getApp(),
                            "Graph Builder", "graph_builder");
                        dialog.show();
                        dialog.LoadGraph(file);
                    }
                });
                menu.add(item);
            }
        }
    }

    @Override
    protected CommandBar createAnalysisToolBar() {
        final CommandBar toolBar = super.createAnalysisToolBar();

        addCommandsToToolBar(toolBar, new String[]{
                "editMetadata"
        });
        return toolBar;
    }

    protected CommandBar createProcessorToolBar() {
        // context of action in module.xml used as key
        final CommandBar toolBar = new CommandBar(PROCESSORS_TOOL_BAR_ID, "Processors");
        toolBar.addDockableBarListener(new ToolBarListener());

        addCommandsToToolBar(toolBar, new String[]{
                "openGraphBuilderDialog",
                "batchProcessing"
        });

        return toolBar;
    }

    protected CommandBar createLabelToolBar() {
        final CommandBar toolBar = createToolBar(LABELS_TOOL_BAR_ID, "Labels and GeoTags");
        final LinkedList<String> cmdList = new LinkedList<String>();
        final Map<String, String> placeAfterMap = new HashMap<String, String>();

        cmdList.add("pinTool");
        cmdList.add("gcpTool");
        
        final CommandManager cmdMan = getCommandManager();
        final int numCmds = cmdMan.getNumCommands();
        for(int i=0; i < numCmds; ++i) {
            final Command cmd = cmdMan.getCommandAt(i);
            final String parent = cmd.getParent();
            if(parent != null && parent.equals(LABELS_TOOL_BAR_ID)) {
                placeAfterMap.put(cmd.getCommandID(), cmd.getPlaceAfter());
                cmdList.add(cmd.getCommandID());
            }
        }

        // order
        final Set<String> placeAfterSet = placeAfterMap.keySet();
        for(String id : placeAfterSet) {
            final String placeAfter = placeAfterMap.get(id);
            int index = cmdList.indexOf(placeAfter);
            if(index != -1) {
                cmdList.remove(id);
                index = cmdList.indexOf(placeAfter);
                cmdList.add(index+1, id);
            }
        }

        addCommandsToToolBar(toolBar, cmdList.toArray(new String[cmdList.size()]));

        return toolBar;
    }

    @Override
    protected CommandBar createInteractionsToolBar() {
        final CommandBar toolBar = createToolBar(INTERACTIONS_TOOL_BAR_ID, "Interactions");
        addCommandsToToolBar(toolBar, new String[]{
                // These IDs are defined in the module.xml
                "selectLayerTool",
                "rangeFinder",
                "zoomTool",
                "pannerTool",
                null,
                "drawLineTool",
                "drawPolylineTool",
                "drawRectangleTool",
                "drawEllipseTool",
                "drawPolygonTool",
                "createVectorDataNode",
                // Magic Wand removed for 4.10 release
                "true".equalsIgnoreCase(System.getProperty("beam.magicWandTool.enabled", "false")) ? "magicWandTool" : null,
        });

        return toolBar;
    }

        /**
     * Closes all (internal) frames associated with the given product.
     *
     * @param product The product to close the internal frames for.
     */
    @Override
    public synchronized void closeAllAssociatedFrames(final Product product) {
        super.closeAllAssociatedFrames(product);

        boolean frameFound;
        do {
            frameFound = false;
            final JInternalFrame[] frames = getDesktopPane().getAllFrames();
            if (frames == null) {
                break;
            }
            for (final JInternalFrame frame : frames) {
                final Container cont = frame.getContentPane();
                Product frameProduct = null;
                if (cont instanceof PolarView) {
                    final PolarView view = (PolarView) cont;
                    frameProduct = view.getProduct();
                }
                if (frameProduct != null && frameProduct == product) {
                    getDesktopPane().closeFrame(frame);
                    frameFound = true;
                    break;
                }
            }
        } while (frameFound);
    }
}
