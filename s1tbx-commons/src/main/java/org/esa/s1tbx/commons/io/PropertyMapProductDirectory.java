/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.io;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;

public abstract class PropertyMapProductDirectory extends AbstractProductDirectory {

    protected PropertyMap propertyMap = new DefaultPropertyMap();

    public PropertyMapProductDirectory(final File headerFile) {
        super(headerFile);
    }

    public void readProductDirectory() throws IOException {
        final File headerFile = getFile(getRootFolder() + getHeaderFileName());
        propertyMap.load(headerFile.toPath());
    }

    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        AbstractMetadataIO.AddXMLMetadata(propertyMapToXML("ProductMetadata", propertyMap),
                AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }

    protected Element propertyMapToXML(final String elementName, final PropertyMap propertyMap) {
        Element root = new Element(elementName);
        for (String key : propertyMap.getPropertyKeys()) {
            root.setAttribute(key, propertyMap.getPropertyString(key));
        }
        return root;
    }

    protected abstract void addAbstractedMetadataHeader(final MetadataElement root) throws IOException;
}
