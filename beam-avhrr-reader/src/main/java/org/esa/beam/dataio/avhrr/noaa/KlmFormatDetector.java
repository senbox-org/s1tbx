/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.framework.dataio.DecodeQualification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;

/**
 * Detects whether the AVHRR file starts with an ARS header or without it.
 */
class KlmFormatDetector {

    private static final List<String> DATASET_CREATION_SITES = Arrays.asList("CMS", "DSS", "NSS", "UKM");

    private static final CompoundType ARS_HEADER_DETECTOR_TYPE =
            COMPOUND("test",
                    MEMBER("no_ars_dscs", SEQUENCE(SimpleType.BYTE, 3)),
                    MEMBER("fill", SEQUENCE(SimpleType.BYTE, AvhrrConstants.ARS_LENGTH - 3)),
                    MEMBER("ars_dscs", SEQUENCE(SimpleType.BYTE, 3))
            );

    private DecodeQualification decodeQualification = DecodeQualification.UNABLE;
    private boolean hasArsHeader;

    private RandomAccessFile raf;
    private IOHandler ioHandler;

    KlmFormatDetector(File file) throws FileNotFoundException {
        raf = new RandomAccessFile(file, "r");
        ioHandler = new RandomAccessFileIOHandler(raf);
        detectArsHeader();
    }

    ProductFormat getProductFormat() throws  IOException {
        CompoundType type;
        if (hasArsHeader) {
            type = COMPOUND("test",
                    MEMBER("arsHeader", SEQUENCE(SimpleType.BYTE, AvhrrConstants.ARS_LENGTH)),
                    MEMBER("dataHeader", SEQUENCE(SimpleType.SHORT, 15000))
            );
        } else {
            type = COMPOUND("test",
                    MEMBER("dataHeader", SEQUENCE(SimpleType.SHORT, 15000))
            );
        }

        DataFormat dataFormat = new DataFormat(type, ByteOrder.BIG_ENDIAN);
        DataContext context = null;
        try {
            context = dataFormat.createContext(ioHandler);
            CompoundData data = context.getData();
            SequenceData dataHeader = data.getSequence("dataHeader");
            // the actual data in the header is in all formats less than 1000 bytes
            for (int index = 500; index < dataHeader.getSize() - 4; index++) {
                if (dataHeader.getShort(index) != 0) {
                    // detect start of first scan line
                    int year = dataHeader.getShort(index+1);
                    int dayOfYear = dataHeader.getShort(index+2);
                    if (year > 1970 && year < 2050 && dayOfYear > 0 && dayOfYear < 367) {
                        return ProductFormat.findByBlockSize(index*2);
                    }
                }
            }
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
        throw new IOException("Could not detect AVHRR data record size.");
    }

    public void dispose() {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException ignore) {
            }
            raf = null;
            ioHandler = null;
        }
    }

    private void detectArsHeader() {
        DataFormat dataFormat = new DataFormat(ARS_HEADER_DETECTOR_TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = null;
        try {
            context = dataFormat.createContext(ioHandler);
            CompoundData data = context.getData();
            String s1 = HeaderWrapper.getAsString(data.getSequence(0));
            String s2 = HeaderWrapper.getAsString(data.getSequence(2));
            if (DATASET_CREATION_SITES.contains(s1)) {
                hasArsHeader = false;
                decodeQualification = DecodeQualification.INTENDED;
            } else if (DATASET_CREATION_SITES.contains(s2)) {
                decodeQualification = DecodeQualification.INTENDED;
                hasArsHeader = true;
            }
        } catch (Throwable ignore) {
            decodeQualification = DecodeQualification.UNABLE;
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
    }

    public boolean canDecode() {
        return decodeQualification == DecodeQualification.INTENDED;
    }

    boolean hasArsHeader() {
        return hasArsHeader;
    }
}
