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
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;

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

    public PinManagerToolView() {
        this.visatApp = VisatApp.getApp();
        propertyMap = visatApp.getPreferences();
        productToSelectedBands = new HashMap<Product, Band[]>();
        productToSelectedGrids = new HashMap<Product, TiePointGrid[]>();
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
        if (product != null) {
            final Pin[] pins = product.getPins();
            final Pin selectedPin = product.getSelectedPin();
            final Map<String, Pin> sortedMap = new TreeMap<String, Pin>();
            for (Pin pin : pins) {
                product.removePin(pin);
                sortedMap.put(pin.getName(), pin);
            }
            final Collection<Pin> pinsSorted = sortedMap.values();
            for (Pin pin : pinsSorted) {
                product.addPin(pin);
            }
            if (selectedPin != null) {
                product.setSelectedPin(selectedPin.getName());
            }
        }
    }

    private void zoomToSelectedPin(Pin pin) {
        if (pin == null || !pin.isSelected()) {
            return;
        }
        final ProductSceneView view = getSceneView();
        if (view == null) {
            return;
        }
        final ImageDisplay imageDisplay = view.getImageDisplay();
        final PixelPos pos = pin.getPixelPos();
        imageDisplay.zoom(pos.getX(), pos.getY(), imageDisplay.getViewModel().getViewScale());
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

    private Pin getSelectedPinFromTable() {
        final int selectedRow = pinTable.getSelectedRow();
        return getPinAt(selectedRow);
    }

    private Pin[] getSelectedPinsFromTable() {
        final int[] selectedRows = pinTable.getSelectedRows();
        return getPinsAt(selectedRows);
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

    private Pin[] getPinsAt(final int[] rows) {
        List<Pin> pinList = new ArrayList<Pin>();
        if (product != null) {
            for (int rowIndex : rows) {
                if (rowIndex > -1 && rowIndex < product.getNumPins()) {
                    pinList.add(product.getPinAt(rowIndex));
                }

            }
        }
        return pinList.toArray(new Pin[pinList.size()]);
    }

    private Pin checkPinWithTheSameNameExists(Pin newPin) {
        while (newPin != null) {
            final String name = newPin.getName();
            if (product.containsPin(name)) {
                int result = JOptionPane.showConfirmDialog(null,
                                                           "A pin with the name '" + name + "' already exists.\n" +
                                                           "Do you want to replace the existing pin?");
                if (result == JOptionPane.YES_OPTION) {
                    break;
                } else if (result == JOptionPane.NO_OPTION) {
                    newPin = copyPin(newPin, product, getWindowAncestor());
                } else {
                    newPin = null;
                }
            } else {
                break;
            }
        }
        return newPin;
    }

    private static AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    private void editSelectedPin() {
        Pin pin = getSelectedPinFromTable();
        if (pin != null) {
            if (product != null) {
                Pin newPin = editPin(pin, product, getWindowAncestor());
                product.replacePin(pin, newPin);
            }
        }
    }

    private void updateUIState() {
        final GeoCoding geoCoding = product != null ? product.getGeoCoding() : null;
        boolean hasUsableGeocoding = geoCoding != null && geoCoding.canGetPixelPos();
        Pin selectedPin = null;
        if (hasUsableGeocoding) {
            if (pinTable != null) {
                selectedPin = getSelectedPinFromTable();
            }
            if (selectedPin != null) {
                product.setSelectedPin(selectedPin.getName());
            }
        }
        boolean hasSelectedPin = selectedPin != null;
        boolean hasPins = hasUsableGeocoding && product.getNumPins() > 0;
        newButton.setEnabled(hasUsableGeocoding);
        copyButton.setEnabled(hasSelectedPin);
        editButton.setEnabled(hasSelectedPin);
        removeButton.setEnabled(hasSelectedPin);
        importButton.setEnabled(hasUsableGeocoding);
        exportButton.setEnabled(hasSelectedPin);
        importNButton.setEnabled(hasUsableGeocoding);
        exportNButton.setEnabled(hasPins);
        filterButton.setEnabled(hasUsableGeocoding);
        exportTableButton.setEnabled(hasPins);
    }

    private void updatePinList() {
        if (product != null) {
            final Pin pin = product.getSelectedPin();
            if (pin != null) {
                final int pinIndex = product.indexOfPin(pin.getName());
                if(!pinTable.isRowSelected(pinIndex)) {
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
        if (pins == null) {
            return;
        }
        int numPinsOutOfBounds = 0;
        for (Pin pin : pins) {
            if (pin != null) {
                pin = checkPinWithTheSameNameExists(pin);
                if (pin != null) {
                    final PixelPos pixelPos = product.getGeoCoding().getPixelPos(pin.getGeoPos(), null);
                    if (product.containsPixel(pixelPos)) {
                        product.addPin(pin);
                    } else {
                        numPinsOutOfBounds++;
                    }
                }
                if (!allPins) {
                    break; // import only the first one
                }
            }
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

    private Pin[] loadPinsFromFile() throws IOException {
        Pin[] pins;
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Pin"); /*I18N*/
        fileChooser.addChoosableFileFilter(getOrCreateFlatPinFileFilter());
        fileChooser.setFileFilter(getOrCreateXMLPinFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        int result = fileChooser.showOpenDialog(getWindowAncestor());
        pins = null;
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
        exportPins(getSelectedPinsFromTable());
    }

    private void exportPins(Pin[] pins) {
        if (pins != null) {
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
        } else {
            showErrorDialog("No Pin defined");
        }
    }

    private void exportPinDataTable() {
        if (product == null) {
            return;
        }
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
        if (inputFile.canRead()) {
            final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(inputFile));
            byte[] magicBytes;
            try {
                magicBytes = new byte[5];
                dataInputStream.read(magicBytes);
            } finally {
                dataInputStream.close();
            }
            if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
                return createPinsFromXMLFile(inputFile);
            } else {
                return readPinsFromFlatFile(inputFile);
            }
        }
        return null;
    }

    private static Pin[] readPinsFromFlatFile(File inputFile) throws IOException {
        assert inputFile != null;
        int[] columnIndexes = null;
        int biggestIndex = 0;
        Vector<Pin> pins = new Vector<Pin>();

        if (inputFile.canRead()) {
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
                        String label = null;
                        if (columnIndexes[indexForLabel] >= 0 && strings.length > columnIndexes[indexForLabel]) {
                            label = strings[columnIndexes[indexForLabel]];
                        }
                        Pin pin = new Pin(name);
                        if (pin != null) {
                            if (label != null) {
                                pin.setLabel(label);
                            }
                            pin.setDescription(desc);
                            pin.setLongitude(lon);
                            pin.setLatitude(lat);
                            pins.add(pin);
                        }
                    } else {
                        throw new IOException("Invalid pin file format:\n" +
                                              "data row " + row + ": values for 'Name', 'Lon' and 'Lat' must be given.");   /*I18N*/
                    }
                }
            }
            file.close();

            return pins.toArray(new Pin[pins.size()]);
        }
        return null;
    }

    private static Pin[] createPinsFromXMLFile(File inputFile) throws IOException {
        assert inputFile != null;
        if (inputFile.canRead()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document w3cDocument = builder.parse(inputFile);
                Document document = new DOMBuilder().build(w3cDocument);
                final Element rootElement = document.getRootElement();
                final List children = rootElement.getChildren(DimapProductConstants.TAG_PIN);
                if (children != null) {
                    final Pin[] pins = new Pin[children.size()];
                    for (int i = 0; i < children.size(); i++) {
                        final Element element = (Element) children.get(i);
                        pins[i] = Pin.createPin(element);
                    }
                    return pins;
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
        }
        return null;
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

    @Override
    public JComponent createControl() {
        pinTable = new JTable();
        pinTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        pinTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pinTable.setRowHeight(34);
        pinTable.setRowSelectionAllowed(true);
        // IMPORTANT: We set ReorderingAllowed=false, because we export the
        // table model AS IS to a flat text file.
        pinTable.getTableHeader().setReorderingAllowed(false);

        final CellRenderer renderer = new CellRenderer();
        pinTable.setDefaultRenderer(PixelPos.class, renderer);
        pinTable.setDefaultRenderer(GeoPos.class, renderer);
        pinTable.setDefaultRenderer(String[].class, renderer);

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
                if (PinTableModel.DEFAULT_COLUMN_NAMES[2].equals(columnModel.getColumn(colIdx).getHeaderValue())) {
                    sortPins();
                }
            }
        });

        newButton = createButton("icons/New24.gif");
        newButton.setToolTipText("Create and add new pin."); /*I18N*/
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null) {
                    addNewPin(product, 0, 0, getWindowAncestor());
                    updateUIState();
                }
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setToolTipText("Copy and add existing pin."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Pin pin = getSelectedPinFromTable();
                if (pin != null) {
                    if (product == null) {
                        return;
                    }
                    Pin newPin = copyPin(pin, product, getWindowAncestor());
                    newPin = checkPinWithTheSameNameExists(newPin);
                    product.addPin(newPin);
                }
                updateUIState();
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setToolTipText("Edit selected pin."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editSelectedPin();
                updateUIState();
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setToolTipText("Remove selected pins."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final Pin pins[] = getSelectedPinsFromTable();
                final int selectedRow = pinTable.getSelectedRow();
                for (Pin pin : pins) {
                    final Product product = pin.getProduct();
                    if (product != null) {
                        product.removePin(pin);
                        int newSelectionIndex = selectedRow;
                        if (pinTable.getRowCount() == selectedRow) {
                            newSelectionIndex -= 1;
                        }
                        if (newSelectionIndex > -1) {
                            pinTable.setRowSelectionInterval(newSelectionIndex, newSelectionIndex);
                        }
                    }
                }
                updateUIState();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setToolTipText("Import the first pin from XML or text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(false);
                updateUIState();
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setToolTipText("Export the selected pins to XML file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportSelectedPins();
                updateUIState();
            }
        });

        importNButton = createButton("icons/ImportN24.gif");
        importNButton.setToolTipText("Import all pins from XML or text file."); /*I18N*/
        importNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(true);
                updateUIState();
            }
        });

        exportNButton = createButton("icons/ExportN24.gif");
        exportNButton.setToolTipText("Export all pins to XML file."); /*I18N*/
        exportNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null && product.getNumPins() > 0) {
                    exportPins(product.getPins());
                }
            }
        });

        filterButton = createButton("icons/Filter24.gif");
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
        exportTableButton.setToolTipText("Export pixel data at pin to flat text file."); /*I18N*/
        exportTableButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportPinDataTable();
                updateUIState();
            }
        });

        zoomToPinButton = createButton("icons/ZoomTo24.gif");
        zoomToPinButton.setToolTipText("Zoom to selected pin."); /*I18N*/
        zoomToPinButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomToSelectedPin(getSelectedPinFromTable());
                updateUIState();
            }
        });

        AbstractButton helpButton = createButton("icons/Help24.gif");

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

    private static class CellRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(isSelected);
            panel.setBackground(table.getSelectionBackground());
            if (value instanceof String) {
                panel.add(new JLabel((String) value));
            } else {
                final String[] array = new String[2];
                boolean isNameDesc = false;
                if (value instanceof String[]) {
                    String[] strings = (String[]) value;
                    array[0] = strings[0];
                    array[1] = strings[1];
                    isNameDesc = true;
                } else if (value instanceof PixelPos) {
                    PixelPos pos = (PixelPos) value;
                    array[0] = String.valueOf(pos.getX());
                    array[1] = String.valueOf(pos.getY());
                } else if (value instanceof GeoPos) {
                    GeoPos pos = (GeoPos) value;
                    array[0] = String.valueOf(pos.getLon());
                    array[1] = String.valueOf(pos.getLat());
                }
                final JLabel topLable = new JLabel(array[0]);
                if (isNameDesc) {
                    topLable.setFont(topLable.getFont().deriveFont(Font.BOLD));
                }
                final JLabel botomLabel = new JLabel(array[1]);
                panel.add(topLable, BorderLayout.NORTH);
                panel.add(botomLabel, BorderLayout.CENTER);
            }
            return panel;
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
                    pinTable.setToolTipText(geoPos.getLonString() + " / " + geoPos.getLatString());
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
                minWidth = 40;
                break;
            case 2:
                minWidth = 200;
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
                editSelectedPin();
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

    private static Pin copyPin(Pin pin, Product product, final Window parent) {
        return createNewPin(pin, true, product, parent);
    }

    static Pin editPin(Pin pin, Product product, final Window parent) {
        return createNewPin(pin, false, product, parent);
    }

    private static Pin createNewPin(Pin pin, boolean copy, Product product, Window parent) {
        if (pin != null) {
            if (product == null) {
                product = pin.getProduct();
            }
            final PinDialog pinDialog = new PinDialog(parent, product);
            if (copy) {
                pinDialog.getJDialog().setTitle("Copy Pin"); /*I18N*/
                pinDialog.setName(pin.getName() + "_copy");
            } else {
                pinDialog.getJDialog().setTitle("Edit Pin"); /*I18N*/
                pinDialog.setName(pin.getName());
            }
            pinDialog.setLabel(pin.getLabel());
            pinDialog.setUsePixelPos(false);
            pinDialog.setLat(pin.getLatitude());
            pinDialog.setLon(pin.getLongitude());
            pinDialog.setUsePixelPos(true);
            String description = pin.getDescription();
            pinDialog.setDescription(description != null ? description : "");
            pinDialog.setPinSymbol(pin.getSymbol());
            return createPin(pinDialog);
        }
        return null;
    }

    private static Pin createPin(final PinDialog pinDialog) {
        if (pinDialog.show() == PinDialog.ID_OK) {
            return new Pin(pinDialog.getName(), pinDialog.getLabel(), pinDialog.getDescription(),
                           pinDialog.getLat(), pinDialog.getLon(), pinDialog.getPinSymbol());
        }
        return null;
    }

    private static Pin addNewPin(Product product, int pixelX, int pixelY, final Window parent) {
        Guardian.assertNotNull("product", product);
        PinDialog pinDialog = new PinDialog(parent, product);
        final String[] uniquePinNameAndLabel = createUniquePinNameAndLabel(product, pixelX, pixelY);
        pinDialog.setName(uniquePinNameAndLabel[0]);
        pinDialog.setLabel(uniquePinNameAndLabel[1]);
        pinDialog.setUsePixelPos(true);
        pinDialog.setPixelX(pixelX + 1);
        pinDialog.setPixelX(pixelX);
        pinDialog.setPixelY(pixelY + 1);
        pinDialog.setPixelY(pixelY);
        Pin pin = createPin(pinDialog);
        if (pin != null) {
            boolean canceled = false;
            while (product.containsPin(pin.getName())) {
                VisatApp.getApp().showInfoDialog("A pin with the name '" + pin.getName() + "' already exists.\n" +
                                                 "Please enter another name.", null);/*I18N*/
                pin = editPin(pin, product, parent);
                if (pin == null) {
                    canceled = true;
                    break;
                }
            }
            if (!canceled) {
                product.addPin(pin);
            }
        }
        return pin;
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
        return "Pin " + pinNumber + " @ " + pixelX + ", " + pixelY;
    }

    /**
     * Returns the geographical position which is equivalent to the given pixel position. Returns <code>null</code> if
     * the given pixel position is outside of the given product or the geocoding cannot get geographical positions.
     *
     * @param product  must be given and must contain a geocoding.
     * @param pixelPos must be given.
     *
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
