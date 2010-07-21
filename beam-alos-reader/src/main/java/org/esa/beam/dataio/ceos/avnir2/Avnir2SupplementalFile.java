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

package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * * This class represents a supplemental file of an Avnir-2 product.
 *
 * @author Marco Peters
 */
class Avnir2SupplementalFile {

    private static final String META_ELEMENT_NAME = "Supplemental";

    private CeosFileReader _ceosReader;

    public Avnir2SupplementalFile(final ImageInputStream supplementalStream) {
        _ceosReader = new CeosFileReader(supplementalStream);
    }

    public MetadataElement getAsMetadata() {
        final MetadataElement root = new MetadataElement(META_ELEMENT_NAME);
        addPCDData(root);
        return root;
    }

    private void addPCDData(final MetadataElement root) {
    }

    public void close() throws IOException {
        _ceosReader.close();
        _ceosReader = null;
    }
}
