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
package org.esa.snap.gpf.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Reader OperatorUI
 */
public class SourceUI extends BaseOperatorUI {

    SourceProductSelector sourceProductSelector = null;
    private static final String FILE_PARAMETER = "file";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        paramMap = parameterMap;
        final List<SourceProductSelector> sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelector = new SourceProductSelector(appContext);
        sourceProductSelectorList.add(sourceProductSelector);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioParametersPanel = new JPanel(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            ioParametersPanel.add(selector.createDefaultPanel());
        }
        ioParametersPanel.add(tableLayout.createVerticalSpacer());

        initSourceProductSelectors(sourceProductSelectorList);

        initParameters();

        return ioParametersPanel;
    }

    private static void initSourceProductSelectors(java.util.List<SourceProductSelector> sourceProductSelectorList) {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    @Override
    public void initParameters() {
        assert (paramMap != null);
        final Object value = paramMap.get(FILE_PARAMETER);
        if (value != null) {

            try {
                final Product product = ProductIO.readProduct((File) value);
                sourceProductSelector.setSelectedProduct(product);
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    @Override
    public UIValidation validateParameters() {
        if (sourceProductSelector != null) {
            if (sourceProductSelector.getSelectedProduct() == null)
                return new UIValidation(UIValidation.State.ERROR, "Source product not selected");
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        if (sourceProductSelector != null) {
            final Product prod = sourceProductSelector.getSelectedProduct();
            if (prod != null && prod.getFileLocation() != null) {
                paramMap.put(FILE_PARAMETER, prod.getFileLocation());
            }
        }
    }

    public void setSourceProduct(final Product product) {
        if (sourceProductSelector != null) {
            sourceProductSelector.setSelectedProduct(product);
            if (product != null && product.getFileLocation() != null) {
                paramMap.put(FILE_PARAMETER, product.getFileLocation());
            }
        }
    }
}
