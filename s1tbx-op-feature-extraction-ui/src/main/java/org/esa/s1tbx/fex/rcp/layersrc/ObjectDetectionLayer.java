/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.fex.rcp.layersrc;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.s1tbx.dat.graphics.GraphicShape;
import org.esa.s1tbx.dat.graphics.GraphicText;
import org.esa.s1tbx.dat.layers.ScreenPixelConverter;
import org.esa.s1tbx.fex.gpf.oceantools.ObjectDiscriminationOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.geotools.feature.FeatureIterator;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows a detected object
 */
public class ObjectDetectionLayer extends Layer {

    private final Product product;
    private final Band band;

    private final List<ObjectDiscriminationOp.ShipRecord> targetList = new ArrayList<>(200);
    private double rangeSpacing;
    private double azimuthSpacing;
    private final static float lineThickness = 2.0f;
    private final static double border = 5.0;
    private final DecimalFormat frmt = new DecimalFormat("0.00");

    public ObjectDetectionLayer(PropertySet configuration) {
        super(LayerTypeRegistry.getLayerType(ObjectDetectionLayerType.class.getName()), configuration);
        setName("Object Detection");
        product = (Product) configuration.getValue("product");
        band = (Band) configuration.getValue("band");

        getPixelSize();

        LoadTargets();
    }

    private void getPixelSize() {
        final MetadataElement absMetadata = AbstractMetadata.getAbstractedMetadata(product);
        if (absMetadata != null) {
            rangeSpacing = absMetadata.getAttributeDouble(AbstractMetadata.range_spacing, 0);
            azimuthSpacing = absMetadata.getAttributeDouble(AbstractMetadata.azimuth_spacing, 0);
        }
    }

    public static File getTargetFile(final Product product) {
        final MetadataElement absMetadata = AbstractMetadata.getAbstractedMetadata(product);
        if (absMetadata != null) {
            final String shipFilePath = absMetadata.getAttributeString(AbstractMetadata.target_report_file, null);
            if (shipFilePath != null) {
                final File file = new File(shipFilePath);
                if (file.exists())
                    return file;
            }
        }
        return null;
    }

