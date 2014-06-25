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

package org.esa.beam.visat.toolviews.stat;

import com.jidesoft.grid.CellSpan;
import com.jidesoft.grid.SpanTableModel;
import org.esa.beam.framework.datamodel.BasicPixelGeoCoding;
import org.esa.beam.framework.datamodel.CombinedFXYGeoCoding;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.FXYGeoCoding;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.FXYSum;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
class GeoCodingTablePanel extends TablePagePanel {

    private static final String DEFAULT_INFORMATION_TEXT = "No geo-coding information available."; /*I18N*/
    private static final String TITLE_PREFIX = "Geo-Coding";   /*I18N*/

    private GeoCoding geoCoding;
    private GeoCodingTableModel tableModel;
    List<Integer> wrappingRows = new ArrayList<>();
    private final TableCellRenderer alternatingRows;
    private final TableCellRenderer alternatingWrap;
    private final TableCellRenderer wrapTooltipAlternating;

    public GeoCodingTablePanel(ToolView toolView, String helpId) {
        super(toolView, helpId, TITLE_PREFIX, DEFAULT_INFORMATION_TEXT);
        alternatingRows = RendererFactory.createRenderer(RendererFactory.ALTERNATING_ROWS);
        alternatingWrap = RendererFactory.createRenderer(
                RendererFactory.ALTERNATING_ROWS |
                RendererFactory.WRAP_TEXT, wrappingRows);
        wrapTooltipAlternating = RendererFactory.createRenderer(
                RendererFactory.ALTERNATING_ROWS |
                RendererFactory.TOOLTIP_AWARE |
                RendererFactory.WRAP_TEXT, wrappingRows);
    }

