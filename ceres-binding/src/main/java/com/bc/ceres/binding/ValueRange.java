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

package com.bc.ceres.binding;

/**
 * This class represents a numerical value range.
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueRange {
    private final double min;
    private final double max;
    private final boolean minIncluded;
    private final boolean maxIncluded;

    public ValueRange(double min, double max) {
        this(min, max, true, true);
    }

    public ValueRange(double min, double max, boolean minIncluded, boolean maxIncluded) {
        this.min = min;
        this.max = max;
        this.minIncluded = minIncluded;
        this.maxIncluded = maxIncluded;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean isMinIncluded() {
        return minIncluded;
    }

    public boolean isMaxIncluded() {
        return maxIncluded;
    }

    public boolean hasMin() {
        return min > Double.NEGATIVE_INFINITY;
    }

    public boolean hasMax() {
        return max < Double.POSITIVE_INFINITY;
    }

    public boolean contains(double v) {
        boolean b1 = minIncluded ? (v >= min) : (v > min);
        boolean b2 = maxIncluded ? (v <= max) : (v < max);
        return b1 && b2;
    }

    /**
     * Parses an {@link ValueRange}.
     * <p>The syntax of a version range is:
     * <pre>
     *   interval ::= ( '[' | '(' ) min ',' max ( ']' | ')' )
     *   min ::= number | '*'
     *   max ::= number | '*'
     * </pre>
     * @param text The textual representation of the value range.
     * @return The value range.
     * @throws IllegalArgumentException If the text has an invalid format.
     */
    public static ValueRange parseValueRange(final String text) throws IllegalArgumentException {
        if (text.length() > 0) {
            final char c1 = text.charAt(0);
            if (c1 == '(' || c1 == '[') {
                final char c2 = text.charAt(text.length() - 1);
                if (c2 == ')' || c2 == ']') {
                    int d = text.indexOf(',', 1);
                    if (d > 1 && d < text.length() - 2) {
                        String s1 = text.substring(1, d).trim();
                        String s2 = text.substring(d + 1, text.length() - 1).trim();
                        try {
                            return new ValueRange(
                                    s1.equals("*") ? Double.NEGATIVE_INFINITY : Double.valueOf(ValueRange.trimNumberString(s1)),
                                    s2.equals("*") ? Double.POSITIVE_INFINITY : Double.valueOf(ValueRange.trimNumberString(s2)),
                                    c1 == '[', c2 == ']');
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid number format in interval.");
                        }
                    } else {
                        throw new IllegalArgumentException("Missing ',' in interval.");
                    }
                } else {
                    throw new IllegalArgumentException("Missing trailing ')' or ']' in interval.");
                }
            } else {
                throw new IllegalArgumentException("Missing leading '(' or '[' in interval.");
            }
        } else {
            throw new IllegalArgumentException("Empty string.");
        }
    }

    private static String trimNumberString(String s) {
        s = s.trim();
        if (s.startsWith("+")) {
            s = s.substring(1);
        }
        return s;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(minIncluded ? '[' : '(');
        sb.append(hasMin() ? toString(min) : "*");
        sb.append(',');
        sb.append(hasMax() ? toString(max) : "*");
        sb.append(maxIncluded ? ']' : ')');
        return sb.toString();
    }

    private static String toString(double d) {
        final long l = Math.round(d);
        return d == l ? Long.toString(l) : Double.toString(d);
    }
}
