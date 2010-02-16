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
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
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
            if (qualifier.length() > 0) {
                sb.append('-');
                sb.append(qualifier);
            }
            text = sb.toString();
        }
        return text;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * <p/>
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of <i>expression</i>
     * is negative, zero or positive.
     * <p/>
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)<p>
     * <p/>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.<p>
     * <p/>
     * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.<p>
     * <p/>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(Object o) {
        Version other = (Version) o;
        return compare(this, other);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The <code>equals</code> method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     * <code>x</code>, <code>x.equals(x)</code> should return
     * <code>true</code>.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     * <code>x</code> and <code>y</code>, <code>x.equals(y)</code>
     * should return <code>true</code> if and only if
     * <code>y.equals(x)</code> returns <code>true</code>.
     * <li>It is <i>transitive</i>: for any non-null reference values
     * <code>x</code>, <code>y</code>, and <code>z</code>, if
     * <code>x.equals(y)</code> returns <code>true</code> and
     * <code>y.equals(z)</code> returns <code>true</code>, then
     * <code>x.equals(z)</code> should return <code>true</code>.
     * <li>It is <i>consistent</i>: for any non-null reference values
     * <code>x</code> and <code>y</code>, multiple invocations of
     * <tt>x.equals(y)</tt> consistently return <code>true</code>
     * or consistently return <code>false</code>, provided no
     * information used in <code>equals</code> comparisons on the
     * objects is modified.
     * <li>For any non-null reference value <code>x</code>,
     * <code>x.equals(null)</code> should return <code>false</code>.
     * </ul>
     * <p/>
     * The <tt>equals</tt> method for class <code>Object</code> implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values <code>x</code> and
     * <code>y</code>, this method returns <code>true</code> if and only
     * if <code>x</code> and <code>y</code> refer to the same object
     * (<code>x == y</code> has the value <code>true</code>).
     * <p/>
     * Note that it is generally necessary to override the <tt>hashCode</tt>
     * method whenever this method is overridden, so as to maintain the
     * general contract for the <tt>hashCode</tt> method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     * @see #hashCode()
     * @see java.util.Hashtable
     */
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

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p/>
     * The general contract of <code>hashCode</code> is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     * an execution of a Java application, the <tt>hashCode</tt> method
     * must consistently return the same integer, provided no information
     * used in <tt>equals</tt> comparisons on the object is modified.
     * This integer need not remain consistent from one execution of an
     * application to another execution of the same application.
     * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     * method, then calling the <code>hashCode</code> method on each of
     * the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     * according to the {@link Object#equals(Object)}
     * method, then calling the <tt>hashCode</tt> method on each of the
     * two objects must produce distinct integer results.  However, the
     * programmer should be aware that producing distinct integer results
     * for unequal objects may improve the performance of hashtables.
     * </ul>
     * <p/>
     * As much as is reasonably practical, the hashCode method defined by
     * class <tt>Object</tt> does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
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
