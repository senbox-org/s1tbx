package org.esa.beam.dataio.ceos.avnir2.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseImageFileDescriptorRecord;

import java.io.IOException;

/*
 * $Id: Avnir2ImageFDR.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
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

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class Avnir2ImageFDR extends BaseImageFileDescriptorRecord {

    private String _locatorDummyPixel;
    private String _locatorOpticalBlack;
    private String _locatorOpticalWhite;
    private String _locatorElectricalCalibration;
    private String _locatorImageAuxiliaryData;
    private String _locatorQualityInformation;

    public Avnir2ImageFDR(final CeosFileReader reader) throws IOException,
                                                              IllegalCeosFormatException {
        this(reader, -1);
    }

    public Avnir2ImageFDR(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                   IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(340));
        _locatorDummyPixel = reader.readAn(8);
        _locatorOpticalBlack = reader.readAn(8);
        _locatorOpticalWhite = reader.readAn(8);
        _locatorElectricalCalibration = reader.readAn(8);
        _locatorImageAuxiliaryData = reader.readAn(8);
        _locatorQualityInformation = reader.readAn(8);
    }

    public String getLocatorDummyPixel() {
        return _locatorDummyPixel;
    }

    public String getLocatorElectricalCalibration() {
        return _locatorElectricalCalibration;
    }

    public String getLocatorImageAuxiliaryData() {
        return _locatorImageAuxiliaryData;
    }

    public String getLocatorOpticalBlack() {
        return _locatorOpticalBlack;
    }

    public String getLocatorOpticalWhite() {
        return _locatorOpticalWhite;
    }

    public String getLocatorQualityInformation() {
        return _locatorQualityInformation;
    }
}
