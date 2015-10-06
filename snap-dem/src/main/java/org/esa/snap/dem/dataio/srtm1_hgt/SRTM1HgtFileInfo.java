/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm1_hgt;

import org.esa.snap.core.util.Guardian;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Holds information about a HGT file.
 *
 */
public class SRTM1HgtFileInfo {

    private static final EastingNorthingParser PARSER = new EastingNorthingParser();

    private String fileName;
    private float easting;
    private float northing;

    private SRTM1HgtFileInfo() {
    }

    public String getFileName() {
        return fileName;
    }

    public float getEasting() {
        return easting;
    }

    public float getNorthing() {
        return northing;
    }

    public static SRTM1HgtFileInfo create(final File dataFile) throws IOException {
        final SRTM1HgtFileInfo fileInfo = new SRTM1HgtFileInfo();
        fileInfo.setFromData(dataFile.getName());
        return fileInfo;
    }

    public void setFromData(final String fileName) throws IOException {
        this.fileName = fileName;

        final int[] eastingNorthing;
        try {
            eastingNorthing = parseEastingNorthing(fileName);
        } catch (ParseException e) {
            throw new IOException("Illegal file name format: " + fileName);
        }
        easting = eastingNorthing[0];
        northing = eastingNorthing[1];
    }

    public static int[] parseEastingNorthing(final String text) throws ParseException {
        Guardian.assertNotNullOrEmpty("text", text);
        if (text.length() == 0) {
            return null;
        }
        return PARSER.parse(text);
    }

    private static class EastingNorthingParser {

        private static final int ILLEGAL_DIRECTION_VALUE = -999;

        private final int directionWest = 0;
        private final int directionEast = 1;
        private final int directionNorth = 2;
        private final int directionSouth = 3;

        private final int indexEasting = 0;
        private final int indexNorthing = 1;

        private String text;
        private int pos;
        private static final char EOF = (char) -1;

        private int[] parse(final String text) throws ParseException {
            initParser(text);
            return parseImpl();
        }

        private void initParser(final String text) {
            this.text = text;
            this.pos = -1;
        }

        private int[] parseImpl() throws ParseException {
            final int[] eastingNorthing = new int[]{ILLEGAL_DIRECTION_VALUE, ILLEGAL_DIRECTION_VALUE};
            parseDirectionValueAndAssign(eastingNorthing); // one per direction
            parseDirectionValueAndAssign(eastingNorthing); // one per direction
            validateThatValuesAreAssigned(eastingNorthing);
            validateCorrectSuffix();

            return eastingNorthing;
        }

        private void validateThatValuesAreAssigned(final int[] eastingNorthing) throws ParseException {
            if (eastingNorthing[indexEasting] == ILLEGAL_DIRECTION_VALUE) {
                throw new ParseException("Easting value not available.", -1);
            }
            if (eastingNorthing[indexNorthing] == ILLEGAL_DIRECTION_VALUE) {
                throw new ParseException("Northing value not available.", -1);
            }
        }

        private void validateCorrectSuffix() throws ParseException {
            final String suffix = text.substring(++pos);
            if (!suffix.matches("\\..+")) {
                throw new ParseException("Illegal string format.", pos);
            }
        }

        private void parseDirectionValueAndAssign(final int[] eastingNorthing) throws ParseException {
            final int direction = getDirection();
            int value = readNumber();
            value = correctValueByDirection(value, direction);
            assignValueByDirection(eastingNorthing, value, direction);
        }

        private void assignValueByDirection(final int[] eastingNorthing, final int value, final int direction) {
            if (isWest(direction) || isEast(direction)) {
                eastingNorthing[indexEasting] = value;
            } else {
                eastingNorthing[indexNorthing] = value;
            }
        }

        private int correctValueByDirection(int value, final int direction) throws ParseException {
            value *= (isWest(direction) || isSouth(direction)) ? -1 : +1;
            if (isWest(direction) && (value > 0 || value < -180)) {
                throw new ParseException(
                        "The value '" + value + "' for west direction is out of the range -180 ... 0.", pos);
            }
            if (isEast(direction) && (value < 0 || value > 180)) {
                throw new ParseException("The value '" + value + "' for east direction is out of the range 0 ... 180.",
                                         pos);
            }
            if (isSouth(direction) && (value > 0 || value < -90)) {
                throw new ParseException(
                        "The value '" + value + "' for south direction is out of the range -90 ... 0.", pos);
            }
            if (isNorth(direction) && (value < 0 || value > 90)) {
                throw new ParseException("The value '" + value + "' for north direction is out of the range 0 ... 90.",
                                         pos);
            }
            return value;
        }

        private boolean isNorth(final int direction) {
            return compareDirection(directionNorth, direction);
        }

        private boolean isEast(final int direction) {
            return compareDirection(directionEast, direction);
        }

        private boolean isSouth(final int direction) {
            return compareDirection(directionSouth, direction);
        }

        private boolean isWest(final int direction) {
            return compareDirection(directionWest, direction);
        }

        private boolean compareDirection(final int expected, final int direction) {
            return expected == direction;
        }

        private int getDirection() throws ParseException {
            final char c = nextChar();
            if (c == 'w' || c == 'W') {
                return directionWest;
            }
            if (c == 'e' || c == 'E') {
                return directionEast;
            }
            if (c == 'n' || c == 'N') {
                return directionNorth;
            }
            if (c == 's' || c == 'S') {
                return directionSouth;
            }
            throw new ParseException("Illegal direction character.", pos);
        }

        private int readNumber() throws ParseException {
            char c = nextChar();
            if (!Character.isDigit(c)) {
                throw new ParseException("Digit character expected.", pos);
            }
            int value = 0;
            while (Character.isDigit(c)) {
                value *= 10;
                value += (c - '0');
                c = nextChar();
            }
            pos--;
            return value;
        }

        private char nextChar() {
            pos++;
            return pos < text.length() ? text.charAt(pos) : EOF;
        }

        private EastingNorthingParser() {
        }

    }
}
