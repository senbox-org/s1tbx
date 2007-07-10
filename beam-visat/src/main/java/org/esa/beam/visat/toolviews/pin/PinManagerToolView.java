/*
 * $Id: PinManagerToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.*;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class PinManagerToolView extends AbstractToolView {

    public static final String ID = PinManagerToolView.class.getName();

    public static final String PROPERTY_KEY_IO_DIR = "pin.io.dir";
    public static final String NAME_COL_NAME = "Name";
    public static final String LABEL_COL_NAME = "Label";
    public static final String DESC_COL_NAME = "Desc";
    public static final String X_COL_NAME = "X";
    public static final String Y_COL_NAME = "Y";
    public static final String LON_COL_NAME = "Lon";
    public static final String LAT_COL_NAME = "Lat";

    private static final String _FILE_EXTENSION_FLAT = ".pnf";
    private static final String _FILE_EXTENSION_XML = ".pnx";

    private VisatApp visatApp;
    private final PropertyMap propertyMap;
    private final HashMap<Product, Band[]> productToSelectedBands;
    private final HashMap<Product, TiePointGrid[]> productToSelectedGrids;

    private AbstractButton newButton;
    private AbstractButton copyButton;
    private AbstractButton editButton;
    private AbstractButton removeButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private AbstractButton importNButton;
    private AbstractButton exportNButton;
    private Product product;
    private JTable pinTable;
    private PinListener pinListener;
    private AbstractButton filterButton;
    private AbstractButton exportTableButton;
    private AbstractButton zoomToPinButton;
    private BeamFileFilter pinXMLFileFilter;
    private BeamFileFilter pinFlatFileFilter;
    private Band[] selectedBands;
    private TiePointGrid[] selectedGrids;
    private static final int indexForName = 0;
    private static final int indexForLon = 1;
    private static final int indexForLat = 2;
    private static final int indexForDesc = 3;
    private static final int indexForLabel = 4;
    private boolean synchronizingPinSelectedState;

    public PinManagerToolView() {
        this.visatApp = VisatApp.getApp();
        propertyMap = visatApp.getPreferences();
        productToSelectedBands = new HashMap<Product, Band[]>();
        productToSelectedGrids = new HashMap<Product, TiePointGrid[]>();
    }

    @Override
    public JComponent createControl() {
        pinTable = new JTable();
        pinTable.setName("pinTable");
        pinTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        pinTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pinTable.setRowSelectionAllowed(true);
        // IMPORTANT: We set ReorderingAllowed=false, because we export the
        // table model AS IS to a flat text file.
        pinTable.getTableHeader().setReorderingAllowed(false);

        final TableColumnModel columnModel = pinTable.getColumnModel();
        columnModel.addColumnModelListener(new ColumnModelListener());

        ToolTipSetter toolTipSetter = new ToolTipSetter();
        pinTable.addMouseMotionListener(toolTipSetter);
        pinTable.addMouseListener(toolTipSetter);
        pinTable.addMouseListener(new EditListener());
        pinTable.addMouseListener(new PopupListener());

        pinTable.setModel(new PinTableModel(product, selectedBands, selectedGrids));

        JScrollPane tableScrollPane = new JScrollPane(pinTable);
        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.add(tableScrollPane, BorderLayout.CENTER);

        pinTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final TableColumnModel columnModel = pinTable.getColumnModel();
                final int colIdx = columnModel.getColumnIndexAtX(e.getX());
                if (colIdx == 4) { // "Label" column
                    sortPins();
                }
            }
        });

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        newButton.setToolTipText("Create and add new pin."); /*I18N*/
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newPin();
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy an existing pin."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copyActivePin();
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit selected pin."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editActivePin();
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove selected pins."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeSelectedPins();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import the first pin from XML or text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(false);
                updateUIState();
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export the selected pins to XML file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportSelectedPins();
                updateUIState();
            }
        });

        importNButton = createButton("icons/ImportN24.gif");
        importNButton.setName("importNButton");
        importNButton.setToolTipText("Import all pins from XML or text file."); /*I18N*/
        importNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(true);
                updateUIState();
            }
        });

        exportNButton = createButton("icons/ExportN24.gif");
        exportNButton.setName("exportNButton");
        exportNButton.setToolTipText("Export all pins to XML file."); /*I18N*/
        exportNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null && product.getNumPins() > 0) {
                    exportPins(product.getPins());
                }
            }
        });

        filterButton = createButton("icons/Filter24.gif");
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Filter pixel data to be displayed in table."); /*I18N*/
        filterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null) {
                    Band[] allBands = product.getBands();
                    TiePointGrid[] allGrids = product.getTiePointGrids();
                    BandChooser bandChooser = new BandChooser(getWindowAncestor(),
                                                              "Available Bands And Tie Point Grids",
                                                              getDescriptor().getHelpId(), false,
                                                              allBands, selectedBands, allGrids, selectedGrids);
                    if (bandChooser.show() == ModalDialog.ID_OK) {
                        selectedBands = bandChooser.getSelectedBands();
                        selectedGrids = bandChooser.getSelectedTiePointGrids();
                        productToSelectedBands.put(product, selectedBands);
                        productToSelectedGrids.put(product, selectedGrids);
                        pinTable.setModel(new PinTableModel(product, selectedBands, selectedGrids));
                        updatePinList();
                    }
                }
                updateUIState();
            }
        });

        exportTableButton = createButton("icons/ExportTable.gif");
        exportTableButton.setName("exportTableButton");
        exportTableButton.setToolTipText("Export pixel data at pin to flat text file."); /*I18N*/
        exportTableButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportPinDataTable();
                updateUIState();
            }
        });

        zoomToPinButton = createButton("icons/ZoomTo24.gif");
        zoomToPinButton.setName("zoomToPinButton");
        zoomToPinButton.setToolTipText("Zoom to selected pin."); /*I18N*/
        zoomToPinButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomToActivePin();
            }
        });

        AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setName("helpButton");

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
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
        buttonPane.add(filterButton, gbc);
        buttonPane.add(exportTableButton, gbc);
        gbc.gridy++;
        buttonPane.add(zoomToPinButton, gbc);
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

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(BorderLayout.CENTER, mainPane);
        content.add(BorderLayout.EAST, buttonPane);

        content.setPreferredSize(new Dimension(370, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(getContentPane(), getDescriptor().getHelpId());
        }

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            setProduct(view.getProduct());
        }

        visatApp.addInternalFrameListener(new PinManagerIFL(visatApp));

        getContext().getPage().addPageComponentListener(new PageComponentListenerAdapter() {
            @Override
            public void componentOpened(PageComponent component) {
                ExecCommand command = (ExecCommand) visatApp.getCommandManager().getCommand(
                        PinTool.CMD_ID_SHOW_PIN_OVERLAY);
                command.setSelected(true);
                command.execute();
            }
        });


        updateUIState();

        return content;
    }

    public void setProduct(Product product) {
        if (this.product == product) {
            return;
        }
        Product oldProduct = this.product;
        if (oldProduct != null) {
            oldProduct.removeProductNodeListener(pinListener);
        }
        this.product = product;
        selectedBands = productToSelectedBands.get(this.product);
        selectedGrids = productToSelectedGrids.get(this.product);
        if (this.product != null) {
            setTitle(getDescriptor().getTitle() + " - " + this.product.getProductRefString());
            if (pinListener == null) {
                pinListener = new PinListener();
            }
            this.product.addProductNodeListener(pinListener);
        } else {
            setTitle(getDescriptor().getTitle());
        }

        pinTable.setModel(new PinTableModel(this.product, selectedBands, selectedGrids));
        updatePinList();
        updateUIState();
    }

    private void sortPins() {
        Guardian.assertNotNull("product", product);
        // todo - sort table here
        JOptionPane.showMessageDialog(getWindowAncestor(), "TODO: Implement sort!");
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

    private Pin getPinAt(final int selectedRow) {
        Pin pin = null;
        if (product != null) {
            if (selectedRow > -1 && selectedRow < product.getNumPins()) {
                pin = product.getPinAt(selectedRow);
            }
        }
        return pin;
    }

    private static AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // {{ Actions

    private void newPin() {
        Guardian.assertNotNull("product", product);
        String[] uniquePinNameAndLabel = createUniquePinNameAndLabel(product, 0, 0);
        Pin newPin = new Pin(uniquePinNameAndLabel[0],
                             uniquePinNameAndLabel[1],
                             new PixelPos(0,0));
        if (showEditPinDialog(getWindowAncestor(), product, newPin)) {
            makePinNameUnique(newPin);
            product.addPin(newPin);
            PinTool.setPinSelected(product, newPin, true);
            updateUIState();
        }
    }

    private void copyActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = product.getSelectedPin();
        Guardian.assertNotNull("activePin", activePin);
        Pin newPin = new Pin("copy_of_" + activePin.getName(),
                             activePin.getLabel(),
                             activePin.getDescription(),
                             activePin.getPixelPos(),
                             activePin.getGeoPos(),
                             PinSymbol.createDefaultPinSymbol()); // todo - create symbol clone
        if (showEditPinDialog(getWindowAncestor(), product, newPin)) {
            makePinNameUnique(newPin);
            product.addPin(newPin);
            PinTool.setPinSelected(product, newPin, true);
            updateUIState();
        }
    }

    private void editActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = product.getSelectedPin();
        Guardian.assertNotNull("activePin", activePin);
        if (showEditPinDialog(getWindowAncestor(), product, activePin)) {
            makePinNameUnique(activePin);
            updateUIState();
        }
    }

    private void removeSelectedPins() {
        final Pin pins[] = product.getSelectedPins();
        int i = JOptionPane.showConfirmDialog(getWindowAncestor(),
                                              "Do you really want to remove " + pins.length + " selected pin(s)?\n" +
                                                      "This action can not be undone.",
                                              getDescriptor().getTitle() + " - Remove Pins",
                                              JOptionPane.OK_CANCEL_OPTION);
        if (i == JOptionPane.OK_OPTION) {
            int selectedRow = pinTable.getSelectedRow();
            for (Pin pin : pins) {
                product.removePin(pin);
            }
            if (selectedRow >= product.getNumPins()) {
                selectedRow = product.getNumPins() - 1;
            }
            if (selectedRow >= 0) {
                pinTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            }
            updateUIState();
        }
    }


    private void zoomToActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = product.getSelectedPin();
        Guardian.assertNotNull("activePin", activePin);
        final ProductSceneView view = getSceneView();
        if (view != null) {
            final ImageDisplay imageDisplay = view.getImageDisplay();
            final PixelPos pos = activePin.getPixelPos();
            imageDisplay.zoom(pos.getX(), pos.getY(), imageDisplay.getViewModel().getViewScale());
            updateUIState();
        }
    }

    // }} Actions
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private void makePinNameUnique(Pin newPin) {
        if (makePinNameUnique(product, newPin)) {
            showWarningDialog("Pin has been renamed to '" + newPin.getName() + "',\n" +
                    "because a pin with the former name already exists.");
        }
    }

    private void updateUIState() {
        boolean productSelected = product != null;
        boolean hasPins = product != null && product.getNumPins() > 0;
        int numSelectedPins = 0;
        if (product != null) {
            synchronizePinSelectedState();
            numSelectedPins = product.getSelectedPins().length;
        }
        boolean hasSelectedPins = numSelectedPins > 0;
        boolean hasActivePin = numSelectedPins == 1;

        pinTable.setEnabled(productSelected);
        newButton.setEnabled(productSelected);
        copyButton.setEnabled(hasActivePin);
        editButton.setEnabled(hasActivePin);
        removeButton.setEnabled(hasSelectedPins);
        zoomToPinButton.setEnabled(hasActivePin);
        importButton.setEnabled(productSelected);
        exportButton.setEnabled(hasPins);
        importNButton.setEnabled(productSelected);
        exportNButton.setEnabled(hasPins);
        exportTableButton.setEnabled(hasPins);
        filterButton.setEnabled(productSelected);
    }

    private void synchronizePinSelectedState() {
        if (!synchronizingPinSelectedState) {
            try {
                synchronizingPinSelectedState = true;
                int[] selectedIndexes = pinTable.getSelectedRows();
                Pin[] pins = product.getPins();
                for (Pin pin : pins) {
                    boolean selected = false;
                    for (int selectedIndex : selectedIndexes) {
                        if (pin == product.getPinAt(selectedIndex)) {
                            selected = true;
                            break;
                        }
                    }
                    pin.setSelected(selected);
                }
            } finally {
                synchronizingPinSelectedState = false;
            }
        }
    }

    private void updatePinList() {
        if (product != null) {
            final Pin pin = product.getSelectedPin();
            if (pin != null) {
                final int pinIndex = product.getPinIndex(pin.getName());
                if (!pinTable.isRowSelected(pinIndex)) {
                    pinTable.setRowSelectionInterval(pinIndex, pinIndex);
                }
            } else {
                final int selectedRow = pinTable.getSelectedRow();
                if (selectedRow != -1 && pinTable.getRowCount() > selectedRow) {
                    pinTable.removeRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
        pinTable.revalidate();
        pinTable.repaint();
    }

    private void importPins(boolean allPins) {
        Pin[] pins;
        try {
            pins = loadPinsFromFile();
        } catch (IOException e) {
            showErrorDialog("I/O error, failed to import pins:\n" + e.getMessage());    /*I18N*/
            return;
        }
        if (pins.length == 0) {
            return;
        }
        int numPinsOutOfBounds = 0;
        int numPinsRenamed = 0;
        for (Pin pin : pins) {
            if (makePinNameUnique(product, pin)) {
                numPinsRenamed++;
            }
            final PixelPos pixelPos = product.getGeoCoding().getPixelPos(pin.getGeoPos(), null);
            if (product.containsPixel(pixelPos)) {
                product.addPin(pin);
            } else {
                numPinsOutOfBounds++;
            }
            if (!allPins) {
                break; // import only the first one
            }
        }
        if (numPinsRenamed > 0) {
            showWarningDialog("One or more pins have been renamed,\n" +
                    "because their former names are already existing."); /*I18N*/
        }
        if (numPinsOutOfBounds > 0) {
            if (numPinsOutOfBounds == pins.length) {
                showErrorDialog("No pins have been imported, because their pixel\n" +
                        "positions are outside the product's bounds."); /*I18N*/
            } else {
                showErrorDialog(numPinsOutOfBounds + " pins have not been imported, because their pixel\n" +
                        "positions are outside the product's bounds."); /*I18N*/
            }
        }
    }

    private static boolean makePinNameUnique(Product product, Pin pin) {
        if (product.getPin(pin.getName()) == pin) {
            return false;            
        }
        String name0 = pin.getName();
        String name = name0;
        int id = 1;
        while (product.containsPin(name)) {
            name = name0 + "_" + id;
        }
        if (!name0.equals(name)) {
            pin.setName(name);
            return true;
        }
        return false;
    }

    private Pin[] loadPinsFromFile() throws IOException {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Pin"); /*I18N*/
        fileChooser.addChoosableFileFilter(getOrCreateFlatPinFileFilter());
        fileChooser.setFileFilter(getOrCreateXMLPinFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        int result = fileChooser.showOpenDialog(getWindowAncestor());
        Pin[] pins = new Pin[0];
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                pins = readPinsFromFile(file);
            }
        }
        return pins;
    }

    private void exportSelectedPins() {
        exportPins(product.getSelectedPins());
    }

    private void exportPins(Pin[] pins) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Pins");   /*I18N*/
        fileChooser.setFileFilter(getOrCreateXMLPinFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, "Pins"));
        int result = fileChooser.showSaveDialog(getWindowAncestor());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, _FILE_EXTENSION_XML);
                try {
                    writePinsToFile(pins, file);
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\n   Failed to export Pins.");    /*I18N*/
                }
            }
        }
    }

    private void exportPinDataTable() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Pin Data Table");/*I18N*/
        fileChooser.setFileFilter(getOrCreateFlatPinFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, "Data"));
        int result = fileChooser.showSaveDialog(getWindowAncestor());

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, _FILE_EXTENSION_FLAT);
                try {
                    final Writer writer = new FileWriter(file);
                    writePinDataTableText(writer);
                    writer.close();
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to export pin data table."); /*I18N*/
                }
            }
        }
    }

    private void writePinDataTableText(final Writer writer) {
        final PrintWriter pw = new PrintWriter(writer);

        final int columnCountMin = PinTableModel.DEFAULT_COLUMN_NAMES.length;
        final int columnCount = pinTable.getColumnCount();

        // Write file header
        pw.println("# BEAM pin export table");
        pw.println("#");
        pw.println("# Product:\t" + product.getName());
        pw.println("# Created on:\t" + new Date());
        pw.println();

        // Write header columns
        pw.print(NAME_COL_NAME + "\t");
        pw.print(X_COL_NAME + "\t");
        pw.print(Y_COL_NAME + "\t");
        pw.print(LAT_COL_NAME + "\t");
        pw.print(LON_COL_NAME + "\t");
        pw.print(LABEL_COL_NAME + "\t");
        pw.print(DESC_COL_NAME + "\t");
        for (int i = columnCountMin; i < columnCount; i++) {
            pw.print(pinTable.getColumnName(i) + "\t");
        }
        pw.println();

        final int rowCount = pinTable.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final Pin pin = product.getPinAt(row);
            pw.print(pin.getName() + "\t");
            pw.print(pin.getPixelPos().getX() + "\t");
            pw.print(pin.getPixelPos().getY() + "\t");
            pw.print(pin.getGeoPos().getLat() + "\t");
            pw.print(pin.getGeoPos().getLon() + "\t");
            pw.print(pin.getLabel() + "\t");
            pw.print(pin.getDescription() + "\t");
            for (int col = columnCountMin; col < columnCount; col++) {
                final Object value = pinTable.getValueAt(row, col);
                pw.print(value.toString() + "\t");
            }
            pw.println();
        }

        pw.close();
    }

    private static void writePinsToFile(Pin[] pins, File outputFile) throws IOException {
        assert pins != null;
        assert outputFile != null;

        XmlWriter writer = new XmlWriter(outputFile);
        final String[] tags = XmlWriter.createTags(0, "BEAM-PINS");
        writer.println(tags[0]);
        for (Pin pin : pins) {
            if (pin != null) {
                pin.writeXML(writer, 1);
            }
        }
        writer.println(tags[1]);
        writer.close();
    }

    private static Pin[] readPinsFromFile(File inputFile) throws IOException {
        assert inputFile != null;
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(inputFile));
        byte[] magicBytes;
        try {
            magicBytes = new byte[5];
            dataInputStream.read(magicBytes);  // todo - BAD PRACTICE HERE!!!
        } finally {
            dataInputStream.close();
        }
        if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
            return readPinsFromXMLFile(inputFile);
        } else {
            return readPinsFromFlatFile(inputFile);
        }
    }

    private static Pin[] readPinsFromFlatFile(File inputFile) throws IOException {
        assert inputFile != null;
        int[] columnIndexes = null;
        int biggestIndex = 0;
        ArrayList<Pin> pins = new ArrayList<Pin>();

        final RandomAccessFile file = new RandomAccessFile(inputFile, "r");
        int row = 0;
        while (true) {
            String line = file.readLine();
            if (line == null) {
                break;
            }
            line = line.trim(); // cut \n and \r from the end of the line
            if (line.equals("") || line.startsWith("#")) {
                continue;
            }
            String[] strings = StringUtils.toStringArray(line, "\t");
            if (columnIndexes == null) {
                int nameIndex = StringUtils.indexOf(strings, NAME_COL_NAME);
                int lonIndex = StringUtils.indexOf(strings, LON_COL_NAME);
                int latIndex = StringUtils.indexOf(strings, LAT_COL_NAME);
                int descIndex = StringUtils.indexOf(strings, DESC_COL_NAME);
                int labelIndex = StringUtils.indexOf(strings, LABEL_COL_NAME);
                if (nameIndex == -1 || lonIndex == -1 || latIndex == -1) {
                    throw new IOException("Invalid pin file format:\n" +
                            "at least the columns 'Name', 'Lon' and 'Lat' must be given.");
                }
                biggestIndex = biggestIndex > nameIndex ? biggestIndex : nameIndex;
                biggestIndex = biggestIndex > lonIndex ? biggestIndex : lonIndex;
                biggestIndex = biggestIndex > latIndex ? biggestIndex : latIndex;
                columnIndexes = new int[5];
                columnIndexes[indexForName] = nameIndex;
                columnIndexes[indexForLon] = lonIndex;
                columnIndexes[indexForLat] = latIndex;
                columnIndexes[indexForDesc] = descIndex;
                columnIndexes[indexForLabel] = labelIndex;
            } else {
                row++;
                if (strings.length > biggestIndex) {
                    String name = strings[columnIndexes[indexForName]];
                    float lon;
                    try {
                        lon = Float.parseFloat(strings[columnIndexes[indexForLon]]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid pin file format:\n" +
                                "data row " + row + ": value for 'Lon' is invalid");      /*I18N*/
                    }
                    float lat;
                    try {
                        lat = Float.parseFloat(strings[columnIndexes[indexForLat]]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid pin file format:\n" +
                                "data row " + row + ": value for 'Lat' is invalid");      /*I18N*/
                    }
                    String desc = null;
                    if (columnIndexes[indexForDesc] >= 0 && strings.length > columnIndexes[indexForDesc]) {
                        desc = strings[columnIndexes[indexForDesc]];
                    }
                    String label = name;
                    if (columnIndexes[indexForLabel] >= 0 && strings.length > columnIndexes[indexForLabel]) {
                        label = strings[columnIndexes[indexForLabel]];
                    }
                    Pin pin = new Pin(name, label, new GeoPos(lat, lon));
                    if (desc != null) {
                        pin.setDescription(desc);
                    }
                    pins.add(pin);
                } else {
                    throw new IOException("Invalid pin file format:\n" +
                            "data row " + row + ": values for 'Name', 'Lon' and 'Lat' must be given.");   /*I18N*/
                }
            }
        }
        file.close();

        return pins.toArray(new Pin[pins.size()]);
    }

    private static Pin[] readPinsFromXMLFile(File inputFile) throws IOException {
        assert inputFile != null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = builder.parse(inputFile);
            Document document = new DOMBuilder().build(w3cDocument);
            final Element rootElement = document.getRootElement();
            final List children = rootElement.getChildren(DimapProductConstants.TAG_PIN);
            if (children != null) {
                final ArrayList<Pin> pins = new ArrayList<Pin>();
                for (Object child : children) {
                    final Element element = (Element) child;
                    try {
                        pins.add(Pin.createPin(element));
                    } catch (IllegalArgumentException e) {
                        // todo - ?
                    }
                }
                return pins.toArray(new Pin[0]);
            }
        } catch (FactoryConfigurationError error) {
            throw new IOException(error.toString());
        } catch (ParserConfigurationException e) {
            throw new IOException(e.toString(), e);
        } catch (SAXException e) {
            throw new IOException(e.toString(), e);
        } catch (IOException e) {
            throw new IOException(e.toString(), e);
        }
        return new Pin[0];
    }

    private BeamFileFilter getOrCreateFlatPinFileFilter() {
        if (pinFlatFileFilter == null) {
            String formatName = "PIN_FLAT_FILE";
            String description = "Pin files (*" + _FILE_EXTENSION_FLAT + ") - flat text format";
            pinFlatFileFilter = new BeamFileFilter(formatName, _FILE_EXTENSION_FLAT, description);
        }
        return pinFlatFileFilter;
    }

    private BeamFileFilter getOrCreateXMLPinFileFilter() {
        if (pinXMLFileFilter == null) {
            String formatName = "PIN_XML_FILE";
            String description = "Pin files (*" + _FILE_EXTENSION_XML + ") - XML format";
            pinXMLFileFilter = new BeamFileFilter(formatName, _FILE_EXTENSION_XML, description);
        }
        return pinXMLFileFilter;
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
        JOptionPane.showMessageDialog(this.getContentPane(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showWarningDialog(String message) {
        JOptionPane.showMessageDialog(this.getContentPane(),
                                      message,
                                      getDescriptor().getTitle() + " - Warning",
                                      JOptionPane.WARNING_MESSAGE);
    }

    private class PinListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            maybeRevalidatePinList(event);
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            maybeRevalidatePinList(event);
        }

        public void nodeAdded(ProductNodeEvent event) {
            maybeRevalidatePinList(event);
        }

        public void nodeRemoved(ProductNodeEvent event) {
            maybeRevalidatePinList(event);
        }

        private void maybeRevalidatePinList(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin) {
                updatePinList();
                updateUIState();
            }
        }
    }

    private class ToolTipSetter extends MouseInputAdapter {

        private int _rowIndex;

        public ToolTipSetter() {
            _rowIndex = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            _rowIndex = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowIndex = pinTable.rowAtPoint(e.getPoint());
            if (rowIndex != _rowIndex) {
                _rowIndex = rowIndex;
                if (_rowIndex >= 0 && _rowIndex < pinTable.getRowCount()) {
                    GeoPos geoPos = getPinAt(_rowIndex).getGeoPos();
                    if (geoPos != null) {
                        pinTable.setToolTipText(geoPos.getLonString() + " / " + geoPos.getLatString());
                    }
                }
            }
        }

    }

    private static class ColumnModelListener implements TableColumnModelListener {

        public void columnAdded(TableColumnModelEvent e) {
            int minWidth;
            final int index = e.getToIndex();
            switch (index) {
                case 0:
                case 1:
                    minWidth = 40;
                    break;
                default:
                    minWidth = 80;
            }
            TableColumnModel columnModel = (TableColumnModel) e.getSource();
            columnModel.getColumn(index).setPreferredWidth(minWidth);
        }

        public void columnRemoved(TableColumnModelEvent e) {
        }

        public void columnMoved(TableColumnModelEvent e) {
        }

        public void columnMarginChanged(ChangeEvent e) {
        }

        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    private class EditListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            action(e);
        }

        private void action(MouseEvent e) {
            updateUIState();
            if (e.getClickCount() == 2) {
                editActivePin();
            }
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
                final JPopupMenu popupMenu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Copy data to clipboard");
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent actionEvent) {
                        final StringWriter stringWriter = new StringWriter();
                        writePinDataTableText(stringWriter);
                        String text = stringWriter.toString();
                        text = text.replaceAll("\r\n", "\n");
                        text = text.replaceAll("\r", "\n");
                        SystemUtils.copyToClipboard(text);
                    }
                });
                popupMenu.add(menuItem);
                final Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), pinTable);
                popupMenu.show(pinTable, point.x, point.y);
            }
        }
    }

    public static boolean showEditPinDialog(Window parent, Product product, Pin pin) {
        final PinDialog pinDialog = new PinDialog(parent, product);
        pinDialog.getJDialog().setTitle(pin.getProduct() == null ? "New Pin" : "Edit Pin"); /*I18N*/
        pinDialog.setName(pin.getName());
        pinDialog.setLabel(pin.getLabel());
        pinDialog.setDescription(pin.getDescription() != null ? pin.getDescription() : "");
        pinDialog.setPixelPos(pin.getPixelPos());
        pinDialog.setGeoPos(pin.getGeoPos());
        pinDialog.setPinSymbol(pin.getSymbol());
        boolean ok = (pinDialog.show() == PinDialog.ID_OK);
        if (ok) {
            pin.setName(pinDialog.getName());
            pin.setLabel(pinDialog.getLabel());
            pin.setDescription(pinDialog.getDescription());
            pin.setGeoPos(pinDialog.getGeoPos());
            pin.setPixelPos(pinDialog.getPixelPos());
            pin.setSymbol(pinDialog.getPinSymbol());
        }
        return ok;
    }

    static String[] createUniquePinNameAndLabel(Product product, int pixelX, int pixelY) {
        int pinNumber = product.getNumPins() + 1;
        String name = createPinName(pinNumber);
        while (product.getPin(name) != null) {
            name = createPinName(++pinNumber);
        }
        final String label = createPinLabel(pinNumber, pixelX, pixelY);
        return new String[]{name, label};
    }

    private static String createPinName(final int pinNumber) {
        return "pin_" + pinNumber;
    }

    private static String createPinLabel(final int pinNumber, int pixelX, int pixelY) {
        return "Pin " + pinNumber;
    }

    /**
     * Returns the geographical position which is equivalent to the given pixel position. Returns <code>null</code> if
     * the given pixel position is outside of the given product or the geocoding cannot get geographical positions.
     *
     * @param product  must be given and must contain a geocoding.
     * @param pixelPos must be given.
     * @return the geographical position which is equivalent to the given pixel position or <code>null</code> if the
     *         given pixel position is outside of the given product or the geocoding cannot get geographical positions.
     */
    static GeoPos getGeoPos(Product product, PixelPos pixelPos) {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("pixelPos", pixelPos);
        final GeoCoding geoCoding = product.getGeoCoding();
        Guardian.assertNotNull("geoCoding", geoCoding);
        if (product.containsPixel(pixelPos)) {
            return geoCoding.getGeoPos(pixelPos, null);
        }
        return null;
    }

    private class PinManagerIFL extends InternalFrameAdapter {

        private final VisatApp visatApp;

        public PinManagerIFL(VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            updatePinManager();
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            updatePinManager();
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            updatePinManager();
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            updatePinManager();
        }

        private void updatePinManager() {
            final ProductSceneView view = visatApp.getSelectedProductSceneView();
            setProduct(view != null ? view.getProduct() : null);
        }
    }
}
