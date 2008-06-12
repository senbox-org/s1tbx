package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.avnir2.records.Avnir2TrailerRecord;
import org.esa.beam.dataio.ceos.records.TrailerFileDescriptorRecord;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/*
 * $Id: Avnir2TrailerFile.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
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
 * * This class represents a trailer file of an Avnir-2 product.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
class Avnir2TrailerFile {

    private Avnir2TrailerRecord _trailerRecord;
    private CeosFileReader _ceosReader;

    public Avnir2TrailerFile(final ImageInputStream trailerStream) throws IOException,
                                                                          IllegalCeosFormatException {
        _ceosReader = new CeosFileReader(trailerStream);
        // must be created even it is not (yet) used
        // it is needed for positioning the reader correctly
        new TrailerFileDescriptorRecord(_ceosReader);
        _trailerRecord = new Avnir2TrailerRecord(_ceosReader);
    }

    public int[] getHistogramBinsForBand(final int index) throws IOException,
                                                                 IllegalCeosFormatException {
        return _trailerRecord.getHistogramFor(index);
    }

    public void close() throws IOException {
        _ceosReader.close();
        _ceosReader = null;
        _trailerRecord = null;
    }
}
