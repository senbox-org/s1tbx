package org.esa.nest.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**

 */
public class FileUtils {

    /**
     * Reads a text file and replaces all outText with newText
     * @param inFile input file
     * @param outFile output file
     * @param oldText text to replace
     * @param newText replacement text
     * @throws IOException on io error
     */
    public static void replaceText(final File inFile, final File outFile,
                                   final String oldText, final String newText) throws IOException {
        final List<String> lines;
        final FileReader fileReader = new FileReader(inFile);
        try {
            lines = IOUtils.readLines(fileReader);

            for(int i=0; i < lines.size(); ++i) {
                String line = lines.get(i);
                if(line.contains(oldText)) {
                    lines.set(i, line.replaceAll(oldText, newText));
                }
            }
        } finally {
            fileReader.close();
        }

        if(!lines.isEmpty()) {
            final FileWriter fileWriter = new FileWriter(outFile);
            try {
                IOUtils.writeLines(lines, "\n", fileWriter);
            } finally {
                fileWriter.close();
            }
        }
    }
}
