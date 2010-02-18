/*
 * $Id: AtsrGSSTFile.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.atsr;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

/**
 * An ERS ATSR file specialization for ATSR GSST products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public class AtsrGSSTFile extends AtsrFile {

    private int _nadirSSTOffset;
    private int _dualSSTOffset;
    private int _confidenceSSTOffset;
    private int _latOffset;
    private int _lonOffset;
    private int _nadirXOffset;
    private int _nadirYOffset;
    private int _forwardXOffset;
    private int _forwardYOffset;
    private int _nadirFlagsOffset;
    private int _forwardFlagsOffset;
    private FlagCoding _flagCodingNadir;
    private FlagCoding _flagCodingForward;
    private FlagCoding _flagCodingConfidence;

    /**
     * Opens the ATSR GSST file at the given location.
     */
    @Override
    public void open(ImageInputStream inStream, File file) throws IOException {
        super.open(inStream, file);

        scanBands();
        createFlagBands();
        calculateFileOffsets();
        createBandReader();
        readLatLonTiePoints(_latOffset, _lonOffset);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Scans the product file for contained bands.
     */
    private void scanBands() {

        createSSTBands();

        if (getHeader().isXYPresent()) {
            createNadirOffsetBands();
            if (!getHeader().isNadirOnly()) {
                createForwardOffsetBands();
            }
        }
    }

    /**
     * Creates the sst bands and the confidence band and adds them to the bands vector.
     */
    private void createSSTBands() {
        Band band = null;

        band = new Band(AtsrGSSTConstants.NADIR_SST_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.SST_UNIT);
        band.setDescription(AtsrGSSTConstants.NADIR_SST_DESCRIPTION);
        addBand(band);

        band = new Band(AtsrGSSTConstants.DUAL_SST_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.SST_UNIT);
        band.setDescription(AtsrGSSTConstants.DUAL_SST_DESCRIPTION);
        addBand(band);
    }

    /**
     * Creates the nadir view coordinate offset bands and adds them to the bands vector.
     */
    private void createNadirOffsetBands() {
        Band band = null;

        band = new Band(AtsrGSSTConstants.NADIR_X_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGSSTConstants.NADIR_X_OFFS_DESCRIPTION);
        addBand(band);

        band = new Band(AtsrGSSTConstants.NADIR_Y_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGSSTConstants.NADIR_Y_OFFS_DESCRIPTION);
        addBand(band);
    }

    /**
     * Creates the forward view coordinate offset bands and adds them to the bands vector.
     */
    private void createForwardOffsetBands() {
        Band band = null;

        band = new Band(AtsrGSSTConstants.FORWARD_X_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGSSTConstants.FORWARD_X_OFFS_DESCRIPTION);
        addBand(band);

        band = new Band(AtsrGSSTConstants.FORWARD_Y_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGSSTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGSSTConstants.FORWARD_Y_OFFS_DESCRIPTION);
        addBand(band);
    }

    /**
     * Creates the flag bands and the associated coding.
     */
    private void createFlagBands() {
        Band band = null;

        createFlagCodings();

        band = new Band(AtsrGSSTConstants.SST_CONFIDENCE_NAME, ProductData.TYPE_INT16,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setDescription(AtsrGSSTConstants.SST_CONFIDENCE_DESCRIPTION);
        band.setFlagCoding(_flagCodingConfidence);
        addBand(band);

        if (getHeader().areFlagsPresent()) {
            band = new Band(AtsrConstants.NADIR_FLAGS_NAME, ProductData.TYPE_INT16,
                            AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
            band.setDescription(AtsrConstants.NADIR_FLAGS_DESCRIPTION);
            band.setFlagCoding(_flagCodingNadir);
            addBand(band);

            if (!getHeader().isNadirOnly()) {
                band = new Band(AtsrConstants.FORWARD_FLAGS_NAME, ProductData.TYPE_INT16,
                                AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
                band.setDescription(AtsrConstants.FORWARD_FLAGS_DESCRIPTION);
                band.setFlagCoding(_flagCodingForward);
                addBand(band);
            }
        }
    }

    /**
     * Creates the flag codings for nadir and (optionally) forward bands
     */
    private void createFlagCodings() {
        _flagCodingConfidence = new FlagCoding(AtsrGSSTConstants.CONFIDENCE_FLAGS_NAME);
        _flagCodingConfidence = addConfidenceFlagsToCoding(_flagCodingConfidence);

        if (getHeader().areFlagsPresent()) {
            _flagCodingNadir = new FlagCoding(AtsrConstants.NADIR_FLAGS_NAME);
            _flagCodingNadir = addCloudAndLandFlagsToCoding(_flagCodingNadir);

            if (!getHeader().isNadirOnly()) {
                _flagCodingForward = new FlagCoding(AtsrConstants.FORWARD_FLAGS_NAME);
                _flagCodingForward = addCloudAndLandFlagsToCoding(_flagCodingForward);
            }
        }
    }

    /**
     * Adds the confidence flags to the coding passed in.
     */
    private FlagCoding addConfidenceFlagsToCoding(FlagCoding coding) {
        coding.addFlag(AtsrGSSTConstants.NADIR_SST_VALID_FLAG_NAME, AtsrGSSTConstants.NADIR_SST_VALID_FLAG_MASK,
                       AtsrGSSTConstants.NADIR_SST_VALID_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.NADIR_SST_37_FLAG_NAME, AtsrGSSTConstants.NADIR_SST_37_FLAG_MASK,
                       AtsrGSSTConstants.NADIR_SST_37_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.DUAL_SST_VALID_FLAG_NAME, AtsrGSSTConstants.DUAL_SST_VALID_FLAG_MASK,
                       AtsrGSSTConstants.DUAL_SST_VALID_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.DUAL_SST_37_FLAG_NAME, AtsrGSSTConstants.DUAL_SST_37_FLAG_MASK,
                       AtsrGSSTConstants.DUAL_SST_37_FLAG_DESCRIPTION);

        coding.addFlag(AtsrGSSTConstants.LAND_FLAG_NAME, AtsrGSSTConstants.LAND_FLAG_MASK,
                       AtsrGSSTConstants.LAND_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.NADIR_CLOUDY_FLAG_NAME, AtsrGSSTConstants.NADIR_CLOUDY_FLAG_MASK,
                       AtsrGSSTConstants.NADIR_CLOUDY_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.NADIR_BLANKING_FLAG_NAME, AtsrGSSTConstants.NADIR_BLANKING_FLAG_MASK,
                       AtsrGSSTConstants.NADIR_BLANKING_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.NADIR_COSMETIC_FLAG_NAME, AtsrGSSTConstants.NADIR_COSMETIC_FLAG_MASK,
                       AtsrGSSTConstants.NADIR_COSMETIC_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_NAME, AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_MASK,
                       AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.FORWARD_BLANKING_FLAG_NAME, AtsrGSSTConstants.FORWARD_BLANKING_FLAG_MASK,
                       AtsrGSSTConstants.FORWARD_BLANKING_FLAG_DESCRIPTION);
        coding.addFlag(AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_NAME, AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_MASK,
                       AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_DESCRIPTION);
        return coding;
    }

    /**
     * Calculates the offsets of the bands present into the file as bytes.
     */
    private void calculateFileOffsets() {
        int runningOffset = AtsrConstants.SADIST_2_HEAER_SIZE;

        _nadirSSTOffset = runningOffset;
        runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.SST_PIXEL_SIZE;
        _dualSSTOffset = runningOffset;
        runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.SST_PIXEL_SIZE;
        _confidenceSSTOffset = runningOffset;
        runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.SST_PIXEL_SIZE;

        if (getHeader().isLatLonPresent()) {
            _latOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.LATLON_PIXEL_SIZE;
            _lonOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.LATLON_PIXEL_SIZE;
        }

        if (getHeader().isXYPresent()) {
            _nadirXOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
            _nadirYOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
            _forwardXOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
            _forwardYOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
        }

        if (getHeader().areFlagsPresent()) {
            _nadirFlagsOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.FLAGS_PIXEL_SIZE;
            _forwardFlagsOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.FLAGS_PIXEL_SIZE;
        }
    }

    /**
     * Creates the band readers for the gsst file
     */
    private void createBandReader() {
        AtsrBandReader reader = null;

        // sst nadir
        reader = new AtsrShortBandReader(AtsrGSSTConstants.NADIR_SST_NAME, _nadirSSTOffset,
                                         AtsrGSSTConstants.SST_FACTOR, getStream());
        addBandReader(reader);

        // sst dual
        reader = new AtsrShortBandReader(AtsrGSSTConstants.DUAL_SST_NAME, _dualSSTOffset,
                                         AtsrGSSTConstants.SST_FACTOR, getStream());
        addBandReader(reader);

        // confidence flags
        reader = new AtsrFlagBandReader(AtsrGSSTConstants.SST_CONFIDENCE_NAME, _confidenceSSTOffset,
                                        1, getStream());
        addBandReader(reader);

        if (getHeader().isXYPresent()) {
            reader = new AtsrByteBandReader(AtsrGSSTConstants.NADIR_X_OFFS_NAME, _nadirXOffset,
                                            AtsrGSSTConstants.COORDINATE_OFFSET_FACTOR, getStream());
            addBandReader(reader);

            reader = new AtsrByteBandReader(AtsrGSSTConstants.NADIR_Y_OFFS_NAME, _nadirYOffset,
                                            AtsrGSSTConstants.COORDINATE_OFFSET_FACTOR, getStream());
            addBandReader(reader);

            if (!getHeader().isNadirOnly()) {
                reader = new AtsrByteBandReader(AtsrGSSTConstants.FORWARD_X_OFFS_NAME, _forwardXOffset,
                                                AtsrGSSTConstants.COORDINATE_OFFSET_FACTOR, getStream());
                addBandReader(reader);

                reader = new AtsrByteBandReader(AtsrGSSTConstants.FORWARD_Y_OFFS_NAME, _forwardYOffset,
                                                AtsrGSSTConstants.COORDINATE_OFFSET_FACTOR, getStream());
                addBandReader(reader);
            }
        }

        if (getHeader().areFlagsPresent()) {
            reader = new AtsrFlagBandReader(AtsrConstants.NADIR_FLAGS_NAME, _nadirFlagsOffset,
                                            1, getStream());
            addBandReader(reader);

            if (!getHeader().isNadirOnly()) {
                reader = new AtsrFlagBandReader(AtsrConstants.FORWARD_FLAGS_NAME, _forwardFlagsOffset,
                                                1, getStream());
                addBandReader(reader);
            }
        }
    }
}
