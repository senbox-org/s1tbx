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
package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.ProductUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
class TransferMaskDialog extends ModalDialog {

    private final Mask[] selectedMasks;
    private final Product sourceProduct;
    private final Product[] allProducts;
    private final Map<Product, ButtonModel> definitionMap;
    private final Map<Product, ButtonModel> dataMap;

    TransferMaskDialog(Window window, Product product, Product[] allProducts, Mask[] selectedMasks) {
        super(window, "Transfer Mask(s) to other product", ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP, "transferMaskEditor");
        this.sourceProduct = product;
        this.allProducts = allProducts;
        this.selectedMasks = selectedMasks;
        definitionMap = new HashMap<Product, ButtonModel>();
        dataMap = new HashMap<Product, ButtonModel>();
        getJDialog().setResizable(true);
        
        setContent(createUI());
    }
    
    Product[] getMaskPixelTargets() {
        return getSelectedProducts(dataMap);
    }
    
    Product[] getMaskDefinitionTargets() {
        return getSelectedProducts(definitionMap);
    }
    
    private static Product[] getSelectedProducts(Map<Product, ButtonModel> buttonMap) {
        List<Product> selectedProducts = new ArrayList<Product>(buttonMap.size());
        for (Map.Entry<Product, ButtonModel> entry : buttonMap.entrySet()) {
            Product product = entry.getKey();
            ButtonModel buttonModel = entry.getValue();
            if (buttonModel.isSelected()) {
                selectedProducts.add(product);
            }
        }
        return selectedProducts.toArray(new Product[selectedProducts.size()]);
    }
    
    private JComponent createUI() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setTablePadding(5, 5);
        
        
        final JPanel panel = new JPanel(layout);
        panel.add(new JLabel("<html><u>Target product</u></html>"));
        panel.add(new JLabel("<html><u>Definition</u></html>"));
        panel.add(new JLabel("<html><u>Pixels</u></html>"));
        int row = 1;
        for (Product targetProduct : allProducts) {
            if (targetProduct != sourceProduct) {
                panel.add(new JLabel(targetProduct.getDisplayName()));
                
                boolean canCopyDef = canCopyDefinition(selectedMasks, targetProduct);
                JCheckBox defCheckBox = createCeckbox(panel, canCopyDef);
                if (canCopyDef) {
                    definitionMap.put(targetProduct, defCheckBox.getModel());
                }
                
                boolean canCopyData = intersectsWith(sourceProduct, targetProduct);
                JCheckBox dataCheckBox = createCeckbox(panel, canCopyData);
                if (canCopyData) {
                    dataMap.put(targetProduct, dataCheckBox.getModel());
                }
                
                if (canCopyData && canCopyDef) {
                    ButtonGroup buttonGroup = new ButtonGroup();
                    buttonGroup.add(dataCheckBox);
                    buttonGroup.add(defCheckBox);
                }
                row++;
            }
        }
        layout.setCellColspan(row, 0, 3);
        panel.add(createHelpPanel());
        return panel;
    }
    
    private JComponent createHelpPanel() {
        JEditorPane helpPane = new JEditorPane("text/html", null);
        helpPane.setEditable(false);
        helpPane.setPreferredSize(new Dimension(400, 120));
        helpPane.setText("<html><body>Copying the <b>definition</b> of a mask means the mathematical expression " +
        		"is evaluated in the target product. This is only possible,  " +
        		"if the bands which are used in this expression are present in the target product.<br/> " +
        		"Copying the <b>pixel</b> means the data of the mask is transferred to the target product. " +
        		"This is only possible when both product overlap spatially.</body></html>");
        JScrollPane helpPanelScrollPane = new JScrollPane(helpPane);
        helpPanelScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
        return helpPanelScrollPane;
    }

    private static JCheckBox createCeckbox(final JPanel panel, boolean enabled) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.setEnabled(enabled);
        panel.add(checkBox);
        return checkBox;
    }

    private static boolean canCopyDefinition(Mask[] masks, Product targetProduct) {
        boolean canCopyDef = true;
        for (Mask mask : masks) {
            boolean canTransferMask = mask.getImageType().canTransferMask(mask, targetProduct);
            canCopyDef = canCopyDef && canTransferMask;
        }
        return canCopyDef;
    }
    
    private static boolean intersectsWith(Product sourceProduct, Product targetProduct) {
        final GeoCoding srcGC = sourceProduct.getGeoCoding();
        final GeoCoding targetGC = targetProduct.getGeoCoding();
        if (srcGC != null && srcGC.canGetGeoPos() && targetGC != null && targetGC.canGetGeoPos()) {
            final GeneralPath[] sourcePath = ProductUtils.createGeoBoundaryPaths(sourceProduct);
            final GeneralPath[] targetPath = ProductUtils.createGeoBoundaryPaths(targetProduct);
            for (GeneralPath spath : sourcePath) {
                Rectangle bounds = spath.getBounds();
                for (GeneralPath tPath : targetPath) {
                    if (tPath.getBounds().intersects(bounds)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
