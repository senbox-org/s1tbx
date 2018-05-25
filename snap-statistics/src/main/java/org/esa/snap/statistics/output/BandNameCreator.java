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
import org.esa.snap.statistics.tools.TimeInterval;

/**
 * Provides the possibility to create unique attribute names for combinations of a measure name and a band name.
 * Additionally, the mapping between original measure and band name and the resulting unique name may be written to
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
     * @param measureName  The name of the algorithm.
     * @param sourceBandName The name of the source band.
     * @return A unique name for the combination of both.
     */
    public String createUniqueAttributeName(String measureName, String sourceBandName) {
        final String desiredAttributeName = measureName + "_" + sourceBandName;
        if (mappedNames.containsKey(desiredAttributeName)) {
            return mappedNames.get(desiredAttributeName);
        }
        String attributeName = desiredAttributeName;
        final boolean tooLong = desiredAttributeName.length() > 10;
        if (tooLong) {
            attributeName = measureName + "_" + sourceBandName.replace("_", "");
            attributeName = shorten(attributeName);
        }
        if (attributeName.length() > 10) {
            final int index = getIndex(measureName);
            attributeName = shorten(measureName) + "_" + index;
            if (attributeName.length() > 10) {
                final String indexPart = Integer.toString(index);
                final int idxLength = indexPart.length();
                attributeName = attributeName.substring(0, 10 - idxLength - 1) + "_" + indexPart;
            }
            indexMap.put(measureName, index + 1);
        }
        if (tooLong) {
            SystemUtils.LOG.warning(
                    "attribute name '" + desiredAttributeName + "' exceeds 10 characters in length. Shortened to '" +
                            attributeName +
                            "'.");
        }
        addMapping(desiredAttributeName, attributeName);
        print(desiredAttributeName, attributeName, null);
        return attributeName;
    }

    /**
     * Creates an unique name -- unique in the scope of each instance of this class -- from the given input parameters.
     * The unique names created are maximally 10 characters in length.
     *
     * @param measureName  The name of the algorithm.
     * @param sourceBandName The name of the source band.
     * @param timeInterval The time interval to which the statistics refer
     * @return A unique name for the combination of the three.
     */
    public String createUniqueAttributeName(String measureName, String sourceBandName, TimeInterval timeInterval) {
        final String desiredAttributeName = measureName + "_" + sourceBandName + "_" + timeInterval.getId();
        if (mappedNames.containsKey(desiredAttributeName)) {
            return mappedNames.get(desiredAttributeName);
        }
        String attributeName = desiredAttributeName;
        final boolean tooLong = desiredAttributeName.length() > 10;
        if (tooLong) {
            attributeName = measureName + "_" + sourceBandName.replace("_", "") + "_" + timeInterval.getId();
            attributeName = shorten(attributeName);
        }
        if (attributeName.length() > 10) {
            final int index = getIndex(measureName);
            attributeName = shorten(measureName) + "_" + index + "_" + timeInterval.getId();
            if (attributeName.length() > 10) {
                final String indexPart = Integer.toString(index);
                final int idxLength = indexPart.length();
                final String intervalIdPart = Integer.toString(timeInterval.getId());
                final int intervalIdLength = intervalIdPart.length();
                attributeName = attributeName.substring(0, 10 - idxLength - intervalIdLength - 2) + "_" +
                        indexPart + "_" + intervalIdPart;
            }
            indexMap.put(measureName, index + 1);
        }
        if (tooLong) {
            SystemUtils.LOG.warning(
                    "attribute name '" + desiredAttributeName + "' exceeds 10 characters in length. Shortened to '" +
                            attributeName +
                            "'.");
        }
        addMapping(desiredAttributeName, attributeName);
        print(desiredAttributeName, attributeName, timeInterval);
        return attributeName;
    }

    String getUniqueAttributeName(String measureName, String sourceBandName, TimeInterval timeInterval) {
        final String desiredAttributeName = measureName + "_" + sourceBandName + "_" + timeInterval.getId();
        if (mappedNames.containsKey(desiredAttributeName)) {
            return mappedNames.get(desiredAttributeName);
        }
        return getUniqueAttributeName(measureName, sourceBandName);
    }

    private String getUniqueAttributeName(String measureName, String sourceBandName) {
        final String desiredAttributeName = measureName + "_" + sourceBandName;
        if (mappedNames.containsKey(desiredAttributeName)) {
            return mappedNames.get(desiredAttributeName);
        }
        throw new IllegalArgumentException("No such attribute: " + desiredAttributeName);
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
        mappedNames.put(desiredAttributeName, attributeName);
    }

    private void print(String desiredAttributeName, String attributeName, TimeInterval timeInterval) {
        printStream
                .append(attributeName)
                .append("=")
                .append(desiredAttributeName);
        if (timeInterval != null) {
            printStream
                    .append(" between ")
                    .append(timeInterval.getIntervalStart().format())
                    .append(" and ")
                    .append(timeInterval.getIntervalEnd().format());
        }
        printStream
                .append("\n");
    }

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
        }
    }
}
