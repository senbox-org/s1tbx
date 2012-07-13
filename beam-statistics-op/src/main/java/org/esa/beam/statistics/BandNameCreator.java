/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.statistics;

import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a method used by {@link ShapefileOutputter} in order to create unique attribute names and a mapping file.
 *
 * @author Thomas Storm
 */
class BandNameCreator {

    private Map<String, Integer> indexMap;
    private final PrintStream printStream;
    private Set<String> mappedNames;

    /**
     * Constructor that creates no band mapping output.
     */
    BandNameCreator() {
        this(new PrintStream(new NullOutputStream()));
    }

    /**
     * Constructor that writes band mapping output to the given print stream.
     */
    BandNameCreator(PrintStream printStream) {
        this.printStream = printStream;
        indexMap  = new HashMap<String, Integer>();
        mappedNames = new HashSet<String>();
    }

    String createUniqueAttributeName(String algorithmName, String sourceBandName) {
        final String desiredAttributeName = algorithmName + "_" + sourceBandName;
        String attributeName = desiredAttributeName;
        final boolean tooLong = desiredAttributeName.length() > 10;
        if (tooLong) {
            attributeName = algorithmName + "_" + sourceBandName.replace("_", "");
            attributeName = shorten(attributeName);
        }
        if (attributeName.length() > 10) {
            attributeName = shorten(algorithmName) + "_" + getIndex(algorithmName);
            indexMap.put(algorithmName, getIndex(algorithmName) + 1);
        }
        if (tooLong) {
            addMapping(desiredAttributeName, attributeName);
            BeamLogManager.getSystemLogger().warning(
                    "attribute name '" + desiredAttributeName + "' exceeds 10 characters in length. Shortened to '" + attributeName +
                    "'.");
        }
        return attributeName;
    }

    private static String shorten(String attributeName) {
        attributeName = attributeName.replace("minimum", "min").replace("maximum", "max");
        attributeName = attributeName.replace("a", "").replace("e", "").replace("i", "").replace("o", "").replace("u", "");
        return attributeName;
    }

    private int getIndex(String attributeName) {
        if (indexMap.containsKey(attributeName)) {
            return indexMap.get(attributeName);
        }
        return 0;
    }

    private void addMapping(String desiredAttributeName, String attributeName) {
        if(!mappedNames.contains(desiredAttributeName)) {
            printStream
                    .append(attributeName)
                    .append("=")
                    .append(desiredAttributeName)
                    .append("\n");
            mappedNames.add(desiredAttributeName);
        }
    }

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
        }
    }
}
