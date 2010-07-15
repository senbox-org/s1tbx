package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.MetadataElement;

class MetadataTN extends ProductNodeTN {
    private MetadataElement metadataElement;

    MetadataTN(String name, MetadataElement element, AbstractTN parent) {
        super(name, element, parent);
        metadataElement = element;
    }

    public MetadataElement getMetadataElement() {
        return metadataElement;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        return new MetadataTN(metadataElement.getName(), metadataElement.getElementAt(index), this);
    }

    @Override
    public int getChildCount() {
        return metadataElement.getNumElements();
    }

    @Override
    protected int getIndex(AbstractTN child) {
        if (child instanceof MetadataTN) {
            MetadataTN metadataTN = (MetadataTN) child;
            MetadataElement[] metadataElements = metadataElement.getElements();
            for (int i = 0, metadataElementsLength = metadataElements.length; i < metadataElementsLength; i++) {
                MetadataElement element = metadataElements[i];
                if(element == metadataTN.getMetadataElement()) {
                    return i;
                }
            }
        }
        return -1;
    }

}
