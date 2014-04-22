/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.geotiffxml;

import org.esa.beam.dataio.geotiff.GeoTiffProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;

/**
 * The product writer for SAFE products.
 *
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
        File file = ReaderUtils.getFileFromInput(getOutput());
        AbstractMetadata.saveExternalMetadata(getSourceProduct(), absRoot, new File(outputFile.getParentFile(),
                FileUtils.getFilenameWithoutExtension(file.getName())+".xml"));
    }
}