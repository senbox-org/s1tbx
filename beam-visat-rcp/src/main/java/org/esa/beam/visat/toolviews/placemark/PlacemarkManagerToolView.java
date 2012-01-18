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

package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.jidesoft.grid.SortableTable;
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.FloatCellEditor;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.*;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A dialog used to manage the list of pins or ground control points associated
 * with a selected product.
 */
public class PlacemarkManagerToolView extends AbstractToolView {

    public static final String PROPERTY_KEY_IO_DIR = "pin.io.dir";

    private final PlacemarkDescriptor placemarkDescriptor;

    private VisatApp visatApp;
    private final PropertyMap propertyMap;
    private final HashMap<Product, Band[]> productToSelectedBands;
    private final HashMap<Product, TiePointGrid[]> productToSelectedGrids;

    private Product product;
    private SortableTable placemarkTable;
    private PlacemarkListener placemarkListener;
    private Band[] selectedBands;
    private TiePointGrid[] selectedGrids;
    private boolean synchronizingPlacemarkSelectedState;
    private AbstractPlacemarkTableModel placemarkTableModel;
    private String prefixTitle;
    private PlacemarkManagerButtons buttonPane;
    private ProductSceneView currentView;
    private final SelectionChangeListener selectionChangeHandler;

    public PlacemarkManagerToolView(PlacemarkDescriptor placemarkDescriptor, TableModelFactory modelFactory) {
        this.placemarkDescriptor = placemarkDescriptor;
        visatApp = VisatApp.getApp();
        propertyMap = visatApp.getPreferences();
        productToSelectedBands = new HashMap<Product, Band[]>(50);
        productToSelectedGrids = new HashMap<Product, TiePointGrid[]>(50);
        placemarkTableModel = modelFactory.createTableModel(placemarkDescriptor, product, null, null);
        selectionChangeHandler = new ViewSelectionChangeHandler();
    }

    @Override
    public JComponent createControl() {
        prefixTitle = getDescriptor().getTitle();
        placemarkTable = new SortableTable();
        placemarkTable.setName("placemarkTable");
        placemarkTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        placemarkTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        placemarkTable.setRowSelectionAllowed(true);
        // IMPORTANT: We set ReorderingAllowed=false, because we export the
        // table model AS IS to a flat text file.
        placemarkTable.getTableHeader().setReorderingAllowed(false);

        ToolTipSetter toolTipSetter = new ToolTipSetter();
        placemarkTable.addMouseMotionListener(toolTipSetter);
        placemarkTable.addMouseListener(toolTipSetter);
        placemarkTable.addMouseListener(new PopupListener());
        placemarkTable.setModel(placemarkTableModel);
        placemarkTable.setDefaultCellRenderer(new RightAlignmentTableCellRenderer());
        placemarkTable.setDefaultRenderer(Float.class, new DecimalTableCellRenderer(new DecimalFormat("0.000")));
        placemarkTable.getSelectionModel().addListSelectionListener(new PlacemarkTableSelectionHandler());
        updateTableModel();

        final TableColumnModel columnModel = placemarkTable.getColumnModel();
        columnModel.addColumnModelListener(new ColumnModelListener());

        JScrollPane tableScrollPane = new JScrollPane(placemarkTable);
        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.add(tableScrollPane, BorderLayout.CENTER);

        buttonPane = new PlacemarkManagerButtons(this);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(BorderLayout.CENTER, mainPane);
        content.add(BorderLayout.EAST, buttonPane);
        Component southExtension = getSouthExtension();
        if (southExtension != null) {
            content.add(BorderLayout.SOUTH, southExtension);
        }
        content.setPreferredSize(new Dimension(420, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpKey(content, getDescriptor().getHelpId());
        }

        setCurrentView(visatApp.getSelectedProductSceneView());
        setProduct(visatApp.getSelectedProduct());

        visatApp.addInternalFrameListener(new PlacemarkManagerIFL());

        visatApp.getProductTree().addProductTreeListener(new ProductSelectionListener());

        updateUIState();

        return content;
    }

    void applyFilteredGrids() {
        if (product != null) {
            Band[] allBands = product.getBands();
            TiePointGrid[] allGrids = product.getTiePointGrids();
            BandChooser bandChooser = new BandChooser(getPaneWindow(),
                                                      "Available Bands And Tie Point Grids",
                                                      getDescriptor().getHelpId(), false,
                                                      allBands, selectedBands, allGrids, selectedGrids);
            if (bandChooser.show() == ModalDialog.ID_OK) {
                selectedBands = bandChooser.getSelectedBands();
                selectedGrids = bandChooser.getSelectedTiePointGrids();
                productToSelectedBands.put(product, selectedBands);
                productToSelectedGrids.put(product, selectedGrids);
                updateTableModel();
            }
        }
    }

    @Override
    public void componentOpened() {
        final CommandManager commandManager = visatApp.getCommandManager();
        final String layerCommandId = placemarkDescriptor.getShowLayerCommandId();

        ExecCommand command = (ExecCommand) commandManager.getCommand(layerCommandId);
        command.setSelected(true);
        command.execute();
    }

    PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    private void setCurrentView(ProductSceneView sceneView) {
        if (sceneView != currentView) {
            if (currentView != null) {
                currentView.getSelectionContext().removeSelectionChangeListener(selectionChangeHandler);
            }
            currentView = sceneView;
            if (currentView != null) {
                currentView.getSelectionContext().addSelectionChangeListener(selectionChangeHandler);
                setProduct(currentView.getProduct());
            } else {
                setProduct(null);
            }
        }
    }

    protected final Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if (this.product == product) {
            return;
        }
        Product oldProduct = this.product;
        if (oldProduct != null) {
            oldProduct.removeProductNodeListener(placemarkListener);
        }
        this.product = product;
        selectedBands = productToSelectedBands.get(this.product);
        selectedGrids = productToSelectedGrids.get(this.product);
        if (this.product != null) {
            setTitle(getDescriptor().getTitle() + " - " + this.product.getProductRefString());
            if (placemarkListener == null) {
                placemarkListener = new PlacemarkListener();
            }
            this.product.addProductNodeListener(placemarkListener);
        } else {
            setTitle(getDescriptor().getTitle());
        }

        updateTableModel();
        updatePlacemarkTableSelectionFromView();
        updateUIState();
    }

