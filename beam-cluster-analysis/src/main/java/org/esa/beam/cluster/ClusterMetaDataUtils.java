/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import java.text.NumberFormat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;


class ClusterMetaDataUtils {
    
    public static void addCenterToIndexCoding(IndexCoding indexCoding, Band[] sourceBands, double[][] means) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(3);
        final int numAttributes = indexCoding.getNumAttributes();
        for (int i = 0; i < numAttributes; i++) {
            MetadataAttribute attribute = indexCoding.getAttributeAt(i);
            String description = "Cluster " + i + ", Center=(";
            for (int j = 0; j < sourceBands.length; j++) {
                String number = numberFormat.format(means[i][j]);
                description += sourceBands[j].getName()+"="+number;
                if (j != sourceBands.length-1) {
                    description += ", ";
                }
            }
            description += ")";
            attribute.setDescription(description);
        }
    }

    public static void addCenterToMetadata(
            MetadataElement clusterAnalysis, Band[] sourceBands, double[][] means) {
        
        for (int i = 0; i < means.length; i++) {
            MetadataElement element = new MetadataElement("cluster_"+i);
            for (int j = 0; j < sourceBands.length; j++) {
                ProductData pData = ProductData.createInstance(new double[] {means[i][j]});
                MetadataAttribute metadataAttribute = new MetadataAttribute("center."+sourceBands[j].getName(), pData , true);
                element.addAttribute(metadataAttribute);
            }
            clusterAnalysis.addElement(element);
        }
    }

}
