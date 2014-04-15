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
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.framework.gpf.descriptor.SourceProductDescriptor;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 */
public class DefaultIOParametersPanel extends JPanel {

    private ArrayList<SourceProductSelector> sourceProductSelectorList;
    private HashMap<SourceProductDescriptor, SourceProductSelector> sourceProductSelectorMap;
    private AppContext appContext;

    public DefaultIOParametersPanel(AppContext appContext, OperatorDescriptor descriptor, TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        sourceProductSelectorList = new ArrayList<>(3);
        sourceProductSelectorMap = new HashMap<>(3);
        // Fetch source products
        createSourceProductSelectors(descriptor);
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

    /**
     * @deprecated since BEAM 5, use {@link #DefaultIOParametersPanel(AppContext, OperatorDescriptor, TargetProductSelector)}
     */
    @Deprecated
    public DefaultIOParametersPanel(AppContext appContext, OperatorSpi operatorSpi,
                                    TargetProductSelector targetProductSelector) {
        this(appContext, operatorSpi.getOperatorDescriptor(), targetProductSelector);
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
        final HashMap<String, Product> sourceProducts = new HashMap<>(8);
        for (SourceProductDescriptor descriptor : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(descriptor);
            String alias = descriptor.getAlias();
            String key = alias != null ? alias : descriptor.getName();
            sourceProducts.put(key, selector.getSelectedProduct());
        }
        return sourceProducts;
    }


    private void createSourceProductSelectors(OperatorDescriptor operatorDescriptor) {
        for (SourceProductDescriptor descriptor : operatorDescriptor.getSourceProductDescriptors()) {
            final ProductFilter productFilter = new AnnotatedSourceProductFilter(descriptor);
            SourceProductSelector sourceProductSelector = new SourceProductSelector(appContext);
            sourceProductSelector.setProductFilter(productFilter);
            sourceProductSelectorList.add(sourceProductSelector);
            sourceProductSelectorMap.put(descriptor, sourceProductSelector);
        }
    }

    private void setSourceProductSelectorLabels() {
        for (SourceProductDescriptor descriptor : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(descriptor);
            String label = descriptor.getLabel();
            String alias = descriptor.getAlias();
            if (label == null && alias != null) {
                label = alias;
            }
            if (label == null) {
                label = PropertyDescriptor.createDisplayName(descriptor.getName());
            }
            if (!label.endsWith(":")) {
                label += ":";
            }
            selector.getProductNameLabel().setText(label);
        }
    }

    private void setSourceProductSelectorToolTipTexts() {
        for (SourceProductDescriptor descriptor : sourceProductSelectorMap.keySet()) {
            final String description = descriptor.getDescription();
            if (description != null) {
                final SourceProductSelector selector = sourceProductSelectorMap.get(descriptor);
                selector.getProductNameComboBox().setToolTipText(description);
            }
        }
    }

    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProductDescriptor productDescriptor;

        private AnnotatedSourceProductFilter(SourceProductDescriptor productDescriptor) {
            this.productDescriptor = productDescriptor;
        }

        @Override
        public boolean accept(Product product) {

            String productType = productDescriptor.getProductType();
            if (productType != null && !product.getProductType().matches(productType)) {
                return false;
            }

            for (String bandName : productDescriptor.getBands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }

            return true;
        }
    }
}
