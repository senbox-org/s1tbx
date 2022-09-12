package org.esa.s1tbx.io.strix;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class StriXTrailerFile extends StriXLeaderFile {
    private final static String trailer_recordDefinitionFile = "trailer_file.xml";
    private final static Document trailerXML = BinaryDBReader.loadDefinitionFile(mission, trailer_recordDefinitionFile);

    public StriXTrailerFile(final ImageInputStream stream) throws IOException {
        super(stream, trailerXML);
    }
}