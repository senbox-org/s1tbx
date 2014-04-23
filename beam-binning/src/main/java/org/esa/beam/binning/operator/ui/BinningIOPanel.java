/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.SourceProductList;
import org.esa.beam.util.io.WildcardMatcher;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * The panel in the binning operator UI which allows for setting input products and the path of the output product.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningIOPanel extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private final TargetProductSelector targetProductSelectorPanel;
    private SourceProductList sourceProductList;

    BinningIOPanel(AppContext appContext, BinningFormModel binningFormModel, TargetProductSelector targetProductSelectorPanel) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        this.targetProductSelectorPanel = targetProductSelectorPanel;
        this.targetProductSelectorPanel.getModel().setProductName("level-3");
        final SourceProductSelector sourceProductSelectorPanel = new SourceProductSelector(appContext);
        sourceProductSelectorPanel.setProductFilter(null); // todo -- set product filter
        init();
    }

    private void init() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);
        tableLayout.setRowWeightY(0, 1.0);
        add(createSourceProductsPanel());
        add(targetProductSelectorPanel.createDefaultPanel());
    }

    private JPanel createSourceProductsPanel() {
        BorderLayout layout = new BorderLayout();

        final JPanel sourceProductPanel = new JPanel(layout);
        sourceProductPanel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        ListDataListener changeListener = new ListDataListener() {

            @Override
            public void contentsChanged(ListDataEvent e) {

                final Product[] sourceProducts = binningFormModel.getSourceProducts();
                if (sourceProducts.length > 0) {
                    binningFormModel.setContextProduct(sourceProducts[0]);
                    return;
                }
                String[] sourceProductPath = binningFormModel.getSourceProductPath();
                if (sourceProductPath != null && sourceProductPath.length > 0) {
                    openFirstProduct(sourceProductPath);
                    return;
                }
                binningFormModel.setContextProduct(null);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }
        };

        sourceProductList = new SourceProductList(appContext);
        sourceProductList.setLastOpenInputDir("org.esa.beam.binning.lastDir");
        sourceProductList.setLastOpenedFormat("org.esa.beam.binning.lastFormat");
        sourceProductList.addChangeListener(changeListener);
        sourceProductList.setXAxis(false);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCT_PATHS, sourceProductList);
        JComponent[] panels = sourceProductList.getComponents();
        sourceProductPanel.add(panels[0], BorderLayout.CENTER);
        sourceProductPanel.add(panels[1], BorderLayout.EAST);

        return sourceProductPanel;
    }

    private void openFirstProduct(final String[] inputPaths) {
        final SwingWorker<Product, Void> worker = new SwingWorker<Product, Void>() {
            @Override
            protected Product doInBackground() throws Exception {
                for (String inputPath : inputPaths) {
                    if (inputPath == null || inputPath.trim().length() == 0) {
                        continue;
                    }
                    try {
                        final TreeSet<File> fileSet = new TreeSet<>();
                        WildcardMatcher.glob(inputPath, fileSet);
                        for (File file : fileSet) {
                            final Product product = ProductIO.readProduct(file);
                            if (product != null) {
                                return product;
                            }
                        }
                    } catch (IOException e) {
                        Logger logger = BeamLogManager.getSystemLogger();
                        logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Product firstProduct = get();
                    if (firstProduct != null) {
                        binningFormModel.setContextProduct(firstProduct);
                    }
                } catch (Exception ex) {
                    String msg = String.format("Cannot open source products.\n%s", ex.getMessage());
                    appContext.handleError(msg, ex);
                }
            }
        };
        worker.execute();
    }

}
