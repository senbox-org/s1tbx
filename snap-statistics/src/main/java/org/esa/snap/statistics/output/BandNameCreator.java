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

package org.esa.snap.statistics.output;

import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the possibility to create unique attribute names for combinations of an algorithm name and a band name.
 * Additionally, the mapping between original algorithm and band name and the resulting unique name may be written to
 * a print stream.
 *
 * @author Thomas Storm
 */
public class BandNameCreator {

    private Map<String, Integer> indexMap;
    private final PrintStream printStream;
    private Map<String, String> mappedNames;

    /**
     * Constructor that creates no band mapping output.
     */
    public BandNameCreator() {
        this(new PrintStream(new NullOutputStream()));
    }

    /**
     * Constructor that writes band mapping output to the given print stream.
     */
    public BandNameCreator(PrintStream printStream) {
        this.printStream = printStream;
        indexMap = new HashMap<String, Integer>();
        mappedNames = new HashMap<String, String>();
    }

    /**
     * Creates an unique name -- unique in the scope of each instance of this class -- from the given input parameters.
     * The unique names created are maximally 10 characters in length.
     *
     * @param algorithmName  The name of the algorithm.
     * @param sourceBandName The name of the source band.
     * @return A unique name for the combination of both.
     */
    public String createUniqueAttributeName(String algorithmName, String sourceBandName) {
        final String desiredAttributeName = algorithmName + "_" + sourceBandName;
        if (mappedNames.containsKey(desiredAttributeName)) {
            return mappedNames.get(desiredAttributeName);
        }
        String attributeName = desiredAttributeName;
        final boolean tooLong = desiredAttributeName.length() > 10;
        if (tooLong) {
            attributeName = algorithmName + "_" + sourceBandName.replace("_", "");
            attributeName = shorten(attributeName);
        }
        if (attributeName.length() > 10) {
            final int index = getIndex(algorithmName);
            attributeName = shorten(algorithmName) + "_" + index;
            if (attributeName.length() > 10) {
                final String indexPart = Integer.toString(index);
                final int idxLength = indexPart.length();
                attributeName = attributeName.substring(0, 10 - idxLength - 1) + "_" + indexPart;
            }
            indexMap.put(algorithmName, index + 1);
        }
        if (tooLong) {
            SystemUtils.LOG.warning(
                    "attribute name '" + desiredAttributeName + "' exceeds 10 characters in length. Shortened to '" +
                            attributeName +
                            "'.");
        }
        addMapping(desiredAttributeName, attributeName);
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
        printStream
                .append(attributeName)
                .append("=")
                .append(desiredAttributeName)
                .append("\n");
        mappedNames.put(desiredAttributeName, attributeName);
    }

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
        }
    }
}
