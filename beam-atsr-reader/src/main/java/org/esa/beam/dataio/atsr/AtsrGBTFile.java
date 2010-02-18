/*
 * $Id: AtsrGBTFile.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
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
 * An ERS ATSR file specialization for ATSR GBT products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public class AtsrGBTFile extends AtsrFile {

    private int _nadir1200offset;
    private int _nadir1100offset;
    private int _nadir370offset;
    private int _nadir1600offset;
    private int _nadir870offset;
    private int _nadir650offset;
    private int _nadir550offset;
    private int _forward1200offset;
    private int _forward1100offset;
    private int _forward370offset;
    private int _forward1600offset;
    private int _forward870offset;
    private int _forward650offset;
    private int _forward550offset;
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

    /**
     * Constructs the object with default values.
     */
    public AtsrGBTFile() {
    }

    /**
     * Opens the ATSR GBT file at the given location.
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
        if (getHeader().isThermalPresent()) {
            createNadirThermalBands();
            if (!getHeader().isNadirOnly()) {
                createForwardThermalBands();
            }
        }

        if ((getHeader().isVisiblePresent()) || (getHeader().isThermalPresent())) {
            createNadir1600Band();
            if (!getHeader().isNadirOnly()) {
                createForward1600Band();
            }
        }

        if (getHeader().isVisiblePresent()) {
            createNadirVisibleBands();
            if (!getHeader().isNadirOnly()) {
                createForwardVisibleBands();
            }
        }

        if (getHeader().isXYPresent()) {
            createNadirOffsetBands();
            if (!getHeader().isNadirOnly()) {
                createForwardOffsetBands();
            }
        }
    }

    /**
     * Creates the nadir view thermal bands and adds them to the bands vector.
     */
    private void createNadirThermalBands() {

        Band band = new Band(AtsrGBTConstants.NADIR_1200_BT_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_1200_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_12_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_12_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.NADIR_1100_BT_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_1100_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_11_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_11_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.NADIR_370_BT_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_370_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_37_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_37_WIDTH);
        addBand(band);
    }

    /**
     * Creates the forward view thermal bands and adds them to the bands vector.
     */
    private void createForwardThermalBands() {

        Band band = new Band(AtsrGBTConstants.FORWARD_1200_BT_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_1200_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_12_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_12_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.FORWARD_1100_BT_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_1100_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_11_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_11_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.FORWARD_370_BT_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_370_BT_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_37_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_37_WIDTH);
        addBand(band);
    }

    /**
     * Creates the nadir view visible bands and adds them to the bands vector.
     */
    private void createNadirVisibleBands() {

        Band band = new Band(AtsrGBTConstants.NADIR_870_REF_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_870_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_87_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_87_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.NADIR_650_REF_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_650_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_65_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_65_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.NADIR_550_REF_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_550_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_55_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_55_WIDTH);
        addBand(band);
    }

    /**
     * Creates the forward view visible bands and adds them to the bands vector.
     */
    private void createForwardVisibleBands() {

        Band band = new Band(AtsrGBTConstants.FORWARD_870_REF_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_870_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_87_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_87_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.FORWARD_650_REF_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_650_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_65_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_65_WIDTH);
        addBand(band);

        band = new Band(AtsrGBTConstants.FORWARD_550_REF_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_550_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_55_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_55_WIDTH);
        addBand(band);
    }

    /**
     * Creates the nadir view 1600 nm band
     */
    private void createNadir1600Band() {

        Band band = new Band(AtsrGBTConstants.NADIR_1600_REF_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_1600_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_16_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_16_WIDTH);
        addBand(band);
    }

    /**
     * Creates the forward view 1600 nm band
     */
    private void createForward1600Band() {

        Band band = new Band(AtsrGBTConstants.FORWARD_1600_REF_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.REFLECTANCE_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_1600_REF_DESCRIPTION);
        band.setSpectralWavelength(AtsrConstants.BAND_16_WAVELENGTH);
        band.setSpectralBandwidth(AtsrConstants.BAND_16_WIDTH);
        addBand(band);
    }

    /**
     * Creates the nadir view coordinate offset bands and adds them to the bands vector.
     */
    private void createNadirOffsetBands() {

        Band band = new Band(AtsrGBTConstants.NADIR_X_OFFS_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_X_OFFS_DESCRIPTION);
        addBand(band);

        band = new Band(AtsrGBTConstants.NADIR_Y_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGBTConstants.NADIR_Y_OFFS_DESCRIPTION);
        addBand(band);
    }

    /**
     * Creates the forward view coordinate offset bands and adds them to the bands vector.
     */
    private void createForwardOffsetBands() {

        Band band = new Band(AtsrGBTConstants.FORWARD_X_OFFS_NAME, ProductData.TYPE_FLOAT32,
                             AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_X_OFFS_DESCRIPTION);
        addBand(band);

        band = new Band(AtsrGBTConstants.FORWARD_Y_OFFS_NAME, ProductData.TYPE_FLOAT32,
                        AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        band.setUnit(AtsrGBTConstants.COORDINATE_OFFSET_UNIT);
        band.setDescription(AtsrGBTConstants.FORWARD_Y_OFFS_DESCRIPTION);
        addBand(band);
    }

    /**
     * Calculates the offsets of the bands present into the file as bytes.
     */
    private void calculateFileOffsets() {
        int runningOffset = AtsrConstants.SADIST_2_HEAER_SIZE;

        if (getHeader().isThermalPresent()) {
            _nadir1200offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
            _nadir1100offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
            _nadir370offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
        }

        if (getHeader().isThermalPresent() || getHeader().isVisiblePresent()) {
            _nadir1600offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
        }

        if (getHeader().isVisiblePresent()) {
            _nadir870offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
            _nadir650offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
            _nadir550offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
        }

        if (getHeader().isThermalPresent() && !getHeader().isNadirOnly()) {
            _forward1200offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
            _forward1100offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
            _forward370offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
        }

        if ((getHeader().isThermalPresent() || getHeader().isVisiblePresent())
            && !getHeader().isNadirOnly()) {
            _forward1600offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.BT_PIXEL_SIZE;
        }

        if (getHeader().isVisiblePresent() && !getHeader().isNadirOnly()) {
            _forward870offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
            _forward650offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
            _forward550offset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.REF_PIXEL_SIZE;
        }

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

            if (!getHeader().isNadirOnly()) {
                _forwardXOffset = runningOffset;
                runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
                _forwardYOffset = runningOffset;
                runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.OFFSET_PIXEL_SIZE;
            }
        }

        if (getHeader().areFlagsPresent()) {
            _nadirFlagsOffset = runningOffset;
            runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.FLAGS_PIXEL_SIZE;
            if (!getHeader().isNadirOnly()) {
                _forwardFlagsOffset = runningOffset;
                runningOffset += AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.ATSR_SCENE_RASTER_HEIGHT * AtsrConstants.FLAGS_PIXEL_SIZE;
            }
        }
    }

    /**
     * Creates the flag bands and the associated coding.
     */
    private void createFlagBands() {
        if (getHeader().areFlagsPresent()) {

            createFlagCodings();

            Band band = new Band(AtsrConstants.NADIR_FLAGS_NAME, ProductData.TYPE_INT16,
                                 AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
            band.setDescription(AtsrConstants.NADIR_FLAGS_DESCRIPTION);
            band.setSampleCoding(_flagCodingNadir);
            addBand(band);

            if (!getHeader().isNadirOnly()) {
                band = new Band(AtsrConstants.FORWARD_FLAGS_NAME, ProductData.TYPE_INT16,
                                AtsrConstants.ATSR_SCENE_RASTER_WIDTH, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
                band.setDescription(AtsrConstants.FORWARD_FLAGS_DESCRIPTION);
                band.setSampleCoding(_flagCodingForward);
                addBand(band);
            }
        }
    }

    /**
     * Creates the flag codings for nadir and (optionally) forward bands
     */
    private void createFlagCodings() {
        _flagCodingNadir = new FlagCoding(AtsrConstants.NADIR_FLAGS_NAME);
        _flagCodingNadir = addCloudAndLandFlagsToCoding(_flagCodingNadir);

        if (!getHeader().isNadirOnly()) {
            _flagCodingForward = new FlagCoding(AtsrConstants.FORWARD_FLAGS_NAME);
            _flagCodingForward = addCloudAndLandFlagsToCoding(_flagCodingForward);
        }
    }

    /**
     * Creates all band reader necessary and adds them to the list.
     */
    private void createBandReader() {
        AtsrBandReader reader = null;

        if (getHeader().isThermalPresent()) {
            // 1200nm brightness temperature nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_1200_BT_NAME, _nadir1200offset,
                                             AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
            addBandReader(reader);

            // 1100nm brightness temperature nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_1100_BT_NAME, _nadir1100offset,
                                             AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
            addBandReader(reader);

            // 370nm brightness temperature nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_370_BT_NAME, _nadir370offset,
                                             AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
            addBandReader(reader);

            // add forward bands reader if present
            if (!getHeader().isNadirOnly()) {
                // 1200nm brightness temperature forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_1200_BT_NAME, _forward1200offset,
                                                 AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
                addBandReader(reader);

                // 1100nm brightness temperature forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_1100_BT_NAME, _forward1100offset,
                                                 AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
                addBandReader(reader);

                // 370nm brightness temperature forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_370_BT_NAME, _forward370offset,
                                                 AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_FACTOR, getStream());
                addBandReader(reader);

            }
        }

        if (getHeader().isThermalPresent() || getHeader().isVisiblePresent()) {
            // 1600nm reflectance nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_1600_REF_NAME, _nadir1600offset,
                                             AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
            addBandReader(reader);

            // add forward band reader if present
            if (!getHeader().isNadirOnly()) {
                // 1600nm reflectance forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_1600_REF_NAME, _forward1600offset,
                                                 AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
                addBandReader(reader);
            }
        }

        if (getHeader().isVisiblePresent()) {
            // 870nm reflectance nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_870_REF_NAME, _nadir870offset,
                                             AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
            addBandReader(reader);

            // 650nm reflectance nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_650_REF_NAME, _nadir650offset,
                                             AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
            addBandReader(reader);

            // 550nm reflectance nadir
            reader = new AtsrShortBandReader(AtsrGBTConstants.NADIR_550_REF_NAME, _nadir550offset,
                                             AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
            addBandReader(reader);

            // add forward bands reader when needed
            if (!getHeader().isNadirOnly()) {
                // 870nm reflectance forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_870_REF_NAME, _forward870offset,
                                                 AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
                addBandReader(reader);

                // 650nm reflectance forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_650_REF_NAME, _forward650offset,
                                                 AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
                addBandReader(reader);

                // 550nm reflectance forward
                reader = new AtsrShortBandReader(AtsrGBTConstants.FORWARD_550_REF_NAME, _forward550offset,
                                                 AtsrGBTConstants.REFLECTANCE_FACTOR, getStream());
                addBandReader(reader);
            }
        }

        if (getHeader().isXYPresent()) {
            // nadir x offset
            reader = new AtsrByteBandReader(AtsrGBTConstants.NADIR_X_OFFS_NAME, _nadirXOffset,
                                            AtsrGBTConstants.COORDINATE_OFFSET_FACTOR, getStream());
            addBandReader(reader);

            // nadir y offset
            reader = new AtsrByteBandReader(AtsrGBTConstants.NADIR_Y_OFFS_NAME, _nadirYOffset,
                                            AtsrGBTConstants.COORDINATE_OFFSET_FACTOR, getStream());
            addBandReader(reader);

            // add forward bands reader when needed
            if (!getHeader().isNadirOnly()) {
                // forward x offset
                reader = new AtsrByteBandReader(AtsrGBTConstants.FORWARD_X_OFFS_NAME, _forwardXOffset,
                                                AtsrGBTConstants.COORDINATE_OFFSET_FACTOR, getStream());
                addBandReader(reader);

                // forward y offset
                reader = new AtsrByteBandReader(AtsrGBTConstants.FORWARD_Y_OFFS_NAME, _forwardYOffset,
                                                AtsrGBTConstants.COORDINATE_OFFSET_FACTOR, getStream());
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

