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

package org.esa.beam.dataio.avhrr.binio;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.util.ImageIOHandler;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.framework.dataio.DecodeQualification;

import javax.imageio.stream.ImageInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;

/**
 * Represent a single NOAA AVHRR file.
 */
public class NoaaAvhrrFile {
    private static final List<String> datasetCreationSites = Arrays.asList("CMS", "DSS", "NSS", "UKM");

    private final File file;

    public NoaaAvhrrFile(File file) {
        this.file = file;
    }

    public DecodeQualification canOpen() {
        CompoundType testType =
            COMPOUND("test",
                    MEMBER("no_ars_dscs", SEQUENCE(SimpleType.BYTE, 3)),
                    MEMBER("fill", SEQUENCE(SimpleType.BYTE, AvhrrConstants.ARS_LENGTH-3)),
                    MEMBER("ars_dscs", SEQUENCE(SimpleType.BYTE, 3)));
        DataFormat dataFormat = new DataFormat(testType,  ByteOrder.BIG_ENDIAN);
        DataContext context = null;
        try {
            context = dataFormat.createContext(file, "r");
            CompoundData data = context.getData();
            String s1 = HeaderWrapper.getAsString(data.getSequence(0));
            String s2 = HeaderWrapper.getAsString(data.getSequence(2));
            System.out.println("s1 = '" + s1 + "', s2 = '" + s2+"'");
            if (datasetCreationSites.contains(s1)) {
                System.out.println("open: no ARS header");
                return DecodeQualification.INTENDED;
            }
            if (datasetCreationSites.contains(s2)) {
                System.out.println("open: ARS header");
                return DecodeQualification.INTENDED;
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
            return DecodeQualification.UNABLE;
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
        System.out.println("can not open");
        return DecodeQualification.UNABLE;
    }

}
