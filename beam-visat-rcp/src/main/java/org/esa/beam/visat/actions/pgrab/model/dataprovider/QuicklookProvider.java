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
package org.esa.beam.visat.actions.pgrab.model.dataprovider;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;

import javax.imageio.ImageIO;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;

public class QuicklookProvider implements DataProvider {

    private final Comparator quickLookComparator = new QuickLookComparator();
    private final int maxWidth;
    private TableColumn quickLookColumn;

    public QuicklookProvider(final int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public boolean mustCreateData(final RepositoryEntry entry, final Repository repository) {
        final File quickLookFile = getQuickLookFile(repository.getStorageDir(), entry.getProductFile().getName());
        return !quickLookFile.exists();
    }

    public void createData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final Product product = entry.getProduct();
        final File storageDir = repository.getStorageDir();
        final File quickLookFile = getQuickLookFile(storageDir, entry.getProductFile().getName());
        quickLookFile.createNewFile();
        final BufferedImage bufferedImage = createQuickLook(product);
        ImageIO.write(bufferedImage, "JPG", quickLookFile);
    }

    public Object getData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final File storageDir = repository.getStorageDir();
        final File quickLookFile = getQuickLookFile(storageDir, entry.getProductFile().getName());
        BufferedImage bufferedImage = null;
        if (quickLookFile.canRead()) {
            final FileInputStream fis = new FileInputStream(quickLookFile);
            try {
                bufferedImage = ImageIO.read(fis);
            } finally {
                fis.close();
            }
        }
        final DataObject data = new DataObject();
        data.quickLook = bufferedImage;
        return data;
    }

    /**
     * Returns the {@link java.util.Comparator} for the data provided by this <code>DataProvider</code>.
     *
     * @return the comparator.
     */
    public Comparator getComparator() {
        return quickLookComparator;
    }

    public void cleanUp(final RepositoryEntry entry, final Repository repository) {
        // nothing to do with propertyMap
        final File quickLookFile = getQuickLookFile(repository.getStorageDir(), entry.getProductFile().getName());
        if (quickLookFile != null && quickLookFile.exists()) {
            quickLookFile.delete();
        }
    }

    private BufferedImage createQuickLook(final Product product) throws IOException {
        final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
        int scaleFactor = product.getSceneRasterWidth() / maxWidth;
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        productSubsetDef.setSubSampling(scaleFactor, scaleFactor);
        final Product productSubset = product.createSubset(productSubsetDef, null, null);

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(productSubset);
        return ProductUtils.createColorIndexedImage(productSubset.getBand(quicklookBandName), ProgressMonitor.NULL);
    }

    public TableColumn getTableColumn() {
        if (quickLookColumn == null) {
            quickLookColumn = new TableColumn();
            quickLookColumn.setHeaderValue("Quick Look");        /*I18N*/
            quickLookColumn.setPreferredWidth(150);
            quickLookColumn.setResizable(true);
            quickLookColumn.setCellRenderer(new QuickLookRenderer(100));
            quickLookColumn.setCellEditor(new QuickLookEditor());
        }
        return quickLookColumn;
    }

    private static File getQuickLookFile(final File storageDir, final String productFileName) {
        return new File(storageDir, "QL_" + productFileName + ".jpg");
    }

    private static class DataObject {

        public BufferedImage quickLook = null;
    }

    private static class QuickLookRenderer extends DefaultTableCellRenderer {

        private int rowHeight;
        private JLabel tableComponent;

        public QuickLookRenderer(final int height) {
            rowHeight = height + 3;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            if (tableComponent == null) {
                tableComponent = (JLabel) super.getTableCellRendererComponent(table,
                                                                              value,
                                                                              isSelected,
                                                                              hasFocus,
                                                                              row,
                                                                              column);
                tableComponent.setText("");
                tableComponent.setVerticalAlignment(SwingConstants.CENTER);
                tableComponent.setHorizontalAlignment(SwingConstants.CENTER);

            }

            setBackground(table, isSelected);

            if (value == null) {
                tableComponent.setIcon(null);
                tableComponent.setText("");
                return tableComponent;
            }
            final DataObject data;
            if (value instanceof DataObject) {
                data = (DataObject) value;
            } else {
                data = new DataObject();
            }

            if (data.quickLook != null) {
                final BufferedImage image = data.quickLook;
                final TableColumn tableColumn = table.getColumnModel().getColumn(column);
                final int cellWidth = tableColumn.getWidth();
                tableComponent.setIcon(
                        new ImageIcon(image.getScaledInstance(cellWidth, -1, BufferedImage.SCALE_DEFAULT)));
                tableComponent.setText("");
                setTableRowHeight(table, row);
            } else {
                tableComponent.setIcon(null);
                tableComponent.setText("Not available!");
            }
            return tableComponent;
        }

        private void setBackground(final JTable table, final boolean isSelected) {
            Color backGroundColor = table.getBackground();
            if (isSelected) {
                backGroundColor = table.getSelectionBackground();
            }
            tableComponent.setBorder(BorderFactory.createLineBorder(backGroundColor, 3));
            tableComponent.setBackground(backGroundColor);
        }

        private void setTableRowHeight(final JTable table, final int row) {
            if (table.getRowHeight(row) < rowHeight) {
                table.setRowHeight(row, rowHeight);
            }
        }
    }

    public static class QuickLookEditor extends AbstractCellEditor implements TableCellEditor {

        private JScrollPane scrollPane;

        public QuickLookEditor() {

            scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getViewport().setOpaque(false);
        }

        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column) {
            if (!(value instanceof DataObject)) {
                return null;
            }
            final BufferedImage image = ((DataObject) value).quickLook;
            if (image == null) {
                return null;
            }
            final TableColumn tableColumn = table.getColumnModel().getColumn(column);
            final int cellWidth = tableColumn.getWidth();
            scrollPane.setViewportView(
                    new JLabel(new ImageIcon(image.getScaledInstance(cellWidth, -1, BufferedImage.SCALE_DEFAULT))));
            final Color backgroundColor = table.getSelectionBackground();
            scrollPane.setBackground(backgroundColor);
            scrollPane.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));

            return scrollPane;
        }

        public Object getCellEditorValue() {
            return null;
        }
    }


    private static class QuickLookComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final BufferedImage image1 = ((DataObject) o1).quickLook;
            final BufferedImage image2 = ((DataObject) o2).quickLook;

            if (image1 == null) {
                return -1;
            } else if (image2 == null) {
                return 1;
            }

            final Integer height1 = image1.getHeight();
            final Integer height2 = image2.getHeight();

            return height1.compareTo(height2);
        }
    }
}
