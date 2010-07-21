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
package org.esa.beam.visat.toolviews.bitmask;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.ColorEditor;
import org.esa.beam.framework.param.editors.TextFieldEditor;
import org.esa.beam.framework.param.validators.NumberValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BitmaskOverlayToolView extends AbstractToolView {

    public static final String ID = BitmaskOverlayToolView.class.getName();

    private static final String BMD_FILE_EXTENSION = ".bmd";
    private static final String BMD_FILE_EXTENSION_XML = ".bmdx";

    private final List<BmdAction> bitmaskDefActions;
    private final ProductNodeListener productNodeListener;

    private ProductSceneView productSceneView;
    private BitmaskDefTable bitmaskDefTable;

    private JCheckBox showCompatibleCheck;
    private AbstractButton applyButton;
    private AbstractButton newButton;
    private AbstractButton copyButton;
    private AbstractButton editButton;
    private AbstractButton removeButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private AbstractButton importNButton;
    private AbstractButton exportNButton;
    private AbstractButton moveUpButton;

    private AbstractButton moveDownButton;
    private BeamFileFilter beamFileFilter;
    private BeamFileFilter beamFileFilterXml;

    public BitmaskOverlayToolView() {
        this.bitmaskDefActions = new ArrayList<BmdAction>();
        this.productNodeListener = createProductNodeListener();
    }

    public void setProductSceneView(final ProductSceneView productSceneView) {
        final ProductSceneView productSceneViewOld = this.productSceneView;
        if (productSceneViewOld == productSceneView) {
            return;
        }
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().removeProductNodeListener(productNodeListener);
        }
        this.productSceneView = productSceneView;
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().addProductNodeListener(productNodeListener);
            rebuildDisplayList();
        } else {
            clearDisplayList();
        }

        updateTitle();
    }

    private void updateTitle() {
        final String titleAddtion;
        if (productSceneView != null) {
            if (productSceneView.isRGB()) {
                titleAddtion = " - " + productSceneView.getProduct().getProductRefString() + " RGB";
            } else {
                titleAddtion = " - " + productSceneView.getRaster().getDisplayName();
            }
        } else {
            titleAddtion = "";
        }
        setTitle(getDescriptor().getTitle() + titleAddtion);
    }

    private void rebuildDisplayList() {
        clearDisplayList();
        if (productSceneView != null) {
            final Product product = getSelectedProduct();
            addProductBMDs(product, null);
            if (showCompatibleCheck.isSelected()) {
                addProductManagerBMDs(product);
            }
        }
        updateUIState();
        applyButton.setEnabled(false);
    }

    private void clearDisplayList() {
        bitmaskDefTable.clear();
        clearActions();
        updateUIState();
        applyButton.setEnabled(false);
    }

    private void apply() {
        getPaneWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyActions();
        applyImageUpdate();
        updateUIState();
        applyButton.setEnabled(false);
        getPaneWindow().setCursor(Cursor.getDefaultCursor());
        if (productSceneView != null) {
            productSceneView.setBitmaskOverlayEnabled(true);
        }
    }

    private void applyActions() {
        final Product product = getSelectedProduct();
        if (product == null) {
            return;
        }
        for (final BmdAction action : bitmaskDefActions) {
            action.doAction(product);
        }
        clearActions();
    }

    private void applyImageUpdate() {
        final RasterDataNode raster = getSelectedRaster();
        if (raster == null) {
            return;
        }
        final BitmaskOverlayInfo bitmaskOverlayInfo = new BitmaskOverlayInfo();
        for (int i = 0; i < bitmaskDefTable.getRowCount(); i++) {
            if (bitmaskDefTable.getVisibleFlagAt(i)) {
                final BitmaskDef bitmaskDef = bitmaskDefTable.getBitmaskDefAt(i);
                bitmaskOverlayInfo.addBitmaskDef(bitmaskDef);
                Debug.trace("Applying visibility change " + i + ": switching on '" + bitmaskDef.getName() + "'");
                final Product product = raster.getProduct();
                if (product != null && !product.containsBitmaskDef(bitmaskDef)) {
                    product.addBitmaskDef(bitmaskDef);
                    Debug.trace(
                            "Adding BitmaskDef '" + bitmaskDef.getName() + "' to product '" + product.getName() + "' after switching on this bitmask def from an other Product");
                }
            }
        }
        raster.setBitmaskOverlayInfo(bitmaskOverlayInfo);
    }

    private void newBitmaskDef() {
        final BitmaskDef bitmaskDefNew = showBitmaskDefEditDialog("New Bitmask", null, true);     /*I18N*/
        if (bitmaskDefNew != null) {
            final int index = bitmaskDefTable.getModel().getRowCount();
            doNewAction(bitmaskDefNew, index);
            bitmaskDefTable.setRowSelectionInterval(index, index);
        }
    }


    private void copySelectedBitmaskDef() {
        final BitmaskDef bitmaskDefOld = bitmaskDefTable.getSelectedBitmaskDef();
        if (bitmaskDefOld == null) {
            return;
        }

        final BitmaskDef bitmaskDefNew = showBitmaskDefEditDialog("Copy Bitmask", bitmaskDefOld, true);   /*I18N*/
        if (bitmaskDefNew != null) {
            int index = bitmaskDefTable.getModel().getRowCount();
            doNewAction(bitmaskDefNew, index);
        }
    }

    private void editSelectedBitmaskDef() {
        final int rowIndex = bitmaskDefTable.getSelectedRow();
        if (rowIndex == -1) {
            return;
        }

        final BitmaskDef bitmaskDefOld = bitmaskDefTable.getBitmaskDefAt(rowIndex);
        final BitmaskDef bitmaskDefNew = showBitmaskDefEditDialog("Edit Bitmask", bitmaskDefOld, false);      /*I18N*/
        if (bitmaskDefNew != null) {
            doEditAction(bitmaskDefNew, rowIndex);
        }
    }

    private void removeSelectedBitmaskDef() {
        final int rowIndex;
        rowIndex = bitmaskDefTable.getSelectedRow();
        if (rowIndex == -1) {
            return;
        }

        final BitmaskDef bitmaskDefOld = bitmaskDefTable.getBitmaskDefAt(rowIndex);

        if (isBitmaskDefOfAnotherProduct(bitmaskDefOld)) {
            showInformationDialog("The bitmask '"
                                  + bitmaskDefOld.getName()
                                  + "' does not belong to the selected\n"
                                  + "product and cannot be removed.");              /*I18N*/
            return;
        }
        if (isBitmaskDefReferencedByOtherNodes(bitmaskDefOld)) {
            showInformationDialog("The bitmask '"
                                  + bitmaskDefOld.getName()
                                  + "' is still referenced by other\n"
                                  + "images and cannot be removed.");           /*I18N*/
            return;
        }

        doRemoveAction(rowIndex);
    }

    private void importBitmaskDef() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Bitmask Definition");                /*I18N*/
        fileChooser.setFileFilter(getOrCreateBitmaskDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showOpenDialog(getPaneWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                final PropertyMap propertyMap = new PropertyMap();
                try {
                    propertyMap.load(file); // Overwrite existing values
                    String name = propertyMap.getPropertyString("bitmaskName", "bitmask");
                    final String description = propertyMap.getPropertyString("bitmaskDesc", null);
                    final String expr = propertyMap.getPropertyString("bitmaskExpr", "");
                    final Color color = propertyMap.getPropertyColor("bitmaskColor", Color.yellow);
                    final float transp = (float) propertyMap.getPropertyDouble("bitmaskTransparency", 0.5);
                    name = getUniqueDefaultName(name);
                    final BitmaskDef bitmaskDef = new BitmaskDef(name, description, expr, color, transp);
                    addOrOverwriteBitmaskDef(bitmaskDef);
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to import bitmask definition.");        /*I18N*/
                }
            }
        }
    }

    private void importBitmaskDefs() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Bitmask Definitions From XML File");         /*I18N*/
        fileChooser.setFileFilter(getOrCreateBitmaskDefinitionXmlFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        if (fileChooser.showOpenDialog(getPaneWindow()) == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                if (file.canRead()) {
                    try {
                        try {
                            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            final DocumentBuilder builder = factory.newDocumentBuilder();
                            final org.w3c.dom.Document w3cDocument = builder.parse(file);
                            final Document document = new DOMBuilder().build(w3cDocument);
                            final Element rootElement = document.getRootElement();
                            final List children = rootElement.getChildren(DimapProductConstants.TAG_BITMASK_DEFINITION);
                            if (children != null) {
                                for (Object aChildren : children) {
                                    final Element element = (Element) aChildren;
                                    final BitmaskDef bitmaskDef = BitmaskDef.createBitmaskDef(element);
                                    if (bitmaskDef != null) {
                                        addOrOverwriteBitmaskDef(bitmaskDef);
                                    }
                                }
                            }
                        } catch (FactoryConfigurationError error) {
                            throw new IOException(error.toString());
                        } catch (ParserConfigurationException e) {
                            throw new IOException(e.toString());
                        } catch (SAXException e) {
                            throw new IOException(e.toString());
                        } catch (IOException e) {
                            throw new IOException(e.toString());
                        }
                    } catch (IOException e) {
                        showErrorDialog("I/O Error.\nFailed to import bitmask definition.");        /*I18N*/
                    }
                }
            }
        }
    }

    private void addOrOverwriteBitmaskDef(final BitmaskDef bitmaskDef) {
        if (getSelectedProduct().isCompatibleBitmaskDef(bitmaskDef)) {
            final int index = indexOf(bitmaskDefTable.getBitmaskDefs(), bitmaskDef.getName());
            if (index != -1) {
                doEditAction(bitmaskDef, index);
            } else {
                doNewAction(bitmaskDef, bitmaskDefTable.getModel().getRowCount());
            }
        } else {
            JOptionPane.showMessageDialog(getPaneWindow(), "Failed to import bitmask definition:\n" +
                                                           "Invalid expression: " + bitmaskDef.getExpr());     /*I18N*/
        }
    }

    private static int indexOf(final BitmaskDef[] bitmaskDefs, final String name) {
        int index = -1;
        for (int i = 0; i < bitmaskDefs.length; i++) {
            final BitmaskDef bitmaskDef = bitmaskDefs[i];
            if (bitmaskDef.getName().equals(name)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void exportSelectedBitmaskDef() {
        final BitmaskDef bitmaskDef = bitmaskDefTable.getSelectedBitmaskDef();
        if (bitmaskDef == null) {
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Bitmask Definition");            /*I18N*/
        fileChooser.setFileFilter(getOrCreateBitmaskDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        fileChooser.setSelectedFile(new File(getIODir(), bitmaskDef.getName()));
        final int result = fileChooser.showSaveDialog(getPaneWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!VisatApp.getApp().promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, BMD_FILE_EXTENSION);

                final PropertyMap propertyMap = new PropertyMap();
                try {
                    propertyMap.setPropertyString("bitmaskName", bitmaskDef.getName());
                    propertyMap.setPropertyString("bitmaskDesc", bitmaskDef.getDescription());
                    propertyMap.setPropertyString("bitmaskExpr", bitmaskDef.getExpr());
                    propertyMap.setPropertyColor("bitmaskColor", bitmaskDef.getColor());
                    propertyMap.setPropertyDouble("bitmaskTransparency", bitmaskDef.getTransparency());
                    propertyMap.store(file, "BEAM Bitmask Definition File");            /*I18N*/
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to export bitmask definition.");        /*I18N*/
                }
            }
        }
    }

    private void exportBitmaskDefs() {
        final BitmaskDef[] bitmaskDefs = bitmaskDefTable.getBitmaskDefs();
        if (bitmaskDefs == null) {
            return;
        }
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export All Bitmask Definitions");               /*I18N*/
        fileChooser.setFileFilter(getOrCreateBitmaskDefinitionXmlFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showSaveDialog(getPaneWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!VisatApp.getApp().promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, BMD_FILE_EXTENSION_XML);
                try {
                    final XmlWriter xmlWriter = new XmlWriter(file);
                    final String[] tags = XmlWriter.createTags(0, VisatApp.getApp().getAppName() + "-BITMASKS");
                    xmlWriter.println(tags[0]);
                    for (final BitmaskDef bitmaskDef : bitmaskDefs) {
                        bitmaskDef.writeXML(xmlWriter, 1);
                    }
                    xmlWriter.print(tags[1]);
                    xmlWriter.close();
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to export bitmask definition.");        /*I18N*/
                }
            }
        }
    }

    private void addProductManagerBMDs(final Product referenceProduct) {
        final ProductManager productManager = referenceProduct.getProductManager();
        if (productManager != null) {
            for (int i = 0; i < productManager.getProductCount(); i++) {
                final Product product = productManager.getProduct(i);
                if (product != referenceProduct) {
                    addProductBMDs(product, referenceProduct);
                }
            }
        }
    }

    private void addProductBMDs(final Product product, final Product referenceProduct) {
        final int numBmds = product.getNumBitmaskDefs();
        final RasterDataNode raster = getSelectedRaster();
        for (int i = 0; i < numBmds; i++) {
            final BitmaskDef bitmaskDef = product.getBitmaskDefAt(i);
            if (bitmaskDefTable.getRowIndex(bitmaskDef.getName()) == -1) {
                boolean compatible = true;
                if (referenceProduct != null) {
                    Debug.trace(
                            "Checking bitmask [" + product.getRefNo() + "] " + bitmaskDef.getName() + " for compatibility");        /*I18N*/
                    compatible = referenceProduct.isCompatibleBitmaskDef(bitmaskDef);
                    Debug.trace("Compatible? " + compatible);
                }
                if (compatible) {
                    boolean visibleFlag = false;
                    final BitmaskOverlayInfo bitmaskOverlayInfo = raster.getBitmaskOverlayInfo();
                    if (bitmaskOverlayInfo != null) {
                        visibleFlag = bitmaskOverlayInfo.containsBitmaskDef(bitmaskDef);
                    }
                    bitmaskDefTable.addRow(visibleFlag, bitmaskDef);
                }
            }
        }
    }


    private void toggleShowCompatibleBitmasks() {
        boolean rebuild = true;
        if (getNumActions() > 0) {
            final int status = JOptionPane.showConfirmDialog(getPaneWindow(),
                                                             "This will undo your last changes to the bitmask list.\n"
                                                             + "Continue anyway?",
                                                             getDescriptor().getTitle() + " - Confirm",
                                                             JOptionPane.YES_NO_OPTION);      /*I18N*/
            if (status == JOptionPane.NO_OPTION) {
                final boolean selected = showCompatibleCheck.isSelected();
                showCompatibleCheck.setSelected(!selected);
                rebuild = false;
            }
        }
        if (rebuild) {
            rebuildDisplayList();
        }

    }

    private int getNumActions() {
        return bitmaskDefActions.size();
    }

    private void updateUIState() {
        final int bmRowCount = bitmaskDefTable.getRowCount();
        final int selectedRow = bitmaskDefTable.getSelectedRow();

        final boolean bmSinkAvailable = productSceneView != null;
        final boolean bmAvailable = bmSinkAvailable && bmRowCount > 0;
        final boolean bmSelected = bmAvailable && bitmaskDefTable.getSelectedBitmaskDef() != null;

        newButton.setEnabled(bmSinkAvailable);
        copyButton.setEnabled(bmSelected);
        editButton.setEnabled(bmSelected);
        removeButton.setEnabled(bmSelected);
        importButton.setEnabled(bmSinkAvailable);
        exportButton.setEnabled(bmSelected);
        importNButton.setEnabled(bmSinkAvailable);
        exportNButton.setEnabled(bmAvailable);
        moveUpButton.setEnabled(bmRowCount > 1 && selectedRow > 0);
        moveDownButton.setEnabled(bmRowCount > 1 && selectedRow > -1 && selectedRow < bmRowCount - 1);

        boolean moreProducts = false;
        if (getSelectedProduct() != null
            && getSelectedProduct().getProductManager() != null
            && getSelectedProduct().getProductManager().getProductCount() > 1) {
            moreProducts = true;
        }
        showCompatibleCheck.setEnabled(moreProducts);
    }


    private static AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }


    private void setIODir(final File dir) {
        if (VisatApp.getApp().getPreferences() != null) {
            VisatApp.getApp().getPreferences().setPropertyString("bitmask.io.dir", dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (VisatApp.getApp().getPreferences() != null) {
            dir = new File(VisatApp.getApp().getPreferences().getPropertyString("bitmask.io.dir", dir.getPath()));
        }
        return dir;
    }

    private BitmaskDef showBitmaskDefEditDialog(final String dialogTitle, final BitmaskDef bitmaskDefOld,
                                                final boolean mustNotExist) {

        final Product product = getSelectedProduct();

        String name;
        String desc;
        String expr;
        Color color;
        float transp;

        if (bitmaskDefOld != null) {
            name = bitmaskDefOld.getName();
            desc = bitmaskDefOld.getDescription();
            expr = bitmaskDefOld.getExpr();
            color = bitmaskDefOld.getColor();
            transp = bitmaskDefOld.getTransparency();
        } else {
            name = "bitmask";
            desc = null;
            expr = "";
            color = Color.yellow;
            transp = 0.5F;
        }
        if (mustNotExist) {
            name = getUniqueDefaultName(name);
        }


        final ProductExpressionPane exprPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product},
                                                                                                 product,
                                                                                                 VisatApp.getApp().getPreferences());
        exprPane.setCode(expr);
        final BitmaskDefEditPane bitmaskDefPane = new BitmaskDefEditPane(
                new BitmaskDef(name, desc, expr, color, transp));
        exprPane.setTopAccessory(bitmaskDefPane);

        while (true) {
            if (exprPane.showModalDialog(getPaneWindow(), dialogTitle) == ModalDialog.ID_OK) {
                name = bitmaskDefPane.getName();
                desc = bitmaskDefPane.getDescription();
                expr = exprPane.getCode();
                color = bitmaskDefPane.getColor();
                transp = bitmaskDefPane.getTransparency();
                if (mustNotExist && bitmaskDefTable.getRowIndex(name) != -1) {
                    showErrorDialog("A bitmask with the name '" + name + "' already exists.\n" +
                                    "Please choose another one.");                  /*I18N*/
                    continue;
                }
                if (expr == null || !product.isCompatibleBandArithmeticExpression(expr)) {
                    showErrorDialog("The expression of the bitmask is not valid.");
                    continue;
                }
                return new BitmaskDef(name, desc, expr, color, transp);
            } else {
                return null;
            }
        }
    }

    private String getUniqueDefaultName(final String nameBase) {
        final int rowIndex = bitmaskDefTable.getRowIndex(nameBase);
        if (rowIndex == -1) {
            return nameBase;
        }
        final String nameBaseLC = nameBase.toLowerCase();
        int maxIndex = 0;
        for (int i = 0; i < bitmaskDefTable.getRowCount(); i++) {
            final BitmaskDef bitmaskDef = bitmaskDefTable.getBitmaskDefAt(i);
            final String otherNameLC = bitmaskDef.getName().toLowerCase();
            if (otherNameLC.startsWith(nameBaseLC)) {
                String prefix = otherNameLC.substring(nameBaseLC.length());
                prefix = prefix.replace('_', ' ');
                prefix = prefix.replace('.', ' ');
                prefix = prefix.trim();
                try {
                    final int index = Integer.parseInt(prefix);
                    if (index > maxIndex) {
                        maxIndex = index;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return nameBase + "_" + (maxIndex + 1);
    }


    private void clearActions() {
        bitmaskDefActions.clear();
        Debug.trace("clearActions()");
    }

    private void doNewAction(final BitmaskDef bitmaskDefNew, final int rowIndex) {
        bitmaskDefTable.insertRowAt(true, bitmaskDefNew, rowIndex);
        bitmaskDefTable.setRowSelectionInterval(rowIndex, rowIndex);
        final Rectangle visibleRect = bitmaskDefTable.getVisibleRect();
        final Dimension size = bitmaskDefTable.getSize();
        visibleRect.setLocation(visibleRect.x, size.height);
        bitmaskDefTable.scrollRectToVisible(visibleRect);

        bitmaskDefActions.add(new NewAction(bitmaskDefNew));
        applyButton.setEnabled(true);
        Debug.trace("doNewAction(): new=" + bitmaskDefNew.getName() + " at rowIndex=" + rowIndex);
    }

    private void doEditAction(final BitmaskDef bitmaskDefNew, final int rowIndex) {
        final BitmaskDef bitmaskDefOld = bitmaskDefTable.getBitmaskDefAt(rowIndex);
        bitmaskDefTable.setBitmaskDefAt(bitmaskDefNew, rowIndex);
        bitmaskDefActions.add(new EditAction(bitmaskDefOld, bitmaskDefNew, rowIndex));
        applyButton.setEnabled(true);
        Debug.trace(
                "doEditAction(): old=" + bitmaskDefOld.getName() + ", new=" + bitmaskDefNew.getName() + " at rowIndex=" + rowIndex);
    }

    private void doRemoveAction(final int rowIndex) {
        final BitmaskDef bitmaskDefOld = bitmaskDefTable.getBitmaskDefAt(rowIndex);
        bitmaskDefTable.clearSelection();
        bitmaskDefTable.removeRowAt(rowIndex);
        final int selectionIndex;
        if (rowIndex == bitmaskDefTable.getRowCount()) {
            selectionIndex = rowIndex - 1;
        } else {
            selectionIndex = rowIndex;
        }
        if (selectionIndex > -1) {
            bitmaskDefTable.setRowSelectionInterval(selectionIndex, selectionIndex);
        }
        bitmaskDefActions.add(new RemoveAction(bitmaskDefOld));
        applyButton.setEnabled(true);
        Debug.trace("doRemoveAction(): old=" + bitmaskDefOld.getName() + " at rowIndex=" + rowIndex);
    }

    private void doMoveAction(final int rowIndex, final boolean moveUp) {
        final BitmaskDef bitmaskDef = bitmaskDefTable.getBitmaskDefAt(rowIndex);
        final boolean visibleFlag = bitmaskDefTable.getVisibleFlagAt(rowIndex);
        bitmaskDefTable.removeRowAt(rowIndex);
        final int destIndex;
        final Rectangle visRect = bitmaskDefTable.getVisibleRect();
        if (moveUp) {
            destIndex = rowIndex - 1;
            visRect.y -= bitmaskDefTable.getRowHeight(destIndex);
        } else {
            destIndex = rowIndex + 1;
            visRect.y += bitmaskDefTable.getRowHeight(destIndex);
        }
        bitmaskDefTable.insertRowAt(visibleFlag, bitmaskDef, destIndex);
        bitmaskDefTable.setRowSelectionInterval(destIndex, destIndex);
        bitmaskDefTable.scrollRectToVisible(visRect);
        bitmaskDefActions.add(new MoveAction(bitmaskDef, destIndex));

        applyButton.setEnabled(true);
        Debug.trace("doMoveAction(): move " + bitmaskDef.getName() + " to rowIndex=" + destIndex);
        updateUIState();
    }

    private void showErrorDialog(final String message) {
        JOptionPane.showMessageDialog(getPaneWindow(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);           /*I18N*/
    }

    private void showInformationDialog(final String message) {
        JOptionPane.showMessageDialog(getPaneWindow(),
                                      message,
                                      getDescriptor().getTitle() + " - Information",
                                      JOptionPane.INFORMATION_MESSAGE);     /*I18N*/
    }


    private Product getSelectedProduct() {
        return productSceneView != null ? productSceneView.getProduct() : null;
    }

    private RasterDataNode getSelectedRaster() {
        return productSceneView != null ? productSceneView.getRaster() : null;
    }

    private boolean isBitmaskDefOfAnotherProduct(final BitmaskDef bitmaskDefOld) {
        final Product selectedProduct = getSelectedProduct();
        return selectedProduct != null && bitmaskDefOld.getProduct() != selectedProduct;
    }


    private boolean isBitmaskDefReferencedByOtherNodes(final BitmaskDef bitmaskDefOld) {
        final Product product = getSelectedProduct();
        if (product == null) {
            return false;
        }
        final String bitmaskName = bitmaskDefOld.getName();
        final RasterDataNode rasterDataNode = getSelectedRaster();
        for (int i = 0; i < product.getNumTiePointGrids(); i++) {
            final TiePointGrid grid = product.getTiePointGridAt(i);
            if (grid != rasterDataNode) {
                final BitmaskOverlayInfo bitmaskOverlayInfo = grid.getBitmaskOverlayInfo();
                if (bitmaskOverlayInfo != null) {
                    if (bitmaskOverlayInfo.containsBitmaskDef(bitmaskName)) {
                        return true;
                    }
                }
            }
        }
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            if (band != rasterDataNode) {
                final BitmaskOverlayInfo bitmaskOverlayInfo = band.getBitmaskOverlayInfo();
                if (bitmaskOverlayInfo != null) {
                    if (bitmaskOverlayInfo.containsBitmaskDef(bitmaskName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BeamFileFilter getOrCreateBitmaskDefinitionFileFilter() {
        if (beamFileFilter == null) {
            final String formatName = "BITMASK_DEFINITION_FILE";
            final String description = "Bitmask definition files (*" + BMD_FILE_EXTENSION + ")";      /*I18N*/
            beamFileFilter = new BeamFileFilter(formatName, BMD_FILE_EXTENSION, description);
        }
        return beamFileFilter;
    }

    private BeamFileFilter getOrCreateBitmaskDefinitionXmlFileFilter() {
        if (beamFileFilterXml == null) {
            final String formatName = "BITMASK_DEFINITION_FILE_XML";
            final String description = "Bitmask definition xml files (*" + BMD_FILE_EXTENSION_XML + ")";      /*I18N*/
            beamFileFilterXml = new BeamFileFilter(formatName, BMD_FILE_EXTENSION_XML, description);
        }
        return beamFileFilterXml;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(Product.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((productSceneView.isRGB() && sourceNode == productSceneView.getProduct())
                        || sourceNode == productSceneView.getRaster()) {
                        updateTitle();
                    }
                }
            }
        };
    }

    @Override
    public JComponent createControl() {
        bitmaskDefTable = new BitmaskDefTable();
        bitmaskDefTable.setName("bitmaskDefTable");
        bitmaskDefTable.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(final TableModelEvent e) {
                applyButton.setEnabled(true);
            }
        });
        bitmaskDefTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                updateUIState();
                if (e.getClickCount() == 2) {
                    editSelectedBitmaskDef();
                }
            }
        });

        final JScrollPane tableScrollPane = new JScrollPane(bitmaskDefTable);

        showCompatibleCheck = new JCheckBox("Show bitmasks of all compatible products");           /*I18N*/
        showCompatibleCheck.setMnemonic('c');
        showCompatibleCheck.setToolTipText("Shows bitmask definitions of other compatible products too.");     /*I18N*/
        showCompatibleCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleShowCompatibleBitmasks();
            }
        });

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.add(tableScrollPane, BorderLayout.CENTER);
        mainPane.add(showCompatibleCheck, BorderLayout.SOUTH);
        showCompatibleCheck.setName("showCompatibleCheck");

        applyButton = new JButton("Apply");        /*I18N*/
        applyButton.setName("applyButton");
        applyButton.setToolTipText("Apply bitmask changes."); /*I18N*/
        applyButton.setMnemonic('A');
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                apply();
            }
        });

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        newButton.setToolTipText("Create and add new bitmask."); /*I18N*/
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                newBitmaskDef();
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy and add existing bitmask."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                copySelectedBitmaskDef();
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit selected bitmask."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                editSelectedBitmaskDef();
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove selected bitmask."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                removeSelectedBitmaskDef();
                updateUIState();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import bitmask from text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                importBitmaskDef();
                updateUIState();
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export bitmask to text file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                exportSelectedBitmaskDef();
            }
        });

        importNButton = createButton("icons/ImportN24.gif");
        importNButton.setName("importNButton");
        importNButton.setToolTipText("Import all bitmask definitions from XML file."); /*I18N*/
        importNButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                importBitmaskDefs();
                updateUIState();
            }
        });

        exportNButton = createButton("icons/ExportN24.gif");
        exportNButton.setName("exportNButton");
        exportNButton.setToolTipText("Export all bitmask definitions to XML file."); /*I18N*/
        exportNButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                exportBitmaskDefs();
            }
        });

        moveUpButton = createButton("icons/Up24.gif");
        moveUpButton.setName("moveUpButton");
        moveUpButton.setToolTipText("Moves selected entry up."); /*I18N*/
        moveUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doMoveAction(bitmaskDefTable.getSelectedRow(), true);
            }
        });

        moveDownButton = createButton("icons/Down24.gif");
        moveDownButton.setName("moveDownButton");
        moveDownButton.setToolTipText("Moves selected entry down."); /*I18N*/
        moveDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doMoveAction(bitmaskDefTable.getSelectedRow(), false);
            }
        });


        final AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setName("helpButton");
        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpKey(getPaneControl(), getDescriptor().getHelpId());
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
        }

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;

        gbc.insets.bottom = 4;
        gbc.gridwidth = 2;
        buttonPane.add(applyButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        buttonPane.add(newButton, gbc);
        buttonPane.add(copyButton, gbc);
        gbc.gridy++;
        buttonPane.add(editButton, gbc);
        buttonPane.add(removeButton, gbc);
        gbc.gridy++;
        buttonPane.add(importButton, gbc);
        buttonPane.add(exportButton, gbc);
        gbc.gridy++;
        buttonPane.add(importNButton, gbc);
        buttonPane.add(exportNButton, gbc);
        gbc.gridy++;
        buttonPane.add(moveUpButton, gbc);
        buttonPane.add(moveDownButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        buttonPane.add(helpButton, gbc);

        final JPanel contentPane1 = new JPanel(new BorderLayout(4, 4));
        contentPane1.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPane1.add(BorderLayout.CENTER, mainPane);
        contentPane1.add(BorderLayout.EAST, buttonPane);

        setProductSceneView(VisatApp.getApp().getSelectedProductSceneView());

        // Add an internal frame listsner to VISAT so that we can update our
        // bitmask overlay window with the information of the currently activated
        // product scene view.
        VisatApp.getApp().addInternalFrameListener(new BitmaskOverlayIFL());


        updateUIState();

        return contentPane1;
    }

    private interface BmdAction {

        void doAction(final Product product);
    }

    private static class MoveAction implements BmdAction {

        private final BitmaskDef _bitmaskDef;
        private final int _destIndex;

        public MoveAction(final BitmaskDef bitmaskDef, final int destIndex) {
            _bitmaskDef = bitmaskDef;
            _destIndex = destIndex;
        }

        @Override
        public void doAction(final Product product) {
            Debug.trace("Applying action: move bitmask '" + _bitmaskDef.getName() + "' to index " + _destIndex);
            product.moveBitmaskDef(_bitmaskDef, _destIndex);
        }
    }

    private static class RemoveAction implements BmdAction {

        private final BitmaskDef _bitmaskDefOld;

        public RemoveAction(final BitmaskDef bitmaskDefOld) {
            _bitmaskDefOld = bitmaskDefOld;
        }

        @Override
        public void doAction(final Product product) {
            Debug.trace("Applying action: remove bitmask '" + _bitmaskDefOld.getName() + "'");
            product.removeBitmaskDef(_bitmaskDefOld);
        }
    }

    private static class NewAction implements BmdAction {

        private final BitmaskDef _bitmaskDefNew;

        public NewAction(final BitmaskDef bitmaskDefNew) {
            _bitmaskDefNew = bitmaskDefNew;
        }

        @Override
        public void doAction(final Product product) {
            Debug.trace("Applying action: add bitmask '" + _bitmaskDefNew.getName() + "'");
            product.addBitmaskDef(_bitmaskDefNew);
        }
    }

    private class EditAction implements BmdAction {

        private final BitmaskDef bitmaskDefOld;
        private final BitmaskDef bitmaskDefNew;
        private final int rowIndex;

        public EditAction(final BitmaskDef bitmaskDefOld, final BitmaskDef bitmaskDefNew, int rowIndex) {
            this.bitmaskDefOld = bitmaskDefOld;
            this.bitmaskDefNew = bitmaskDefNew;
            this.rowIndex = rowIndex;
        }

        @Override
        public void doAction(final Product product) {
            Debug.trace("Applying action: replace bitmask '" + bitmaskDefNew.getName() + "'");
            bitmaskDefOld.setName(bitmaskDefNew.getName());
            bitmaskDefOld.setDescription(bitmaskDefNew.getDescription());
            bitmaskDefOld.setColor(bitmaskDefNew.getColor());
            bitmaskDefOld.setTransparency(bitmaskDefNew.getTransparency());
            bitmaskDefOld.setExpr(bitmaskDefNew.getExpr());
            bitmaskDefTable.setBitmaskDefAt(bitmaskDefOld, rowIndex);
//            product.replaceBitmaskDef(bitmaskDefOld, bitmaskDefNew);
        }
    }

    private class BitmaskDefEditPane extends JPanel {

        ParamChangeListener _paramChangeListener = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
                firePropertyChange(event.getParameter().getName(), event.getOldValue(),
                                   event.getParameter().getValue());
            }
        };
        public BitmaskDef _bitmask;
        public Parameter _nameParam;
        public Parameter _descrParam;
        public Parameter _colorParam;
        public Parameter _transParam;

        public BitmaskDefEditPane(final BitmaskDef bitmask) {
            _bitmask = bitmask;
            createUI();
        }

        @Override
        public String getName() {
            return _nameParam != null ? (String) _nameParam.getValue() : "";
        }

        public String getDescription() {
            return (String) _descrParam.getValue();
        }

        public Color getColor() {
            return (Color) _colorParam.getValue();
        }

        public float getTransparency() {
            return (Float) _transParam.getValue();
        }

        private void createUI() {
            createParameter();
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder("Bitmask Properties"));            /*I18N*/
            final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
            GridBagUtils.setAttributes(gbc, "insets.top=3, fill=HORIZONTAL, weightx=1");

            gbc.gridy++;
            GridBagUtils.addToPanel(this, _nameParam.getEditor().getLabelComponent(), gbc);
            GridBagUtils.addToPanel(this, _colorParam.getEditor().getLabelComponent(), gbc);
            gbc.gridy++;
            GridBagUtils.addToPanel(this, _nameParam.getEditor().getComponent(), gbc);
            GridBagUtils.addToPanel(this, _colorParam.getEditor().getComponent(), gbc);
            gbc.gridy++;
            GridBagUtils.addToPanel(this, _descrParam.getEditor().getLabelComponent(), gbc);
            GridBagUtils.addToPanel(this, _transParam.getEditor().getLabelComponent(), gbc);
            gbc.gridy++;
            GridBagUtils.addToPanel(this, _descrParam.getEditor().getComponent(), gbc);
            GridBagUtils.addToPanel(this, _transParam.getEditor().getComponent(), gbc);
        }

        private void createParameter() {
            _nameParam = new Parameter("name", _bitmask.getName());
            _nameParam.getProperties().setLabel("Name");                /*I18N*/
            _nameParam.getProperties().setValueSetBound(true);
            _nameParam.getProperties().setEditorClass(TextFieldEditor.class);
            _nameParam.addParamChangeListener(_paramChangeListener);

            _descrParam = new Parameter("descr", _bitmask.getDescription());
            _descrParam.getProperties().setLabel("Description");                 /*I18N*/
            _descrParam.getProperties().setEditorClass(TextFieldEditor.class);
            _descrParam.addParamChangeListener(_paramChangeListener);

            _colorParam = new Parameter("color", _bitmask.getColor());
            _colorParam.getProperties().setLabel("Colour");                      /*I18N*/
            _colorParam.getProperties().setEditorClass(ColorEditor.class);
            _colorParam.addParamChangeListener(_paramChangeListener);

            _transParam = new Parameter("trans", _bitmask.getTransparency());
            _transParam.getProperties().setLabel("Transparency");               /*I18N*/
            _transParam.getProperties().setEditorClass(TextFieldEditor.class);
            _transParam.getProperties().setValidatorClass(NumberValidator.class);
            _transParam.addParamChangeListener(_paramChangeListener);
        }
    }

    private class BitmaskOverlayIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                setProductSceneView((ProductSceneView) content);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            if (getContent(e) instanceof ProductSceneView) {
                setProductSceneView(null);
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }
}
