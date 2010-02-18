package org.esa.beam.visat.actions.pgrab.model.dataprovider;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.Comparator;

/**
 * Description of ProductPropertiesProvider
 *
 * @author Marco Peters
 */
public class ProductPropertiesProvider implements DataProvider {

    private static final String KEY_PRODUCT_TYPE = ".productProperties.Type";
    private static final String KEY_PRODUCT_CENTER_LAT = ".productProperties.CenterLat";
    private static final String KEY_PRODUCT_CENTER_LON = ".productProperties.CenterLon";
    private static final String KEY_PRODUCT_START_TIME = ".productProperties.StartTime";
    private static final String KEY_PRODUCT_RESOLUTION = ".productProperties.Resolution";
    private static final String KEY_PRODUCT_SIZE = ".productproperties.Size";
    private static final String[] PROPERTY_KEYS = new String[]{
            KEY_PRODUCT_TYPE,
            KEY_PRODUCT_SIZE,
            KEY_PRODUCT_RESOLUTION,
            KEY_PRODUCT_START_TIME,
            KEY_PRODUCT_CENTER_LAT,
            KEY_PRODUCT_CENTER_LON,
    };
    private final String[] propertyLables = new String[]{
            "Type:",
            "Size:",
            "Resolution:",
            "Start time:",
            "Center lat.:",
            "Center lon.:",
    };

    private static final String NOT_AVAILABLE = "not available";

    private final Comparator productPropertiesComparator = new ProductPropertiesComparator();

    private TableColumn propertiesColumn;

    public boolean mustCreateData(final RepositoryEntry entry, final Repository repository) {
        final PropertyMap propertyMap = repository.getPropertyMap();
        final String productName = entry.getProductFile().getName();

        for (final String propertyKey : PROPERTY_KEYS) {
            if (propertyMap.getPropertyString(productName + propertyKey, null) == null) {
                return true;
            }
        }

        return false;
    }

    public void createData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final Product product = entry.getProduct();
        final String productName = entry.getProductFile().getName();
        final PropertyMap propertyMap = repository.getPropertyMap();

        final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(product);

        final String productType = product.getProductType();
        final String resolutionString = product.getSceneRasterWidth() + " x " + product.getSceneRasterHeight();
        final String latString;
        final String lonString;
        if (centerGeoPos != null) {
            latString = centerGeoPos.getLatString();
            lonString = centerGeoPos.getLonString();
        } else {
            latString = NOT_AVAILABLE;
            lonString = NOT_AVAILABLE;
        }
        final String timeString = formatStartTime(product.getStartTime());

        final String sizeString = String.format("%1$.2f MB", entry.getProductSize());

