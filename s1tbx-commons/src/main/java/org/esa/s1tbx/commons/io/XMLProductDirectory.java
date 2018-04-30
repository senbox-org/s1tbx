/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class represents a product directory.
 */
public abstract class XMLProductDirectory extends AbstractProductDirectory {

    protected Document xmlDoc = null;

    protected XMLProductDirectory(final File inputFile) {
        super(inputFile);
    }

    public void readProductDirectory() throws IOException {
        try (final InputStream is = getInputStream(getRootFolder() + getHeaderFileName())) {
            xmlDoc = XMLSupport.LoadXML(is);
        }
    }

    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }
}
