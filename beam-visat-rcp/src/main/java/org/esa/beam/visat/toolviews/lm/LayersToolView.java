package org.esa.beam.visat.toolviews.lm;

import com.jidesoft.combobox.FileChooserPanel;
import com.jidesoft.combobox.FileNameChooserComboBox;
import com.jidesoft.combobox.ListComboBox;
import com.jidesoft.combobox.PopupPanel;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.converter.EnumConverter;
import com.jidesoft.converter.ObjectConverterManager;
import com.jidesoft.grid.*;
import com.jidesoft.icons.IconsFactory;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideSwingUtilities;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.MaskFormatter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * The tool window which displays the layers of the current image view.
 */
public class LayersToolView extends AbstractToolView {

    public static final String ID = LayersToolView.class.getName();
    private VisatApp visatApp;

    public LayersToolView() {
        this.visatApp = VisatApp.getApp();
    }

    @Override
    public JComponent createControl() {
        final JScrollPane layerScrollPane = new JideScrollPane(createMockUpTree());
        layerScrollPane.setPreferredSize(new Dimension(320, 480));
        layerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        layerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        layerScrollPane.setBorder(null);
        layerScrollPane.setViewportBorder(null);

        final JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.addPane(layerScrollPane);
        splitPane.addPane(createStylePane());

        return splitPane;
    }

