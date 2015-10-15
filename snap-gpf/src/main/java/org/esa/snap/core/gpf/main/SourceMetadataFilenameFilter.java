package org.esa.snap.core.gpf.main;

import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author Norman Fomferra
 */
class SourceMetadataFilenameFilter implements FilenameFilter {
    private final String fileName;
    private final String wantedPrefix;

    public SourceMetadataFilenameFilter(String fileName) {
        this.fileName = fileName;
        this.wantedPrefix = FileUtils.getFilenameWithoutExtension(fileName) + "-";
    }

    @Override
    public boolean accept(File dir, String name) {
        if (!name.equalsIgnoreCase(fileName)) {
            if (name.startsWith(wantedPrefix)) {
                return true;
            }
        }
        return false;
    }

    public String getMetadataBaseName(String fileName) {
        return fileName.substring(wantedPrefix.length());
    }
}
