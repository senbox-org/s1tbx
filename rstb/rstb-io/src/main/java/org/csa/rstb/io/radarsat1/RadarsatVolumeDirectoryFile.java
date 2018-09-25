package org.csa.rstb.io.radarsat1;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.ceos.CEOSVolumeDirectoryFile;
import org.jdom2.Document;

import java.io.IOException;

public class RadarsatVolumeDirectoryFile extends CEOSVolumeDirectoryFile {

    private final static String resourcePath = "org/csa/rstb/io/ceos_db/";

    public RadarsatVolumeDirectoryFile(final BinaryFileReader binaryReader, final String mission) throws IOException {
        super(binaryReader, mission);
    }

    @Override
    public Document loadDefinitionFile(final String mission, final String fileName) {
        return BinaryDBReader.loadDefinitionFile(resourcePath, mission, fileName, RadarsatVolumeDirectoryFile.class);
    }
}