    private void LoadTargets() {

        VectorDataNode vectorDataNode = product.getVectorDataGroup().get(ObjectDiscriminationOp.VECTOR_NODE_NAME);
        if (vectorDataNode == null) {
            LoadTargetsFromFile(getTargetFile(product));
            return;
        }

        targetList.clear();
        final FeatureIterator itr = vectorDataNode.getFeatureCollection().features();
        while (itr.hasNext()) {
            final SimpleFeature feature = (SimpleFeature) itr.next();

            Integer x = (Integer)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_X);
            Integer y = (Integer)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_Y);
            Double lat = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LAT);
            if(lat == null) continue;
            Double lon = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LON);
            if(lon == null) continue;
            Double width = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_WIDTH);
            if(width == null) continue;
            Double length = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LENGTH);
            if(length == null) continue;

            ObjectDiscriminationOp.ShipRecord shipRecord = new ObjectDiscriminationOp.ShipRecord(
                    x, y, lat, lon, (width / rangeSpacing) + border, (length / azimuthSpacing) + border);

            Double corr_lat = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_CORR_SHIP_LAT);
            if(corr_lat != null) {
                shipRecord.corr_lat = corr_lat;
            }
            Double corr_lon = (Double)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_CORR_SHIP_LON);
            if(corr_lon != null) {
                shipRecord.corr_lon = corr_lon;
            }

            Integer mmsi = (Integer)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_AIS_MMSI);
            if(mmsi != null) {
                shipRecord.mmsi = mmsi;
            }
            String shipName = (String)feature.getAttribute(ObjectDiscriminationOp.ATTRIB_AIS_SHIP_NAME);
            if(shipName != null) {
                shipRecord.shipName = shipName;
            }

            targetList.add(shipRecord);
        }
    }

    private void LoadTargetsFromFile(final File file) {
        if (file == null)
            return;

        Document doc;
        try {
            doc = XMLSupport.LoadXML(file.getAbsolutePath());
        } catch (IOException e) {
            return;
        }

        targetList.clear();

        final Element root = doc.getRootElement();

        final List<Content> children = root.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element targetsDetectedElem = (Element) aChild;
                if (targetsDetectedElem.getName().equals("targetsDetected")) {
                    final Attribute attrib = targetsDetectedElem.getAttribute("bandName");
                    if (attrib != null ) {
                        final List<Content> content = targetsDetectedElem.getContent();
                        for (Object det : content) {
                            if (det instanceof Element) {
                                final Element targetElem = (Element) det;
                                if (targetElem.getName().equals("target")) {
                                    Attribute x = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_X);
                                    if (x == null) x = new Attribute(ObjectDiscriminationOp.ATTRIB_DETECTED_X, "0");
                                    Attribute y = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_Y);
                                    if (y == null) y = new Attribute(ObjectDiscriminationOp.ATTRIB_DETECTED_Y, "0");

                                    final Attribute lat = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LAT);
                                    if (lat == null) continue;
                                    final Attribute lon = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LON);
                                    if (lon == null) continue;
                                    final Attribute width = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_WIDTH);
                                    if (width == null) continue;
                                    final Attribute length = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_DETECTED_LENGTH);
                                    if (length == null) continue;

                                    ObjectDiscriminationOp.ShipRecord shipRecord = new ObjectDiscriminationOp.ShipRecord(
                                            Integer.parseInt(x.getValue()),
                                            Integer.parseInt(y.getValue()),
                                            Double.parseDouble(lat.getValue()),
                                            Double.parseDouble(lon.getValue()),
                                            (Double.parseDouble(width.getValue()) / rangeSpacing) + border,
                                            (Double.parseDouble(length.getValue()) / azimuthSpacing) + border);

                                    final Attribute corr_lat = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_CORR_SHIP_LAT);
                                    if (corr_lat != null) {
                                        shipRecord.corr_lat = Double.parseDouble(corr_lat.getValue());
                                    }
                                    final Attribute corr_lon = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_CORR_SHIP_LON);
                                    if (corr_lon != null) {
                                        shipRecord.corr_lon = Double.parseDouble(corr_lon.getValue());
                                    }
                                    final Attribute mmsi = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_AIS_MMSI);
                                    if (mmsi != null) {
                                        shipRecord.mmsi = Integer.parseInt(mmsi.getValue());
                                    }
                                    final Attribute shipName = targetElem.getAttribute(ObjectDiscriminationOp.ATTRIB_AIS_SHIP_NAME);
                                    if (shipName != null) {
                                        shipRecord.shipName = shipName.getValue();
                                    }

                                    targetList.add(shipRecord);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {

        if (band == null || targetList.isEmpty())
            return;

        final Viewport vp = rendering.getViewport();
        final RasterDataNode raster = product.getRasterDataNode(product.getBandAt(0).getName());
        final ScreenPixelConverter screenPixel = new ScreenPixelConverter(vp, raster);

        if (!screenPixel.withInBounds()) {
            return;
        }

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos geo = new GeoPos();
        final PixelPos pix = new PixelPos();

        final Graphics2D graphics = rendering.getGraphics();
        graphics.setStroke(new BasicStroke(lineThickness));
        GraphicText.setHighQuality(graphics);

        for (ObjectDiscriminationOp.ShipRecord target : targetList) {
            geo.setLocation(target.lat, target.lon);
            geoCoding.getPixelPos(geo, pix);
            if(!pix.isValid())
                continue;

            Point.Double p = GraphicShape.drawCircle(graphics, screenPixel, pix.getX(), pix.getY(), (int)target.length, Color.RED);

            geo.setLocation(target.corr_lat, target.corr_lon);
            geoCoding.getPixelPos(geo, pix);
            if(pix.isValid()) {
                GraphicShape.drawX(graphics, screenPixel, pix.getX(), pix.getY(), 8, Color.RED);
                if(target.shipName != null && !target.shipName.isEmpty()) {
                    GraphicText.outlineText(graphics, Color.RED, target.shipName, (int) p.x, (int) p.y);
                }
            }

            //final double targetWidthInMeter = (target.width - border) * rangeSpacing;
            final double targetLengthInMeter = (target.length - border) * azimuthSpacing;
            //final double size = Math.sqrt(targetWidthInMeter * targetWidthInMeter + targetLengthInMeter * targetLengthInMeter);
            final double size = targetLengthInMeter;
            graphics.drawString(frmt.format(size) + 'm', (int) p.x, (int) p.y);
        }
    }
}