    private CheckBoxTree createMockUpTree() {
        final CheckBoxTree tree = new CheckBoxTree(createMockUpNodes());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getActualCellRenderer();
        renderer.setLeafIcon(IconsFactory.getImageIcon(LayersToolView.class, "/org/esa/beam/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(IconsFactory.getImageIcon(LayersToolView.class, "/org/esa/beam/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(IconsFactory.getImageIcon(LayersToolView.class, "/org/esa/beam/resources/images/icons/RsGroupOpen16.gif"));
        return tree;
    }

    private DefaultMutableTreeNode createMockUpNodes() {
        final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        rootNode.add(new DefaultMutableTreeNode("Background"));
        rootNode.add(createRasterDatasets());
        rootNode.add(createProductOverlays());
        rootNode.add(createGeographicalOverlays());
        rootNode.add(createImportedLayers());
        return rootNode;
    }

    private DefaultMutableTreeNode createImportedLayers() {
        final DefaultMutableTreeNode importedLayers = new DefaultMutableTreeNode("Imported layers");
        importedLayers.add(new DefaultMutableTreeNode("bottomphotos.shp"));
        importedLayers.add(new DefaultMutableTreeNode("bottomtype.shp"));
        importedLayers.add(new DefaultMutableTreeNode("sedgrabs.shp"));
        importedLayers.add(new DefaultMutableTreeNode("surveylines_sss.shp"));
        return importedLayers;
    }

    private DefaultMutableTreeNode createGeographicalOverlays() {
        final DefaultMutableTreeNode geographicalOverlays = new DefaultMutableTreeNode("Geographical overlays");
        geographicalOverlays.add(new DefaultMutableTreeNode("Cities"));
        geographicalOverlays.add(new DefaultMutableTreeNode("Rivers"));
        geographicalOverlays.add(new DefaultMutableTreeNode("Political borders"));
        geographicalOverlays.add(new DefaultMutableTreeNode("Coastlines"));
        geographicalOverlays.add(new DefaultMutableTreeNode("Streets"));
        return geographicalOverlays;
    }

    private DefaultMutableTreeNode createRasterDatasets() {
        final DefaultMutableTreeNode rasterDatasets = new DefaultMutableTreeNode("Raster datasets");

        final DefaultMutableTreeNode boavi = new DefaultMutableTreeNode("boavi");
        boavi.add(new DefaultMutableTreeNode("No-data mask"));
        boavi.add(new DefaultMutableTreeNode("ROI"));
        rasterDatasets.add(boavi);

        final DefaultMutableTreeNode yellowSubst = new DefaultMutableTreeNode("yellow_subst");
        yellowSubst.add(new DefaultMutableTreeNode("No-data mask"));
        yellowSubst.add(new DefaultMutableTreeNode("ROI"));
        rasterDatasets.add(yellowSubst);

        final DefaultMutableTreeNode cloudTopPress = new DefaultMutableTreeNode("cloud_top_press");
        cloudTopPress.add(new DefaultMutableTreeNode("No-data mask"));
        cloudTopPress.add(new DefaultMutableTreeNode("ROI"));
        rasterDatasets.add(cloudTopPress);
        return rasterDatasets;
    }

    private DefaultMutableTreeNode createProductOverlays() {
        final DefaultMutableTreeNode productOverlays = new DefaultMutableTreeNode("Product overlays");

        final DefaultMutableTreeNode bitmaskOverlays = new DefaultMutableTreeNode("Bitmasks");
        bitmaskOverlays.add(new DefaultMutableTreeNode("INVALID"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("LAND"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("WATER"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("COASTLINE"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("COSMETIC"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("DUPLICATED"));
        bitmaskOverlays.add(new DefaultMutableTreeNode("SUSPICIOUS"));
        productOverlays.add(bitmaskOverlays);

        productOverlays.add(new DefaultMutableTreeNode("Pins"));
        productOverlays.add(new DefaultMutableTreeNode("Ground control points"));
        productOverlays.add(new DefaultMutableTreeNode("Graticule"));
        productOverlays.add(new DefaultMutableTreeNode("User-defined shapes"));
        return productOverlays;
    }


    public Component createStylePane() {
        JPanel panel = new JPanel(new BorderLayout());
        PropertyTable table = createTable();
        PropertyPane propertyPane = new PropertyPane(table);
        panel.add(propertyPane, BorderLayout.CENTER);
        return panel;
    }

    // create property table
    private PropertyTable createTable() {
        ObjectConverterManager.initDefaultConverter();
        CellEditorManager.initDefaultEditor();
        // before use MaskFormatter, you must register converter context and editor context
        ConverterContext ssnConverterContext = null;
        MaskFormatter mask = null;
        try {
            mask = new MaskFormatter("###-##-####");
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        mask.setValueContainsLiteralCharacters(false);
        ssnConverterContext = new ConverterContext("SSN", mask);
        EditorContext ssnEditorContext = new EditorContext("SSN");
        CellEditorManager.registerEditor(String.class, new CellEditorFactory() {
            public CellEditor create() {
                return new FormattedTextFieldCellEditor(String.class);
            }
        }, ssnEditorContext);
        CellEditorManager.registerEditor(String.class, new CellEditorFactory() {
            public CellEditor create() {
                return new FileNameCellEditor() {
                    @Override
                    protected FileNameChooserComboBox createFileNameChooserComboBox() {
                        return new FileNameChooserComboBox() {
                            @Override
                            public PopupPanel createPopupComponent() {
                                FileChooserPanel panel = new FileChooserPanel("" + getSelectedItem()) {
                                    @Override
                                    protected JFileChooser createFileChooser() {
                                        JFileChooser fileChooser = new JFileChooser(getCurrentDirectoryPath());
                                        fileChooser.setFileFilter(new FileFilter() {
                                            @Override
                                            public boolean accept(File f) {
                                                return f.isDirectory() || f.getName().endsWith(".java");
                                            }

                                            @Override
                                            public String getDescription() {
                                                return "Java files (*.java)";
                                            }
                                        });
                                        try {
                                            fileChooser.setSelectedFile(new File(getCurrentDirectoryPath()));
                                        }
                                        catch (Exception e) {
                                            // ignore
                                        }
                                        return fileChooser;
                                    }
                                };
                                panel.setTitle("Choose a file");
                                return panel;
                            }
                        };
                    }
                };
            }
        }, FileNameCellEditor.CONTEXT);

        CellEditorManager.registerEditor(String.class, new CellEditorFactory() {
            public CellEditor create() {
                return new FontNameCellEditor() {
                    @Override
                    protected ListComboBox createListComboBox(ComboBoxModel model, Class type) {
                        ListComboBox listComboBox = super.createListComboBox(model, type);
                        listComboBox.setEditable(false);
                        return listComboBox;
                    }
                };
            }
        }, new EditorContext("FontName-Noneditable"));

        final EnumConverter priorityConverter = new EnumConverter("Priority", Integer.class,
                                                                  new Object[]{
                                                                          new Integer(0),
                                                                          new Integer(1),
                                                                          new Integer(2),
                                                                          new Integer(3)
                                                                  },
                                                                  new String[]{
                                                                          "Low",
                                                                          "Normal",
                                                                          "High",
                                                                          "Urgent"},
                                                                  new Integer(1));
        ObjectConverterManager.registerConverter(priorityConverter.getType(), priorityConverter, priorityConverter.getContext());
        EnumCellRenderer priorityRenderer = new EnumCellRenderer(priorityConverter);
        CellRendererManager.registerRenderer(priorityConverter.getType(), priorityRenderer, priorityRenderer.getContext());
        CellEditorManager.registerEditor(priorityConverter.getType(), new CellEditorFactory() {
            public CellEditor create() {
                return new EnumCellEditor(priorityConverter);
            }
        }, new EditorContext(priorityConverter.getName()));

        CellEditorManager.registerEditor(String.class, new CellEditorFactory() {
            public CellEditor create() {
                return new MultilineStringCellEditor();
            }
        }, MultilineStringCellEditor.CONTEXT);
        TableCellRenderer multilineCellRenderer = new MultilineStringCellRenderer();
        CellRendererManager.registerRenderer(String.class, multilineCellRenderer, MultilineStringCellEditor.CONTEXT);

        CellEditorManager.registerEditor(String[].class, new CellEditorFactory() {
            public CellEditor create() {
                return new CheckBoxListComboBoxCellEditor(new String[]{"A", "B", "C", "D", "E"}, String[].class);
            }
        }, new EditorContext("ABCDE"));

        ArrayList list = new ArrayList();

        SampleProperty property = null;

        property = new SampleProperty("Transparency", "Sets the layer transparency. Enter values between 0 and 1.", Double.class, "Layer Style");
        list.add(property);

        property = new SampleProperty("Fill colour", "The row is intended to show how to create a cell to input colour in RGB format.", Color.class, "Layer Style");
        list.add(property);

        property = new SampleProperty("Line colour", "The row is intended to show how to create a cell to input colour in HEX format.", Color.class, "Layer Style");
        list.add(property);

        property = new SampleProperty("Font", "The row is intended to show how to create a cell to choose a font", Font.class);
        property.setCategory("Layer Style");
        list.add(property);

        PropertyTableModel model = new StripePropertyTableModel(list);
        PropertyTable table = new PropertyTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(400, 600));
        table.expandFirstLevel();
// Support for tree expansion listener
//        table.addTreeExpansionListener(new TreeExpansionListener() {
//            public void treeCollapsed(TreeExpansionEvent event) {
//                System.out.println("-" + event.getPath());
//            }
//
//            public void treeExpanded(TreeExpansionEvent event) {
//                System.out.println("+" + event.getPath());
//            }
//        });
        PropertyTableSearchable searchable = new PropertyTableSearchable(table);
        searchable.setRecursive(true);
        return table;
    }

    class StripePropertyTableModel extends PropertyTableModel implements StyleModel {
        public StripePropertyTableModel() {
        }

        public StripePropertyTableModel(List properties) {
            super(properties);
        }

        public StripePropertyTableModel(List properties, String[] categories) {
            super(properties, categories);
        }

        public StripePropertyTableModel(List properties, int categoryOrder) {
            super(properties, categoryOrder);
        }

        protected final Color BACKGROUND1 = new Color(253, 253, 244);
        protected final Color BACKGROUND2 = new Color(255, 255, 255);

        CellStyle cellStyle = new CellStyle();

        public CellStyle getCellStyleAt(int rowIndex, int columnIndex) {
            cellStyle.setHorizontalAlignment(-1);
            cellStyle.setForeground(Color.BLACK);
            if (rowIndex % 2 == 0) {
                cellStyle.setBackground(BACKGROUND1);
            } else {
                cellStyle.setBackground(BACKGROUND2);
            }
            return cellStyle;
        }

        public boolean isCellStyleOn() {
            return true;
        }
    }

    static HashMap map = new HashMap();

    static {
        map.put("Bounds", new Rectangle(0, 0, 100, 200));
        map.put("Dimension", new Dimension(800, 600));
        map.put("Line colour", new Color(51, 51, 152));
        map.put("Fill colour", new Color(255, 255, 0));
        map.put("File Name", "C:\\Documents and Settings\\David Qiao\\My Documents\\Panel.java");
        map.put("Folder Name", "C:\\");
        map.put("CreationDate", Calendar.getInstance());
        map.put("ExpirationDate", Calendar.getInstance());
        map.put("DateTime", Calendar.getInstance());
        map.put("Name", "Label1");
        map.put("Font Name", "Arial");
        map.put("Font", new Font("Tahoma", Font.BOLD, 11));
        map.put("Default Font Name", "Verdana");
        map.put("Text", "Data");
        map.put("Opaque", Boolean.FALSE);
        map.put("Visible", Boolean.TRUE);
        map.put("Not Editable", new Integer(10));
        map.put("Long", new Long(123456789));
        map.put("Integer", new Integer(1234));
//        map.put("Slider", new Integer(50));
        map.put("Double", new Double(1.0));
        map.put("Float", new Float(0.01));
        map.put("Calculator", 0.0);
        map.put("SSN", "000000000");
        map.put("IP Address", "192.168.0.1");
        map.put("Priority", new Integer(1));
        map.put("Multiline", "This is a multiple line cell editor. \nA new line starts here.");
        map.put("Password", new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
        map.put("Multiple Values", new String[]{"A", "B", "C"});
    }

    static class SampleProperty extends Property {
        public SampleProperty(String name, String description, Class type, String category, ConverterContext context) {
            super(name, description, type, category, context);
        }

        public SampleProperty(String name, String description, Class type, String category) {
            super(name, description, type, category);
        }

        public SampleProperty(String name, String description, Class type) {
            super(name, description, type);
        }

        public SampleProperty(String name, String description) {
            super(name, description);
        }

        public SampleProperty(String name) {
            super(name);
        }

        public void setValue(Object value) {
            Object old = getValue();
            if (!JideSwingUtilities.equals(old, value)) {
                map.put(getFullName(), value);
                firePropertyChange(PROPERTY_VALUE, old, value);
            }
        }

        public Object getValue() {
            Object value = map.get(getFullName());
            return value;
        }

        public boolean hasValue() {
            return map.get(getFullName()) != null;
        }
    }
}

