package org.esa.beam.dataio.ceos.avnir2.records;

/*
 * $Id: Avnir2ImageFDRTest.java,v 1.2 2006/09/14 09:15:55 marcop Exp $
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

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseImageFileDescriptorRecord;
import org.esa.beam.dataio.ceos.records.BaseImageFileDescriptorRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * Created by marco.
 *
 * @author marco
 * @version $Revision$ $Date$
 */
public class Avnir2ImageFDRTest extends BaseImageFileDescriptorRecordTest {

    @Override
    protected BaseImageFileDescriptorRecord createImageFileDescriptorRecord(final CeosFileReader reader) throws
                                                                                                         IOException,
                                                                                                         IllegalCeosFormatException {
        return new Avnir2ImageFDR(reader);
    }

    @Override
    protected BaseImageFileDescriptorRecord createImageFileDescriptor(final CeosFileReader reader,
                                                                      final int startPos) throws IOException,
                                                                                                 IllegalCeosFormatException {
        return new Avnir2ImageFDR(reader, startPos);
    }

    @Override
    protected void writeBytes341To392(final ImageOutputStream ios) throws IOException {
        ios.writeBytes("   112SB"); // locatorDummyPixel
        ios.writeBytes("  13 8SB"); // locatorOpticalBlack
        ios.writeBytes("  2116SB"); // locatorOpticalWhite
        ios.writeBytes("  3716SB"); // locatorElectricalCalibration
        ios.writeBytes("  5312SB"); // locatorImageAuxiliaryData
        ios.writeBytes("  65 2SB"); // locatorQualityInformation
        CeosTestHelper.writeBlanks(ios, 4);
    }

    @Override
    protected void assertBytes341To392(final BaseImageFileDescriptorRecord record) {
        final Avnir2ImageFDR avnir2ImageFDR = (Avnir2ImageFDR) record;

        assertEquals("   112SB", avnir2ImageFDR.getLocatorDummyPixel());
        assertEquals("  13 8SB", avnir2ImageFDR.getLocatorOpticalBlack());
        assertEquals("  2116SB", avnir2ImageFDR.getLocatorOpticalWhite());
        assertEquals("  3716SB", avnir2ImageFDR.getLocatorElectricalCalibration());
        assertEquals("  5312SB", avnir2ImageFDR.getLocatorImageAuxiliaryData());
        assertEquals("  65 2SB", avnir2ImageFDR.getLocatorQualityInformation());

        // nothing else to test
    }

}