/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import org.apache.commons.lang.ArrayUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.SourceProductList;
import org.esa.beam.util.io.WildcardMatcher;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        final Property sourceProductPaths = binningFormModel.getBindingContext().getPropertySet().getProperty(BinningFormModel.PROPERTY_SOURCE_PRODUCT_PATHS);
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        final String[] productFiles = removeDuplicates((String[]) sourceProductPaths.getValue());
                        Product[] products = openProducts(productFiles);
                        Object[] allProducts = ArrayUtils.addAll(products, sourceProductList.getSourceProducts());
                        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS, allProducts);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        } catch (Exception ex) {
                            String msg = String.format("Cannot display source products.\n%s", ex.getMessage());
                            appContext.handleError(msg, ex);
                        }
                    }
                };
                worker.execute();
            }

            private String[] removeDuplicates(String[] files) {
                final Set<String> result = new HashSet<>();
                Collections.addAll(result, files);
                return result.toArray(new String[result.size()]);
            }
        };

        sourceProductList = new SourceProductList(appContext, sourceProductPaths, "org.esa.beam.binning.lastDir", "org.esa.beam.binning.lastFormat", changeListener);
        JPanel[] panels = sourceProductList.createComponents();
        sourceProductPanel.add(panels[0], BorderLayout.CENTER);
        sourceProductPanel.add(panels[1], BorderLayout.EAST);

        return sourceProductPanel;
    }

    private Product[] openProducts(String[] inputPaths) {
        if (inputPaths != null) {

            final Logger logger = BeamLogManager.getSystemLogger();

            List<Product> products = new ArrayList<>();
            for (String inputPath : inputPaths) {
                if (inputPath == null || inputPath.trim().length() == 0) {
                    continue;
                }
                try {
                    final TreeSet<File> fileSet = new TreeSet<>();
                    WildcardMatcher.glob(inputPath, fileSet);
                    boolean firstProduct = true;
                    for (File file : fileSet) {
                        final Product product = ProductIO.readProduct(file);
                        if (product != null) {
                            products.add(product);
                            if (firstProduct) {
                                binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS, new Product[]{product});
                                firstProduct = false;
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                } catch (ValidationException e) {
                    throw new IllegalStateException("Cannot come here.", e);
                }
            }
            return products.toArray(new Product[products.size()]);
        }
        return new Product[0];
    }

}
