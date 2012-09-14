package org.esa.nest.dataio.ceos;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;

import java.io.IOException;

public class FilePointerRecord extends BinaryRecord {

    public FilePointerRecord(final BinaryFileReader reader, final org.jdom.Document filePointerXML, 
                             final String recName) throws IOException {
        this(reader, filePointerXML, -1, recName);
    }

    public FilePointerRecord(final BinaryFileReader reader, final org.jdom.Document filePointerXML, final long startPos,
                             final String recName) throws IOException {
        super(reader, startPos, filePointerXML, recName);
    }

    public void assignMetadataTo(final MetadataElement root, final String suffix) {
        final MetadataElement elem = createMetadataElement("FilePointerRecord", suffix);
        root.addElement(elem);

        super.assignMetadataTo(elem);
    }
}
