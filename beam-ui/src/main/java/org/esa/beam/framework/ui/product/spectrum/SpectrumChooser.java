package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;
import com.jidesoft.grid.JideTable;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TreeLikeHierarchicalPanel;
import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.ArrayUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectrumChooser extends ModalDialog {

    private static final int spectrumSelectedIndex = 0;
    private static final int spectrumNameIndex = 1;
    private static final int spectrumStrokeIndex = 2;
    private static final int spectrumShapeIndex = 3;

    private static final int bandSelectedIndex = 0;
    private static final int bandNameIndex = 1;
    private static final int bandDescriptionIndex = 2;
    private static final int bandWavelengthIndex = 3;
    private static final int bandBandwidthIndex = 4;
    private static List<SpectrumInDisplay> allSpectra;

    private static SpectrumTableModel spectrumTableModel;
    private static HierarchicalTable spectraTable;


    public SpectrumChooser(Window parent, List<SpectrumInDisplay> allSpectra, List<SpectrumInDisplay> selectedSpectra,
                           String helpID) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL, helpID);
        if (allSpectra != null) {
            SpectrumChooser.allSpectra = allSpectra;
        } else {
            SpectrumChooser.allSpectra = new ArrayList<SpectrumInDisplay>();
        }
        initUI();
        setSelectedSpectra(selectedSpectra);
    }

    private void setSelectedSpectra(List<SpectrumInDisplay> selectedSpectra) {
        for (int i = 0; i < allSpectra.size(); i++) {
            SpectrumInDisplay spectrumInDisplay = allSpectra.get(i);
            spectrumTableModel.setValueAt(selectedSpectra.contains(spectrumInDisplay), i, spectrumSelectedIndex);
        }
    }

    private void initUI() {
        final JPanel content = new JPanel(new BorderLayout());
        initSpectraTable();
        JScrollPane spectraScrollPane = new JScrollPane(spectraTable);
        final Dimension preferredSize = spectraTable.getPreferredSize();
        spectraScrollPane.setPreferredSize(new Dimension(Math.max(preferredSize.width + 20, 550),
                                                         Math.max(preferredSize.height + 10, 200)));
        content.add(spectraScrollPane, BorderLayout.CENTER);
        setContent(content);
    }

    @SuppressWarnings("unchecked")
    private void initSpectraTable() {
        spectrumTableModel = new SpectrumTableModel();
        for (SpectrumInDisplay spectrum : allSpectra) {
            spectrumTableModel.addRow(spectrum);
        }
        spectraTable = new HierarchicalTable(spectrumTableModel);
        spectraTable.setComponentFactory(new SpectrumTableComponentFactory());
        AutoFilterTableHeader header = new AutoFilterTableHeader(spectraTable);
        spectraTable.setTableHeader(header);
        spectraTable.setRowHeight(21);

        final TableColumn selectionColumn = spectraTable.getColumnModel().getColumn(spectrumSelectedIndex);
        final JCheckBox selectionCheckBox = new JCheckBox();
        selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
        BooleanRenderer booleanRenderer = new BooleanRenderer();
        selectionColumn.setCellRenderer(booleanRenderer);
        selectionColumn.setMinWidth(38);
        selectionColumn.setMaxWidth(38);

        final TableColumn strokeColumn = spectraTable.getColumnModel().getColumn(spectrumStrokeIndex);
        JComboBox strokeComboBox = new JComboBox(SpectrumConstants.strokeIcons);
        ImageIconComboBoxRenderer strokeComboBoxRenderer = new ImageIconComboBoxRenderer();
        strokeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        strokeComboBox.setRenderer(strokeComboBoxRenderer);
        strokeColumn.setCellEditor(new DefaultCellEditor(strokeComboBox));

        final TableColumn shapeColumn = spectraTable.getColumnModel().getColumn(spectrumShapeIndex);
        JComboBox shapeComboBox = new JComboBox(SpectrumConstants.shapeIcons);
        ImageIconComboBoxRenderer shapeComboBoxRenderer = new ImageIconComboBoxRenderer();
        shapeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        shapeComboBox.setRenderer(shapeComboBoxRenderer);
        shapeColumn.setCellEditor(new DefaultCellEditor(shapeComboBox));

    }

    public List<SpectrumInDisplay> getSelectedSpectra() {
        List<SpectrumInDisplay> selectedSpectra = new ArrayList<SpectrumInDisplay>();
        for (int i = 0; i < spectrumTableModel.getRowCount(); i++) {
            if ((Boolean) spectrumTableModel.getValueAt(i, spectrumSelectedIndex)) {
                selectedSpectra.add(allSpectra.get(i));
            }
        }
        return selectedSpectra;
    }

    /*
     * Used for testing UI
     */
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        String name = "Radiances";
        String description = "Radiance bands";
        int numBands = 5;
        Band[] bands = new Band[numBands];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = createBand(i);
        }
        SpectrumInDisplay spectrum = new SpectrumInDisplay(name, description, bands);
        final List<SpectrumInDisplay> spectra = new ArrayList<SpectrumInDisplay>();
        spectra.add(spectrum);
        final JFrame frame = new JFrame();
        frame.setSize(new Dimension(100, 100));
        JButton button = new JButton("Choose Spectrum");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpectrumChooser chooser = new SpectrumChooser(frame, spectra, spectra, "");
                chooser.show();
            }
        });
        frame.add(button);
        frame.setVisible(true);
    }

    /*
     * Used for testing UI
     */
    static private Band createBand(int index) {
        final Band band = new Band("Radiance_" + (index + 1), ProductData.TYPE_INT16, 100, 100);
        band.setDescription("Radiance for band " + (index + 1));
        band.setSpectralWavelength((float) Math.random());
        band.setSpectralBandwidth((float) Math.random());
        return band;
    }

    static class SpectrumTableModel extends DefaultTableModel implements HierarchicalTableModel {

        final static String[] spectraColumns = new String[]{"", "Spectrum name", "Line style", "Symbol"};
        private final Class[] COLUMN_CLASSES = {
                Boolean.class,
                String.class,
                ImageIcon.class,
                ImageIcon.class,
        };
        String[] bandColumns = new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)"};
        private List<Boolean> spectraSelected;
        private Map<SpectrumInDisplay, BandTableModel> spectrumToModel;

        public SpectrumTableModel() {
            super(spectraColumns, 0);
            spectraSelected = new ArrayList<Boolean>();
            spectrumToModel = new HashMap<SpectrumInDisplay, BandTableModel>();
        }

        public void addRow(SpectrumInDisplay spectrum) {
            final ImageIcon strokeIcon = SpectrumConstants.strokeIcons[spectraSelected.size() % SpectrumConstants.strokeIcons.length];
            final ImageIcon shapeIcon = SpectrumConstants.shapeIcons[spectraSelected.size() % SpectrumConstants.shapeIcons.length];
            spectraSelected.add(Boolean.TRUE);
            spectrum.setLineStyle(SpectrumConstants.strokes[ArrayUtils.getElementIndex(strokeIcon, SpectrumConstants.strokeIcons)]);
            spectrum.setSymbol(SpectrumConstants.shapes[ArrayUtils.getElementIndex(shapeIcon, SpectrumConstants.shapeIcons)]);
            super.addRow(new Object[]{Boolean.TRUE, spectrum.getName(), strokeIcon, shapeIcon});
        }

        @Override
        public Object getChildValueAt(final int row) {
            SpectrumInDisplay spectrum = allSpectra.get(row);
            if (spectrumToModel.containsKey(spectrum)) {
                return spectrumToModel.get(spectrum);
            }
            final Band[] spectralBands = spectrum.getSpectralBands();
            Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
            for (int i = 0; i < spectralBands.length; i++) {
                Band spectralBand = spectralBands[i];
                spectrumData[i][bandSelectedIndex] = spectrum.isBandSelected(i);
                spectrumData[i][bandNameIndex] = spectralBand.getName();
                spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
            }
            final BandTableModel bandTableModel = new BandTableModel(spectrumData, bandColumns);
            spectrumToModel.put(spectrum, bandTableModel);
            bandTableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    e.getSource();
                    if (e.getColumn() == bandSelectedIndex) {
                        final SpectrumInDisplay spectrum = allSpectra.get(row);
                        spectrum.setBandSelected(e.getFirstRow(), (Boolean) bandTableModel.getValueAt(e.getFirstRow(), e.getColumn()));
                    }
                }
            });
            return bandTableModel;
        }

        @Override
        public boolean hasChild(int row) {
            return true;
        }

        @Override
        public boolean isHierarchical(int row) {
            return true;
        }

        @Override
        public boolean isExpandable(int row) {
            return true;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return COLUMN_CLASSES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column != spectrumNameIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == spectrumSelectedIndex) {
                spectraSelected.set(row, (Boolean) aValue);
                if (spectraTable.getChildComponentAt(row) != null) {
                    final JideTable table = (JideTable) spectraTable.getChildComponentAt(row).getAccessibleContext().getAccessibleChild(0).getAccessibleContext().getAccessibleChild(0).getAccessibleContext().getAccessibleChild(0);
                    if (table != null) {
                        table.setEnabled((Boolean) aValue);
                    }
                }
                fireTableCellUpdated(row, column);
            } else if (column == spectrumStrokeIndex) {
                allSpectra.get(row).setLineStyle(SpectrumConstants.strokes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.strokeIcons)]);
                fireTableCellUpdated(row, column);
            } else if (column == spectrumShapeIndex) {
                allSpectra.get(row).setSymbol(SpectrumConstants.shapes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.shapeIcons)]);
                fireTableCellUpdated(row, column);
            }
            super.setValueAt(aValue, row, column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == spectrumSelectedIndex) {
                return spectraSelected.get(row);
            }
            return super.getValueAt(row, column);
        }
    }

    static class BandTableModel extends DefaultTableModel {

        public BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == bandSelectedIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == bandSelectedIndex) {
                this.getRowCount();
            }
            fireTableCellUpdated(row, column);
            super.setValueAt(aValue, row, column);
        }
    }

    class SpectrumTableComponentFactory implements HierarchicalTableComponentFactory {

        @Override
        public Component createChildComponent(HierarchicalTable table, Object value, int row) {
            if (value == null) {
                return new JPanel();
            }
            TableModel model = (TableModel) value;
            SortableTable bandsTable = new SortableTable(model);
            bandsTable.setEnabled((Boolean) spectraTable.getValueAt(row, spectrumSelectedIndex));
            AutoFilterTableHeader bandsHeader = new AutoFilterTableHeader(bandsTable);
            bandsTable.setTableHeader(bandsHeader);

            final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(bandSelectedIndex);
            final JCheckBox selectionCheckBox = new JCheckBox();
            selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
            selectionColumn.setMinWidth(20);
            selectionColumn.setMaxWidth(20);
            BooleanRenderer booleanRenderer = new BooleanRenderer();
            selectionColumn.setCellRenderer(booleanRenderer);

            final TableColumn wavelengthColumn = bandsTable.getColumnModel().getColumn(bandWavelengthIndex);
            wavelengthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(bandBandwidthIndex);
            bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            final JScrollPane jScrollPane = new SpectrumScrollPane(bandsTable);
            return new TreeLikeHierarchicalPanel(jScrollPane);
        }

        @Override
        public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
            // do nothing
        }
    }

    private static class SpectrumScrollPane extends JScrollPane {

        public SpectrumScrollPane(JTable table) {
            super(table);
        }

        @Override
        public Dimension getPreferredSize() {
            getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
            return super.getPreferredSize();
        }
    }

    class ImageIconComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ImageIconComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setIcon((ImageIcon) value);
            return this;
        }

    }

    class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        public BooleanRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            boolean selected = (Boolean) value;
            this.setSelected(selected);
            return this;
        }
    }

}
