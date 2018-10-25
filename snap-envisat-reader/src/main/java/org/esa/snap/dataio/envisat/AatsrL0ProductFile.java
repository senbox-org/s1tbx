/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.esa.snap.dataio.envisat.EnvisatProductReader.createDatasetTable;
import static org.esa.snap.dataio.envisat.EnvisatProductReader.createMetadataElement;
import static org.esa.snap.dataio.envisat.EnvisatProductReader.createMetadataTableGroup;


/**
 * The <code>AatsrL0ProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * AATSR Level 0 data products.
 *
 * @author Marco Peters
 */
public class AatsrL0ProductFile extends ProductFile {
    private static final String PARSE_SPH_ERR_MSG = "Failed to parse main header parameter '%s': %s";
    private static final String DS_NAME_AATSR_SOURCE_PACKETS = "AATSR_SOURCE_PACKETS";

    /**
     * Constructs a <code>AatsrL0ProductFile</code> for the given seekable data input stream.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws IOException if an I/O error occurs
     */
    AatsrL0ProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        try {
            return getMPH().getParamUTC(KEY_SENSING_START);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(String.format(PARSE_SPH_ERR_MSG, KEY_SENSING_START, e.getMessage()));
            return null;
        }
    }

    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        try {
            return getMPH().getParamUTC(KEY_SENSING_STOP);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(String.format(PARSE_SPH_ERR_MSG, KEY_SENSING_STOP, e.getMessage()));
            return null;
        }
    }

    @Override
    public int getSceneRasterWidth() {
        return 0;
    }

    @Override
    public int getSceneRasterHeight() {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return 1;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 1;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return true;
    }

    @Override
    public String getGADSName() {
        return null;
    }

    @Override
    protected void postProcessSPH(Map parameters) throws IOException {
        // TODO - ???
    }


    @Override
    void setInvalidPixelExpression(Band band) {
    }

    /**
     * Returns an array containing the center wavelengths for all bands in the AATSR product (in nm).
     */
    @Override
    public float[] getSpectralBandWavelengths() {
        return new float[0];
    }

    /**
     * Returns an array containing the bandwidth for each band in nm.
     */
    @Override
    public float[] getSpectralBandBandwidths() {
        return new float[0];
    }

    /**
     * Returns an array containing the solar spectral flux for each band.
     */
    @Override
    public float[] getSpectralBandSolarFluxes() {
        return new float[0];
    }

    @Override
    public Mask[] createDefaultMasks(String dsName) {
        return new Mask[0];
    }


    @Override
    protected void addCustomMetadata(Product product) throws IOException {
        MetadataElement root = product.getMetadataRoot();
        String datasetName = DS_NAME_AATSR_SOURCE_PACKETS;
        final RecordReader recordReader = getRecordReader(datasetName);
        root.addElement(createMetadataElement(datasetName,recordReader));
    }

}
