/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.util;

import com.vividsolutions.jts.geom.Envelope;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Created by luis on 03/05/2016.
 */
public class VectorUtils {

    public static AttributeDescriptorImpl createAttribute(final String name, final Class<?> binding) {
        final NameImpl newAttrName = new NameImpl(name);
        final AttributeTypeImpl newAttrType = new AttributeTypeImpl(newAttrName, binding, false, false, null, null, null);
        return new AttributeDescriptorImpl(newAttrType, newAttrName, 0, 1, true, " ");
    }

    public static SimpleFeatureType createFeatureType(final Product product, final String vectorNodeName,
                                               final List<AttributeDescriptor> attributeDescriptors) {
        final CoordinateReferenceSystem modelCrs = Product.findModelCRS(product.getSceneGeoCoding());
        final SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(modelCrs);

        //copy original descriptors
        for (AttributeDescriptor attributeDescriptor : type.getAttributeDescriptors()) {
            if(!attributeDescriptors.contains(attributeDescriptor)) {
                attributeDescriptors.add(attributeDescriptor);
            }
        }

        return new SimpleFeatureTypeImpl(
                new NameImpl(vectorNodeName),
                attributeDescriptors,
                type.getGeometryDescriptor(),
                type.isAbstract(),
                type.getRestrictions(),
                type.getSuper(),
                type.getDescription());
    }

    public static boolean hasFeatures(final VectorDataNode node) {

        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = node.getFeatureCollection();
        return featureCollection != null && !featureCollection.isEmpty();
    }

    public static String[] getAttributesList(final Product[] products) {

        final ArrayList<String> attributeNames = new ArrayList<>();

        if (products != null && products.length > 0) {

            final ProductNodeGroup<VectorDataNode> vectorGroup = products[0].getVectorDataGroup();
            final int numNodes = vectorGroup.getNodeCount();
            for (int i = 0; i < numNodes; i++) {
                final VectorDataNode vectorDataNode = vectorGroup.get(i);
                if (hasFeatures(vectorDataNode)) {
                    final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorDataNode.getFeatureCollection();
                    final SimpleFeature simpleFeature = featureCollection.features().next();
                    final java.util.List<AttributeDescriptor> attributeDescriptors = simpleFeature.getFeatureType().getAttributeDescriptors();
                    for (AttributeDescriptor ad : attributeDescriptors) {
                        if (!attributeNames.contains(ad.getLocalName())) {
                            attributeNames.add(ad.getLocalName());
                        }
                    }
                }
            }
        }
        return attributeNames.toArray(new String[attributeNames.size()]);
    }

    private static Rectangle2D convertRect(final Rectangle r, final GeoCoding geoCoding) {
        PixelPos p1 = new PixelPos(r.x, r.y);
        PixelPos p2 = new PixelPos(r.x + r.width, r.y);
        PixelPos p3 = new PixelPos(r.x + r.width, r.y + r.height);
        PixelPos p4 = new PixelPos(r.x, r.y + r.height);

        GeoPos g1 = geoCoding.getGeoPos(p1, null);
        GeoPos g2 = geoCoding.getGeoPos(p2, null);
        GeoPos g3 = geoCoding.getGeoPos(p3, null);
        GeoPos g4 = geoCoding.getGeoPos(p4, null);

        // not handling curve
        return new Rectangle2D.Double(g1.getLon(), g1.getLat(), g3.getLon() - g1.getLon(), g3.getLat() - g1.getLat());
    }

    public static VectorDataNode[] getPolygonsForOneRectangle(final Rectangle rectangle,
                                                              final GeoCoding geoCoding,
                                                              final VectorDataNode[] polygonVectorDataNodes) {

        final ArrayList<VectorDataNode> list = new ArrayList<>();
        final CoordinateReferenceSystem crs = geoCoding.getMapCRS();

        Rectangle2D refRect = rectangle;
        if(geoCoding instanceof CrsGeoCoding) {
            refRect = convertRect(rectangle, geoCoding);
        }
        final Envelope recEnv = new ReferencedEnvelope(refRect, crs);

        for (VectorDataNode node : polygonVectorDataNodes) {
            final ReferencedEnvelope nodeEnv = node.getEnvelope();
            if (recEnv.intersects(nodeEnv)) {
                list.add(node);
            }
        }

        VectorDataNode[] array = new VectorDataNode[list.size()];
        array = list.toArray(array);

        return array;
    }

    public static VectorDataNode[] getPolygonsForOneRectangle(final Rectangle rectangle,
                                                               final CoordinateReferenceSystem crs,
                                                               final VectorDataNode[] polygonVectorDataNodes) {

        final ArrayList<VectorDataNode> list = new ArrayList<>();
        final ReferencedEnvelope recEnv = new ReferencedEnvelope(rectangle, crs);

        for (VectorDataNode node : polygonVectorDataNodes) {
            final ReferencedEnvelope nodeEnv = node.getEnvelope();
            if (nodeEnv == null) {
                continue;
            }
            try {
                final BoundingBox bbox2 = nodeEnv.toBounds(recEnv.getCoordinateReferenceSystem());
                final BoundingBox bbox = recEnv.toBounds(nodeEnv.getCoordinateReferenceSystem());
                /*
                SystemUtils.LOG.info("rec transformed bounds " + bbox);
                SystemUtils.LOG.info("polygon " + node.getName() + " env minX = " + nodeEnv.getMinX() + " maxX = " + nodeEnv.getMaxX() +
                        " minY = " + nodeEnv.getMinY() + " maxY = " + nodeEnv.getMinY());
                        */
                if (((Envelope) recEnv).intersects(new ReferencedEnvelope(bbox))) {
                    list.add(node);
                }
            } catch (Throwable e) {
                SystemUtils.LOG.info("getPolygonsForOneRectangle: caught exception: " + e.getMessage());
            }
        }

        return list.toArray(new VectorDataNode[list.size()]);
    }

    public static Double getAttribDoubleValue(final VectorDataNode node, final String attribName) {
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection =
                node.getFeatureCollection();
        final SimpleFeature simpleFeature = featureCollection.features().next();
        String valStr = String.valueOf(simpleFeature.getAttribute(attribName)).trim();
        try {
            return Double.parseDouble(valStr);
        } catch (NumberFormatException ignored) {
        }
        return Double.NaN;
    }

    public static String getAttribStringValue(final VectorDataNode node, final String attribName) {
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection =
                node.getFeatureCollection();
        final SimpleFeature simpleFeature = featureCollection.features().next();
        String valStr = String.valueOf(simpleFeature.getAttribute(attribName)).trim();
        if(StringUtils.isIntegerString(valStr)) {
            valStr = StringUtils.padNum(Integer.parseInt(valStr), 5, ' ');
        } else if(StringUtils.isNumeric(valStr, Double.class)) {
            Integer valInt = (int) quantize(Double.parseDouble(valStr), 0, 200, 10);
            valStr = StringUtils.padNum(valInt, 5, '0');
        }
        return valStr;
    }

    public static double quantize(double val, double min, double max, double stepSize) {
        double quantizedVal = val;
        if (quantizedVal < min) {
            quantizedVal = min;
        } else if (quantizedVal > max) {
            quantizedVal = max;
        } else {
            quantizedVal = ((double) Math.round(val / stepSize)) * stepSize;
        }
        return quantizedVal;
    }
}
