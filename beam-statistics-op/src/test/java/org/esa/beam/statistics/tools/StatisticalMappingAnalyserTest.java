package org.esa.beam.statistics.tools;

import org.esa.beam.statistics.StatisticsOp;
import org.junit.*;

import static org.junit.Assert.*;

import java.util.TreeSet;

public class StatisticalMappingAnalyserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_OneStatisticalMeasurement_TwoGeophysicalParameter() {
        final TreeSet<String> fullNames = new TreeSet<String>();
        fullNames.add(StatisticsOp.MEDIAN + "_CHL");
        fullNames.add(StatisticsOp.MEDIAN + "_YS");
        final StatisticalMappingAnalyser mappingAnalyser = new StatisticalMappingAnalyser(fullNames);

        assertEquals(1, mappingAnalyser.getStatisticalMeasureNames().length);
        assertEquals(StatisticsOp.MEDIAN, mappingAnalyser.getStatisticalMeasureNames()[0]);

        assertEquals(2, mappingAnalyser.getGeophysicalParameterNames().length);
        assertEquals("CHL", mappingAnalyser.getGeophysicalParameterNames()[0]);
        assertEquals("YS", mappingAnalyser.getGeophysicalParameterNames()[1]);
    }

    @Test
    public void test_TwoStatisticalMeasurement_OneGeophysicalParameter() {
        final TreeSet<String> fullNames = new TreeSet<String>();
        fullNames.add(StatisticsOp.MEDIAN +"_CHL");
        fullNames.add(StatisticsOp.MINIMUM + "_CHL");
        final StatisticalMappingAnalyser mappingAnalyser = new StatisticalMappingAnalyser(fullNames);

        assertEquals(2, mappingAnalyser.getStatisticalMeasureNames().length);
        assertEquals(StatisticsOp.MEDIAN, mappingAnalyser.getStatisticalMeasureNames()[0]);
        assertEquals(StatisticsOp.MINIMUM, mappingAnalyser.getStatisticalMeasureNames()[1]);

        assertEquals(1, mappingAnalyser.getGeophysicalParameterNames().length);
        assertEquals("CHL", mappingAnalyser.getGeophysicalParameterNames()[0]);
    }

    @Test
    public void testWithManyOfBothSides() {
        final TreeSet<String> fullNames = new TreeSet<String>();
        fullNames.add(StatisticsOp.MEDIAN +"_CHL");
        fullNames.add(StatisticsOp.MEDIAN +"_YS");
        fullNames.add(StatisticsOp.MEDIAN +"_TSM");
        fullNames.add(StatisticsOp.MINIMUM +"_CHL");
        fullNames.add(StatisticsOp.MINIMUM +"_YS");
        fullNames.add(StatisticsOp.MINIMUM +"_TSM");
        fullNames.add(StatisticsOp.MAX_ERROR +"_CHL");
        fullNames.add(StatisticsOp.MAX_ERROR +"_YS");
        fullNames.add(StatisticsOp.MAX_ERROR +"_TSM");
        fullNames.add(StatisticsOp.MAXIMUM +"_CHL");
        fullNames.add(StatisticsOp.MAXIMUM +"_YS");
        fullNames.add(StatisticsOp.MAXIMUM +"_TSM");
        fullNames.add(StatisticsOp.AVERAGE +"_CHL");
        fullNames.add(StatisticsOp.AVERAGE +"_YS");
        fullNames.add(StatisticsOp.AVERAGE +"_TSM");
        fullNames.add(StatisticsOp.SIGMA +"_CHL");
        fullNames.add(StatisticsOp.SIGMA +"_YS");
        fullNames.add(StatisticsOp.SIGMA +"_TSM");
        fullNames.add(StatisticsOp.PERCENTILE_PREFIX +90+StatisticsOp.PERCENTILE_SUFFIX+"_CHL");
        fullNames.add(StatisticsOp.PERCENTILE_PREFIX +90+StatisticsOp.PERCENTILE_SUFFIX+"_YS");
        fullNames.add(StatisticsOp.PERCENTILE_PREFIX +90+StatisticsOp.PERCENTILE_SUFFIX+"_TSM");
        final StatisticalMappingAnalyser mappingAnalyser = new StatisticalMappingAnalyser(fullNames);

        assertEquals(7, mappingAnalyser.getStatisticalMeasureNames().length);
        assertEquals(StatisticsOp.AVERAGE, mappingAnalyser.getStatisticalMeasureNames()[0]);
        assertEquals(StatisticsOp.MAX_ERROR, mappingAnalyser.getStatisticalMeasureNames()[1]);
        assertEquals(StatisticsOp.MAXIMUM, mappingAnalyser.getStatisticalMeasureNames()[2]);
        assertEquals(StatisticsOp.MEDIAN, mappingAnalyser.getStatisticalMeasureNames()[3]);
        assertEquals(StatisticsOp.MINIMUM, mappingAnalyser.getStatisticalMeasureNames()[4]);
        assertEquals("p90_threshold", mappingAnalyser.getStatisticalMeasureNames()[5]);
        assertEquals(StatisticsOp.SIGMA, mappingAnalyser.getStatisticalMeasureNames()[6]);

        assertEquals(3, mappingAnalyser.getGeophysicalParameterNames().length);
        assertEquals("CHL", mappingAnalyser.getGeophysicalParameterNames()[0]);
        assertEquals("TSM", mappingAnalyser.getGeophysicalParameterNames()[1]);
        assertEquals("YS", mappingAnalyser.getGeophysicalParameterNames()[2]);
    }

    @Test
    public void testStingSortingAlongLenth() {
        //preparation
        final String[] strings = {"1", "22", "333", "4444", "55555"};
        //execution
        StatisticalMappingAnalyser.sortAlongLength_BiggestFirst(strings);

        //verification
        assertArrayEquals(new String[]{"55555", "4444", "333", "22", "1"}, strings);
    }
}
