package com.bc.ceres.binding;

// todo - add a step size
// todo - rename to ValueRange

/**
 * @author Norman Fomferra
 * @since 0.6
 */
public class Interval {
    private double min;
    private double max;
    private boolean minIncluded;
    private boolean maxIncluded;

    public Interval(double min, double max, boolean minIncluded, boolean maxIncluded) {
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

    public boolean contains(double v) {
        boolean b1 = minIncluded ? (v >= min) : (v > min);
        boolean b2 = maxIncluded ? (v <= max) : (v < max);
        return b1 && b2;
    }

    /**
     * Parses an {@link com.bc.ceres.binding.Interval}.
     * <p>The syntax of a version range is:
     * <pre>
     *   interval ::= ( '[' | '(' ) min ',' max ( ']' | ')' )
     *   min ::= number | '*'
     *   max ::= number | '*'
     * </pre>
     */
    public static Interval parseInterval(final String intervalString) throws ConversionException {
        if (intervalString.length() > 0) {
            final char c1 = intervalString.charAt(0);
            if (c1 == '(' || c1 == '[') {
                final char c2 = intervalString.charAt(intervalString.length() - 1);
                if (c2 == ')' || c2 == ']') {
                    int d = intervalString.indexOf(',', 1);
                    if (d > 1 && d < intervalString.length() - 2) {
                        String s1 = intervalString.substring(1, d).trim();
                        String s2 = intervalString.substring(d + 1, intervalString.length() - 1).trim();
                        try {
                            return new com.bc.ceres.binding.Interval(
                                    s1.equals("*") ? Double.NEGATIVE_INFINITY : new Double(com.bc.ceres.binding.Interval.trimNumberString(s1)),
                                    s2.equals("*") ? Double.POSITIVE_INFINITY : new Double(com.bc.ceres.binding.Interval.trimNumberString(s2)),
                                    c1 == '[', c2 == ']');
                        } catch (NumberFormatException e) {
                            throw new ConversionException("Invalid number format in interval.");
                        }
                    } else {
                        throw new ConversionException("Missing ',' in interval.");
                    }
                } else {
                    throw new ConversionException("Missing trailing ')' or ']' in interval.");
                }
            } else {
                throw new ConversionException("Missing leading '(' or '[' in interval.");
            }
        } else {
            throw new ConversionException("Empty string.");
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
        sb.append(min == Double.NEGATIVE_INFINITY ? "*" : Double.toString(min));
        sb.append(',');
        sb.append(max == Double.POSITIVE_INFINITY ? "*" : Double.toString(max));
        sb.append(maxIncluded ? ']' : ')');
        return sb.toString();
    }
}
