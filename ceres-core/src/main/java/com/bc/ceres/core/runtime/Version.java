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

package com.bc.ceres.core.runtime;

import com.bc.ceres.core.Assert;

/**
 * Represents a version identifier.
 *
 * @author Norman Fomferra
 */
public class Version implements Comparable {

    private String text;
    private int[] numbers;
    private String qualifier;

    public Version(int major, int minor, int micro, String qualifier) {
        Assert.argument(major >= 0, "major");
        Assert.argument(minor >= 0, "minor");
        Assert.argument(micro >= 0, "micro");
        Assert.notNull(qualifier, "qualifier");
        this.text = null;
        this.numbers = new int[]{major, minor, micro};
        this.qualifier = qualifier;
    }

    /**
     * Parses a version text in the form <code>{&lt;number&gt;&lt;sep&gt;}[&lt;qualifier&gt;]</code>, where &lt;sep&gt; is
     * one of '.', '-' or '_'.
     * If no number was found <code>1.[&lt;qualifier&gt;]</code> is assumed, e.g. "M1", is the same as "1.0.0.M1".
     *
     * @param text the text to parse
     *
     * @return the version identifier
     */
    public static Version parseVersion(String text) {
        return new Version(text.trim());
    }

    public int getNumberCount() {
        return numbers.length;
    }

    public int getNumber(int i) {
        return i < numbers.length ? numbers[i] : 0;
    }

    public int getMajor() {
        return getNumber(0);
    }

    public int getMinor() {
        return getNumber(1);
    }

    public int getMicro() {
        return getNumber(2);
    }

    public String getQualifier() {
        return qualifier;
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private Version(String text) {
        int[] numbers = new int[10];
        numbers[0] = 1;
        String qualifier = "";
        int numberCount = 0;
        int startPos = 0;
        final char EOS = '\0';
        for (int pos = 0; pos <= text.length(); pos++) {
            char c = pos < text.length() ? text.charAt(pos) : EOS;
            if (isPartSeparator(c) || c == EOS) {
                if (startPos < pos) {
                    if (numberCount < numbers.length) {
                        numbers[numberCount] = parseInt(text, startPos, pos);
                        numberCount++;
                        startPos = pos + 1;
                    } else {
                        qualifier = text.substring(startPos);
                        break;
                    }
                } else {
                    qualifier = text.substring(startPos);
                    break;
                }
            } else if (!Character.isDigit(c)) {
                qualifier = text.substring(startPos);
                break;
            }
        }

        numberCount = numberCount > 0 ? numberCount : 1;

        this.text = text;
        this.qualifier = qualifier;
        this.numbers = new int[numberCount];
        System.arraycopy(numbers, 0, this.numbers, 0, numberCount);
    }

    private static boolean isPartSeparator(char c) {
        return c == '.' || c == '-' || c == '_';
    }

    private static int parseInt(String s, int i1, int i2) {
        int n = 0;
        int m = 1;
        for (int i = i1; i < i2; i++) {
            n *= m;
            n += (int) s.charAt(i) - (int) '0';
            m = 10;
        }
        return n;
    }

    public static int compare(Version v1, Version v2) {
        int d = compareVersionNumbers(v1.numbers, v2.numbers);
        if (d != 0) {
            return d;
        }
        return compareQualifiers(v1.qualifier, v2.qualifier);
    }

    private static int compareVersionNumbers(int[] v1, int[] v2) {
        int n = Math.max(v1.length, v2.length);
        for (int i = 0; i < n; i++) {
            int n1 = 0, n2 = 0;
            if (i >= v1.length) {
                n2 = v2[i];
            } else if (i >= v2.length) {
                n1 = v1[i];
            } else {
                n1 = v1[i];
                n2 = v2[i];
            }
            int d = n1 - n2;
            if (d != 0) {
                return d;
            }
        }
        return 0;
    }

    private static int compareQualifiers(String q1, String q2) {
        int n = Math.max(q1.length(), q2.length());
        for (int i = 0; i < n; i++) {
            char c1, c2;
            if (i >= q1.length()) {
                c2 = q2.charAt(i);
                c1 = deriveMissingQualifierCharacter(c2);
            } else if (i >= q2.length()) {
                c1 = q1.charAt(i);
                c2 = deriveMissingQualifierCharacter(c1);
            } else {
                c1 = q1.charAt(i);
                c2 = q2.charAt(i);
            }
            int d = (int) c1 - (int) c2;
            if (d != 0) {
                return d;
            }
        }
        return 0;
    }

    private static char deriveMissingQualifierCharacter(char c) {
        if (Character.isDigit(c)) {
            return '0'; // Compare missing digit with '0'
        } else if (Character.isLowerCase(c)) {
            return 'z'; // Compare missing lower letter with 'z'
        } else if (Character.isUpperCase(c)) {
            return 'Z'; // Compare missing upper letter with 'Z'
        } else {
            return c;   // Other charaters are not compared
        }
    }

    /**
     * Returns the string representation of the version in the
     * form <code>{major}.{minor}.{micro}-{qualifier}</code>.
     *
     * @return a string representation of the version.
     */
    @Override
    public String toString() {
        if (text == null) {
            StringBuilder sb = new StringBuilder(16);
            for (int versionNumber : numbers) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(versionNumber);
            }
            if (!qualifier.isEmpty()) {
                sb.append('-');
                sb.append(qualifier);
            }
            text = sb.toString();
        }
        return text;
    }

    @Override
    public int compareTo(Object o) {
        Version other = (Version) o;
        return compare(this, other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Version) {
            Version other = (Version) obj;
            return compare(this, other) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int n = 0;
        for (int versionNumber : numbers) {
            n += versionNumber;
            n *= 17;
        }
        return n + qualifier.hashCode();
    }
}
