package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.MetadataElement;

class MetadataNode extends ProductTreeNode {
    private MetadataElement metadataElement;

    MetadataNode(String name, MetadataElement element, ProductTreeNode parent) {
        super(name, element, parent);
        metadataElement = element;
    }

    public MetadataElement getMetadataElement() {
        return metadataElement;
    }

    @Override
    public ProductTreeNode getChildAt(int index) {
        return new MetadataNode(metadataElement.getName(), metadataElement.getElementAt(index), this);
    }

    @Override
    public int getChildCount() {
        return metadataElement.getNumElements();
    }

    @Override
    protected int getIndex(ProductTreeNode child) {
        MetadataNode metadataNode = (MetadataNode) child;
        MetadataElement[] metadataElements = metadataElement.getElements();
        for (int i = 0, metadataElementsLength = metadataElements.length; i < metadataElementsLength; i++) {
            MetadataElement element = metadataElements[i];
            if(element == metadataNode.getMetadataElement()) {
                return i;
            }
        }
        return -1;
    }

}
