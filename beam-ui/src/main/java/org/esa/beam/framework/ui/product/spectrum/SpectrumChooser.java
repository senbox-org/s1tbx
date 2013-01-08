package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.combobox.ColorExComboBox;
import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TransposeTableModel;
import com.jidesoft.grid.TreeLikeHierarchicalPanel;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SpectrumChooser extends ModalDialog {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font _SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font _SMALL_ITALIC_FONT = _SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    public static final int spectrumSelectedIndex = 0;
    public static final int spectrumNameIndex = 1;
    public static final int spectrumDescriptionIndex = 2;
    public static final int spectrumPatternIndex = 3;
    public static final int spectrumColorIndex = 4;

    public static final int bandSelectedIndex = 0;
    public static final int bandNameIndex = 1;
    public static final int bandDescriptionIndex = 2;
    public static final int bandWavelengthIndex = 3;
    public static final int bandBandwidthIndex = 4;
    private final Band[] availableSpectralBands;

    public List<Spectrum> spectra;
    private SpectrumTableModel spectrumTableModel;
    private HierarchicalTable spectraTable;


    public SpectrumChooser(Window parent, List<Spectrum> definedSpectra, Band[] availableSpectralBands, String helpID) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL, helpID);
        if (definedSpectra != null) {
            spectra = definedSpectra;
        } else {
            spectra = new ArrayList<Spectrum>();
        }
        this.availableSpectralBands = availableSpectralBands;
        initUI();
    }

    private void initUI() {
        final JPanel upperButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        final AbstractButton copyButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Copy24.gif"), false);
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] selectedRows = spectraTable.getSelectedRows();
                if (selectedRows.length > 0) {
                    for (int selectedRow : selectedRows) {
                        spectrumTableModel.addRow(spectrumTableModel.getSpectrum(selectedRow));
                    }
                }
            }
        });
        upperButtonPanel.add(copyButton);

        final AbstractButton editButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Edit24.gif"), false);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] selectedRows = spectraTable.getSelectedRows();
                if (selectedRows.length > 0) {
                    Spectrum spectrumToBeEdited = spectrumTableModel.getSpectrum(selectedRows[0]);
                    SpectrumEditor editor = new SpectrumEditor(getParent(), "Edit Spectrum", "", availableSpectralBands,
                                                               spectrumToBeEdited, spectrumTableModel.getSpectrumNames());
                    if (editor.show() == ModalDialog.ID_OK) {
                        spectrumTableModel.updateRow(selectedRows[0], editor.getSpectrum());
                    }
                }
            }
        });
        upperButtonPanel.add(editButton);

        final AbstractButton deleteSpectrumButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"), false);
        deleteSpectrumButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] selectedRows = spectraTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    spectrumTableModel.removeRow(selectedRow);
                }
            }
        });
        upperButtonPanel.add(deleteSpectrumButton);

        final AbstractButton createSpectrumButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"), false);
        createSpectrumButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpectrumEditor editor = new SpectrumEditor(getParent(), "Add Spectrum", "", availableSpectralBands, spectrumTableModel.getSpectrumNames());
                if (editor.show() == ModalDialog.ID_OK) {
                    final Spectrum createdSpectrum = editor.getSpectrum();
                    spectrumTableModel.addRow(createdSpectrum);
                }
            }
        });
        upperButtonPanel.add(createSpectrumButton);

        final JPanel content = new JPanel(new BorderLayout());
        initSpectraTable();
        JScrollPane spectraScrollPane = new JScrollPane(spectraTable);
        final Dimension preferredSize = spectraTable.getPreferredSize();
        spectraScrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                                                         Math.min(preferredSize.height + 10, 300)));
        content.add(upperButtonPanel, BorderLayout.NORTH);
        content.add(spectraScrollPane, BorderLayout.CENTER);
        setContent(content);
    }

    private void initSpectraTable() {
        spectrumTableModel = new SpectrumTableModel();
        for (Spectrum spectrum : spectra) {
            spectrumTableModel.addRow(spectrum);
        }
        spectraTable = new HierarchicalTable(spectrumTableModel);
        spectraTable.setComponentFactory(new SpectrumTableComponentFactory());
        AutoFilterTableHeader header = new AutoFilterTableHeader(spectraTable);
        spectraTable.setTableHeader(header);

        final TableColumn selectionColumn = spectraTable.getColumnModel().getColumn(spectrumSelectedIndex);
        final JCheckBox selectionCheckBox = new JCheckBox();
        selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
        TableCellRenderer renderer = new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return selectionCheckBox;
            }
        };
        selectionColumn.setCellRenderer(renderer);

        final TableColumn colorColumn = spectraTable.getColumnModel().getColumn(spectrumColorIndex);
        colorColumn.setCellRenderer(new ColorCR());
        colorColumn.setCellEditor(new ColorCE());
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
        Spectrum spectrum = new Spectrum(name, description, bands);
        final Band[] allBands = new Band[numBands * 2];
        for (int i = 0; i < allBands.length; i++) {
            if (i < numBands) {
                allBands[i] = bands[i];
            } else {
                allBands[i] = createBand(i);
            }
        }
        final List<Spectrum> spectra = new ArrayList<Spectrum>();
        spectra.add(spectrum);
        final JFrame frame = new JFrame();
        frame.setSize(new Dimension(100, 100));
        JButton button = new JButton("Choose Spectrum");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpectrumChooser chooser = new SpectrumChooser(frame, spectra, allBands, "");
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

        static String[] spectraColumns = new String[]{"", "Spectrum name", "Description", "Name pattern", "Color"};
        private static final Class[] COLUMN_CLASSES = {
                Boolean.class,
                String.class,
                String.class,
                String.class,
                String.class,
        };
        static String[] bandColumns = new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)"};
        private List<Spectrum> spectra;


        public SpectrumTableModel() {
            super(spectraColumns, 0);
            spectra = new ArrayList<Spectrum>();
        }

        public void addRow(Spectrum spectrum) {
            spectra.add(spectrum);
            super.addRow(new Object[]{Boolean.TRUE, spectrum.getName(), spectrum.getDescription(), spectrum.getNamePattern(), Color.RED});
        }

        public void updateRow(int row, Spectrum spectrum) {
            final Object savedColor = getValueAt(row, spectrumColorIndex);
            removeRow(row);
            spectra.add(row, spectrum);
            insertRow(row, new Object[]{Boolean.TRUE, spectrum.getName(), spectrum.getDescription(), spectrum.getNamePattern(), savedColor});
        }

        public Spectrum getSpectrum(int row) {
            return spectra.get(row);
        }

        public String[] getSpectrumNames() {
            String[] spectrumNames = new String[spectra.size()];
            for (int i = 0; i < spectrumNames.length; i++) {
                spectrumNames[i] = spectra.get(i).getName();
            }
            return spectrumNames;
        }

        @Override
        public void removeRow(int row) {
            spectra.remove(row);
            super.removeRow(row);
        }

        @Override
        public Object getChildValueAt(int row) {
            Spectrum spectrum = spectra.get(row);
            final Band[] spectralBands = spectrum.getSpectralBands();
            Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
            for (int i = 0; i < spectralBands.length; i++) {
                Band spectralBand = spectralBands[i];
                spectrumData[i][bandSelectedIndex] = Boolean.TRUE;
                spectrumData[i][bandNameIndex] = spectralBand.getName();
                spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
            }
            return new DefaultTableModel(spectrumData, bandColumns);
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

    }

    class SpectrumTableComponentFactory implements HierarchicalTableComponentFactory {

        @Override
        public Component createChildComponent(HierarchicalTable table, Object value, int row) {
            if (value == null) {
                return new JPanel();
            }
            TableModel model = (TableModel) value;
            SortableTable bandsTable = new SortableTable(model);
            AutoFilterTableHeader bandsHeader = new AutoFilterTableHeader(bandsTable);
            bandsTable.setTableHeader(bandsHeader);
            final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(bandSelectedIndex);
            final JCheckBox selectionCheckBox = new JCheckBox();
            selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
            TableCellRenderer renderer = new TableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    return selectionCheckBox;
                }
            };
            selectionColumn.setCellRenderer(renderer);
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

    /*
     * Copied from MaskTable.java
     */
    private static class ColorCE extends ColorCellEditor {

        @Override
        protected ColorExComboBox createColorComboBox() {
            ColorExComboBox comboBox = super.createColorComboBox();
            comboBox.setColorValueVisible(true);
            comboBox.setColorIconVisible(true);
            comboBox.setInvalidValueAllowed(false);
            comboBox.setAllowDefaultColor(true);
            comboBox.setAllowMoreColors(true);
            return comboBox;
        }
    }

    /*
     * Copied from MaskTable.java
     */
    private static class ColorCR extends ColorCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setColorIconVisible(true);
            setColorValueVisible(true);
            setCrossBackGroundStyle(true);
            return super.getTableCellRendererComponent(table, value,
                                                       isSelected, hasFocus, row, column);
        }
    }

    /*
     * From previous attempts
     */
    private JPanel createSpectrumPanel() {
        String name = "Radiances";
        String description = "Radiance bands";
        int numBands = 5;
        Band[] bands = new Band[numBands];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = createBand(i);
        }
        Color color = Color.RED;
        return createSpectrumPanel(name, description, bands, color);
    }

    /*
     * From previous attempts
     */
    private JPanel createSpectrumPanel(String name, String description, Band[] bands, Color color) {
        final JPanel spectrumPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        ColorExComboBox colorComboBox = new ColorExComboBox();
        colorComboBox.setSelectedColor(color);
        colorComboBox.setColorValueVisible(true);
        GridBagUtils.addToPanel(spectrumPanel, createCheckBoxTree(name, bands), gbc, "weightx=1.0,weighty=1.0,gridx=0,gridy=0,gridwidth=1,gridheight=1,fill=HORIZONTAL,anchor=NORTH");
        GridBagUtils.addToPanel(spectrumPanel, new JLabel(description), gbc, "gridx=1");
        GridBagUtils.addToPanel(spectrumPanel, colorComboBox, gbc, "weightx=0.0,gridx=2");
        GridBagUtils.addVerticalFiller(spectrumPanel, gbc);
//        spectrumPanel.setBorder(new LineBorder(Color.BLUE));
        return spectrumPanel;
    }

    /*
     * From previous attempts
     */
    private CheckBoxTree createCheckBoxTree(String name, Band[] bands) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(name);
        TreeModel treeModel = new DefaultTreeModel(root);
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            root.add(new DefaultMutableTreeNode(band.getName()));
        }
        CheckBoxTree checkBoxTree = new CheckBoxTree(treeModel);
        checkBoxTree.setShowsRootHandles(true);
        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) checkBoxTree.getActualCellRenderer();
        renderer.setFont(_SMALL_PLAIN_FONT);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setTextSelectionColor(Color.BLACK);
        Color color = new Color(240, 240, 240);
        checkBoxTree.setBackground(color);
        renderer.setBackgroundSelectionColor(color);
        renderer.setBackgroundNonSelectionColor(color);
        checkBoxTree.collapsePath(new TreePath(root));
        return checkBoxTree;
    }

    /*
     * From previous attempts
     */
    private JPanel createSpectrumPanel2() {
        final JPanel spectrumPanel = GridBagUtils.createPanel();
        Vector<String> columnNames = new Vector<String>();
        columnNames.add("Name");
        columnNames.add("Description");
        columnNames.add("Color");
        TableModel model = new DefaultTableModel(columnNames, 4);
        final HierarchicalTableModel tableModel = new TransposeTableModel(model);
        final HierarchicalTable table = new HierarchicalTable();
        spectrumPanel.add(table);
        return spectrumPanel;
    }


}