        propertyMap.setPropertyString(productName + KEY_PRODUCT_TYPE, productType);
        propertyMap.setPropertyString(productName + KEY_PRODUCT_RESOLUTION, resolutionString);
        propertyMap.setPropertyString(productName + KEY_PRODUCT_CENTER_LAT, latString);
        propertyMap.setPropertyString(productName + KEY_PRODUCT_CENTER_LON, lonString);
        propertyMap.setPropertyString(productName + KEY_PRODUCT_START_TIME, timeString);
        propertyMap.setPropertyString(productName + KEY_PRODUCT_SIZE, sizeString);
    }

    public Object getData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final String productName = entry.getProductFile().getName();
        final PropertyMap propertyMap = repository.getPropertyMap();
        final PropertyMap properties = new PropertyMap();

        for (final String propertyKey : PROPERTY_KEYS) {
            final String typeString = propertyMap.getPropertyString(productName + propertyKey, null);
            properties.setPropertyString(propertyKey, typeString);

        }
        return properties;
    }

    public Comparator getComparator() {
        return productPropertiesComparator;
    }

    public void cleanUp(final RepositoryEntry entry, final Repository repository) {
        final PropertyMap propertyMap = repository.getPropertyMap();
        final String productName = entry.getProductFile().getName();
        for (String aPROPERTY_KEYS : PROPERTY_KEYS) {
            propertyMap.setPropertyString(productName + aPROPERTY_KEYS, null);
        }

    }

    public TableColumn getTableColumn() {
        if (propertiesColumn == null) {
            propertiesColumn = new TableColumn();
            propertiesColumn.setResizable(false);
            propertiesColumn.setHeaderValue("Product Properties");
            propertiesColumn.setCellRenderer(new ProductPropertiesRenderer());
        }
        return propertiesColumn;
    }

    private static String formatStartTime(final UTC sceneRasterStartTime) {
        String timeString = NOT_AVAILABLE;
        if (sceneRasterStartTime != null) {
            timeString = sceneRasterStartTime.getElemString();

        }
        return timeString;
    }

    private class ProductPropertiesRenderer extends JTable implements TableCellRenderer {

        private static final int ROW_HEIGHT = 100;
        private final JPanel centeringPanel = new JPanel(new BorderLayout());
        private Font valueFont;

        public ProductPropertiesRenderer() {
            final DefaultTableModel dataModel = new DefaultTableModel();
            dataModel.setColumnCount(2);
            dataModel.setRowCount(6);


            for (int i = 0; i < propertyLables.length; i++) {
                dataModel.setValueAt(propertyLables[i], i, 0);
                dataModel.setValueAt("", i, 1);
            }

            setModel(dataModel);
            valueFont = getFont().deriveFont(Font.BOLD);
            getColumnModel().getColumn(1).setCellRenderer(new PropertyValueCellRenderer(valueFont));

            getTableHeader().setVisible(false);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
        }

        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            String[] values = null;
            if (value instanceof PropertyMap) {
                final PropertyMap propertyMap = (PropertyMap) value;
                final String typeString = propertyMap.getPropertyString(KEY_PRODUCT_TYPE, null);
                final String sizeString = propertyMap.getPropertyString(KEY_PRODUCT_SIZE, null);
                final String resolutionString = propertyMap.getPropertyString(KEY_PRODUCT_RESOLUTION, null);
                final String timeString = propertyMap.getPropertyString(KEY_PRODUCT_START_TIME, null);
                final String latString = propertyMap.getPropertyString(KEY_PRODUCT_CENTER_LAT, null);
                final String lonString = propertyMap.getPropertyString(KEY_PRODUCT_CENTER_LON, null);

                values = new String[]{typeString, sizeString, resolutionString, timeString, latString, lonString};
                for (int i = 0; i < values.length; i++) {
                    setValueAt(values[i], i, 1);
                }
            } else if (value == null) {
                for (int i = 0; i < propertyLables.length; i++) {
                    setValueAt(null, i, 1);
                }
            } else {
                throw new IllegalStateException("!(value instanceof PropertyMap)");
            }

            final Color backgroundColor;
            final Color foregroundColor;
            if (isSelected) {
                backgroundColor = table.getSelectionBackground();
                foregroundColor = table.getSelectionForeground();
            } else {
                backgroundColor = table.getBackground();
                foregroundColor = table.getForeground();
            }
            setForeground(foregroundColor);
            setBackground(backgroundColor);
            centeringPanel.setForeground(foregroundColor);
            centeringPanel.setBackground(backgroundColor);
            centeringPanel.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));
            centeringPanel.add(this, BorderLayout.CENTER);

            adjustCellSize(table, row, column, values);
            return centeringPanel;
        }

        private void adjustCellSize(JTable table, int row, int column, String[] values) {
            setRowHeight(table, row, ROW_HEIGHT);

            final int lablesLength = getMaxStringLength(propertyLables, getFontMetrics(getFont()));
            int columnIndex = 0;
            increasePreferredColumnWidth(getColumnModel().getColumn(columnIndex), lablesLength);

            int valuesLength = 50;
            if (values != null) {
                valuesLength = getMaxStringLength(values, getFontMetrics(valueFont));
                increasePreferredColumnWidth(getColumnModel().getColumn(1), valuesLength);
            }
            int preferredWidth = lablesLength + valuesLength;
            preferredWidth = (int) (preferredWidth + (preferredWidth * 0.1f));
            final TableColumn valueColumn = table.getColumnModel().getColumn(column);
            int valueColWidth = Math.max(valueColumn.getWidth(), preferredWidth);
            increasePreferredColumnWidth(table.getColumnModel().getColumn(column), valueColWidth);
        }

         private void increasePreferredColumnWidth(TableColumn column, int length) {
             if (column.getPreferredWidth() < length) {
                column.setPreferredWidth(length);
            }
        }

        private void setRowHeight(final JTable table, final int row, final int rowHeight) {
            final int currentRowHeight = table.getRowHeight(row);
            if (currentRowHeight < rowHeight) {
                table.setRowHeight(rowHeight);
            }
        }

        private int getMaxStringLength(final String[] strings, final FontMetrics fontMetrics) {
            int maxWidth = Integer.MIN_VALUE;
            for (String string : strings) {
                if (string == null) {
                    string = String.valueOf(string);
                }
                final int width = SwingUtilities.computeStringWidth(fontMetrics, string);
                maxWidth = Math.max(width, maxWidth);
            }
            return maxWidth;
        }

        private class PropertyValueCellRenderer extends DefaultTableCellRenderer {

            private final Font _font;

            public PropertyValueCellRenderer(final Font font) {
                _font = font;

            }

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                           final boolean isSelected, final boolean hasFocus,
                                                           final int row, final int column) {
                final JLabel jLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                                                                   row, column);
                jLabel.setHorizontalAlignment(JLabel.RIGHT);
                jLabel.setFont(_font);
                return jLabel;
            }
        }
    }

    private static class ProductPropertiesComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final PropertyMap map1 = (PropertyMap) o1;
            final PropertyMap map2 = (PropertyMap) o2;

            final String type1 = map1.getPropertyString(KEY_PRODUCT_TYPE, null);
            final String type2 = map2.getPropertyString(KEY_PRODUCT_TYPE, null);

            return type1.compareTo(type2);
        }
    }
}
