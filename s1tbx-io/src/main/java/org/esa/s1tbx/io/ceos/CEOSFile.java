package org.esa.s1tbx.io.ceos;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.jdom2.Document;

public interface CEOSFile {

    default Document loadDefinitionFile(final String mission, final String fileName) {
        return BinaryDBReader.loadDefinitionFile(mission, fileName);
    }
}
