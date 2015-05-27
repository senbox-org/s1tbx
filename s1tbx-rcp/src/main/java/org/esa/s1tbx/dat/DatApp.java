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
package org.esa.s1tbx.dat;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.db.ProductDB;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.datamodel.CrsGeoCoding;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.MapGeoCoding;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.application.ApplicationDescriptor;
import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.MemUtils;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.util.Settings;
import org.esa.snap.visat.VisatApp;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    protected void configureJaiTileCache() {
        MemUtils.createTileCache();
        super.configureJaiTileCache();
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
        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
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

    private void updateGraphMenu() {
        final JMenu menu = findMenu("Graphs");
        if (menu == null) {
            return;
        }

        final Path graphPath = ResourceUtils.getGraphFolder("");
        if (!Files.exists(graphPath)) return;

        menu.add(new JSeparator());
        createGraphMenu(menu, graphPath.toFile());
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
                        final GraphBuilderDialog dialog = new GraphBuilderDialog(new SnapApp.SnapContext(),
                                "Graph Builder", "graph_builder");
                        dialog.show();
                        dialog.LoadGraph(file);
                    }
                });
                menu.add(item);
            }
        }
    }
}
