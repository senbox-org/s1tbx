/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JPanel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 */
public class DefaultIOParametersPanel extends JPanel {

    private ArrayList<SourceProductSelector> sourceProductSelectorList;
    private HashMap<Field, SourceProductSelector> sourceProductSelectorMap;
    private AppContext appContext;

    public DefaultIOParametersPanel(AppContext appContext, OperatorSpi operatorSpi,
                                    TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        // Fetch source products
        initSourceProductSelectors(operatorSpi);
        if (!sourceProductSelectorList.isEmpty()) {
            setSourceProductSelectorLabels();
            setSourceProductSelectorToolTipTexts();
        }

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        setLayout(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            add(selector.createDefaultPanel());
        }
        add(targetProductSelector.createDefaultPanel());
        add(tableLayout.createVerticalSpacer());
    }

    public ArrayList<SourceProductSelector> getSourceProductSelectorList() {
        return sourceProductSelectorList;
    }

    public void initSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    public void releaseSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    public HashMap<String, Product> createSourceProductsMap() {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String key = field.getName();
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.alias().isEmpty()) {
                key = annot.alias();
            }
            sourceProducts.put(key, selector.getSelectedProduct());
        }
        return sourceProducts;
    }


    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annot);
                SourceProductSelector sourceProductSelector = new SourceProductSelector(appContext);
                sourceProductSelector.setProductFilter(productFilter);
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorLabels() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String label = null;
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.label().isEmpty()) {
                label = annot.label();
            }
            if (label == null && !annot.alias().isEmpty()) {
                label = annot.alias();
            }
            if (label == null) {
                String name = field.getName();
                if (!annot.alias().isEmpty()) {
                    name = annot.alias();
                }
                label = PropertyDescriptor.createDisplayName(name);
            }
            if (!label.endsWith(":")) {
                label += ":";
            }
            selector.getProductNameLabel().setText(label);
        }
    }

    private void setSourceProductSelectorToolTipTexts() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);

            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            final String description = annot.description();
            if (!description.isEmpty()) {
                selector.getProductNameComboBox().setToolTipText(description);
            }
        }
    }

    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProduct annot;

        private AnnotatedSourceProductFilter(SourceProduct annot) {
            this.annot = annot;
        }

        @Override
        public boolean accept(Product product) {

            if (!annot.type().isEmpty() && !product.getProductType().matches(annot.type())) {
                return false;
            }

            for (String bandName : annot.bands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }

            return true;
        }
    }
}
