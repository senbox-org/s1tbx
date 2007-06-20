/*
 * $Id: AtsrFile.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
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
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Vector;

abstract class AtsrFile {

    private ImageInputStream _stream;
    private AtsrHeader _header;
    private File _file;
    private Vector _bands;
    private HashMap _bandReader;
    private GeoCoding _geoCoding;

    /**
     * Constructs the object with default parameters.
     */
    public AtsrFile() {
        _bands = new Vector();
        _bandReader = new HashMap();
    }

    /**
     * Opens the ATSR file at the given location.
     */
    public void open(ImageInputStream inStream, File file) throws IOException {
        _stream = inStream;
        _file = file;

        checkByteSwapping();
        readHeader();
    }

    /**
     * Closes the ATSR file.
     */
    public void close() throws IOException {
        if (_stream != null) {
            _stream.close();
        }
    }

    /**
     * Retrieves the complete set of metadata and adds it to <code>root</code>.
     */
    public MetadataElement getMetadata(MetadataElement root) {
        return _header.getMetadata(root);
    }

    /**
     * Retrieves the number of tie point grids in the file.
     */
    public int getNumTiePointGrids() {
        return _header.getNumTiePointGrids();
    }

    /**
     * Retrieves the tie point grid at the given location.
     */
    public TiePointGrid getTiePointGridAt(int nIndex) {
        return _header.getTiePointGridAt(nIndex);
    }

    /**
     * Retrieves the geocoding for this file
     */
    public GeoCoding getGeoCoding() {
        return _geoCoding;
    }

    /**
     * Retrieves the filename as coded in the header.
     */
    public String getFileName() {
        return _header.getFileName();
    }

    /**
     * Retrieves the <code>File</code>, might be null!
     */
    public File getFile() {
        return _file;
    }

    /**
     * Retrieves the sensor type as coded in the header.
     */
    public String getSensorType() {
        return _header.getSensorType();
    }

    /**
     * Retrieves the number of geophysical bands contained in the product.
     */
    public int getNumBands() {
        return _bands.size();
    }

    /**
     * Retrieves the band at the given index.
     */
    public Band getBandAt(int index) {
        return (Band) _bands.elementAt(index);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the stream associated to this ATSR file
     */
    ImageInputStream getStream() {
        return _stream;
    }

    /**
     * Retrieves the band reader for the band passed in.
     */
    AtsrBandReader getReader(Band band) {
        return (AtsrBandReader) _bandReader.get(band);
    }

    /**
     * Adds the reader to the list of available readers.
     */
    void addBandReader(AtsrBandReader reader) {
        _bandReader.put(getBand(reader.getBandName()), reader);
    }

    private Band getBand(final String bandName) {
        for (int i = 0; i < _bands.size(); i++) {
            Band band = (Band) _bands.elementAt(i);
            if (band.getName().equalsIgnoreCase(bandName)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Retrieves the <code>AtsrHeader</code> object contained in here.
     */
    protected AtsrHeader getHeader() {
        return _header;
    }

    /**
     * Reads the complete SADIST-2 header and decodes it.
     */
    private void readHeader() throws IOException {
        byte[] rawHeader = new byte[AtsrConstants.SADIST_2_HEAER_SIZE];

        _stream.readFully(rawHeader, 0, rawHeader.length);

        _header = new AtsrHeader();
        _header.parse(rawHeader);
    }

    /**
     * Adds a band to the internal list of bands.
     */
    protected void addBand(Band band) {
        _bands.add(band);
    }

    /**
     * Reads the lat lon bands (if they're present) and downsamples them. Then adds the downsampled Bands as tie point
     * grids to the tie point vectors in the header object.
     */
    protected void readLatLonTiePoints(int latOffset, int lonOffset) throws IOException {
        int width = AtsrConstants.ATSR_SCENE_RASTER_WIDTH / AtsrConstants.LAT_LON_SUBS_X;
        int widthInBytes = AtsrConstants.ATSR_SCENE_RASTER_WIDTH * AtsrConstants.LATLON_PIXEL_SIZE;
        int height = AtsrConstants.ATSR_SCENE_RASTER_HEIGHT / AtsrConstants.LAT_LON_SUBS_Y;
        float[] latTiePoints = new float[width * height];
        float[] lonTiePoints = new float[width * height];
        int[] line = new int[AtsrConstants.ATSR_SCENE_RASTER_WIDTH];
        int writeOffset = 0;

        // read latitudes
        for (int n = 0; n < height; n++) {
            _stream.seek(latOffset + n * AtsrConstants.LAT_LON_SUBS_Y * widthInBytes);
            _stream.readFully(line, 0, line.length);
            for (int m = 0; m < width; m++) {
                latTiePoints[writeOffset] = ((float) line[m * AtsrConstants.LAT_LON_SUBS_X]) * AtsrConstants.LAT_LON_CONVERSION;
                ++writeOffset;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(AtsrConstants.LATITUDE_NAME, width, height,
                                                      0, 0, AtsrConstants.LAT_LON_SUBS_X, AtsrConstants.LAT_LON_SUBS_Y,
                                                      latTiePoints);
        latGrid.setDescription(AtsrConstants.LATITUDE_DESCRIPTION);
        latGrid.setUnit(AtsrConstants.ANGLE_UNIT);
        _header.addTiePointGrid(latGrid);

        // read longitudes
        writeOffset = 0;
        for (int n = 0; n < height; n++) {
            _stream.seek(lonOffset + n * AtsrConstants.LAT_LON_SUBS_Y * widthInBytes);
            _stream.readFully(line, 0, line.length);
            for (int m = 0; m < width; m++) {
                lonTiePoints[writeOffset] = ((float) line[m * AtsrConstants.LAT_LON_SUBS_X]) * AtsrConstants.LAT_LON_CONVERSION;
                ++writeOffset;
            }
        }

        final TiePointGrid lonGrid = new TiePointGrid(AtsrConstants.LONGITUDE_NAME, width, height,
                                                      0, 0, AtsrConstants.LAT_LON_SUBS_X, AtsrConstants.LAT_LON_SUBS_Y,
                                                      lonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setDescription(AtsrConstants.LONGITUDE_DESCRIPTION);
        lonGrid.setUnit(AtsrConstants.ANGLE_UNIT);
        _header.addTiePointGrid(lonGrid);

        _geoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
    }

    /**
     * Adds the cloud and land flags to the <code>FlagCoding</code> passed in.
     */
    protected FlagCoding addCloudAndLandFlagsToCoding(FlagCoding coding) {
        coding.addFlag(AtsrConstants.LAND_FLAG_NAME, AtsrConstants.LAND_FLAG_MASK,
                       AtsrConstants.LAND_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.CLOUD_FLAG_NAME, AtsrConstants.CLOUD_FLAG_MASK,
                       AtsrConstants.CLOUD_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.SUNGLINT_FLAG_NAME, AtsrConstants.SUNGLINT_FLAG_MASK,
                       AtsrConstants.SUNGLINT_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.REFL_HIST_FLAG_NAME, AtsrConstants.REFL_HIST_FLAG_MASK,
                       AtsrConstants.REFL_HIST_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.SPAT_COHER_16_FLAG_NAME, AtsrConstants.SPAT_COHER_16_FLAG_MASK,
                       AtsrConstants.SPAT_COHER_16_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.SPAT_COHER_11_FLAG_NAME, AtsrConstants.SPAT_COHER_11_FLAG_MASK,
                       AtsrConstants.SPAT_COHER_11_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.GROSS_12_FLAG_NAME, AtsrConstants.GROSS_12_FLAG_MASK,
                       AtsrConstants.GROSS_12_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.MED_HI_37_12_FLAG_NAME, AtsrConstants.MED_HI_37_12_FLAG_MASK,
                       AtsrConstants.MED_HI_37_12_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_NAME, AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_MASK,
                       AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.VW_DIFF_11_12_FLAG_NAME, AtsrConstants.VW_DIFF_11_12_FLAG_MASK,
                       AtsrConstants.VW_DIFF_11_12_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.VW_DIFF_37_11_FLAG_NAME, AtsrConstants.VW_DIFF_37_11_FLAG_MASK,
                       AtsrConstants.VW_DIFF_37_11_FLAG_DESCRIPTION);
        coding.addFlag(AtsrConstants.THERM_HIST_11_12_FLAG_NAME, AtsrConstants.THERM_HIST_11_12_FLAG_MASK,
                       AtsrConstants.THERM_HIST_11_12_FLAG_DESCRIPTION);
        return coding;
    }

    /**
     * Checks whether byte swapping is needed or not. Sets the stream state according.
     */
    private void checkByteSwapping() throws IOException {
        short swap;

        _stream.seek(0);
        swap = _stream.readShort();

// >>>> Java Image I/O Beta
//        if (swap != AtsrConstants.LITTLE_ENDIAN_TAG) {
//            _stream.setByteOrder(false);
//        } else {
//            _stream.setByteOrder(true);
//        }
// <<<< Java Image I/O Beta

// >>>> J2SDK 1.4
        if (swap == AtsrConstants.LITTLE_ENDIAN_TAG) {
            // must be this way - stream is written on a big endian machine
            _stream.setByteOrder(ByteOrder.BIG_ENDIAN);
        } else {
            _stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
// <<<< J2SDK 1.4

        _stream.seek(0);
    }

}
