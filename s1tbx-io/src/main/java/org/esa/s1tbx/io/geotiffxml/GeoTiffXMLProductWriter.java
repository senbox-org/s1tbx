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
package org.esa.s1tbx.io.geotiffxml;

import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriter;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The product writer for SAFE products.
 */
public class GeoTiffXMLProductWriter extends GeoTiffProductWriter {

    /**
     * Construct a new instance of a product writer for the given product writer plug-in.
     *
     * @param writerPlugIn the given product writer plug-in, must not be <code>null</code>
     */
    public GeoTiffXMLProductWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        super.writeProductNodesImpl();

        writeMetadataXML();
    }

    private void writeMetadataXML() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(getSourceProduct());
        final Path path = ReaderUtils.getPathFromInput(getOutput());
        final Path xmlFile = path.getParent().resolve(FileUtils.getFilenameWithoutExtension(path.getFileName().toString()) + ".xml");
        AbstractMetadataIO.saveExternalMetadata(getSourceProduct(), absRoot, xmlFile.toFile());
    }
}
