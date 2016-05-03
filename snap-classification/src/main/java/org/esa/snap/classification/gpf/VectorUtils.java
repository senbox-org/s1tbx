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
package org.esa.snap.classification.gpf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.util.ArrayList;

/**
 * Created by luis on 03/05/2016.
 */
public class VectorUtils {

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
}