    protected Component getSouthExtension() {
        return null;
    }


    private void updateTableModel() {
        placemarkTableModel.setProduct(product);
        placemarkTableModel.setSelectedBands(selectedBands);
        placemarkTableModel.setSelectedGrids(selectedGrids);
        addCellRenderer(placemarkTable.getColumnModel());
        addCellEditor(placemarkTable.getColumnModel());

    }

    protected void addCellRenderer(TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000")));
        columnModel.getColumn(1).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000")));
        columnModel.getColumn(2).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
        columnModel.getColumn(3).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
    }

    protected void addCellEditor(TableColumnModel columnModel) {
        final FloatCellEditor pixelCellEditor = new FloatCellEditor();
        columnModel.getColumn(0).setCellEditor(pixelCellEditor);
        columnModel.getColumn(1).setCellEditor(pixelCellEditor);
        columnModel.getColumn(2).setCellEditor(new FloatCellEditor(-180, 180));
        columnModel.getColumn(3).setCellEditor(new FloatCellEditor(-90, 90));
    }

    private ProductSceneView getSceneView() {
        final ProductSceneView selectedProductSceneView = visatApp.getSelectedProductSceneView();
        if (selectedProductSceneView == null && product != null) {
            final Band[] bands = product.getBands();
            for (Band band : bands) {
                final JInternalFrame internalFrame = visatApp.findInternalFrame(band);
                if (internalFrame != null) {
                    final Container contentPane = internalFrame.getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        return (ProductSceneView) contentPane;
                    }
                }
            }
            final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
            for (TiePointGrid tiePointGrid : tiePointGrids) {
                final JInternalFrame internalFrame = visatApp.findInternalFrame(tiePointGrid);
                if (internalFrame != null) {
                    final Container contentPane = internalFrame.getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        return (ProductSceneView) contentPane;
                    }
                }
            }
        }
        return selectedProductSceneView;
    }

    private Placemark getPlacemarkAt(final int selectedRow) {
        Placemark placemark = null;
        if (product != null) {
            if (selectedRow > -1 && selectedRow < getPlacemarkGroup().getNodeCount()) {
                placemark = getPlacemarkGroup().get(selectedRow);
            }
        }
        return placemark;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // {{ Actions

    void newPin() {
        Guardian.assertNotNull("product", product);
        String[] uniquePinNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor, product);
        Placemark newPlacemark = Placemark.createPointPlacemark(placemarkDescriptor, uniquePinNameAndLabel[0],
                                                                uniquePinNameAndLabel[1],
                                                                "",
                                                                new PixelPos(0, 0), null,
                                                                product.getGeoCoding());
        if (PlacemarkDialog.showEditPlacemarkDialog(getPaneWindow(), product, newPlacemark, placemarkDescriptor)) {
            makePlacemarkNameUnique(newPlacemark);
            updateUIState();
        }
    }

    void copyActivePlacemark() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        Placemark newPlacemark = Placemark.createPointPlacemark(activePlacemark.getDescriptor(), "copy_of_" + activePlacemark.getName(),
                                                                activePlacemark.getLabel(),
                                                                activePlacemark.getDescription(),
                                                                activePlacemark.getPixelPos(),
                                                                activePlacemark.getGeoPos(),
                                                                activePlacemark.getProduct().getGeoCoding());
        newPlacemark.setStyleCss(activePlacemark.getStyleCss());
        if (PlacemarkDialog.showEditPlacemarkDialog(getPaneWindow(), product, newPlacemark, placemarkDescriptor)) {
            makePlacemarkNameUnique(newPlacemark);
            updateUIState();
        }
    }

    private ProductNodeGroup<Placemark> getPlacemarkGroup() {
        return placemarkDescriptor.getPlacemarkGroup(product);
    }

    void editActivePin() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        if (PlacemarkDialog.showEditPlacemarkDialog(getPaneWindow(), product, activePlacemark,
                                                    placemarkDescriptor)) {
            makePlacemarkNameUnique(activePlacemark);
            updateUIState();
        }
    }

    void removeSelectedPins() {
        final List<Placemark> placemarks = getSelectedPlacemarks();
        int i = JOptionPane.showConfirmDialog(getPaneWindow(),
                                              MessageFormat.format(
                                                      "Do you really want to remove {0} selected {1}(s)?\n" +
                                                              "This action can not be undone.",
                                                      placemarks.size(), placemarkDescriptor.getRoleLabel()),
                                              MessageFormat.format("{0} - Remove {1}s", getDescriptor().getTitle(),
                                                                   placemarkDescriptor.getRoleLabel()),
                                              JOptionPane.OK_CANCEL_OPTION);
        if (i == JOptionPane.OK_OPTION) {
            int selectedRow = placemarkTable.getSelectedRow();
            for (Placemark placemark : placemarks) {
                getPlacemarkGroup().remove(placemark);
            }
            if (selectedRow >= getPlacemarkGroup().getNodeCount()) {
                selectedRow = getPlacemarkGroup().getNodeCount() - 1;
            }
            if (selectedRow >= 0) {
                placemarkTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            }
            updateUIState();
        }
    }

    private int getNumSelectedPlacemarks() {
        int[] rowIndexes = placemarkTable.getSelectedRows();
        return rowIndexes != null ? rowIndexes.length : 0;
    }

    private Placemark getSelectedPlacemark() {
        int rowIndex = placemarkTable.getSelectedRow();
        if (rowIndex >= 0) {
            return placemarkTableModel.getPlacemarkAt(placemarkTable.getActualRowAt(rowIndex));
        }
        return null;
    }

    private List<Placemark> getSelectedPlacemarks() {
        List<Placemark> placemarkList = new ArrayList<Placemark>();
        int[] sortedRowIndexes = placemarkTable.getSelectedRows();
        if (sortedRowIndexes != null) {
            for (int rowIndex : sortedRowIndexes) {
                int modelRowIndex = placemarkTable.getActualRowAt(rowIndex);
                placemarkList.add(placemarkTableModel.getPlacemarkAt(modelRowIndex));
            }
        }
        return placemarkList;
    }

    void zoomToActivePin() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        final ProductSceneView view = getSceneView();
        final PixelPos imagePos = activePlacemark.getPixelPos();  // in image coordinates on Level 0, can be null
        if (view != null && imagePos != null) {
            final ImageLayer layer = view.getBaseImageLayer();
            final AffineTransform imageToModelTransform = layer.getImageToModelTransform(0);
            final Point2D modelPos = imageToModelTransform.transform(imagePos, null);
            view.zoom(modelPos.getX(), modelPos.getY(), view.getZoomFactor());
            updateUIState();
        }
    }

    // }} Actions
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private void makePlacemarkNameUnique(Placemark newPlacemark) {
        if (makePlacemarkNameUnique0(newPlacemark)) {
            showWarningDialog(MessageFormat.format("{0} has been renamed to ''{1}'',\n" +
                                                           "because a {2} with the former name already exists.",
                                                   firstLetterUp(placemarkDescriptor.getRoleLabel()),
                                                   newPlacemark.getName(),
                                                   placemarkDescriptor.getRoleLabel()));
        }
    }

    /**
     * Turns the first letter of the given string to upper case.
     *
     * @param string the string to change
     * @return a changed string
     */
    private String firstLetterUp(String string) {
        String firstChar = string.substring(0, 1).toUpperCase();
        return firstChar + string.substring(1);
    }

    protected void updateUIState() {
        boolean productSelected = product != null;
        int numSelectedPins = 0;
        if (productSelected) {
            updatePlacemarkTableSelectionFromView();
            numSelectedPins = getNumSelectedPlacemarks();
            getDescriptor().setTitle(prefixTitle + " - " + product.getDisplayName());
        } else {
            getDescriptor().setTitle(prefixTitle);
        }

        placemarkTable.setEnabled(productSelected);
        buttonPane.updateUIState(productSelected, numSelectedPins);
    }

    private void updatePlacemarkTableSelectionFromView() {
        if (!synchronizingPlacemarkSelectedState) {
            try {
                synchronizingPlacemarkSelectedState = true;
                if (product != null) {
                    Placemark[] placemarks = placemarkTableModel.getPlacemarks();
                    for (int i = 0; i < placemarks.length; i++) {
                        if (i < placemarkTable.getRowCount()) {
                            Placemark placemark = placemarks[i];
                            int sortedRowAt = placemarkTable.getSortedRowAt(i);
                            boolean selected = isPlacemarkSelectedInView(placemark);
                            if (selected != placemarkTable.isRowSelected(sortedRowAt)) {
                                if (selected) {
                                    placemarkTable.getSelectionModel().addSelectionInterval(sortedRowAt, sortedRowAt);
                                } else {
                                    placemarkTable.getSelectionModel().removeSelectionInterval(sortedRowAt,
                                                                                               sortedRowAt);
                                }
                            }
                        }
                    }
                }
                placemarkTable.revalidate();
                placemarkTable.repaint();
            } finally {
                synchronizingPlacemarkSelectedState = false;
            }
        }
    }

    void importPlacemarks(boolean allPlacemarks) {
        List<Placemark> placemarks;
        try {
            placemarks = loadPlacemarksFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog(
                    MessageFormat.format("I/O error, failed to import {0}s:\n{1}",  /*I18N*/
                                         placemarkDescriptor.getRoleLabel(), e.getMessage()));
            return;
        }
        if (placemarks.isEmpty()) {
            return;
        }
        int numPinsOutOfBounds = 0;
        int numPinsRenamed = 0;
        int numInvalids = 0;
        final GeoCoding geoCoding = product.getGeoCoding();
        for (Placemark placemark : placemarks) {
            if (makePlacemarkNameUnique0(placemark)) {
                numPinsRenamed++;
            }

            final PixelPos pixelPos = placemark.getPixelPos();
            if ((!product.containsPixel(pixelPos) && (geoCoding == null || !geoCoding.canGetPixelPos()))
                    && placemarkDescriptor instanceof PinDescriptor) {
                numInvalids++;
                continue;
            }

            // from here on we only handle GCPs and valid Pins

            final GeoPos geoPos = placemark.getGeoPos();
            if (geoCoding != null && geoCoding.canGetPixelPos()) {
                placemarkDescriptor.updatePixelPos(geoCoding, geoPos, pixelPos);
            }

            if (!product.containsPixel(pixelPos) && placemarkDescriptor instanceof PinDescriptor) {
                numPinsOutOfBounds++;
            } else {
                getPlacemarkGroup().add(placemark);
                placemark.setPixelPos(pixelPos);
            }

            if (!allPlacemarks) {
                break; // import only the first one
            }
        }

        if (numInvalids > 0) {
            showWarningDialog(MessageFormat.format(
                    "One or more {0}s have not been imported,\nbecause they can not be assigned to a product without a geo-coding.",
                    placemarkDescriptor.getRoleLabel())); /*I18N*/
        }
        if (numPinsRenamed > 0) {
            showWarningDialog(MessageFormat.format(
                    "One or more {0}s have been renamed,\nbecause their former names are already existing.",
                    placemarkDescriptor.getRoleLabel())); /*I18N*/
        }
        if (numPinsOutOfBounds > 0) {
            if (numPinsOutOfBounds == placemarks.size()) {
                showErrorDialog(
                        MessageFormat.format(
                                "No {0}s have been imported, because their pixel\npositions are outside the product''s bounds.",
                                placemarkDescriptor.getRoleLabel())); /*I18N*/
            } else {
                showErrorDialog(
                        MessageFormat.format(
                                "{0} {1}s have not been imported, because their pixel\npositions are outside the product''s bounds.",
                                numPinsOutOfBounds, placemarkDescriptor.getRoleLabel())); /*I18N*/
            }
        }
    }

    private boolean isPlacemarkSelectedInView(Placemark placemark) {
        boolean selected = false;
        if (getSceneView() != null) {
            if (getPlacemarkDescriptor() instanceof PinDescriptor) {
                selected = getSceneView().isPinSelected(placemark);
            } else {
                selected = getSceneView().isGcpSelected(placemark);
            }
        }
        return selected;
    }

    private boolean makePlacemarkNameUnique0(Placemark placemark) {
        if (getPlacemarkGroup().get(placemark.getName()) == placemark) {
            return false;
        }
        String name0 = placemark.getName();
        String name = name0;
        int id = 1;
        while (getPlacemarkGroup().contains(name)) {
            name = name0 + "_" + id;
            id++;
        }
        if (!name0.equals(name)) {
            placemark.setName(name);
            return true;
        }
        return false;
    }

    private List<Placemark> loadPlacemarksFromFile() throws IOException {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        String roleLabel = firstLetterUp(placemarkDescriptor.getRoleLabel());
        fileChooser.setDialogTitle("Import " + roleLabel + "s"); /*I18N*/
        setComponentName(fileChooser, "Import");
        fileChooser.addChoosableFileFilter(PlacemarkIO.createTextFileFilter());
        fileChooser.setFileFilter(PlacemarkIO.createPlacemarkFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        int result = fileChooser.showOpenDialog(getPaneWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                GeoCoding geoCoding = null;
                if (product != null) {
                    geoCoding = product.getGeoCoding();
                }
                return PlacemarkIO.readPlacemarks(new FileReader(file), geoCoding, placemarkDescriptor);
            }
        }
        return Collections.emptyList();
    }

    void exportSelectedPlacemarks() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle(MessageFormat.format("Export Selected {0}(s)",
                                                        firstLetterUp(placemarkDescriptor.getRoleLabel())));   /*I18N*/
        setComponentName(fileChooser, "Export_Selected");
        fileChooser.addChoosableFileFilter(PlacemarkIO.createTextFileFilter());
        fileChooser.setFileFilter(PlacemarkIO.createPlacemarkFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, placemarkDescriptor.getRoleName()));
        int result = fileChooser.showSaveDialog(getPaneWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                BeamFileFilter beamFileFilter = fileChooser.getBeamFileFilter();
                if (!StringUtils.contains(beamFileFilter.getExtensions(), FileUtils.getExtension(file))) {
                    file = FileUtils.ensureExtension(file, beamFileFilter.getDefaultExtension());
                }
                try {
                    if (beamFileFilter.getFormatName().equals(
                            PlacemarkIO.createPlacemarkFileFilter().getFormatName())) {
                        PlacemarkIO.writePlacemarksFile(new FileWriter(file), getSelectedPlacemarks());
                    } else {
                        Writer writer = new FileWriter(file);
                        try {
                            writePlacemarkDataTableText(writer);
                        } finally {
                            writer.close();
                        }
                    }
                } catch (IOException ioe) {
                    showErrorDialog(
                            String.format("I/O Error.\n   Failed to export %ss.\n%s",
                                          placemarkDescriptor.getRoleLabel(), ioe.getMessage()));
                    ioe.printStackTrace();
                }
            }
        }
    }

    private void setComponentName(JComponent component, String name) {
        component.setName(getClass().getName() + "." + name);
    }

    void exportPlacemarkDataTable() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle(MessageFormat.format("Export {0} Data Table",  /*I18N*/
                                                        firstLetterUp(placemarkDescriptor.getRoleLabel())));
        setComponentName(fileChooser, "Export_Data_Table");
        fileChooser.setFileFilter(PlacemarkIO.createTextFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, "Data"));
        int result = fileChooser.showSaveDialog(getPaneWindow());

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, PlacemarkIO.FILE_EXTENSION_FLAT_TEXT);
                try {
                    Writer writer = new FileWriter(file);
                    try {
                        writePlacemarkDataTableText(writer);
                    } finally {
                        writer.close();
                    }
                } catch (IOException ignored) {
                    showErrorDialog(MessageFormat.format("I/O Error.\nFailed to export {0} data table.",  /*I18N*/
                                                         placemarkDescriptor.getRoleLabel()));
                }
            }
        }
    }

    private void writePlacemarkDataTableText(final Writer writer) {

        final String[] standardColumnNames = placemarkTableModel.getStandardColumnNames();
        final int columnCountMin = standardColumnNames.length;
        final int columnCount = placemarkTableModel.getColumnCount();
        String[] additionalColumnNames = new String[columnCount - columnCountMin];
        for (int i = 0; i < additionalColumnNames.length; i++) {
            additionalColumnNames[i] = placemarkTableModel.getColumnName(columnCountMin + i);
        }

        List<Placemark> placemarkList = new ArrayList<Placemark>();
        List<Object[]> valueList = new ArrayList<Object[]>();
        for (int sortedRow = 0; sortedRow < placemarkTable.getRowCount(); ++sortedRow) {
            if (placemarkTable.getSelectionModel().isSelectedIndex(sortedRow)) {
                final int modelRow = placemarkTable.getActualRowAt(sortedRow);
                placemarkList.add(placemarkTableModel.getPlacemarkAt(modelRow));
                Object[] values = new Object[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    values[col] = placemarkTableModel.getValueAt(modelRow, col);
                }
                valueList.add(values);
            }
        }

        PlacemarkIO.writePlacemarksWithAdditionalData(writer,
                                                      placemarkDescriptor.getRoleLabel(),
                                                      product.getName(),
                                                      placemarkList,
                                                      valueList,
                                                      standardColumnNames,
                                                      additionalColumnNames);
    }

    private void setIODir(File dir) {
        if (propertyMap != null && dir != null) {
            propertyMap.setPropertyString(PROPERTY_KEY_IO_DIR, dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (propertyMap != null) {
            dir = new File(propertyMap.getPropertyString(PROPERTY_KEY_IO_DIR, dir.getPath()));
        }
        return dir;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(getPaneControl(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showWarningDialog(String message) {
        JOptionPane.showMessageDialog(getPaneControl(),
                                      message,
                                      getDescriptor().getTitle() + " - Warning",
                                      JOptionPane.WARNING_MESSAGE);
    }

    private class PlacemarkListener implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product) && sourceNode instanceof Placemark) {
                updateUIState();
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product) && sourceNode instanceof Placemark) {
                updateUIState();
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product) && sourceNode instanceof Placemark) {
                placemarkTableModel.addPlacemark((Placemark) sourceNode);
                updateUIState();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark
                    && sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(product)) {
                placemarkTableModel.removePlacemark((Placemark) sourceNode);
                updateUIState();
            }
        }

    }

    private class ToolTipSetter extends MouseInputAdapter {

        private int _rowIndex;

        private ToolTipSetter() {
            _rowIndex = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            _rowIndex = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowIndex = placemarkTable.rowAtPoint(e.getPoint());
            if (rowIndex != _rowIndex) {
                _rowIndex = rowIndex;
                if (_rowIndex >= 0 && _rowIndex < placemarkTable.getRowCount()) {
                    GeoPos geoPos = getPlacemarkAt(placemarkTable.getActualRowAt(_rowIndex)).getGeoPos();
                    if (geoPos != null) {
                        placemarkTable.setToolTipText(geoPos.getLonString() + " / " + geoPos.getLatString());
                    }
                }
            }
        }

    }

    private static class ColumnModelListener implements TableColumnModelListener {

        @Override
        public void columnAdded(TableColumnModelEvent e) {
            int minWidth;
            final int index = e.getToIndex();
            switch (index) {
                case 0:
                case 1:
                    minWidth = 60;
                    break;
                default:
                    minWidth = 80;
            }
            TableColumnModel columnModel = (TableColumnModel) e.getSource();
            columnModel.getColumn(index).setPreferredWidth(minWidth);
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    private class PopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            action(e);
        }

        private void action(MouseEvent e) {
            if (e.isPopupTrigger()) {

                if (getNumSelectedPlacemarks() > 0) {
                    final JPopupMenu popupMenu = new JPopupMenu();
                    final JMenuItem menuItem;
                    menuItem = new JMenuItem("Copy selected data to clipboard");
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            final StringWriter stringWriter = new StringWriter();
                            writePlacemarkDataTableText(stringWriter);
                            String text = stringWriter.toString();
                            text = text.replaceAll("\r\n", "\n");
                            text = text.replaceAll("\r", "\n");
                            SystemUtils.copyToClipboard(text);
                        }
                    });

                    popupMenu.add(menuItem);
                    final Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), placemarkTable);
                    popupMenu.show(placemarkTable, point.x, point.y);
                }
            }
        }
    }

    private class PlacemarkManagerIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) contentPane;
                setCurrentView(sceneView);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) contentPane;
                if (sceneView == currentView) {
                    setCurrentView(null);
                }
            }
        }

    }

    private class ProductSelectionListener extends ProductTreeListenerAdapter {

        @Override
        public void productRemoved(Product product) {
            if (product == getProduct()) {
                setProduct(null);
                productToSelectedBands.remove(product);
                productToSelectedGrids.remove(product);
            }
        }

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            setProduct(productNode.getProduct());
        }
    }


    private class PlacemarkTableSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ProductSceneView sceneView = getSceneView();
            if (sceneView == null) {
                return;
            }

            if (e.getValueIsAdjusting() || e.getFirstIndex() == -1 || synchronizingPlacemarkSelectedState) {
                return;
            }

            try {
                synchronizingPlacemarkSelectedState = true;
                Placemark[] placemarks = placemarkTableModel.getPlacemarks();
                ArrayList<Placemark> selectedPlacemarks = new ArrayList<Placemark>();
                for (int i = 0; i < placemarks.length; i++) {
                    Placemark placemark = placemarks[i];
                    int sortedIndex = placemarkTable.getSortedRowAt(i);
                    boolean selected = placemarkTable.isRowSelected(sortedIndex);
                    if (selected) {
                        selectedPlacemarks.add(placemark);
                    }
                }
                Placemark[] placemarkArray = selectedPlacemarks.toArray(new Placemark[selectedPlacemarks.size()]);
                if (getPlacemarkDescriptor() instanceof PinDescriptor) {
                    sceneView.selectPins(placemarkArray);
                } else {
                    sceneView.selectGcps(placemarkArray);
                }
            } finally {
                updateUIState();
                synchronizingPlacemarkSelectedState = false;
            }

        }
    }

    private class ViewSelectionChangeHandler implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            if (synchronizingPlacemarkSelectedState) {
                return;
            }
            Layer layer = getSceneView().getSelectedLayer();
            if (layer instanceof VectorDataLayer) {
                VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
                if (vectorDataLayer.getVectorDataNode() == getProduct().getPinGroup().getVectorDataNode() ||
                        vectorDataLayer.getVectorDataNode() == getProduct().getGcpGroup().getVectorDataNode()) {
                    updateUIState();
                }
            }
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
        }
    }

    private static class RightAlignmentTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                                              column);
            label.setHorizontalAlignment(JLabel.RIGHT);
            return label;


        }
    }
}
