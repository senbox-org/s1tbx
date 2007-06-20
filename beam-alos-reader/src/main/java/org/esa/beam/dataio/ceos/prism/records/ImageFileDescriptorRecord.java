/*
 * $Id: ImageFileDescriptorRecord.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
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
package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseImageFileDescriptorRecord;

import java.io.IOException;

public class ImageFileDescriptorRecord extends BaseImageFileDescriptorRecord {

    private String _locatorAUXData;
    private String _locatorQualityInformation;
    private String _locatorExtractionStartPoint;

    public ImageFileDescriptorRecord(final CeosFileReader reader) throws IOException,
                                                                         IllegalCeosFormatException {
        this(reader, -1);
    }

    public ImageFileDescriptorRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                              IllegalCeosFormatException {
        super(reader, startPos);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(340));
        _locatorAUXData = reader.readAn(8);
        _locatorQualityInformation = reader.readAn(8);
        _locatorExtractionStartPoint = reader.readAn(8);
    }

    public String getLocatorAUXData() {
        return _locatorAUXData;
    }

    public String getLocatorQualityInformation() {
        return _locatorQualityInformation;
    }

    public String getLocatorExtractionStartPoint() {
        return _locatorExtractionStartPoint;
    }
}