    @Override
    protected boolean mustHandleSelectionChange() {
        final RasterDataNode raster = getRaster();
        return super.mustHandleSelectionChange() || (raster != null && geoCoding != raster.getGeoCoding());
    }

    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
            if (event.getSourceNode() instanceof Product) {
                geoCoding = getProduct().getGeoCoding();
            } else {
                geoCoding = getRaster().getGeoCoding();
            }
            updateComponents();
        }
    }

    @Override
    protected void initComponents() {
        tableModel = new GeoCodingTableModel();
        getTable().setTableHeader(null);
        getTable().addMouseListener(new PopupHandler());
        getTable().setShowGrid(false);
        getTable().setRowSelectionAllowed(false);
        getTable().setColumnSelectionAllowed(false);
        add(new JScrollPane(getTable()), BorderLayout.CENTER);
    }

    @Override
    protected void updateComponents() {
        if (isVisible()) {
            ensureTableModel();
            createRows();
            if (tableModel.getRowCount() == 0) {
                showNoInformationAvailableMessage();
            }
        }
    }

    private void ensureTableModel() {
        if (getTable().getModel() != tableModel) {
            getTable().setModel(tableModel);
            getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            setColumnRenderer(0, alternatingWrap);
            setColumnRenderer(1, alternatingRows);
            setColumnRenderer(2, alternatingRows);
            setColumnRenderer(3, wrapTooltipAlternating);
            setColumnRenderer(4, alternatingRows);
            setColumnRenderer(5, alternatingRows);
            setFirstColumnWidth(120);
        }
    }

    private void setFirstColumnWidth(int width) {
        getTable().getColumnModel().getColumn(0).setMaxWidth(width);
        getTable().getColumnModel().getColumn(0).setMinWidth(width);
        getTable().getColumnModel().getColumn(0).setPreferredWidth(width);
    }

    @Override
    protected String getDataAsText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            builder.append(tableModel.getRows().get(i));
            if (i < tableModel.getRowCount() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private void createRows() {
        tableModel.clear();
        final RasterDataNode raster = getRaster();
        final Product product = getProduct();

        boolean usingUniformGeoCodings = false;
        if (product != null) {
            usingUniformGeoCodings = product.isUsingSingleGeoCoding();
        }

        final GeoCoding geoCoding;
        final PixelPos sceneCenter;
        final PixelPos sceneUL;
        final PixelPos sceneUR;
        final PixelPos sceneLL;
        final PixelPos sceneLR;
        final String nodeType;
        final Rectangle region;
        if (usingUniformGeoCodings) {
            nodeType = "product";
            geoCoding = product.getGeoCoding();
            sceneCenter = new PixelPos(product.getSceneRasterWidth() / 2 + 0.5f,
                                       product.getSceneRasterHeight() / 2 + 0.5f);
            sceneUL = new PixelPos(0 + 0.5f, 0 + 0.5f);
            sceneUR = new PixelPos(product.getSceneRasterWidth() - 1 + 0.5f, 0 + 0.5f);
            sceneLL = new PixelPos(0 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            sceneLR = new PixelPos(product.getSceneRasterWidth() - 1 + 0.5f,
                                   product.getSceneRasterHeight() - 1 + 0.5f);
            region = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());

        } else {
            if (raster == null) {
                return;
            }
            assert product != null;

            nodeType = "band";
            geoCoding = raster.getGeoCoding();
            sceneCenter = new PixelPos(raster.getSceneRasterWidth() / 2 + 0.5f,
                                       raster.getSceneRasterHeight() / 2 + 0.5f);
            sceneUL = new PixelPos(0 + 0.5f, 0 + 0.5f);
            sceneUR = new PixelPos(raster.getSceneRasterWidth() - 1 + 0.5f, 0 + 0.5f);
            sceneLL = new PixelPos(0 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            sceneLR = new PixelPos(raster.getSceneRasterWidth() - 1 + 0.5f,
                                   raster.getSceneRasterHeight() - 1 + 0.5f);
            region = new Rectangle(0, 0, raster.getSceneRasterWidth(), raster.getRasterHeight());
        }
        writeGeoCoding(geoCoding, sceneCenter, sceneUL, sceneUR, sceneLL, sceneLR, nodeType, region);

    }

    private void writeGeoCoding(final GeoCoding geoCoding,
                                final PixelPos sceneCenter, final PixelPos sceneUpperLeft,
                                final PixelPos sceneUpperRight, final PixelPos sceneLowerLeft,
                                final PixelPos sceneLowerRight, final String nodeType, Rectangle region) {
        if (geoCoding != null) {
            GeoPos gp = new GeoPos();

            gp = geoCoding.getGeoPos(sceneCenter, gp);
            addRow("Center latitude", gp.getLatString());
            addRow("Center longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneUpperLeft, gp);
            addRow("Upper left latitude", gp.getLatString());
            addRow("Upper left longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneUpperRight, gp);
            addRow("Upper right latitude", gp.getLatString());
            addRow("Upper right longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneLowerLeft, gp);
            addRow("Lower left latitude", gp.getLatString());
            addRow("Lower left longitude", gp.getLonString());

            gp = geoCoding.getGeoPos(sceneLowerRight, gp);
            addRow("Lower right latitude", gp.getLatString());
            addRow("Lower right longitude", gp.getLonString());

            addEmptyRow();

            addRow("WKT of the image CRS", geoCoding.getImageCRS().toString());
            addRow("WKT of the geographical CRS", geoCoding.getGeoCRS().toString());

            addEmptyRow();
        }

        if (geoCoding instanceof TiePointGeoCoding) {
            writeTiePointGeoCoding((TiePointGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof BasicPixelGeoCoding) {
            writePixelGeoCoding((BasicPixelGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof MapGeoCoding) {
            writeMapGeoCoding((MapGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof FXYGeoCoding) {
            writeFXYGeoCoding((FXYGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof CombinedFXYGeoCoding) {
            writeCombinedFXYGeoCoding((CombinedFXYGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof GcpGeoCoding) {
            writeGcpGeoCoding((GcpGeoCoding) geoCoding, nodeType);
        } else if (geoCoding instanceof CrsGeoCoding) {
            writeCrsGeoCoding((CrsGeoCoding) geoCoding, nodeType);
        } else if (geoCoding != null) {
            writeUnknownGeoCoding(geoCoding, nodeType);
        } else {
            addRow("The " + nodeType + " has no geo-coding information.");

        }
    }

    private void addHeaderRow(String content) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            b.append('=');
        }
        tableModel.addRow(new SingleInformationRow(b.toString()));
        tableModel.addRow(new SingleInformationRow(content));
        tableModel.addRow(new SingleInformationRow(b.toString()));
    }

    private void addRow(String content) {
        if (content.contains("\n")) {
            int currentRowIndex = tableModel.getRowCount();
            wrappingRows.add(currentRowIndex);
        }
        tableModel.addRow(new SingleInformationRow(content));
    }

    private void addRow(String name, String value) {
        if (value.contains("\n")) {
            int currentRowIndex = tableModel.getRowCount();
            wrappingRows.add(currentRowIndex);
        }
        tableModel.addRow(new GeoCodingTableRow(name, value.replaceAll("\r\n", "\n")));
    }

    private void addRow(String... values) {
        tableModel.addRow(new GeoCodingTableRow(values));
    }

    private void writeGcpGeoCoding(GcpGeoCoding gcpGeoCoding, String nodeType) {
        addEmptyRow();
        addRow("The " + nodeType + " uses a geo-coding which is based on ground control points (GCPs).");
        addEmptyRow();

        ProductNodeGroup<Placemark> gcpGroup = getProduct().getGcpGroup();
        addRow("Number Of GCPs", String.valueOf(gcpGroup.getNodeCount()));
        addRow("Function", String.valueOf(gcpGeoCoding.getMethod()));
        addRow("Datum", String.valueOf(gcpGeoCoding.getDatum().getName()));
        addRow("Latitude RMSE", String.valueOf(gcpGeoCoding.getRmseLat()));
        addRow("Longitude RMSE", String.valueOf(gcpGeoCoding.getRmseLon()));
        addEmptyRow();

        addRow("Table of used GCPs");
        Placemark[] gcps = gcpGroup.toArray(new Placemark[0]);
        addRow("Number", "Label", "X", "Y", "Latitude", "Longitude");
        for (int i = 0; i < gcps.length; i++) {
            Placemark gcp = gcps[i];
            PixelPos pixelPos = gcp.getPixelPos();
            GeoPos geoPos = gcp.getGeoPos();
            addRow(String.valueOf(i), gcp.getLabel(),
                   String.valueOf(pixelPos.getX()), String.valueOf(pixelPos.getY()),
                   geoPos.getLatString(), geoPos.getLonString());
        }

        setFirstColumnWidth(40);
    }

    private void writeCrsGeoCoding(CrsGeoCoding geoCoding, String nodeType) {
        addRow("The " + nodeType + " uses a geo-coding based on a cartographic map CRS.");
        addEmptyRow();
        addRow("WKT of the map CRS", geoCoding.getMapCRS().toString());
        addEmptyRow();
        addRow("Image-to-map transformation", geoCoding.getImageToMapTransform().toString());
    }

    private void writeUnknownGeoCoding(GeoCoding geoCoding, String nodeType) {
        addRow("The " + nodeType + " uses an unknown geo-coding implementation.");
        addRow("Class", geoCoding.getClass().getName());
        addRow("Instance", geoCoding.toString());
    }

    private void writeCombinedFXYGeoCoding(CombinedFXYGeoCoding combinedGeoCoding, String nodeType) {
        final CombinedFXYGeoCoding.CodingWrapper[] codingWrappers = combinedGeoCoding.getCodingWrappers();

        addEmptyRow();
        addRow("The " + nodeType + " uses a geo-coding which consists of multiple polynomial based geo-coding.");
        addEmptyRow();

        addRow("The geo-coding uses " + codingWrappers.length + " polynomial based geo-codings");

        for (int i = 0; i < codingWrappers.length; i++) {
            final CombinedFXYGeoCoding.CodingWrapper codingWrapper = codingWrappers[i];
            final Rectangle region = codingWrapper.getRegion();
            addHeaderRow("Geo-coding[" + (i + 1) + "]");
            addRow("The region in the scene which is covered by this geo-coding is defined by:");
            addRow("Location: X = " + region.x + ", Y = " + region.y + "\n");
            addRow("Dimension: W = " + region.width + ", H = " + region.height);
            addEmptyRow();

            final FXYGeoCoding fxyGeoCoding = codingWrapper.getGeoGoding();
            addRow("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
                   "by using following polynomial equations");
            addRow(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y"));
            addRow(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y"));
            addEmptyRow();

            addRow("Pixels (x,y) are computed from geographic coordinates (lat,lon)\n" +
                   "by using the following polynomial equations");
            addRow(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon"));
            addRow(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon"));
        }
    }

    private void writeFXYGeoCoding(FXYGeoCoding fxyGeoCoding, String nodeType) {
        addEmptyRow();
        addRow("The" + nodeType + " uses a polynomial based geo-coding.");
        addEmptyRow();

        addRow("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
               "by using following polynomial equations");
        addRow(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y"));
        addRow(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y"));
        addEmptyRow();

        addRow("Pixels (x,y) are computed from geographic coordinates (lat,lon)\n" +
               "by using the following polynomial equations");
        addRow(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon"));
        addRow(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon"));
    }

    private void writeMapGeoCoding(MapGeoCoding mgc, String nodeType) {
        final MapInfo mi = mgc.getMapInfo();

        addEmptyRow();
        addRow("The " + nodeType + " uses a map-projection based geo-coding.");
        addEmptyRow();

        addRow("Projection", mi.getMapProjection().getName());

        addRow("Projection parameters");
        final Parameter[] parameters = mi.getMapProjection().getMapTransform().getDescriptor().getParameters();
        final double[] parameterValues = mi.getMapProjection().getMapTransform().getParameterValues();
        for (int i = 0; i < parameters.length; i++) {
            addRow(parameters[i].getName(),
                   String.valueOf(parameterValues[i]) + " " + parameters[i].getProperties().getPhysicalUnit());

        }
        addEmptyRow();

        addRow("Map CRS Name", mgc.getMapCRS().getName().toString());
        addRow("Map CRS WKT");
        addRow(mgc.getMapCRS().toWKT());

        addEmptyRow();

        addRow("Output parameters");
        addRow("Datum", mi.getDatum().getName());
        addRow("Reference pixel X", String.valueOf(mi.getPixelX()));
        addRow("Reference pixel Y", String.valueOf(mi.getPixelY()));
        addRow("Orientation", String.valueOf(mi.getOrientation()) + " degree");

        String mapUnit = mi.getMapProjection().getMapUnit();
        addRow("Northing", String.valueOf(mi.getNorthing()) + " " + mapUnit);
        addRow("Easting", String.valueOf(mi.getEasting()) + " " + mapUnit);
        addRow("Pixel size X", String.valueOf(mi.getPixelSizeX()) + " " + mapUnit);
        addRow("Pixel size Y", String.valueOf(mi.getPixelSizeY()) + " " + mapUnit);
    }

    private void writePixelGeoCoding(BasicPixelGeoCoding gc, String nodeType) {
        addEmptyRow();
        addRow("The " + nodeType + " uses a pixel based geo-coding.");
        addEmptyRow();
        addRow("Name of latitude band", gc.getLatBand().getName());
        addRow("Name of longitude band", gc.getLonBand().getName());

        addRow("Search radius", gc.getSearchRadius() + " pixels");
        final String validMask = gc.getValidMask();
        addRow("Valid pixel mask", validMask != null ? validMask : "");
        addRow("Crossing 180 degree meridian", String.valueOf(gc.isCrossingMeridianAt180()));

        addEmptyRow();
        addRow("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
               "by linear interpolation between pixels.");

        addEmptyRow();
        addRow("Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)\n" +
               "by a search algorithm.");
        addEmptyRow();
    }

    private void writeTiePointGeoCoding(TiePointGeoCoding tgc, String nodeType) {
        addRow("The " + nodeType + " uses a tie-point based geo-coding.");
        addEmptyRow();
        addRow("Name of latitude tie-point grid", tgc.getLatGrid().getName());
        addRow("Name of longitude tie-point grid", tgc.getLonGrid().getName());
        addRow("Crossing 180 degree meridian", String.valueOf(tgc.isCrossingMeridianAt180()));
        addEmptyRow();
        addRow("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
               "by linear interpolation between tie points.");

        final int numApproximations = tgc.getNumApproximations();
        if (numApproximations > 0) {
            addRow("Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)\n" +
                   "by polynomial approximations for " + numApproximations + " tile(s).");
            addEmptyRow();

            for (int i = 0; i < numApproximations; i++) {

                final TiePointGeoCoding.Approximation approximation = tgc.getApproximation(i);
                final FXYSum fX = approximation.getFX();
                final FXYSum fY = approximation.getFY();

                addHeaderRow("Approximation for tile " + (i + 1));
                addRow("Center latitude", String.valueOf(approximation.getCenterLat()) + " degree");
                addRow("Center longitude", String.valueOf(approximation.getCenterLon()) + " degree");
                addRow("RMSE for X", String.valueOf(fX.getRootMeanSquareError()) + " pixels");
                addRow("RMSE for Y", String.valueOf(fY.getRootMeanSquareError()) + " pixels");
                addRow("Max. error for X", String.valueOf(fX.getMaxError()) + " pixels");
                addRow("Max. error for Y", String.valueOf(fY.getMaxError()) + " pixels");
            }
        } else {
            addEmptyRow();
            addRow(
                    "WARNING: Pixel coordinates (x,y) cannot be computed from geographic coordinates (lat,lon)\n" +
                    "because appropriate polynomial approximations could not be found.");
        }
    }

    private static class GeoCodingTableRow implements TableRow {

        private final String[] values;

        public GeoCodingTableRow(String... values) {
            this.values = values;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                builder.append(values[i]);
                if (i < values.length - 1) {
                    builder.append(" ");
                }
            }
            return builder.toString();
        }

        @Override
        public CellSpan getCellspan(int rowIndex, int columnIndex, TableModel model) {
            return new CellSpan(rowIndex, columnIndex, 1, getColSpan(model));
        }

        public Object getValue(int columnIndex, TableModel model) {
            return values[columnIndex / getColSpan(model)];
        }

        private int getColSpan(TableModel model) {
            return model.getColumnCount() / values.length;
        }
    }


    private static class GeoCodingTableModel extends TablePagePanelModel implements SpanTableModel {

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TableRow row = rows.get(rowIndex);
            if (row instanceof GeoCodingTableRow) {
                GeoCodingTableRow tableRow = (GeoCodingTableRow) row;
                return tableRow.getValue(columnIndex, this);
            }
            return row.toString();
        }

        @Override
        public CellSpan getCellSpanAt(int rowIndex, int columnIndex) {
            return rows.get(rowIndex).getCellspan(rowIndex, columnIndex, this);
        }

        @Override
        public boolean isCellSpanOn() {
            return true;
        }
    }
}
