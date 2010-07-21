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
package org.esa.beam.dataio.ceos.prism;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.FilePointerRecord;
import org.esa.beam.dataio.ceos.records.TextRecord;
import org.esa.beam.dataio.ceos.records.VolumeDescriptorRecord;
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class PrismVolumeDirectoryFile {

    private CeosFileReader _ceosReader;
    private VolumeDescriptorRecord _volumeDescriptorRecord;
    private FilePointerRecord[] _filePointerRecords;
    private TextRecord _textRecord;

    public PrismVolumeDirectoryFile(final File baseDir) throws IOException,
                                                               IllegalCeosFormatException {
        final File volumeFile = CeosHelper.getVolumeFile(baseDir);
        _ceosReader = new CeosFileReader(new FileImageInputStream(volumeFile));
        _volumeDescriptorRecord = new VolumeDescriptorRecord(_ceosReader);
        _filePointerRecords = CeosHelper.readFilePointers(_volumeDescriptorRecord);
        _textRecord = new TextRecord(_ceosReader);
    }

    public String getLeaderFileName() {
        return CeosHelper.getLeaderFileName(_textRecord);
    }

    public String getTrailerFileName() {
        return CeosHelper.getTrailerFileName(_textRecord);
    }

    public String getSupplementalFileName() {
        return CeosHelper.getSupplementalFileName(_textRecord);
    }

    public String[] getImageFileNames() {
        final ArrayList list = new ArrayList();
        for (int i = 0; i < _filePointerRecords.length; i++) {
            final FilePointerRecord filePointerRecord = _filePointerRecords[i];
            if (filePointerRecord.isImageFileRecord()) {
                final String fileID = filePointerRecord.getFileID();
                list.add(CeosHelper.getImageFileName(_textRecord, fileID.substring(15)));
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public void close() throws IOException {
        _ceosReader.close();
        _ceosReader = null;
        for (int i = 0; i < _filePointerRecords.length; i++) {
            _filePointerRecords[i] = null;
        }
        _filePointerRecords = null;
        _volumeDescriptorRecord = null;
        _textRecord = null;
    }

    public String getProductName() {
        return CeosHelper.getProductName(_textRecord);
    }

    public void assignMetadataTo(final MetadataElement elem) {
        final MetadataElement vdfElem = new MetadataElement("VOLUME_DESCRIPTOR");
        elem.addElement(vdfElem);

        _volumeDescriptorRecord.assignMetadataTo(vdfElem, null);
        for (int i = 0; i < _filePointerRecords.length; i++) {
            _filePointerRecords[i].assignMetadataTo(vdfElem, "" + (i + 1));
        }
    }

}

