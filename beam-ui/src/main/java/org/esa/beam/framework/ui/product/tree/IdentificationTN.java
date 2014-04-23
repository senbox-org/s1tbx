/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.framework.datamodel.MetadataAttribute;

class IdentificationTN extends AbstractTN {
    private MetadataElement metadataElement;

    IdentificationTN(String name, MetadataElement element, AbstractTN parent) {
        super(name, element, parent);

		//todo hard coded strings to be replaced with constants once abstracted metadata is rolled into BEAM
        final MetadataElement absRoot = element.getElement("Abstracted_Metadata");
        if (absRoot != null)
            metadataElement = createIdentificationNodes(element, absRoot);
        else
            metadataElement = element;
        metadataElement.setOwner(element.getProduct());
    }

    public MetadataElement getMetadataElement() {
        return metadataElement;
    }

    @Override
    public AbstractTN getChildAt(int index) {
        final MetadataElement elem = metadataElement.getElementAt(index);
        return new IdentificationTN(elem.getName(), elem, this);
    }

    @Override
    public int getChildCount() {
        return metadataElement.getNumElements();
    }

    @Override
    protected int getIndex(AbstractTN child) {
        IdentificationTN metadataNode = (IdentificationTN) child;
        MetadataElement[] metadataElements = metadataElement.getElements();
        for (int i = 0, metadataElementsLength = metadataElements.length; i < metadataElementsLength; i++) {
            MetadataElement element = metadataElements[i];
            if (element == metadataNode.getMetadataElement()) {
                return i;
            }
        }
        return -1;
    }

    private static MetadataElement createIdentificationNodes(final MetadataElement rootElement, final MetadataElement absRoot) {
        final MetadataElement identNode = new MetadataElement("Identification");

        addIDNode(absRoot, identNode, "Mission", "MISSION");
        addIDNode(absRoot, identNode, "Type", "PRODUCT_TYPE");
        addIDNode(absRoot, identNode, "Acquisition", "first_line_time");
        addIDNode(absRoot, identNode, "Pass", "PASS");
        addIDNode(absRoot, identNode, "Track", "REL_ORBIT");
        addIDNode(absRoot, identNode, "Orbit", "ABS_ORBIT");

        final MetadataElement slaveRoot = rootElement.getElement("Slave Metadata");
        if (slaveRoot != null) {
            for (MetadataElement slvElem : slaveRoot.getElements()) {
                final MetadataElement slvNode = new MetadataElement(slvElem.getName());
                addIDNode(slvElem, slvNode, "Mission", "MISSION");
                addIDNode(slvElem, slvNode, "Type", "PRODUCT_TYPE");
                addIDNode(slvElem, slvNode, "Acquisition", "first_line_time");
                addIDNode(slvElem, slvNode, "Pass", "PASS");
                addIDNode(slvElem, slvNode, "Track", "REL_ORBIT");
                addIDNode(slvElem, slvNode, "Orbit", "ABS_ORBIT");
                identNode.addElement(slvNode);
            }
        }
        return identNode;
    }

    private static void addIDNode(final MetadataElement absRoot, final MetadataElement identNode,
                                  final String title, final String tag) {
        final MetadataAttribute attrib = absRoot.getAttribute(tag);
        if (attrib == null) return;
        final String value = title + ": " + attrib.getData().getElemString();
        final MetadataElement newAttrib = new MetadataElement(value);
        identNode.addElement(newAttrib);
    }

}