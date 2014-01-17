/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.fuzzykmeans;
import org.esa.beam.framework.datamodel.*;

import java.text.NumberFormat;


class ClusterMetaDataUtils {

    public static void addCenterToIndexCoding(IndexCoding indexCoding, Band[] sourceBands, double[][] means) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(3);
        final int numAttributes = indexCoding.getNumAttributes();
        for (int i = 0; i < numAttributes; i++) {
            MetadataAttribute attribute = indexCoding.getAttributeAt(i);
            String description = "Cluster " + i + ", Center(";
            for (int j = 0; j < sourceBands.length; j++) {
                String number = numberFormat.format(means[i][j]);
                description += sourceBands[j].getName() + "=" + number;
                if (j != sourceBands.length - 1) {
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
            MetadataElement element = new MetadataElement("class." + i);
            for (int j = 0; j < sourceBands.length; j++) {
                ProductData pData = ProductData.createInstance(new double[]{means[i][j]});
                MetadataAttribute metadataAttribute = new MetadataAttribute(
                        "cluster_center." + sourceBands[j].getName(), pData, true);
                element.addAttribute(metadataAttribute);
            }
            clusterAnalysis.addElement(element);
        }
    }

    public static void addEMInfoToMetadata(MetadataElement clusterAnalysis,
                                           double[][][] covariances, double[] priorProbabilities) {
        int numElements = clusterAnalysis.getNumElements();
        for (int i = 0; i < numElements; i++) {
            MetadataElement element = clusterAnalysis.getElementAt(i);

            double[][] covariance = covariances[i];
            for (int k = 0; k < covariance.length; k++) {
                ProductData cData = ProductData.createInstance(covariance[k]);
                MetadataAttribute cAttribute = new MetadataAttribute("covariance." + k, cData, true);
                element.addAttribute(cAttribute);
            }

            ProductData pData = ProductData.createInstance(new double[]{priorProbabilities[i]});
            MetadataAttribute pAttribute = new MetadataAttribute("prior_probability", pData, true);
            element.addAttribute(pAttribute);
        }

    }

}
