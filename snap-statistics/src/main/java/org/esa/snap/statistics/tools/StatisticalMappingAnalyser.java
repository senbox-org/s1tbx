package org.esa.snap.statistics.tools;

import java.util.ArrayList;
import java.util.List;
import org.esa.snap.statistics.StatisticsOp;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class StatisticalMappingAnalyser {

    private final TreeSet<String> statisticalMeasure;
    private final TreeSet<String> geophysicalParameter;

    public StatisticalMappingAnalyser(Set<String> fullNames) {
        final int[] defaultPercentiles = StatisticsOp.DEFAULT_PERCENTILES_INTS;
        final String[] measureNames = getMeasureNames(defaultPercentiles);
        sortAlongLength_BiggestFirst(measureNames);
        geophysicalParameter = new TreeSet<String>();
        statisticalMeasure = new TreeSet<String>();
        for (String fullName : fullNames) {
            for (String measureName : measureNames) {
                if (fullName.startsWith(measureName)) {
                    statisticalMeasure.add(measureName);
                    final String paramName = fullName.substring(measureName.length());
                    geophysicalParameter.add(trimWithUnderscores(paramName));
                    break;
                }
            }
        }
    }

    private static String[] getMeasureNames(int[] percentiles) {
        final List<String> algorithms = new ArrayList<>();
        algorithms.add(StatisticsOp.MINIMUM);
        algorithms.add(StatisticsOp.MAXIMUM);
        algorithms.add(StatisticsOp.MEDIAN);
        algorithms.add(StatisticsOp.AVERAGE);
        algorithms.add(StatisticsOp.SIGMA);
        for (int percentile : percentiles) {
            algorithms.add(StatisticsOp.PERCENTILE_PREFIX + percentile + StatisticsOp.PERCENTILE_SUFFIX);
        }
        algorithms.add(StatisticsOp.MAX_ERROR);
        algorithms.add(StatisticsOp.TOTAL);
        return algorithms.toArray(new String[0]);
    }

    public String[] getStatisticalMeasureNames() {
        return toStrings(statisticalMeasure);
    }

    public String[] getGeophysicalParameterNames() {
        return toStrings(geophysicalParameter);
    }

    private String trimWithUnderscores(String s) {
        while (s.startsWith("_") && s.length() > 1) {
            s = s.substring(1);
            s = s.trim();
        }
        while (s.endsWith("_") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
            s = s.trim();
        }
        return s;
    }

    private String[] toStrings(final Set<String> set) {
        return set.toArray(new String[set.size()]);
    }

    public static void sortAlongLength_BiggestFirst(String[] measureNames) {
        Arrays.sort(measureNames, (o1, o2) -> o2.length() - o1.length());
    }
}
