/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.io.terrasarx.TerraSarXProductDirectory;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Element;

import java.io.*;

/**
 * This class represents a product directory.
 */
public class PazProductDirectory extends TerraSarXProductDirectory {


    public PazProductDirectory(final File inputFile) {
        super(inputFile);
    }

    @Override
    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }

    @Override
    protected void findImages(final MetadataElement newRoot) throws IOException {
        final String parentPath = getRelativePathToImageFolder();
        findImages(parentPath, newRoot);
    }

    @Override
    protected String getMission() {
        return "PAZ";
    }
}
