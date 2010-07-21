/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
