/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Thomas Storm
 */
class LandsatMetadataFactory {

    static LandsatMetadata create(File mtlFile) throws IOException {
        LandsatLegacyMetadata landsatMetadata = new LandsatLegacyMetadata(new FileReader(mtlFile));
        if (landsatMetadata.isLegacyFormat()) {
            // legacy format case
            if (landsatMetadata.isLandsatTM() || landsatMetadata.isLandsatETM_Plus()) {
                return landsatMetadata;
            } else {
                throw new ProductIOException("Product is of a legacy landsat format, not a legacy Landsat5 or Landsat7 ETM+ product.");
            }
        } else {
            // new format case
            BufferedReader reader = null;
            try {
                FileReader fileReader = new FileReader(mtlFile);
                reader = new BufferedReader(fileReader);
                String line = reader.readLine();
                while (line != null) {
                    if (line.contains("SPACECRAFT_ID")) {
                        if (line.contains("LANDSAT_4") ||line.contains("LANDSAT_5") || line.contains("LANDSAT_7")) {
                            return new LandsatReprocessedMetadata(new FileReader(mtlFile));
                        } else if (line.contains("LANDSAT_8")) {
                            return new Landsat8Metadata(new FileReader(mtlFile));
                        }
                    }
                    line = reader.readLine();
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            throw new IllegalStateException(
                    "File '" + mtlFile + "' does not contain spacecraft information. (Field 'SPACECRAFT_ID' missing)");
        }
    }
}
