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
package org.esa.nest.dat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.action.DockableBarContext;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.actions.LoadTabbedLayoutAction;
import org.esa.snap.dat.graphbuilder.GraphBuilderDialog;
import org.esa.nest.dat.views.polarview.PolarView;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.db.ProductDB;
import org.esa.snap.util.MemUtils;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.util.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DatApp extends VisatApp {

    public static final String PROCESSORS_TOOL_BAR_ID = "processorsToolBar";
    public static final String LABELS_TOOL_BAR_ID = "labelsToolBar";

    public DatApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);

        //DEFAULT_VALUE_SAVE_PRODUCT_ANNOTATIONS = true;

        // enable anti-aliased text:

        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
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

        updateGraphMenu();

        postInit();
    }

    @Override
    protected void configureJaiTileCache() {
        MemUtils.createTileCache();
        super.configureJaiTileCache();
    }

    protected void loadLayout() {
        final String getStarted = VisatApp.getApp().getPreferences().getPropertyString("visat.showGettingStarted", "true");
        getMainFrame().setMinimumSize(new Dimension(1200, 800));
        if (getStarted == null || getStarted.equals("true")) {
            LoadTabbedLayoutAction.loadTabbedLayout();

            HelpSys.showTheme("top");
            VisatApp.getApp().getPreferences().setPropertyString("visat.showGettingStarted", "false");

            getMainFrame().setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        } else {
            getMainFrame().getLayoutPersistence().loadLayoutData(); // </JIDE>
        }
    }

    protected void postInit() {
        try {
            //disable JAI media library
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            disableOperatorPlugins();
            disableIOPlugins();

            validateAuxDataFolder();

            UIManager.put("List.lockToPositionOnScroll", Boolean.FALSE);

            installDefaultColorPalettes();

            backgroundInitTasks();
        } catch (Throwable t) {
            VisatApp.getApp().showErrorDialog("PostInit failed. " + t.toString());
        }
    }

    private void installDefaultColorPalettes() {
        final URL codeSourceUrl = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        final File auxdataDir = new File(SystemUtils.getApplicationDataDir(), "beam-ui/auxdata/color-palettes");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(codeSourceUrl, "auxdata/color_palettes/",
                auxdataDir);
        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(getMainFrame(),
                "Installing Auxdata...") {
            @Override
            protected Object doInBackground(ProgressMonitor progressMonitor) throws Exception {
                resourceInstaller.install(".*.cpd", progressMonitor);
                return Boolean.TRUE;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Could not install auxdata", e);
                }
            }
        };
        swingWorker.executeWithBlocking();
    }

    protected void disableOperatorPlugins() {

        removeOperator("org.esa.beam.gpf.operators.standard.MergeOp$Spi");
        removeOperator("org.esa.beam.pixex.PixExOp$Spi");
        removeOperator("org.esa.beam.statistics.StatisticsOp$Spi");
        removeOperator("org.esa.beam.gpf.operators.meris.N1PatcherOp$Spi");
    }

    protected void disableIOPlugins() {

        removeReaderPlugIn("org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn");
    }

    protected void removeOperator(final String spi) {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final OperatorSpi op = registry.getOperatorSpi(spi);
        if (op != null) {
            registry.removeOperatorSpi(op);
        }
    }

    protected void removeReaderPlugIn(final String name) {
        final ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
     //   ProductReaderPlugIn rp = registry.getReaderPlugIn(name);
     //   if (rp != null) {
     //       registry.removeReaderPlugIn(rp);
     //   }
    }

    private static void validateAuxDataFolder() throws IOException {
        File NestData = new File("~\\NestData");
        if (Settings.isWindowsOS()) {
            NestData = new File("c:\\NestData");
        }
        File auxDataFolder = Settings.getAuxDataFolder();
        if (!auxDataFolder.exists()) {
            if (NestData.exists()) {
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
                //speed up init of Product Library
                ProductDB.instance();

            } catch (Exception e) {
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

        //ProductCache.instance().clearCache();
        MemUtils.freeAllMemory();
    }

    @Override
    public synchronized void shutDown() {
        cleanTempFolder();

        super.shutDown();
    }

    private static void cleanTempFolder() {
        final File tempFolder = ResourceUtils.getApplicationUserTempDataDir();

        File[] fileList = tempFolder.listFiles();
        if (fileList == null) return;

        for (File file : fileList) {
            if (file.getName().startsWith("tmp_")) {
                ResourceUtils.deleteFile(file);
            }
        }

        long freeSpace = tempFolder.getFreeSpace() / 1024 / 1024 / 1024;
        int cutoff = 20;
        if (freeSpace > 30)
            cutoff = 60;

        fileList = tempFolder.listFiles();
        if (fileList != null && fileList.length > cutoff) {
            final long[] dates = new long[fileList.length];
            int i = 0;
            for (File file : fileList) {
                dates[i++] = file.lastModified();
            }
            Arrays.sort(dates);
            final long cutoffDate = dates[dates.length - cutoff];

            for (File file : fileList) {
                if (file.lastModified() < cutoffDate) {
                    file.delete();
                }
            }
        }
    }

    @Override
    protected String getCSName(final RasterDataNode raster) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(raster.getProduct());
        if (!AbstractMetadata.isNoData(absRoot, AbstractMetadata.map_projection)) {
            return absRoot.getAttributeString(AbstractMetadata.map_projection, AbstractMetadata.NO_METADATA_STRING);
        }

        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding instanceof MapGeoCoding || geoCoding instanceof CrsGeoCoding) {
            return geoCoding.getMapCRS().getName().toString();
        } else {
            return "Satellite coordinates";
        }
    }

    @Override
    protected HashSet<String> getExcludedToolbars() {
        final HashSet<String> excludedIds = super.getExcludedToolbars();

        excludedIds.add("org.esa.beam.visat.toolviews.spectrum.SpectrumToolView");
        excludedIds.add("org.esa.beam.visat.toolviews.placemark.pin.PinManagerToolView");
        excludedIds.add("org.esa.beam.visat.toolviews.placemark.gcp.GcpManagerToolView");
        excludedIds.add("org.esa.nest.dat.toolviews.worldmap.NestWorldMapToolView");
        excludedIds.add("org.csa.rstb.dat.toolviews.HaAlphaPlotToolView");

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
                "open",
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
        menuBar.setHidable(true);
        menuBar.setStretch(true);

        boolean incImageProcessing = false;
        final CommandManager cmdMan = getCommandManager();
        for (int i = 0; i < cmdMan.getNumCommands(); i++) {
            final String parent = cmdMan.getCommandAt(i).getParent();
            if (parent == null)
                continue;

            if (parent.equals("image-processing"))
                incImageProcessing = true;
        }

        menuBar.add(createJMenu("file", "File", 'F'));
        menuBar.add(createJMenu("edit", "Edit", 'E'));
        menuBar.add(createJMenu("view", "View", 'V'));
        menuBar.add(createAnalysisMenu());
        menuBar.add(createJMenu("tools", "Utilities", 'U'));
        //menuBar.add(createJMenu("processing", "Optical Processing", 'O'));
        menuBar.add(createJMenu("Sar Processing", "SAR Processing", 'S'));
        if (incImageProcessing)
            menuBar.add(createJMenu("image-processing", "Image Processing", 'M'));
        menuBar.add(createJMenu("processing.imageAnalysis", "Image Analysis", 'I'));
        menuBar.add(createJMenu("Graphs", "Graphs", 'G'));
        menuBar.add(createJMenu("window", "Window", 'W'));
        menuBar.add(createJMenu("help", "Help", 'H'));

        return menuBar;
    }

    private void updateGraphMenu() {
        final JMenu menu = findMenu("Graphs");
        if (menu == null) {
            return;
        }

        final File graphPath = ResourceUtils.getGraphFolder("");
        if (!graphPath.exists()) return;

        menu.add(new JSeparator());
        createGraphMenu(menu, graphPath);
    }

    private static void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
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
    protected JMenu createAnalysisMenu() {
        final JMenu menu = super.createAnalysisMenu();

        addCommandToMenu(menu, "org.csa.rstb.dat.toolviews.HaAlphaPlotToolView" + SHOW_TOOLVIEW_CMD_POSTFIX);
        return menu;
    }

    protected void addCommandToMenu(final JMenu menu, final String cmdID) {
        final Command command = getCommandManager().getCommand(cmdID);
        if (command != null) {
            menu.getPopupMenu().add(command.createMenuItem());
        }
    }

    @Override
    protected CommandBar createAnalysisToolBar() {
        final CommandBar toolBar = super.createAnalysisToolBar();

        addCommandsToToolBar(toolBar, new String[]{
                "org.csa.rstb.dat.toolviews.HaAlphaPlotToolView" + SHOW_TOOLVIEW_CMD_POSTFIX
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
        for (int i = 0; i < numCmds; ++i) {
            final Command cmd = cmdMan.getCommandAt(i);
            final String parent = cmd.getParent();
            if (parent != null && parent.equals("labels")) {
                placeAfterMap.put(cmd.getCommandID(), cmd.getPlaceAfter());
                cmdList.add(cmd.getCommandID());
            }
        }

        // order
        final Set<String> placeAfterSet = placeAfterMap.keySet();
        for (String id : placeAfterSet) {
            final String placeAfter = placeAfterMap.get(id);
            int index = cmdList.indexOf(placeAfter);
            if (index != -1) {
                cmdList.remove(id);
                index = cmdList.indexOf(placeAfter);
                cmdList.add(index + 1, id);
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
                "magicWandTool",
                "drawLineTool",
                "drawPolylineTool",
                "drawRectangleTool",
                "drawEllipseTool",
                "drawPolygonTool",
                "createVectorDataNode",
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
