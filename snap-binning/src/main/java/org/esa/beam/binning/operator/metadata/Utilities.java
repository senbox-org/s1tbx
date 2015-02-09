package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;

class Utilities {

    static MetadataElement createSourceMetaElement(String productName, int index) {
        final MetadataElement productElement = new MetadataElement("source." + Integer.toString(index));
        final MetadataAttribute nameAttribute = new MetadataAttribute("name", new ProductData.ASCII(productName), true);
        productElement.addAttribute(nameAttribute);
        return productElement;
    }

    static MetadataElement getProcessingGraphElement(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        return metadataRoot.getElement("Processing_Graph");
    }

    static String extractProductName(Product product) {
        String productName;

        final MetadataElement processingGraphElement = getProcessingGraphElement(product);
        productName = extractProductName(processingGraphElement);
        if (StringUtils.isNullOrEmpty(productName)) {
            productName = product.getName();
        }

        return productName;
    }

    static String extractProductName(MetadataElement processingGraphElement) {
        if (processingGraphElement != null) {
            final MetadataElement nodeElement = processingGraphElement.getElement("node.0");
            if (nodeElement != null) {
                final MetadataElement sourcesElement = nodeElement.getElement("sources");
                if (sourcesElement != null) {
                    final MetadataAttribute sourceProductAttribute = sourcesElement.getAttribute("sourceProduct");
                    if (sourceProductAttribute != null) {
                         return sourceProductAttribute.getData().getElemString();
                    }
                }
            }
        }
        return null;
    }
}
