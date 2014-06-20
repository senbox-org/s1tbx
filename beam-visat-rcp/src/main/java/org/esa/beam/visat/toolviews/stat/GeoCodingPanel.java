/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A pane within the statistcs window which displays geo-coding information.
 */
class GeoCodingPanel extends PagePanel {

    private static final String DEFAULT_GEOCODING_TEXT = "No geo-coding information available."; /*I18N*/
    private static final String TITLE_PREFIX = "Geo-Coding";   /*I18N*/
    private final String defaultText;
    private GeoCoding _geoCoding;
    private JTextArea textArea;

    GeoCodingPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, helpID, TITLE_PREFIX);
        this.defaultText = DEFAULT_GEOCODING_TEXT;
    }

    @Override
    protected boolean mustHandleSelectionChange() {
        final RasterDataNode raster = getRaster();
        return super.mustHandleSelectionChange() || (raster != null && _geoCoding != raster.getGeoCoding());
    }

    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
            if (event.getSourceNode() instanceof Product) {
                _geoCoding = getProduct().getGeoCoding();
            } else {
                _geoCoding = getRaster().getGeoCoding();
            }
            updateComponents();
        }
    }

    private String createText() {
        final RasterDataNode raster = getRaster();
        final Product product = getProduct();

        boolean usingUniformGeoCodings = false;
        if (product != null) {
            usingUniformGeoCodings = product.isUsingSingleGeoCoding();
        }

        final StringBuffer sb = new StringBuffer(1024);
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
                return DEFAULT_GEOCODING_TEXT;
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
        return writeGeoCoding(sb, geoCoding, sceneCenter, sceneUL, sceneUR, sceneLL, sceneLR, nodeType, region);

    }

    private String writeGeoCoding(final StringBuffer sb, final GeoCoding geoCoding,
                                  final PixelPos sceneCenter, final PixelPos sceneUpperLeft,
                                  final PixelPos sceneUpperRight, final PixelPos sceneLowerLeft,
                                  final PixelPos sceneLowerRight, final String nodeType, Rectangle region) {
        if (geoCoding != null) {
            GeoPos gp = new GeoPos();

            sb.append('\n');

            gp = geoCoding.getGeoPos(sceneCenter, gp);
            sb.append(String.format("%1$-18s \t%2$s\n", "Center latitude:", gp.getLatString()));
            sb.append(String.format("%1$-18s \t%2$s\n", "Center longitude:", gp.getLonString()));

            gp = geoCoding.getGeoPos(sceneUpperLeft, gp);
            sb.append(String.format("%1$-18s \t%2$s\n", "Upper left latitude:", gp.getLatString()));
            sb.append(String.format("%1$-18s \t%2$s\n", "Upper left longitude:", gp.getLonString()));

            gp = geoCoding.getGeoPos(sceneUpperRight, gp);
            sb.append(String.format("%1$-18s \t%2$s\n", "Upper right latitude:", gp.getLatString()));
            sb.append(String.format("%1$-18s \t%2$s\n", "Upper right longitude:", gp.getLonString()));

            gp = geoCoding.getGeoPos(sceneLowerLeft, gp);
            sb.append(String.format("%1$-18s \t%2$s\n", "Lower left latitude:", gp.getLatString()));
            sb.append(String.format("%1$-18s \t%2$s\n", "Lower left longitude:", gp.getLonString()));

            gp = geoCoding.getGeoPos(sceneLowerRight, gp);
            sb.append(String.format("%1$-18s \t%2$s\n", "Lower right latitude:", gp.getLatString()));
            sb.append(String.format("%1$-18s \t%2$s\n", "Lower right longitude:", gp.getLonString()));

            sb.append("\n");
            sb.append("Well-known text format (WKT) of the image CRS:\n\n");
            sb.append(geoCoding.getImageCRS().toString());
            sb.append("\n");
            sb.append("Well-known text format (WKT) of the geographical CRS:\n\n");
            sb.append(geoCoding.getGeoCRS().toString());
            sb.append("\n");
            sb.append("\n");
        }

        if (geoCoding instanceof TiePointGeoCoding) {
            writeTiePointGeoCoding((TiePointGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof BasicPixelGeoCoding) {
            writePixelGeoCoding((BasicPixelGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof MapGeoCoding) {
            writeMapGeoCoding((MapGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof FXYGeoCoding) {
            writeFXYGeoCoding((FXYGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof CombinedFXYGeoCoding) {
            writeCombinedFXYGeoCoding((CombinedFXYGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof GcpGeoCoding) {
            writeGcpGeoCoding((GcpGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding instanceof CrsGeoCoding) {
            writeCrsGeoCoding((CrsGeoCoding) geoCoding, nodeType, sb);
        } else if (geoCoding != null) {
            writeUnknownGeoCoding(geoCoding, nodeType, sb);
        } else {

            sb.append("\n");
            sb.append("\nThe ").append(nodeType).append(" has no geo-coding information.\n");

        }

        sb.append('\n');

        return sb.toString();
    }

    private void writeGcpGeoCoding(GcpGeoCoding gcpGeoCoding, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(
                " uses a geo-coding which is based on ground control points (GCPs).\n");
        sb.append("\n");

        ProductNodeGroup<Placemark> gcpGroup = getProduct().getGcpGroup();
        String formatString = "%1$-18s \t%2$s\n";
        sb.append(String.format(formatString, "Number Of GCPs:", String.valueOf(gcpGroup.getNodeCount())));
        sb.append(String.format(formatString, "Function:", String.valueOf(gcpGeoCoding.getMethod())));
        sb.append(String.format(formatString, "Datum:", String.valueOf(gcpGeoCoding.getDatum().getName())));
        sb.append(String.format(formatString, "Latitude RMSE:", String.valueOf(gcpGeoCoding.getRmseLat())));
        sb.append(String.format(formatString, "Longitude RMSE:", String.valueOf(gcpGeoCoding.getRmseLon())));
        sb.append("\n");

        sb.append("Table of used GCPs:\n");
        Placemark[] gcps = gcpGroup.toArray(new Placemark[0]);
        formatString = "%1$-10s \t%2$-15s \t%3$-10s \t%4$-10s \t%5$-18s \t%6$-18s\n";
        sb.append(String.format(formatString,
                                "Number", "Label", "X", "Y", "Latitude", "Longitude"));
        for (int i = 0; i < gcps.length; i++) {
            Placemark gcp = gcps[i];
            PixelPos pixelPos = gcp.getPixelPos();
            GeoPos geoPos = gcp.getGeoPos();
            sb.append(String.format(formatString,
                                    String.valueOf(i), gcp.getLabel(),
                                    String.valueOf(pixelPos.getX()), String.valueOf(pixelPos.getY()),
                                    geoPos.getLatString(), geoPos.getLonString()));
        }
    }

    private void writeCrsGeoCoding(CrsGeoCoding geoCoding, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses a geo-coding based on a cartographic map CRS.\n");
        sb.append("\n");
        sb.append("Well-known text format (WKT) of the map CRS:\n\n");
        sb.append(geoCoding.getMapCRS().toString());
        sb.append("\n");
        sb.append("Image-to-map transformation:\n\n");
        sb.append(geoCoding.getImageToMapTransform().toString());
    }

    private void writeUnknownGeoCoding(GeoCoding geoCoding, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses an unknown geo-coding implementation.\n");
        sb.append("\n");

        sb.append(String.format("\t%1$-10s \t%2$s\n",
                                "Class:",
                                geoCoding.getClass().getName()));

        sb.append(String.format("\t%1$-10s \t%2$s\n",
                                "Instance:",
                                geoCoding.toString()));
    }

    private void writeCombinedFXYGeoCoding(CombinedFXYGeoCoding combinedGeoCoding, String nodeType, StringBuffer sb) {
        final CombinedFXYGeoCoding.CodingWrapper[] codingWrappers = combinedGeoCoding.getCodingWrappers();

        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(
                " uses a geo-coding which consists of multiple polynomial based geo-coding.\n");
        sb.append("\n");


        sb.append("The geo-coding uses ").append(codingWrappers.length).append(" polynomial based geo-codings\n");

        for (int i = 0; i < codingWrappers.length; i++) {
            final CombinedFXYGeoCoding.CodingWrapper codingWrapper = codingWrappers[i];
            final Rectangle region = codingWrapper.getRegion();
            sb.append("\n==== Geo-coding[").append(i + 1).append("] ====\n");
            sb.append("\nThe region in the scene which is covered by this geo-coding is defined by:\n");
            sb.append("Location  : X = ").append(region.x).append(" , Y = ").append(region.y).append("\n");
            sb.append("Dimension : W = ").append(region.width).append(" , H = ").append(region.height).append("\n");
            sb.append("\n");

            final FXYGeoCoding fxyGeoCoding = codingWrapper.getGeoGoding();
            sb.append("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
                      "by using following polynomial equations: \n\n");
            sb.append(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y")).append("\n");
            sb.append(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y")).append("\n");
            sb.append("\n");
            sb.append("Pixels (x,y) are computed from geographic coordinates (lat,lon)\n" +
                      "by using the following polynomial equations: \n\n");
            sb.append(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon")).append("\n");
            sb.append(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon")).append("\n");
            sb.append("\n");

        }
    }

    private void writeFXYGeoCoding(FXYGeoCoding fxyGeoCoding, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses a polynomial based geo-coding.\n");
        sb.append("\n");

        sb.append("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
                  "by using following polynomial equations: \n\n");
        sb.append(fxyGeoCoding.getLatFunction().createCFunctionCode("latitude", "x", "y")).append("\n");
        sb.append(fxyGeoCoding.getLonFunction().createCFunctionCode("longitude", "x", "y")).append("\n");
        sb.append("\n");
        sb.append("Pixels (x,y) are computed from geographic coordinates (lat,lon)\n" +
                  "by using the following polynomial equations: \n\n");
        sb.append(fxyGeoCoding.getPixelXFunction().createCFunctionCode("x", "lat", "lon")).append("\n");
        sb.append(fxyGeoCoding.getPixelYFunction().createCFunctionCode("y", "lat", "lon")).append("\n");
        sb.append("\n");
    }

    private void writeMapGeoCoding(MapGeoCoding mgc, String nodeType, StringBuffer sb) {
        final MapInfo mi = mgc.getMapInfo();

        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses a map-projection based geo-coding.\n");
        sb.append("\n");

        sb.append(String.format("%1$-20s \t%2$s\n",
                                "Projection:",
                                mi.getMapProjection().getName()));

        sb.append("Projection parameters:\n");
        final Parameter[] parameters = mi.getMapProjection().getMapTransform().getDescriptor().getParameters();
        final double[] parameterValues = mi.getMapProjection().getMapTransform().getParameterValues();
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            sb.append(String.format("\t%1$-30s \t: %2$s %3$s\n",
                                    parameter.getName(),
                                    String.valueOf(parameterValues[i]),
                                    parameter.getProperties().getPhysicalUnit()));

        }
        sb.append("\n");

        sb.append(String.format("%1$-20s \t%2$s\n",
                                "Map CRS Name:",
                                mgc.getMapCRS().getName()));
        sb.append("Map CRS WKT:\n");
        sb.append(String.format("\t%s\n",
                                mgc.getMapCRS().toWKT().replace("\n", "\n\t")));
        sb.append("\n");

        sb.append("Output parameters:\n");

        sb.append(String.format("\t%1$-30s \t%2$s\n",
                                "Datum:",
                                mi.getDatum().getName()));

        sb.append(String.format("\t%1$-30s \t%2$s\n",
                                "Reference pixel X:",
                                String.valueOf(mi.getPixelX())));

        sb.append(String.format("\t%1$-30s \t%2$s\n",
                                "Reference pixel Y:",
                                String.valueOf(mi.getPixelY())));

        sb.append(String.format("\t%1$-30s \t%2$s %3$s\n",
                                "Orientation:",
                                String.valueOf(mi.getOrientation()),
                                "degree"));

        sb.append(String.format("\t%1$-30s \t%2$s %3$s\n",
                                "Northing:",
                                String.valueOf(mi.getNorthing()),
                                mi.getMapProjection().getMapUnit()));

        sb.append(String.format("\t%1$-30s \t%2$s %3$s\n",
                                "Easting:",
                                String.valueOf(mi.getEasting()),
                                mi.getMapProjection().getMapUnit()));

        sb.append(String.format("\t%1$-30s \t%2$s %3$s\n",
                                "Pixel size X:",
                                String.valueOf(mi.getPixelSizeX()),
                                mi.getMapProjection().getMapUnit()));

        sb.append(String.format("\t%1$-30s \t%2$s %3$s\n",
                                "Pixel size Y:",
                                String.valueOf(mi.getPixelSizeY()),
                                mi.getMapProjection().getMapUnit()));
    }

    private void writePixelGeoCoding(BasicPixelGeoCoding gc, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses a pixel based geo-coding.\n");
        sb.append("\n");

        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Name of latitude band:", gc.getLatBand().getName()));
        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Name of longitude band:", gc.getLonBand().getName()));

        sb.append(String.format("%1$-35s \t%2$d \t%3$s\n",
                                "Search radius:", gc.getSearchRadius(), "pixels"));

        final String validMask = gc.getValidMask();
        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Valid pixel mask:", validMask != null ? validMask : ""));

        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Crossing 180 degree meridian:",
                                String.valueOf(gc.isCrossingMeridianAt180())));

        sb.append("\n");
        sb.append("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
                  "by linear interpolation between pixels.\n");

        sb.append("\n");
        sb.append("Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)\n" +
                  "by a search algorithm.\n");
        sb.append("\n");
    }

    private void writeTiePointGeoCoding(TiePointGeoCoding tgc, String nodeType, StringBuffer sb) {
        sb.append("\n");
        sb.append("\nThe ").append(nodeType).append(" uses a tie-point based geo-coding.\n");
        sb.append("\n");

        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Name of latitude tie-point grid:", tgc.getLatGrid().getName()));
        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Name of longitude tie-point grid:", tgc.getLonGrid().getName()));

        sb.append(String.format("%1$-35s \t%2$s\n",
                                "Crossing 180 degree meridian:",
                                String.valueOf(tgc.isCrossingMeridianAt180())));

        sb.append("\n");
        sb.append("Geographic coordinates (lat,lon) are computed from pixel coordinates (x,y)\n" +
                  "by linear interpolation between tie points.\n");

        final int numApproximations = tgc.getNumApproximations();
        if (numApproximations > 0) {
            sb.append("\n");
            sb.append("Pixel coordinates (x,y) are computed from geographic coordinates (lat,lon)\n" +
                      "by polynomial approximations for ").append(numApproximations).append(" tile(s).\n");
            sb.append("\n");

            for (int i = 0; i < numApproximations; i++) {

                final TiePointGeoCoding.Approximation approximation = tgc.getApproximation(i);
                final FXYSum fX = approximation.getFX();
                final FXYSum fY = approximation.getFY();

                sb.append(
                        "=======================================================================================\n");
                sb.append("Approximation for tile ").append(i + 1).append("\n");
                sb.append(
                        "=======================================================================================\n");

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "Center latitude:",
                                        String.valueOf(approximation.getCenterLat()),
                                        "degree"));

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "Center longitude:",
                                        String.valueOf(approximation.getCenterLon()),
                                        "degree"));

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "RMSE for X:",
                                        String.valueOf(fX.getRootMeanSquareError()),
                                        "pixels"));

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "RMSE for Y:",
                                        String.valueOf(fY.getRootMeanSquareError()),
                                        "pixels"));

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "Max. error for X:",
                                        String.valueOf(fX.getMaxError()),
                                        "pixels"));

                sb.append(String.format("%1$-18s \t%2$s %3$s\n",
                                        "Max. error for Y:",
                                        String.valueOf(fY.getMaxError()),
                                        "pixels"));

                final String xCCode = fX.createCFunctionCode("compute_x", "lat", "lon");
                if (xCCode != null) {
                    sb.append("\n");
                    sb.append(xCCode);
                    sb.append("\n");
                }

                final String yCCode = fY.createCFunctionCode("compute_y", "lat", "lon");
                if (yCCode != null) {
                    sb.append("\n");
                    sb.append(yCCode);
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\n");
            sb.append(
                    "WARNING: Pixel coordinates (x,y) cannot be computed from geographic coordinates (lat,lon)\n" +
                    "because appropriate polynomial approximations could not be found.\n");
        }
    }

    @Override
    protected void initComponents() {
        textArea = new JTextArea();
        textArea.setText(defaultText);
        textArea.setEditable(false);
        textArea.addMouseListener(new PopupHandler());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    @Override
    protected void updateComponents() {
        if (isVisible()) {
            ensureValidData();
            textArea.setText(createText());
            textArea.setCaretPosition(0);
        }
    }

    protected void ensureValidData() {
    }

    @Override
    protected String getDataAsText() {
        return textArea.getText();
    }

    @Override
    protected void handlePopupCreated(final JPopupMenu popupMenu) {
        final JMenuItem menuItem = new JMenuItem("Select All");     /*I18N*/
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                textArea.selectAll();
                textArea.requestFocus();
            }
        });
        popupMenu.add(menuItem);
    }
}
