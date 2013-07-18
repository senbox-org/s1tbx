/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.dataio.ProductIOException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Thomas Storm
 */
interface LandsatMetadataFactory {

    public LandsatMetadata create(File mtlFile) throws IOException;

    class Landsat57MetadataFactory implements LandsatMetadataFactory {

        public LandsatMetadata create(File mtlFile) throws IOException {
            Landsat57Metadata landsatMetadata = new Landsat57Metadata(new FileReader(mtlFile));
            if (!(landsatMetadata.isLandsatTM() || landsatMetadata.isLandsatETM_Plus())) {
                throw new ProductIOException("Product is not a 'Landsat5' or 'Landsat7' product.");
            }
            return landsatMetadata;
        }
    }

    class Landsat8MetadataFactory implements LandsatMetadataFactory {

        public LandsatMetadata create(File mtlFile) throws IOException {
            return new Landsat8Metadata(new FileReader(mtlFile));
        }
    }

}
