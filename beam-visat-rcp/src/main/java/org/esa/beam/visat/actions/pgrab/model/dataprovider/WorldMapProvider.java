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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.WorldMapImageLoader;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;
import org.esa.beam.visat.actions.pgrab.util.WorldMapPainter;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;


/**
 * Description of WorldMapProvider
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapProvider implements DataProvider {

    private static final String KEY_BOUNDARY_PATH = ".worldMap.boundaryPath";
    private static final String KEY_PRODUCT_CENTER_LAT = ".worldMap.centerLat";
    private static final String KEY_PRODUCT_CENTER_LON = ".worldMap.centerLon";

    private TableColumn worldMapColumn;
    private boolean createWorldMapFile;
    private final Comparator worldMapComparator = new WorldMapComparator();

    public WorldMapProvider(final boolean createWorldMapFile) {
        this.createWorldMapFile = createWorldMapFile;
    }

    public boolean mustCreateData(final RepositoryEntry entry, final Repository repository) {
        final PropertyMap propertyMap = repository.getPropertyMap();
        final String productName = entry.getProductFile().getName();

        final String boundaryPathString = propertyMap.getPropertyString(productName + KEY_BOUNDARY_PATH, null);

        if (boundaryPathString == null) {
            return true;
        }

        final String latString = propertyMap.getPropertyString(productName + KEY_PRODUCT_CENTER_LAT, null);

        if (latString == null) {
            return true;
        }

        final String lonString = propertyMap.getPropertyString(productName + KEY_PRODUCT_CENTER_LON, null);

        if (lonString == null) {
            return true;
        }

        if (createWorldMapFile) {
            final File worldMapImageFile = getWorldMapImageFile(repository.getStorageDir(),
                                                                entry.getProductFile().getName());
            if (worldMapImageFile == null || !worldMapImageFile.exists()) {
                return true;
            }
        }

        return false;
    }

    public void createData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final Product product = entry.getProduct();
        final PropertyMap propertyMap = repository.getPropertyMap();
        final String productName = entry.getProductFile().getName();

        if (product.getGeoCoding() != null) {
            final String boundaryPathString = createPathsString(product);
            propertyMap.setPropertyString(productName + KEY_BOUNDARY_PATH, boundaryPathString);
            final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(product);
            propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LAT, centerGeoPos.lat);
            propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LON, centerGeoPos.lon);
        } else {
            propertyMap.setPropertyString(productName + KEY_BOUNDARY_PATH, "");
            propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LAT, null);
            propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LON, null);
        }

        if (createWorldMapFile) {
            final File worldMapImageFile = getWorldMapImageFile(repository.getStorageDir(),
                                                                entry.getProductFile().getName());
            final BufferedImage worldMapImage = createWorldMapImage(product);
            writeImage(worldMapImage, worldMapImageFile);
        }
    }

    public Object getData(final RepositoryEntry entry, final Repository repository) throws IOException {
        final String productName = entry.getProductFile().getName();
        final PropertyMap propertyMap = repository.getPropertyMap();

        final String boundaryPathString = propertyMap.getPropertyString(productName + KEY_BOUNDARY_PATH, null);
        final Double latValue = propertyMap.getPropertyDouble(productName + KEY_PRODUCT_CENTER_LAT, null);
        final Double lonValue = propertyMap.getPropertyDouble(productName + KEY_PRODUCT_CENTER_LON, null);

        final PropertyMap dataMap = new PropertyMap();
        dataMap.setPropertyString(KEY_BOUNDARY_PATH, boundaryPathString);
        dataMap.setPropertyDouble(KEY_PRODUCT_CENTER_LAT, latValue);
        dataMap.setPropertyDouble(KEY_PRODUCT_CENTER_LON, lonValue);

        return dataMap;
    }

    public Comparator getComparator() {
        return worldMapComparator;
    }

    public void cleanUp(final RepositoryEntry entry, final Repository repository) {
        final String productName = entry.getProductFile().getName();
        final PropertyMap propertyMap = repository.getPropertyMap();
        propertyMap.setPropertyString(productName + KEY_BOUNDARY_PATH, null);
        propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LAT, null);
        propertyMap.setPropertyDouble(productName + KEY_PRODUCT_CENTER_LON, null);


        if (createWorldMapFile) {
            final File worldMapImageFile = getWorldMapImageFile(repository.getStorageDir(),
                                                                entry.getProductFile().getName());
            if (worldMapImageFile != null && worldMapImageFile.exists()) {
                worldMapImageFile.delete();
            }
        }
    }

    public TableColumn getTableColumn() {
        if (worldMapColumn == null) {
            worldMapColumn = new TableColumn();
            worldMapColumn.setHeaderValue("Geo-Location");      /*I18N*/
            worldMapColumn.setPreferredWidth(250);
            worldMapColumn.setCellRenderer(new WorldMapCellRenderer(250));
            worldMapColumn.setCellEditor(new WorldMapCellEditor());
            worldMapColumn.setResizable(true);
        }
        return worldMapColumn;
    }


    private static String createPathsString(final Product product) {
        final GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(product);
        final StringWriter pathWriter = new StringWriter();
        for (int i = 0; i < geoBoundaryPaths.length; i++) {
            final GeneralPath geoBoundaryPath = geoBoundaryPaths[i];
            if (i > 0) {
                pathWriter.write(",");
            }
            write(geoBoundaryPath, pathWriter);
        }
        return pathWriter.toString();
    }

    private static void write(final GeneralPath geoBoundaryPath, final StringWriter writer) {
        final PathIterator iterator = geoBoundaryPath.getPathIterator(null);
        writer.write("path(");
        final float[] floats = new float[6];
        String comma = "";
        while (!iterator.isDone()) {
            iterator.currentSegment(floats);
            writer.write(comma + floats[0] + "," + floats[1]);
            comma = ",";
            iterator.next();
        }
        writer.write(")");
    }

    private static File getWorldMapImageFile(final File storageDir, final String productFilename) {
        return new File(storageDir, "WM_" + productFilename + ".jpg");
    }

    private static BufferedImage createWorldMapImage(final Product product) {
        final GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(product);
        final Image wmImage = WorldMapImageLoader.getWorldMapImage(false);
        final WorldMapPainter worldMapPainter = new WorldMapPainter(wmImage.getScaledInstance(
                wmImage.getHeight(null) / 2, -1,
                Image.SCALE_SMOOTH));

        return worldMapPainter.createWorldMapImage(geoBoundaryPaths);
    }

    private static void writeImage(final BufferedImage worldMapImage, final File worldMapImageFile) throws IOException {
        FileImageOutputStream fios = null;
        try {
            fios = new FileImageOutputStream(worldMapImageFile);
            ImageIO.write(worldMapImage, "JPEG", fios);
        } finally {
            if (fios != null) {
                fios.close();
            }
        }
    }

    private static GeneralPath[] createGeoBoundaryPathes(final String pathsString) {
        final ArrayList<GeneralPath> pathes = new ArrayList<GeneralPath>(5);
        if (pathsString != null && pathsString.trim().length() > 0) {
            int openIndex = 0;
            int closeIndex = 0;
            openIndex = pathsString.indexOf("(", closeIndex);
            closeIndex = pathsString.indexOf(")", openIndex);
            while (openIndex != -1 && closeIndex != -1) {
                pathes.add(createGeoBoundaryPath(pathsString.substring(openIndex + 1, closeIndex - 1)));
                openIndex = pathsString.indexOf("(", closeIndex);
                closeIndex = pathsString.indexOf(")", openIndex);
            }
        }
        return pathes.toArray(new GeneralPath[pathes.size()]);
    }

    private static GeneralPath createGeoBoundaryPath(final String s) {
        final float[] geoFloats = StringUtils.toFloatArray(s, ",");
        final GeneralPath gp = new GeneralPath();
        gp.moveTo(geoFloats[0], geoFloats[1]);
        for (int i = 2; i < geoFloats.length; i += 2) {
            gp.lineTo(geoFloats[i], geoFloats[i + 1]);
        }
        gp.closePath();
        return gp;
    }


    public static class WorldMapCellRenderer extends DefaultTableCellRenderer {

        private static final Image worldMap = WorldMapImageLoader.getWorldMapImage(false);

        private final int cellWidth;
        private final int cellHeight;

        private WorldMapPainter wmPainter;
        private JLabel cellComponent;

        public WorldMapCellRenderer(final int cellWidth) {
            this.cellWidth = cellWidth;
            cellHeight = cellWidth / 2;
            wmPainter = new WorldMapPainter(worldMap.getScaledInstance(this.cellWidth, -1, Image.SCALE_DEFAULT));
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column) {

            if (cellComponent == null) {
                cellComponent = (JLabel) super.getTableCellRendererComponent(table, value,
                                                                             isSelected, hasFocus,
                                                                             row, column);
                cellComponent.setText("");
                cellComponent.setHorizontalAlignment(SwingConstants.CENTER);
                cellComponent.setVerticalAlignment(SwingConstants.CENTER);
            }

            setBackground(table, isSelected);

            final String boundaryString;

            if (value instanceof PropertyMap) {
                final PropertyMap propertyMap = (PropertyMap) value;
                boundaryString = propertyMap.getPropertyString(KEY_BOUNDARY_PATH, null);
            } else {
                boundaryString = null;
            }

            final GeneralPath[] geoBoundaryPathes = createGeoBoundaryPathes(boundaryString);
            if (geoBoundaryPathes.length > 0) {
                final BufferedImage worldMapImage = wmPainter.createWorldMapImage(geoBoundaryPathes);
                final Image scaledWorldMap = worldMapImage.getScaledInstance(cellWidth, -1, Image.SCALE_DEFAULT);

                cellComponent.setIcon(new ImageIcon(scaledWorldMap));
                cellComponent.setText("");

                adjustCellHeight(table, row);
            } else {
                cellComponent.setIcon(null);
                if (value == null) {
                    cellComponent.setText("");
                } else {
                    cellComponent.setText("Geo-location not available!");
                }
            }

            return cellComponent;
        }

        private void setBackground(final JTable table, final boolean isSelected) {
            Color backgroundColor = table.getBackground();
            if (isSelected) {
                backgroundColor = table.getSelectionBackground();
            }
            cellComponent.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));
            cellComponent.setBackground(backgroundColor);
        }

        private void adjustCellHeight(final JTable table, final int row) {
            if (table.getRowHeight(row) < cellHeight) {
                table.setRowHeight(row, cellHeight);
            }
        }

    }

    public static class WorldMapCellEditor extends AbstractCellEditor implements TableCellEditor {

        private static final Image worldMap = WorldMapImageLoader.getWorldMapImage(false);

        private final WorldMapPainter wmPainter;
        private final JScrollPane scrollPane;
        private Image scaledImage;

        public WorldMapCellEditor() {
            scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.getViewport().setOpaque(false);
            wmPainter = new WorldMapPainter(worldMap);
            scaledImage = worldMap.getScaledInstance(worldMap.getWidth(null) / 2, -1, Image.SCALE_SMOOTH);
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(final JTable table, final Object value,
                                                     final boolean isSelected, final int row, final int column) {

            final String boundaryString;
            if (value instanceof PropertyMap) {
                final PropertyMap map = (PropertyMap) value;
                boundaryString = map.getPropertyString(KEY_BOUNDARY_PATH, null);
            } else {
                boundaryString = null;
            }
            final GeneralPath[] geoBoundaryPathes = createGeoBoundaryPathes(boundaryString);
            if (geoBoundaryPathes.length > 0) {
                wmPainter.setWorldMapImage(scaledImage);
                final BufferedImage worldMapImage = wmPainter.createWorldMapImage(geoBoundaryPathes);
                scrollPane.setViewportView(new JLabel(new ImageIcon(worldMapImage)));

                final Color backgroundColor = table.getSelectionBackground();
                scrollPane.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));
                scrollPane.setBackground(backgroundColor);

                // todo: first time scrolling is not good, cause the viewRect has no width and height at this time
                // hack: this is not good, there might be a better solution
                final JViewport viewport = scrollPane.getViewport();
                if (viewport.getWidth() == 0 && viewport.getHeight() == 0) {
                    viewport.setSize(table.getColumnModel().getColumn(column).getWidth(), table.getRowHeight(row));
                }

                scrollToCenterPath(geoBoundaryPathes[0], viewport);
                return scrollPane;
            } else {
                return null;
            }

        }

        private void scrollToCenterPath(final GeneralPath geoBoundaryPath, final JViewport viewport) {
            final GeneralPath pixelPath = ProductUtils.convertToPixelPath(geoBoundaryPath, wmPainter.getGeoCoding());
            final Rectangle viewRect = viewport.getViewRect();
            final Rectangle pathBounds = pixelPath.getBounds();
            setCenter(viewRect, new Point((int) pathBounds.getCenterX(), (int) pathBounds.getCenterY()));
            final Dimension bounds = new Dimension(scaledImage.getWidth(null), scaledImage.getHeight(null));
            ensureRectIsInBounds(viewRect, bounds);
            viewport.scrollRectToVisible(viewRect);
        }

        private void ensureRectIsInBounds(final Rectangle rectangle, final Dimension bounds) {
            if (rectangle.x < 0) {
                rectangle.x = 0;
            }
            if (rectangle.x + rectangle.width > bounds.width) {
                final JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                final int scrollBarWidth;
                if (verticalScrollBar != null) {
                    scrollBarWidth = verticalScrollBar.getWidth();
                } else {
                    scrollBarWidth = 0;
                }
                rectangle.x = bounds.width - rectangle.width - scrollBarWidth;
            }
            if (rectangle.y < 0) {
                rectangle.y = 0;
            }
            if (rectangle.y + rectangle.height > bounds.height) {
                final JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                final int scrollBarHeight;
                if (horizontalScrollBar != null) {
                    scrollBarHeight = horizontalScrollBar.getHeight();
                } else {
                    scrollBarHeight = 0;
                }
                rectangle.y = bounds.height - rectangle.height - scrollBarHeight;
            }
        }

        private static void setCenter(final Rectangle rectangle, final Point center) {
            final int diffX = center.x - (int) rectangle.getCenterX();
            final int diffY = center.y - (int) rectangle.getCenterY();
            final int x = ((int) rectangle.getX()) + diffX;
            final int y = (int) (rectangle.getY() + diffY);
            rectangle.setLocation(x, y);
        }

    }


    private static class WorldMapComparator implements Comparator {

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
            final double lat1 = map1.getPropertyDouble(KEY_PRODUCT_CENTER_LAT, 180);
            final double lon1 = map1.getPropertyDouble(KEY_PRODUCT_CENTER_LON, -90);
            final double lat2 = map2.getPropertyDouble(KEY_PRODUCT_CENTER_LAT, 180);
            final double lon2 = map2.getPropertyDouble(KEY_PRODUCT_CENTER_LON, -90);

            // sorting from upper left to lower right
            if (lat1 > lat2) {
                return 1;
            } else if (lat1 < lat2) {
                return -1;
            } else {
                if (lon1 < lon2) {
                    return 1;
                } else if (lon1 > lon2) {
                    return -1;
                } else {
                    return 0;
                }
            }

        }
    }
}
